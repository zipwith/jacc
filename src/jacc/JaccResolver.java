// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc;

import jacc.grammar.Grammar;
import jacc.grammar.LR0Items;
import jacc.grammar.LookaheadMachine;
import jacc.grammar.Resolver;
import jacc.grammar.Tables;
import jacc.util.IntSet;

/** Describes the strategy for resolving conflicts in jacc generated parsers.
 */
public class JaccResolver extends Resolver {
    private LookaheadMachine machine;

    /** Construct a conflict resolver for a given machine, following the
     *  rules and conventions of Jacc/yacc.
     */
    public JaccResolver(LookaheadMachine machine) {
        this.machine = machine;
        conflicts    = new Conflicts[machine.getNumStates()];
    }

    /** Records the number of shift/reduce conflicts that we have
     *  failed to resolve.
     */
    private int numSRConflicts = 0;

    /** Records the number of reduce/reduce conflicts that we have
     *  failed to resolve.
     */
    private int numRRConflicts = 0;

    /** Records the conflicts found at each state.
     */
    private Conflicts[] conflicts;

    /** Return the number of shift/reduce conflicts detected.
     */
    public int getNumSRConflicts() {
        return numSRConflicts;
    }

    /** Return the number of reduce/reduce conflicts detected.
     */
    public int getNumRRConflicts() {
        return numRRConflicts;
    }

    /** Returns a description of the conflicts at a given state.
     */
    public String getConflictsAt(int st) {
        return Conflicts.describe(machine, st, conflicts[st]);
    }

    /** Resolve a shift/reduce conflict.  First, see if the conflict
     *  can be resolved using fixity information.  If that fails, we
     *  choose the shift over the reduce and report a conflict.
     */
    public void srResolve(Tables tables, int st, int tok, int redNo) {
        Grammar        grammar = machine.getGrammar();
        Grammar.Symbol sym     = grammar.getTerminal(tok);
        IntSet         its     = machine.getItemsAt(st);
        LR0Items       items   = machine.getItems();
        Grammar.Prod   prod    = items.getItem(its.at(redNo)).getProd();

        if ((sym instanceof JaccSymbol) && (prod instanceof JaccProd)) {
            JaccSymbol jsym  = (JaccSymbol)sym;
            JaccProd   jprod = (JaccProd)prod;
            switch (Fixity.which(jprod.getFixity(), jsym.getFixity())) {
                case Fixity.LEFT:   // Choose reduce
                    tables.setReduce(st, tok, redNo);
                    return;
                case Fixity.RIGHT:  // Choose shift, which is already in
                    return;         // the table, so nothing more to do.
            }
        }
        conflicts[st]
            = Conflicts.sr(tables.getArgAt(st)[tok], redNo, sym, conflicts[st]);
        numSRConflicts++;
    }

    /** Resolve a reduce/reduce conflict.  We cannot ever avoid a
     *  reduce/reduce conflict, but the entry that we leave in the
     *  table must be for the production with the lowest number.
     */
    public void rrResolve(Tables tables, int st, int tok, int redNo) {
        Grammar        grammar = machine.getGrammar();
        int            redNo0  = tables.getArgAt(st)[tok];
        IntSet         its     = machine.getItemsAt(st);
        LR0Items       items   = machine.getItems();
        Grammar.Prod   prod0   = items.getItem(its.at(redNo0)).getProd();
        Grammar.Prod   prod    = items.getItem(its.at(redNo)).getProd();
        Grammar.Symbol sym     = grammar.getTerminal(tok);

        if (prod.getSeqNo()<prod0.getSeqNo()) {
            tables.setReduce(st, tok, redNo);
        }
        conflicts[st] = Conflicts.rr(redNo0, redNo, sym, conflicts[st]);
        numRRConflicts++;
    }
}
