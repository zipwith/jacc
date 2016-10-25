// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc.grammar;

import jacc.util.BitSet;

/** Calculation of first sets.  The first set of a given nonterminal X
 *  is the set of all terminal symbols that can appear at the beginning
 *  of a string derived from X.
 */
public final class First extends Analysis {
    private Grammar  grammar;
    private Nullable nullable;
    private int      numNTs;
    private int      numTs;
    private int[][]  first;

    /** Construct a first set analysis for a given grammar.
     */
    public First(Grammar grammar, Nullable nullable) {
        super(grammar.getComponents());
        this.grammar  = grammar;
        this.nullable = nullable;
        this.numNTs   = grammar.getNumNTs();
        this.numTs    = grammar.getNumTs();
        first         = new int[numNTs][];
        for (int i=0; i<numNTs; i++) {
            first[i] = BitSet.make(numTs);
        }
        bottomUp();
    }

    /** Run the analysis at a particular point.  Return a boolean true
     *  if this changed the current approximation at this point.
     */
    protected boolean analyze(int c) {
        boolean changed = false;
        Grammar.Prod[] prods = grammar.getProds(c);
        for (int k=0; k<prods.length; k++) {
            int[] rhs = prods[k].getRhs();
            int   l   = 0;
            for (; l<rhs.length; l++) {
                if (grammar.isTerminal(rhs[l])) {
                    if (BitSet.addTo(first[c], rhs[l] - numNTs)) {
                        changed = true;
                    }
                    break;
                } else {
                    if (BitSet.addTo(first[c], first[rhs[l]])) {
                        changed = true;
                    }
                    if (!nullable.at(rhs[l])) {
                        break;
                    }
                }
            }
        }
        return changed;
    }

    /** Return a bitset of the first symbols for a given nonterminal.
     */
    public int[] at(int i) {
        return first[i];
    }

    /** Display the results of the analysis for the purposes of debugging
     *  and inspection.
     */
    public void display(java.io.PrintWriter out) {
        out.println("First sets:");
        for (int i=0; i<first.length; i++) {
            out.print(" First(" + grammar.getSymbol(i) + "): {");
            out.print(grammar.displaySymbolSet(this.at(i), numNTs));
            out.println("}");
        }
    }
}
