// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc.grammar;

import jacc.util.BitSet;
import jacc.util.IntSet;
import jacc.util.SCC;
import jacc.util.Interator;

/** A machine that provides LALR lookahead sets for each reduction.
 */
public class LALRMachine extends LookaheadMachine {
    // For convenience, we cache the following fields from grammar:
    protected Nullable nullable;
    protected First    first;

    /** Construct a machine for a given grammar.
     */
    public LALRMachine(Grammar grammar) {
        super(grammar);
        this.nullable = grammar.getNullable();
        this.first    = grammar.getFirst();
        predState     = SCC.invert(succState, numStates);
        calcGotoLA();
        calcLookahead();
    }

    /** Records the states that we could have come from in a single step to
     *  reach each state.
     */
    private int[][] predState;

    /** The total number of gotos in all states.
     */
    private int numGotos;

    /** An array mapping each state to the number of the first goto
     *  in that state.
     */
    private int[] stateFirstGoto;

    /** An array mapping each goto number to the number of the state
     *  in which it starts.
     */
    private int[] gotoSource;

    /** An array mapping each goto number to the corresponding goto.
     */
    private int[] gotoTrans;

    /** Records the lookahead sets for each goto.
     */
    private int[][] gotoLA;

    /** Records the target set for each goto.
     */
    private int[][] gotoTargets;

    /** Records the lookahead sets for reduce items.  Lookahead sets are
     *  stored in the order specified by Machine.getReducesAt().
     */
    private int[][][] laReds;

    /** Return lookahead sets for the reductions at a given state.
     */
    public int[] getLookaheadAt(int st, int i) {
        return laReds[st][i];
    }

    /** Calculate the lookaheads on each goto.
     */
    private void calcGotoLA() {
        // Start by calculating the number of gotos, and storing them
        // in a table.
        stateFirstGoto = new int[numStates];
        numGotos       = 0;
        for (int st=0; st<numStates; st++) {
            stateFirstGoto[st] = numGotos;
            numGotos          += getGotosAt(st).length;
        }
        gotoSource  = new int[numGotos];
        gotoTrans   = new int[numGotos];
        int count   = 0;
        for (int st=0; st<numStates; st++) {
            int[] gotos = getGotosAt(st);
            for (int i=0; i<gotos.length; i++) {
                gotoSource[count] = st;
                gotoTrans[count]  = gotos[i];
                count++;
            }
        }

        // Now we calculate the targets and the immediate first
        // sets for each goto.
        gotoLA      = new int[numGotos][];
        gotoTargets = new int[numGotos][];
        for (int g=0; g<numGotos; g++) {
            calcTargets(g);
        }

        // Now we've identified the dependencies between gotos, sort
        // them into components and do a fix point iteration to get
        // the final lookaheads at each one.

        int[][] comps = SCC.get(gotoTargets);
        for (int c=0; c<comps.length; c++) {
            int[] comp = comps[c];
            boolean changed = true;
            while (changed) {
                changed = false;
                for (int i=0; i<comp.length; i++) {
                    int[] ts = gotoTargets[comp[i]];
                    for (int j=0; j<ts.length; j++) {
                        if (BitSet.addTo(gotoLA[comp[i]], gotoLA[ts[j]])) {
                            changed = true;
                        }
                    }
                }
            }
        }
    }

    /** Calculate lookahead targets.  A lookahead target for a goto g
     *  from st to st1 on nt is a goto that we might execute after g
     *  without any intervening shifts or gotos.  If (lhs -> v nt _ w) is
     *  in state st1, w is nullable, and we can reach state k from st by
     *  going back along v, then the goto from k on Y will be a target
     *  for g.
     *
     *  @param g   is the number of the goto in the global tables
     *             gotoLA and gotoTargets.  We write back the results
     *             of this call into the slots of these arrays with
     *             index gn.
     */
    private void calcTargets(int g) {
        int    st  = gotoSource[g];
        int    st1 = gotoTrans[g];
        int    nt  = getEntry(st1);
        IntSet its = getItemsAt(st1);
        int    sz  = its.size();
        int[]  fs  = BitSet.make(numTs);
        IntSet ts  = IntSet.empty();
        for (int j=0; j<sz; j++) {
            LR0Items.Item it  = items.getItem(its.at(j));
            int           lhs = it.getLhs();
            int           pos = it.getPos();
            if (lhs>=0) {
                int[] rhs = it.getProd().getRhs();
                if (pos>0 && rhs[--pos]==nt) {
                    if (calcFirsts(fs, it).canReduce()) {
                        findTargets(ts, st, lhs, rhs, pos);
                    }
                }
            } else if (pos>0) {
                BitSet.set(fs, numTs-1);
            }
        }
        gotoLA[g]      = fs;
        gotoTargets[g] = ts.toArray();
    }

