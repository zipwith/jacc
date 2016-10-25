// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc;

import java.io.PrintWriter;
import jacc.grammar.Grammar;
import jacc.grammar.Tables;
import compiler.Handler;
import compiler.Failure;

/** Used to output a Java class that implements the generated parser.
 *  This code is based on the ideas presented in the following
 *  paper:
 *  <quote>
 *    Very fast YACC-compatible parsers (for very little effort)
 *    Achyutram Bhamidipaty and Todd A. Proebsting
 *    Technical Report TR 95-09
 *    Department of Computer Science
 *    University of Arizona
 *    September 22, 1995
 *  </quote>
 *  Some changes were necessary to adapt the ideas to Java---most
 *  notably because Java doesn't have the goto statement.  In addition,
 *  I wanted to reduce the amount of code that appears in the main
 *  parser loop because current JVMs don't allow methods with more than
 *  64K of bytecode ... which could be a problem for large parsers.
 */
public class ParserOutput extends Output {
    public ParserOutput(Handler handler, JaccJob job) {
        super(handler, job);
        tables.analyzeRows();
    }

    /** Output a description of a generated machine to a Writer.
     */
    public void write(PrintWriter out) {
        datestamp(out);
        String pkg = settings.getPackageName();
        if (pkg!=null) {
            out.println("package " + pkg +";");
        }

        if (settings.getPreText()!=null) {
            out.println(settings.getPreText());
        }

        yyaccept           = 2*numStates;
        stack_overflow     = 2*numStates+1;
        yyabort            = 2*numStates+2;
        error_handler      = 2*numStates+3;
        user_error_handler = 2*numStates+4;

        // ntBranchCount records the number of branches that will
        // occur in the case statement in the code for nonterminal_J.
        // These values are then used to calculate ntGoto[].
        int[] ntBranchCount = new int[numNTs];
        stNumSwitches       = new int[numStates];
        for (int i=0; i<numStates; i++) {
            int[] ts = machine.getGotosAt(i);
            for (int j=0; j<ts.length; j++) {
                ntBranchCount[machine.getEntry(ts[j])]++;
            }
            byte[] action    = tables.getActionAt(i);
            int[]  arg       = tables.getArgAt(i);
            int    def       = tables.getDefaultRowAt(i);
            stNumSwitches[i] = 0;
            for (int j=0; j<action.length; j++) {
                if (def<0 || action[j]!=action[def] || arg[j]!=arg[def]) {
                    stNumSwitches[i]++;
                }
            }
        }

        ntGoto     = new int[numNTs][];
        ntGotoSrc  = new int[numNTs][];
        ntDefault  = new int[numNTs];
        ntDistinct = new int[numNTs];
        for (int nt=0; nt<numNTs; nt++) {
            ntGoto[nt]    = new int[ntBranchCount[nt]];
            ntGotoSrc[nt] = new int[ntBranchCount[nt]];
        }
        for (int i=0; i<numStates; i++) {
            int[] ts = machine.getGotosAt(i);
            for (int j=0; j<ts.length; j++) {
                int nt = machine.getEntry(ts[j]);
                ntGoto[nt][--ntBranchCount[nt]]  = ts[j];
                ntGotoSrc[nt][ntBranchCount[nt]] = i;
            }
        }
        for (int nt=0; nt<numNTs; nt++) {
            int bestPos   = (-1);
            int bestCount = 0;
            int len       = ntGoto[nt].length;
            for (int j=0; j+bestCount<len; j++) {
                int count = 1;
                for (int k=j+1; k<len; k++) {
                    if (ntGoto[nt][k]==ntGoto[nt][j]) {
                        count++;
                    }
                }
                if (count>bestCount) {
                    bestCount = count;
                    bestPos   = j;
                }
            }
            ntDefault[nt]  = bestPos;
            ntDistinct[nt] = ntGoto[nt].length - (bestCount-1);
        }

        // check if any custom error messages have been defined
        errMsgs = tables.getNumErrors()>0;

        // find the error token and see if it was used
        for (errTok=numNTs; errTok<numSyms; errTok++) {
            if (grammar.getSymbol(errTok).getName().equals("error")) {
                break;
            }
        }
        if (errTok<numSyms) {
            for (int st=0; st<numStates && !errUsed; st++) {
                int[] shifts = machine.getShiftsAt(st);
                for (int j=0; j<shifts.length && !errUsed; j++) {
                    if (machine.getEntry(shifts[j])==errTok) {
                        errUsed = true;
                    }
                }
            }
        }

        // Do output here!
        out.print("class " + settings.getClassName());
        if (settings.getExtendsName()!=null) {
            out.print(" extends " + settings.getExtendsName());
        }
        if (settings.getImplementsNames()!=null) {
            out.print(" implements " + settings.getImplementsNames());
        }
        out.println(" {");
        indent(out, 1, new String[] {
            "private int yyss = 100;",  // sets initial stack size
            "private int yytok;",       // records current token
            "private int yysp = 0;",    // records stack pointer
            "private int[] yyst;",      // holds state stack
            "protected int yyerrno = (-1);"});
                                        // var to hold error code, if any
        if (errUsed) {
            indent(out, 1, "private int yyerrstatus = 3;");
        }
        indent(out, 1, "private " + settings.getTypeName() + "[] yysv;");
                                        // holds semantic stack
        indent(out, 1, "private " + settings.getTypeName() + " yyrv;");
                                        // holds semantic result of prod'n
        out.println();

        defineParse(out, 1);
        defineExpand(out, 1);
        defineErrRec(out, 1);
        for (int st=0; st<numStates; st++) {
            defineState(out, 1, st);
        }
        for (int i=0; i<numNTs; i++) {
            Grammar.Prod[] prods = grammar.getProds(i);
            for (int j=0; j<prods.length; j++) {
                defineReduce(out, 1, prods[j], i);
            }
            defineNonterminal(out, 1, i);
        }

        defineErrMsgs(out);

        if (settings.getPostText()!=null) {
            out.println(settings.getPostText());
        }

        out.println("}");
    }

