// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc.grammar;

import jacc.util.IntSet;
import jacc.util.BitSet;
import jacc.util.Interator;

/** Describes the construction of parse tables for a given machine, using
 *  lookahead information to help avoid shift/reduce conflicts.
 */
public class Tables {
    /** Holds the underlying machine for this set of parse tables.
     */
    protected LookaheadMachine machine;

    /** Holds the resolver for dealing with any conflicts that occur.
     */
    protected Resolver resolver;

    // For convenience, we cache the following fields from the
    // underlying grammar:
    protected int numNTs;
    protected int numTs;

    /** Construct a set of parse tables using lookahead information for
     *  the given machine.
     */
    public Tables(LookaheadMachine machine, Resolver resolver) {
        this.machine    = machine;
        this.resolver   = resolver;
        Grammar grammar = machine.getGrammar();
        this.numNTs     = grammar.getNumNTs();
        this.numTs      = grammar.getNumTs();
        int numStates   = machine.getNumStates();
        this.action     = new byte[numStates][];
        this.arg        = new int[numStates][];
        this.prodUsed   = new boolean[numNTs][];
        this.prodUnused = 0;
        for (int i=0; i<numNTs; i++) {
            prodUsed[i] = new boolean[grammar.getProds(i).length];
            prodUnused += prodUsed[i].length;
        }
        for (int i=0; i<numStates; i++) {
            fillTablesAt(i);
        }
    } 

    /** Code used to signal that no action has been specified.
     */
    public final static byte NONE   = 0;

    /** Code used to specify that a shift action is required.
     */
    public final static byte SHIFT  = 1;

    /** Code used to specify that a reduce action is required.
     */
    public final static byte REDUCE = 2;

    /** Holds a table recording the actions to be taken on each state
     *  and at each terminal symbol.
     */
    protected byte[][] action;

    /** Holds the arguments for each action, which is either a state
     *  number for a SHIFT, or a reduce number for a REDUCE.
     */
    protected int[][] arg;

    /** A table of booleans that records whether a reduce for the
     *  corresponding production appeared in the generated tables.
     */
    private boolean[][] prodUsed;

    /** A count of the total number of distinct productions with reduce
     *  actions that are used in the generated tables.
     */
    private int         prodUnused;

    /** Return the machine for these lookahead tables.
     */
    public LookaheadMachine getMachine() {
        return machine;
    }

    /** Return the action table for a particular state.  Action tables
     *  are indexed by terminal symbols, and contain one of three values:
     *  <ul>
     *  <li> NONE indicates that no action is possible, and hence an
     *       error has occured.
     *  <li> SHIFT indicates that a shift is required.
     *  <li> REDUCE indicates that a reduce step is required.
     *  </ul>
     */
    public byte[] getActionAt(int st) {
        return action[st];
    }

    /** Return the argument table at a particular state.  The
     *  interpretation of the entries in this table (indexed by
     *  terminals) varies according to the corresponding action
     *  entry.
     *  <ul>
     *  <li> If the action is NONE, then the argument is not used.
     *  <li> If the action is SHIFT, then the argument is the number
     *       of the state to which the machine will shift.
     *  <li> If the action is REDUCE, then the argument is the offset
     *       of the item in Machine.getItemsAt(st) by which we should
     *       reduce.
     *  </ul>
     */
    public int[] getArgAt(int st) {
        return arg[st];
    }

    /** Return the number of unused productions.  A production is unused
     *  if there are no entries in the constructed parse tables for the
     *  corresponding reduction.
     */
    public int getProdUnused() {
        return prodUnused;
    }

    /** Return the array of booleans indicating which of the productions
     *  for a particular nonterminal have been entered into the table.
     */
    public boolean[] getProdsUsedAt(int nt) {
        return prodUsed[nt];
    }

    /** Store a SHIFT entry in the table for a particular state.
     */
    public void setShift(int st, int tok, int to) {
        action[st][tok] = SHIFT;
        arg[st][tok]    = to;
    }

    /** Store a REDUCE entry in the table for a particular state.
     */
    public void setReduce(int st, int tok, int num) {
        action[st][tok] = REDUCE;
        arg[st][tok]    = num;
    }

    /** Fill in tables for a particular state using info from the machine.
     */
    private void fillTablesAt(int st) {
        action[st]   = new byte[numTs];      // all initialized to NONE
        arg[st]      = new int[numTs];
        int[] shifts = machine.getShiftsAt(st);
        int[] rs     = machine.getReducesAt(st);

        // Enter shifts into table.
        for (int i=0; i<shifts.length; i++) {
            setShift(st, machine.getEntry(shifts[i])-numNTs, shifts[i]);
        }
        // Enter reduces into table.
        for (int i=0; i<rs.length; i++) {
            Interator bts = BitSet.interator(machine.getLookaheadAt(st,i), 0);
            while (bts.hasNext()) {
                int tok = bts.next();
                switch (action[st][tok]) {
                    case NONE:
                        setReduce(st, tok, rs[i]);
                        break;
                    case SHIFT:
                        resolver.srResolve(this, st, tok, rs[i]);
                        break;
                    case REDUCE:
                        resolver.rrResolve(this, st, tok, rs[i]);
                        break;
                }
            }
        }

        // Register which productions are actually used
        LR0Items items = machine.getItems();
        IntSet   its   = machine.getItemsAt(st);
        for (int i=0; i<rs.length; i++) {
            for (int j=0; j<numTs; j++) {
                if (action[st][j]==REDUCE && arg[st][j]==rs[i]) {
                    // Under normal circumstances, every reduction
                    // will be used at least once ... however, it is
                    // possible that uses of a reduce step in the machine
                    // have been eliminated when a conflict was resolved.
                    LR0Items.Item it = items.getItem(its.at(rs[i]));
                    int lhs    = it.getLhs();
                    int prodNo = it.getProdNo();
                    if (!prodUsed[lhs][prodNo]) {
                        prodUsed[lhs][prodNo] = true;
                        prodUnused--;
                    }
                    break;
                }
            }
        }
    }
}
