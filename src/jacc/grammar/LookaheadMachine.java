// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc.grammar;

/** A base class for machines that provide lookahead information.
 *  This makes it possible to support both SLR and LALR parsers
 *  in the same framework.
 */
public abstract class LookaheadMachine extends Machine {
    /** Construct a machine for a given grammar.
     */
    public LookaheadMachine(Grammar grammar) {
        super(grammar);
    }

    /** Return lookahead sets for the reductions at a given state.
     */
    public abstract int[] getLookaheadAt(int st, int i);
}