    /** Output the array of custom error messages.
     */
    private void defineErrMsgs(PrintWriter out) {
        if (errMsgs) {
            indent(out, 1, new String[] {
              "private int yyerr(int e, int n) {",
              "    yyerrno = e;",
              "    return n;",
              "}"
            });
        }
        indent(out, 1, "protected String[] yyerrmsgs = {");
        int n = tables.getNumErrors();
        if (n>0) {
            for (int i=0; i<n-1; i++) {
                indent(out, 2, "\"" + tables.getError(i) + "\",");
            }
            indent(out, 2, "\"" + tables.getError(n-1) + "\"");
        }
        indent(out, 1, "};");
    }

    /** Numbers of special states in the machine.
     *  Other labels map to cases in the main switch:
     *      state_N           N
     *      state_action_N    numStates + N
     */
    private int yyaccept, yyabort;
    private int stack_overflow, error_handler, user_error_handler;

    /** Records the number of cases to switch over at each state.
     */
    private int[] stNumSwitches;

    /** Calculates a table of gotos organized by the corresponding
     *  nonterminal rather than the start state.
     */
    private int[][] ntGoto;

    /** Records the start state for each of the transitions in corresponding
     *  positions in ntGoto[].
     */
    private int[][] ntGotoSrc;

    /** Records the default target of a nonterminal goto function.
     */
    private int[] ntDefault;

    /** Records the number of distinct targets in each ntGoto table.
     */
    private int[] ntDistinct;

    /** Records the number of the "error" token.
     */
    private int errTok;

    /** Flag to indicate whether custom error messages have been
     *  defined.
     */
    private boolean errMsgs = false;

    /** Flag to indicate whether error recovery code is required.
     */
    private boolean errUsed = false;

    /** Output code for function that expands the stack size.
     */
    private void defineExpand(PrintWriter out, int ind) {
        indent(out, ind, new String[] {
            "protected void yyexpand() {",
               "    int[] newyyst = new int[2*yyst.length];" });
        indent(out, ind+1, settings.getTypeName() + "[] newyysv = new " +
                           settings.getTypeName() + "[2*yyst.length];");
        indent(out, ind, new String[] {
            "    for (int i=0; i<yyst.length; i++) {",
            "        newyyst[i] = yyst[i];",
            "        newyysv[i] = yysv[i];",
            "    }",
            "    yyst = newyyst;",
            "    yysv = newyysv;",
            "}" });
        out.println();
    }

