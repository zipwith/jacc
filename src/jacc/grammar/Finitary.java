// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc.grammar;

/** Calculation of finitaryness.  A nonterminal is finitary if it derives
 *  a finite string of terminal symbols.  If X -> a1 ... an is a production
 *  then:
 *         finitary a1 && ... && finitary an => finitary X
 *
 *  Using also the fact that finitary t = true for any terminal t, we can
 *  iterate to find finitary values for each nonterminal.
 */
final public class Finitary extends Analysis {
    private boolean[] finitary;
    private boolean[] consider;
    private Grammar   grammar;
    private int       numNTs;

    /** Construct a finitary analysis for a given grammar.
     */
    public Finitary(Grammar grammar) {
        super(grammar.getComponents());
        this.grammar = grammar;
        this.numNTs  = grammar.getNumNTs();
        finitary = new boolean[numNTs];
        consider = new boolean[numNTs];
        for (int i=0; i<numNTs; i++) {
            finitary[i] = false;
            consider[i] = true;
        }
        bottomUp();
    }

    /** Run the analysis at a particular point.  Return a boolean true
     *  if this changed the current approximation at this point.
     */
    protected boolean analyze(int c) {
        boolean changed = false;
        if (consider[c]) {
            int blocked = 0;
            Grammar.Prod[] prods = grammar.getProds(c);
            for (int k=0; k<prods.length; k++) {
                int[] rhs = prods[k].getRhs();
                int   l   = 0;
                while (l<rhs.length && this.at(rhs[l])) {
                    l++;
                }
                if (l>=rhs.length) {
                    finitary[c] = true;
                    consider[c] = false;
                    changed     = true;
                    break;
                } else if (!consider[rhs[l]]) {
                    blocked++;
                }
            }
            if (blocked==prods.length) {
                consider[c] = false;
            }
        }
        return changed;
    }

    /** Return a boolean true if the given symbol is finitary.
     */
    public boolean at(int i) {
        return grammar.isTerminal(i) || finitary[i];
    }

    /** Display the results of the analysis for the purposes of debugging
     *  and inspection.
     */
    public void display(java.io.PrintWriter out) {
        out.print("Finitary = {");
        int count = 0;
        for (int i=0; i<numNTs; i++) {
            if (this.at(i)) {
                if (count>0) {
                    out.print(", ");
                }
                out.print(grammar.getSymbol(i).getName());
                count++;
            }
        }
        out.println("}");
    }
}