    /** Calculate the tokens that we can reach from a given item without
     *  leaving the production concerned.  If the item is A -> v _ w,
     *  then we will add all the elements of FIRST(w) to the accumulating
     *  fs parameter, and return the item A -> v w1 _ w2, where w = w1 w2,
     *  w1 is nullable, and either w2 is empty or w2 is not nullable.
     *
     *  @param fs  An accumulating parameter that holds a bitset of the
     *             tokens that might occur at the beginning of the string
     *             to the right of the _ mark in the specified item.
     *  @param it  An item of the grammar.
     */
    private LR0Items.Item calcFirsts(int[] fs, LR0Items.Item it) {
        while (it.canGoto()) {
            int sym = it.getNextSym();
            if (grammar.isTerminal(sym)) {
                BitSet.addTo(fs,sym-numNTs);
                break;
            } else {
                BitSet.union(fs, first.at(sym));
                if (!nullable.at(sym)) {
                    break;
                }
                it = items.getItem(it.getNextItem());
            }
        }
        if (it.canAccept()) {
            BitSet.set(fs,numTs-1);
        }
        return it;
    }

    /** Find target gotos by tracing back from a state st along a
     *  prefix of symbols from the rhs of a production.  An initial
     *  accumulating parameter is used to collect any targets that
     *  are found.
     *
     * @param ts   an accumulating parameter for the set of targets.
     * @param st   the state in which we are currently looking.
     * @param lhs  the nonterminal on the lhs of the production.
     * @param rhs  the right hand side of the production.
     * @param pos  current position with the production; 0 means
     *             that we have found our way back to a state
     *             that (potentially) contains a relevant goto.
     */
    private void findTargets(IntSet ts, int st, int lhs, int[] rhs, int pos) {
        if (pos==0) {
            int[] gotos = getGotosAt(st);
            for (int i=0; i<gotos.length; i++) {
                if (getEntry(gotos[i])==lhs) {
                    ts.add(stateFirstGoto[st]+i);
                    break;
                }
            }
        } else {
            if (entry[st]==rhs[--pos]) {
                for (int i=0; i<predState[st].length; i++) {
                    findTargets(ts, predState[st][i], lhs, rhs, pos);
                }
            }
        }
    }

    /** Calculate lookahead sets.  Fills out the entries of laReds for
     *  each reduce item in each state by unioning together the lookaheads
     *  for each goto that is (potentially) reachable from this reduction.
     */
    private void calcLookahead() {
        // Fill out the entries of laRed to record lookaheads for
        // reduce items in individual states.
        laReds = new int[numStates][][];
        for (int st=0; st<numStates; st++) {
            int[]  rs  = getReducesAt(st);
            IntSet its = getItemsAt(st);
            laReds[st] = new int[rs.length][];
            for (int j=0; j<rs.length; j++) {
                LR0Items.Item it = items.getItem(its.at(rs[j]));
                int   lhs        = it.getLhs();
                int[] rhs        = it.getProd().getRhs();
                int[] lookahead  = BitSet.make(numTs);
                lookBack(lookahead, st, lhs, rhs, rhs.length);
                laReds[st][j]    = lookahead;
            }
        }
    }

    /** Calculate the lookahead for a given reduce item by taking
     *  the union of all lookaheads on the gotos that we might pass
     *  through to complete this reduction.
     *
     * @param la   an accumulating parameter for the lookahead set.
     * @param st   the state in which we are currently looking.
     * @param lhs  the nonterminal on the lhs of the production.
     * @param rhs  the right hand side of the production.
     * @param pos  current position with the production; 0 means
     *             that we have found our way back to a state
     *             that (potentially) contains a relevant goto.
     */
    private void lookBack(int[] la, int st, int lhs, int[] rhs, int pos) {
        if (pos==0) {
            int[] gotos = getGotosAt(st);
            for (int i=0; i<gotos.length; i++) {
                if (getEntry(gotos[i])==lhs) {
                    BitSet.union(la, gotoLA[stateFirstGoto[st]+i]);
                    return;
                }
            }
        } else {
            if (entry[st]==rhs[--pos]) {
                for (int i=0; i<predState[st].length; i++) {
                    lookBack(la, predState[st][i], lhs, rhs, pos);
                }
            }
        }
    }

    /** Output the results of lookahead calculations for
     *  debugging and inspection.
     */
    public void display(java.io.PrintWriter out) {
        super.display(out);

        // Display lookahead information for each goto.
        for (int g=0; g<numGotos; g++) {
            out.println("Goto #"+g
                        + ", in state "
                        + gotoSource[g]
                        + " on symbol "
                        + grammar.getSymbol(getEntry(gotoTrans[g]))
                        + " to state "
                        + gotoTrans[g]);
            out.print("  Lookahead: {");
            out.print(grammar.displaySymbolSet(gotoLA[g], numNTs));
            out.println("}");
            out.print("  Targets  : {");
            for (int j=0; j<gotoTargets[g].length; j++) {
                if (j>0) {
                    out.print(", ");
                }
                out.print(gotoTargets[g][j]);
            }
            out.println("}");
        }

        // Display lookahead information for each reduce item.
        for (int st=0; st<numStates; st++) {
            int[]  rs  = getReducesAt(st);
            if (rs.length>0) {
                out.println("State " + st + ": ");
                IntSet its = getItemsAt(st);
                for (int j=0; j<rs.length; j++) {
                    LR0Items.Item it = items.getItem(its.at(rs[j]));
                    out.print("  Item     : ");
                    it.display(out);
                    out.println();
                    out.print("  Lookahead: {");
                    out.print(grammar.displaySymbolSet(laReds[st][j],
                                                       numNTs));
                    out.println("}");
                }
            }
        }
    }
}