    /** Output code for error recovery functions
     */
    private void defineErrRec(PrintWriter out, int ind) {
        if (errUsed) {
            indent(out, ind,   "public void yyerrok() {");
            indent(out, ind+1, "yyerrstatus = 3;");
            if (errMsgs) {
                indent(out, ind+1, "yyerrno     = (-1);");
            }
            indent(out, ind,   "}");
            out.println();
            indent(out,ind, "public void yyclearin() {");
            indent(out, ind+1, "yytok = (" + settings.getNextToken());
            indent(out, ind+1, "        );");
            indent(out,ind, "}");
            out.println();
        }
    }

    /** Output main state loop.
     */
    private void defineParse(PrintWriter out, int ind) {
        indent(out, ind, "public boolean parse() {");
        indent(out, ind+1, new String[] {
            "int yyn = 0;",
            "yysp = 0;",
            "yyst = new int[yyss];" });
        if (errUsed) {
            indent(out, ind+1, "yyerrstatus = 3;");
        }
        if (errMsgs) {
            indent(out, ind+1, "yyerrno = (-1);");
        }
        indent(out, ind+1, "yysv = new " + settings.getTypeName() + "[yyss];");
        indent(out, ind+1, "yytok = (" + settings.getGetToken());
        indent(out, ind+1, "         );");
        indent(out, ind, new String[] {
            "loop:",
            "    for (;;) {",
            "        switch (yyn) {" });
        for (int st=0; st<numStates; st++) {
            stateCases(out, ind+3, st);
        }

        // Stack overflow, accept, and abort:
        indent(out, ind+3, "case " + yyaccept +":");
        indent(out, ind+4, "return true;");

        indent(out, ind+3, "case " + stack_overflow +":");
        indent(out, ind+4, "yyerror(\"stack overflow\");");

        indent(out, ind+3, "case " + yyabort +":");
        indent(out, ind+4, "return false;");

        // Error Handler:
        errorCases(out, ind+3);
        indent(out, ind, new String[] {
               "        }",
               "    }",
               "}" });
        out.println();
    }

    /** Produce branches in switch statement for a given state.
     */
    private void stateCases(PrintWriter out, int ind, int st) {
        indent(out, ind,   "case "+st+":");
        indent(out, ind+1, "yyst[yysp] = " + st + ";");
        if (grammar.isTerminal(machine.getEntry(st))) {
            indent(out, ind+1, "yysv[yysp] = (" + settings.getGetSemantic());
            indent(out, ind+1, "             );");
            indent(out, ind+1, "yytok = (" + settings.getNextToken());
            indent(out, ind+1, "        );");
            if (errUsed) {
                indent(out, ind+1, "yyerrstatus++;");
            }
        }
        indent(out, ind+1, new String [] {
            //"dump(yyn);",
            "if (++yysp>=yyst.length) {",
            "    yyexpand();",
            "}" });

        indent(out, ind, "case "+(st+numStates)+":");
        if (stNumSwitches[st]>5) {
            continueTo(out, ind+1, "yys"+st+"()", true);
        } else {
            switchState(out, ind+1, st, true);
        }
        out.println();
    }

    /** Generate code to produce a transition to a particular state.
     */
    private void continueTo(PrintWriter out, int ind,
                            String dst, boolean inLoop) {
        if (inLoop) {
            indent(out, ind, "yyn = " + dst + ";");
            indent(out, ind, "continue;");
        } else {
            indent(out, ind, "return " + dst + ";");
        }
    }

    /** Generate the out of line function for a particular state, if
     *  necessary.
     */
    private void defineState(PrintWriter out, int ind, int st) {
        if (stNumSwitches[st]>5) {
            indent(out, ind, "private int yys"+st+"() {");
            switchState(out, ind+1, st, false);
            indent(out, ind, "}");
            out.println();
        }
    }

