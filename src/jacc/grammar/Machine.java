// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc.grammar;

import jacc.util.BitSet;
import jacc.util.IntSet;
import jacc.util.SCC;
import jacc.util.Interator;

/** A representation for basic shift/reduce machines built using LR(0)
 *  items.
 */
public class Machine {
    /** The underlying grammar for this machine.
     */
    protected Grammar grammar;

    /** Construct a machine for a given grammar.
     */
    public Machine(Grammar grammar) {
        this.grammar  = grammar;
        this.numSyms  = grammar.getNumSyms();
        this.numNTs   = grammar.getNumNTs();
        this.numTs    = grammar.getNumTs();
        this.left     = grammar.getLeft();
        this.items    = new LR0Items(grammar);
        calcLR0states();
        calcGotosShifts();
        calcReduceOffsets();
    }

    // For convenience, we cache the following fields from grammar:
    protected int  numSyms;
    protected int  numNTs;
    protected int  numTs;
    protected Left left;

    /** Holds the collection of LR(0) items for the given grammar.
     */
    protected LR0Items items;

    /** Counts the number of states in the machine.
     */
    protected int numStates;

    /** Holds the sets of kernel items in any given state.  Null reductions
     *  are added in once the machine has been built.
     */
    protected IntSet[] stateSets;

    /** Holds the entry symbol for the state.  Because of the way that
     *  our machines are built, all entries into any given state are by
     *  a transition on a particular, fixed symbol.
     */
    protected int[] entry;

    /** Records the null reduction items that are encountered while exploring
     *  this state.  Null reductions are reductions corresponding to items of
     *  the form A -> _, which are not kernel items, but must be included in
     *  the finished machine.
     */
    private IntSet[] nullReds;

    /** Records the states that can reached by a single step from each state.
     */

    protected int[][] succState;

    /** Records the gotos for nonterminals in each state.
     */
    protected int[][] gotos;

    /** Records the shifts for terminals in each state.
     */
    protected int[][] shifts;

    /** Records the offsets of any reduce items in each stateSets[] entry.
     */
    protected int[][] reduceOffsets;

    /** Return the grammar that was used to construct this machine.
     */
    public Grammar getGrammar() {
        return grammar;
    }

    /** Return the number of states in this machine.
     */
    public int getNumStates() {
        return numStates;
    }

    /** Return the LR0Items that were used to construct this machine.
     */
    public LR0Items getItems() {
        return items;
    }

    /** Return the LR0 item for a given reduction number in
     *  a particular state.
     */
    public LR0Items.Item reduceItem(int st, int redNo) {
        return items.getItem(stateSets[st].at(redNo));
    }

    /** Return the entry symbol for a given state.
     */
    public int getEntry(int st) {
        return (st<0) ? (numSyms-1) : entry[st];
    }

    /** Return the set of items for a given state.
     */
    public IntSet getItemsAt(int st) {
        return stateSets[st];
    }

    /** Return the goto table for a given state.
     */
    public int[] getGotosAt(int st) {
        return gotos[st];
    }

    /** Return the shift table for a given state.
     */
    public int[] getShiftsAt(int st) {
        return shifts[st];
    }

    /** Return the offsets of all reduce items in the set of items
     *  returned by getItemsAt.
     */
    public int[] getReducesAt(int st) {
        return reduceOffsets[st];
    }

    //---------------------------------------------------------------------
    // The main state machine:
    //
    // What is a good representation for the item sets that we will
    // build up during construction of the state machine?  Whichever
    // representation we choose, we will need to be able to compare
    // sets of items for equality, and iterate over the elements of
    // an item set.  If the ratio of entries in an item set over the
    // total number of items is quite high, then a bitset would provide
    // a good representation.  However, experiments with yacc generated
    // grammars for Haskell, Java, and Pascal suggest that by far the
    // majority of the item sets that we encounter have just one element,
    // especially if we focus on calculating kernels, and that very few
    // have more than 8 elements.  In addition, the total number of
    // nonterminals can be quite high.  For these reasons, we have
    // chosen a representation based on the IntSet type, which allows
    // relatively compact representations of small sets, and decent
    // implementations of equality and iteration.d

