// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc;

import java.io.PrintWriter;
import jacc.grammar.Tables;
import compiler.Handler;
import jacc.util.IntSet;

/** Used to generate a textual description of the parser machine.
 */
public class TextOutput extends Output {
    private boolean wantFirst = false;

    public TextOutput(Handler handler, JaccJob job, boolean wantFirst) {
        super(handler, job);
        this.wantFirst = wantFirst;
        tables.analyzeRows();
    }

    /** Output a textual description of the generated machine.
     */
    public void write(PrintWriter out) {
        datestamp(out);
        for (int i=0; i<numStates; i++) {
            out.print(resolver.getConflictsAt(i));
            out.println(describeEntry(i));

            // Display the items for this state:
            IntSet its = machine.getItemsAt(i);
            int    sz  = its.size();
            for (int j=0; j<sz; j++) {
                indent(out, 1);
                machine.getItems().getItem(its.at(j)).display(out);
                out.println();
            }
            out.println();

            // Output main action table:
            byte[] action = tables.getActionAt(i);
            int[]  arg    = tables.getArgAt(i);
            int    def    = tables.getDefaultRowAt(i);
            int[]  idx    = tables.indexAt(i);
            for (int j=0; j<action.length; j++) {
                int xj = idx[j];
                if (def<0 || action[xj]!=action[def] || arg[xj]!=arg[def]) {
                    indent(out, 1);
                    out.print(grammar.getTerminal(xj).getName());
                    out.print(' ');
                    out.println(describeAction(i, action[xj], arg[xj]));
                }
            }
            indent(out, 1);
            if (def<0) {
                out.println(". error");
            } else {
                out.print(". ");
                out.println(describeAction(i, action[def], arg[def]));
            }
            out.println();

            // Output gotos:
            int[] ts = machine.getGotosAt(i);
            if (ts.length>0) {
                for (int j=0; j<ts.length; j++) {
                    int sym = machine.getEntry(ts[j]);
                    int st  = ts[j];
                    indent(out, 1);
                    out.print(grammar.getSymbol(sym).getName());
                    out.println(" " + describeGoto(st));
                }
                out.println();
            }
        }

        if (wantFirst) {
            grammar.getFirst().display(out);
            out.println();
            grammar.getFollow().display(out);
            out.println();
            grammar.getNullable().display(out);
            out.println();
        }

        // Output list of unused productions
        if (tables.getProdUnused()>0) {
            for (int nt=0; nt<numNTs; nt++) {
                boolean[] used = tables.getProdsUsedAt(nt);
                for (int j=0; j<used.length; j++) {
                    if (!used[j]) {
                        int[] rhs = grammar.getProds(nt)[j].getRhs();
                        out.print("Rule not reduced: ");
                        out.print(grammar.getNonterminal(nt).getName());
                        out.print(" : ");
                        out.println(grammar.displaySymbols(rhs, "", " "));
                    }
                }
            }
            out.println();
        }

        // Output summary of statistics:
        out.println(numTs + " terminals, " + numNTs + " nonterminals;");
        out.println(grammar.getNumProds()
                    + " grammar rules, "
                    + numStates
                    + " states;");
        out.println(resolver.getNumSRConflicts()
                    + " shift/reduce and "
                    + resolver.getNumRRConflicts()
                    + " reduce/reduce conflicts reported.");
    }

    /** Generate a description of the entry point to a state.
     */
    protected String describeEntry(int st) {
        return "state " + st
                        + " (entry on "
                        + grammar.getSymbol(machine.getEntry(st))
                        + ")";
    }

    /** Generate a description of a particular action in a table.
     */
    private String describeAction(int st, int act, int arg) {
        if (act==Tables.NONE) {
            if (arg==0) {
                return "error";
            } else {
                return "error \"" + tables.getError(arg-1) + "\"";
            }
        } else if (act==Tables.REDUCE) {
            return "reduce " + machine.reduceItem(st,arg).getSeqNo();
        } else if (arg<0) {
            return "accept";
        } else {
            return describeShift(arg);
        }
    }

    /** Generate a description of a shift.
     */
    protected String describeShift(int st) {
        return "shift " + st;
    }

    /** Generate a description of a goto.
     */
    protected String describeGoto(int st) {
        return "goto " + st;
    }
}