    /** Generate the main switch for a particular state.
     */
    private void switchState(PrintWriter out, int ind, int st, boolean inLoop) {
        byte[] action = tables.getActionAt(st);
        int[]  arg    = tables.getArgAt(st);
        int    def    = tables.getDefaultRowAt(st);
        if (stNumSwitches[st]>0) {
            indent(out, ind, "switch (yytok) {");
            int[] idx = tables.indexAt(st);
            for (int j=0; j<idx.length;) {
                int  oj = idx[j];
                byte aj = action[oj];
                int  bj = arg[oj];
                int  k  = j;
                while (++k<idx.length && action[idx[k]]==aj
                                      && arg[idx[k]]==bj) {
                    // empty body
                }
                // rows idx[j], idx[j+1], ... , idx[k-1] are the same
                if (def<0 || aj!=action[def] || bj!=arg[def]) {
                    for (int l=j; l<k; l++) {
                        indent(out, ind+1);
                        out.print("case ");
                        if (idx[l]==numTs-1) {
                            out.print("ENDINPUT");
                        } else {
                            out.print(grammar.getTerminal(idx[l]).getName());
                        }
                        out.println(":");
                    }
                    continueTo(out, ind+2, codeAction(st, aj, bj), inLoop);
                }
                j = k;
            }
            indent(out, ind, "}");
        }
        if (def<0) {
            continueTo(out, ind, Integer.toString(error_handler), inLoop);
        } else {
            continueTo(out, ind,
                       codeAction(st, action[def], arg[def]),
                       inLoop);
        }
    }

    /** Output code for a particular action in a table.
     */
    private String codeAction(int st, int act, int arg) {
        if (act==Tables.NONE) {
            String yyn = Integer.toString(error_handler);
            return (arg==0) ? yyn : ("yyerr(" + (arg-1) + ", " + yyn + ")");
        } else if (act==Tables.REDUCE) {
            return "yyr" + machine.reduceItem(st,arg).getSeqNo() +"()";
        } else {
            return Integer.toString((arg<0) ? yyaccept : arg);
        }
    }

    /** Produce code to `goto' a particular reduce function.
     */
    private void gotoReduce(PrintWriter out, int ind, int st, int redNo) {
        indent(out, ind, "return yyr" +
                         machine.reduceItem(st, redNo).getSeqNo() + "();");
    }

    /** Produce code to define a particular reduction.
     */
    private void defineReduce(PrintWriter out, int ind,
                              Grammar.Prod prod, int nt) {
        if (prod instanceof JaccProd && ntDefault[nt]>=0) {
            JaccProd jprod = (JaccProd)prod;
            indent(out, ind);
            out.print("private int yyr" + jprod.getSeqNo() + "() { // ");
            out.print(grammar.getSymbol(nt).getName() + " : ");
            out.println(grammar.displaySymbols(jprod.getRhs(),
                                               "/* empty */", " "));
            String action = jprod.getAction();
            int    n      = jprod.getRhs().length;
            if (action!=null) {
                indent(out, ind+1);
                translateAction(out, jprod, action);
                indent(out, ind+1, "yysv[yysp-=" + n + "] = yyrv;");
            } else if (n>0) {
                indent(out, ind+1, "yysp -= " + n + ";");
            }
            gotoNonterminal(out, ind+1, nt);
            indent(out, ind, "}");
            out.println();
        }
    }

    /** Copy action to output, translating references to $$ to yyrv,
     *  and references to $n to yysv[yysp-i], adding casts to the
     *  latter if a type has been specified.
     */
    private void translateAction(PrintWriter out,
                                 JaccProd jprod,
                                 String action) {
        int[] rhs = jprod.getRhs();
        int   len = action.length();
        for (int i=0; i<len; i++) {
            char c = action.charAt(i);
            if (c=='$') {
                c = action.charAt(i+1);
                if (c=='$') {
                    i++;
                    out.print("yyrv");
                } else if (Character.isDigit(c)) {
                    int n = 0;
                    do {
                        n = n*10 + Character.digit(c, 10);
                        i++;
                        c = action.charAt(i + 1);
                    } while (Character.isDigit(c));
                    if (n<1 || n>rhs.length) {
                        report(new Failure(jprod.getActionPos(),
                               "$" + n + " cannot be used in this action."));
                    } else {
                        int symNo   = (1+ rhs.length) - n;
                        String type = null;
                        if (grammar.getSymbol(rhs[n-1])
                            instanceof JaccSymbol) {
                            JaccSymbol jsym
                                = (JaccSymbol)(grammar.getSymbol(rhs[n-1]));
                            type = jsym.getType();
                        }
                        if (type!=null) {
                            out.print("((" + type + ")");
                        }
                        out.print("yysv[yysp-" + symNo + "]");
                        if (type!=null) {
                            out.print(")");
                        }
                    }
                } else {
                    out.print('$');
                }
            } else if (c=='\n') {
                out.println();
            } else {
                out.print(c);
            }
        }
        out.println();
    }