    /** Used to set the initial number of states in the machine that
     *  we build.  The machine expands to add new states as construction
     *  proceeds.
     */
    private final int DEFAULT_NUM_STATES = 16;

    /** Main loop to calculate an LR(0) machine for the given grammar.
     *  As new states are added, they form a queue of states that are
     *  yet to be processed.  When we reach the end of the queue, we
     *  know that we have examined all reachable states and hence that
     *  our job is done.
     */
    private void calcLR0states() {
        stateSets    = new IntSet[DEFAULT_NUM_STATES];
        succState    = new int[DEFAULT_NUM_STATES][];
        entry        = new int[DEFAULT_NUM_STATES];
        nullReds     = new IntSet[DEFAULT_NUM_STATES];
        stateSets[0] = IntSet.singleton(items.getStartItem());
        numStates    = 1;

        IntSet[] trans    = new IntSet[numSyms];
        int      numTrans = 0;
        int[]    leftnt   = BitSet.make(numNTs);

        for (int head = 0; head<numStates; head++) {
            IntSet kernel = stateSets[head];
            BitSet.clear(leftnt);

            // Calculate transitions for (the closure of) the
            // kernel of this state.  Start with items in the
            // kernel itself.

            Interator its = kernel.interator();
            while (its.hasNext()) {
                LR0Items.Item it = items.getItem(its.next());
                if (it.canGoto()) {
                    int sym = it.getNextSym();
                    int nxt = it.getNextItem();
                    if (grammar.isNonterminal(sym)) {
                        BitSet.addTo(leftnt, left.at(sym));
                    }
                    if (addValue(trans, sym, nxt)) {
                        numTrans++;
                    }
                }
            }

            // Now continue with initial items for the nonterminals
            // recorded in nts.

            if (!BitSet.isEmpty(leftnt)) {
                Interator nts = BitSet.interator(leftnt, 0);
                while (nts.hasNext()) {
                    int nt = nts.next();
                    Grammar.Prod[] prods = grammar.getProds(nt);
                    for (int i=0; i<prods.length; i++) {
                        int[] rhs = prods[i].getRhs();
                        int   nxt = items.getFirstKernel(nt, i);
                        if (rhs.length!=0) {
                            if (addValue(trans, rhs[0], nxt)) {
                                numTrans++;
                            }
                        } else {
                            addValue(nullReds, head, nxt);
                        }
                    }
                }
            }

            // Transfer information into successor transition
            // table, adding new states as necessary.

            int[] toState = new int[numTrans];
            int[] onSym   = new int[numTrans];
            int   count   = 0;
            for (int i=0; count<numTrans; i++) {
                if (trans[i]!=null) {
                    toState[count] = addState(i, trans[i]);
                    onSym[count]   = i;
                    trans[i]       = null;
                    count++;
                }
            }
            numTrans         = 0;
            succState[head]  = toState;
        }
        mergeNullReds();
    }

    /** Add a value to one of the sets in a table of integer sets.
     *  Empty slots in the table are represented by null values
     *  so we have to check for nulls before adding an element.
     *
     *  @return a boolean to indicate if a new set was created
     *  (i.e., if a table row was written to for the first time).
     */
    private boolean addValue(IntSet[] collect, int no, int val) {
        if (collect[no]==null) {
           collect[no] = IntSet.singleton(val);
           return true;
        } else {
            collect[no].add(val);
            return false;
        }
    }

    /** Return the state number corresponding to a given set of kernel
     *  items.  If there is no state with that particular set of items,
     *  we add another new state on to the end of the current list,
     *  which serves as a work queue for the main loop in calcLR0States.
     *
     *  @param  sym    The number of the symbol on which this transition
     *                 occurs.
     *  @param  state  A set of integers representing a set of items.
     *  @return        The number of the corresponding state.
     */
    private int addState(int sym, IntSet state) {
        for (int i=0; i<numStates; i++) {
            if (stateSets[i].equals(state)) {
                return i;
            }
        }
        if (acceptItems.equals(state)) {
            return (-1);
        }
        if (numStates>=stateSets.length) {
            int newLen             = 2*stateSets.length;
            IntSet[] newStateSets  = new IntSet[newLen];
            int[][]  newSuccState  = new int[newLen][];
            IntSet[] newNullReds   = new IntSet[newLen];
            int[]    newEntry      = new int[newLen];
            for (int i=0; i<numStates; i++) {
                newStateSets[i]  = stateSets[i];
                newSuccState[i]  = succState[i];
                newEntry[i]      = entry[i];
                newNullReds[i]   = nullReds[i];
            }
            stateSets  = newStateSets;
            succState  = newSuccState;
            entry      = newEntry;
            nullReds   = newNullReds;
        }
        stateSets[numStates] = state;
        entry[numStates]     = sym;
        return numStates++;
    }

    /** A dummy item set, equal to {-1}, and representing the
     *  accept state, numbered (-1).
     */
    private final IntSet acceptItems = IntSet.singleton(-1);

    /** Add null reductions to the item set for each state.
     *  Null reductions are for items of the form A -> _
     *  where the rhs of the production is empty.  These
     *  items cannot be included in the item sets that we
     *  construct while building the basic LR(0) machine
     *  because they are not kernel items.  So we record
     *  these reductions in nullReds[] during the main
     *  construction loop, and then add them in to the item
     *  sets when the machine states have all been built.
     */
    private void mergeNullReds() {
        for (int i=0; i<numStates; i++) {
            if (nullReds[i]!=null) {
                Interator its = nullReds[i].interator();
                while (its.hasNext()) {
                    stateSets[i].add(its.next());
                }
                nullReds[i] = null;
            }
        }
    }

    /** Calculate goto and shift tables.
     */
    private void calcGotosShifts() {
        gotos  = new int[numStates][];
        shifts = new int[numStates][];
        for (int i=0; i<numStates; i++) {
            int numGotos  = 0;
            int numShifts = 0;
            for (int j=0; j<succState[i].length; j++) {
                int dst = succState[i][j];
                if (grammar.isTerminal(entry[dst])) {
                    numShifts++;
                } else {
                    numGotos++;
                }
            }
            if (stateSets[i].contains(items.getEndItem())) {
                numShifts++;
            }
            gotos[i]  = new int[numGotos];
            shifts[i] = new int[numShifts];
            for (int j=succState[i].length; 0<j--; ) {
                int dst = succState[i][j];
                if (grammar.isTerminal(entry[dst])) {
                    shifts[i][--numShifts] = dst;
                } else {
                    gotos[i][--numGotos] = dst;
                }
            }
            if (numShifts>0) {
                shifts[i][0] = (-1);
            }
        }
    }

    /** Calculate reduce items.
     */
    private void calcReduceOffsets() {
        reduceOffsets = new int[numStates][];
        for (int i=0; i<numStates; i++) {
            int    numReds = 0;
            IntSet set     = stateSets[i];
            int    sz      = set.size();
            for (int j=0; j<sz; j++) {
                if (items.getItem(set.at(j)).canReduce()) {
                    numReds++;
                }
            }
            reduceOffsets[i] = new int[numReds];
            int pos = 0;
            for (int j=0; j<sz; j++) {
                if (items.getItem(set.at(j)).canReduce()) {
                    reduceOffsets[i][pos++] = j;
                }
            }
        }
    }

    /** Output the constructed machine for debugging and inspection.
     */
    public void display(java.io.PrintWriter out) {
        for (int i=0; i<numStates; i++) {
            out.println("state " + i);
            for (Interator its = stateSets[i].interator(); its.hasNext();) {
                out.print("\t");
                items.getItem(its.next()).display(out);
                out.println();
            }
            out.println();
            if (succState[i].length>0) {
                for (int j=0; j<succState[i].length; j++) {
                    int dst = succState[i][j];
                    out.println("\t" + grammar.getSymbol(entry[dst]) +
                                " goto " + succState[i][j]);
                }
                out.println();
            }
        }
    }
}