    /** Produce code to goto a particular nonterminal function.  If
     *  there is only one entry in the corresponding goto table, or
     *  if there is only one production for this nonterminal (and
     *  hence no need to share the code), then we `inline' the call.
     */
    private void gotoNonterminal(PrintWriter out, int ind, int nt) {
        if (ntDefault[nt] < 0) {
            return;
        } else if (ntDistinct[nt]==1) {
            indent(out, ind, "return " + ntGoto[nt][0] + ";");
        } else if (grammar.getProds(nt).length==1) {
            nonterminalSwitch(out, ind, nt);
        } else {
            indent(out, ind, "return " + ntName(nt) + "();");
        }
    }

    /** Produce code to define a given nonterminal function.  If this
     *  function is going to be inlined, then no output will be generated.
     */
    private void defineNonterminal(PrintWriter out, int ind, int nt) {
        if (ntDefault[nt]>=0
            && ntDistinct[nt]!=1
            && grammar.getProds(nt).length!=1) {
            indent(out, ind, "private int " + ntName(nt) + "() {");
            nonterminalSwitch(out, ind+1, nt);
            indent(out, ind, "}");
            out.println();
        }
    }

    /** Produce code to generate the switch for a nonterminal.
     */
    private void nonterminalSwitch(PrintWriter out, int ind, int nt) {
        int def = ntGoto[nt][ntDefault[nt]];
        indent(out, ind);
        out.println("switch (yyst[yysp-1]) {");
        for (int i=0; i<ntGoto[nt].length; i++) {
            int dest = ntGoto[nt][i];
            if (dest!=def) {
                indent(out, ind+1);
                out.print("case " + ntGotoSrc[nt][i]);
                out.println(": return " + dest + ";");
            }
        }
        indent(out, ind+1);
        out.println("default: return " + def + ";");
        indent(out, ind);
        out.println("}");
    }

    /** Return a name for the nonterminal function for a given nt.
     */
    private String ntName(int nt) {
        return "yyp" + grammar.getSymbol(nt).getName();
    }

    /** Produce code to do error recovery.
     */
    private void errorCases(PrintWriter out, int ind) {
        indent(out, ind, "case " + error_handler + ":");
        if (!errUsed) {
            indent(out, ind+1, new String[] {
                "yyerror(\"syntax error\");",
                "return false;"});
            return;
        } else {
            indent(out, ind+1, new String[] {
                "if (yyerrstatus>2) {",
                "    yyerror(\"syntax error\");",
                "}"});
            indent(out, ind, "case " + user_error_handler + " :");
            indent(out, ind+1, new String[] {
                "if (yyerrstatus==0) {",
                "    if ((" + settings.getGetToken(),
                "         )==ENDINPUT) {",
                "        return false;",
                "    }",
                "    " + settings.getNextToken(),
                "    ;"});
            indent(out, ind+2, "yyn = " + numStates +
                               " + yyst[yysp-1];");
            indent(out, ind+1, new String[] {
                "    continue;",
                "} else {",
                "    yyerrstatus = 0;",
                "    while (yysp>0) {",
                "        switch(yyst[yysp-1]) {"});

            for (int st=0; st<numStates; st++) {
                int[] shifts = machine.getShiftsAt(st);
                for (int j=0; j<shifts.length; j++) {
                    if (machine.getEntry(shifts[j])==errTok) {
                        indent(out, ind+4, "case " + st + ":");
                        indent(out, ind+5, "yyn = " + shifts[j] + ";");
                        indent(out, ind+5, "continue loop;");
                    }
                }
            }

            indent(out, ind+1, new String[] {
                "        }",
                "        yysp--;",
                "    }",
                "    return false;",
                "}"});
        }
    }
}
