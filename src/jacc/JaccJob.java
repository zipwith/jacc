// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc;

import java.io.Reader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import compiler.Handler;
import compiler.Position;
import compiler.Diagnostic;
import compiler.Failure;
import compiler.Warning;
import compiler.JavaSource;
import compiler.Phase;

import jacc.grammar.Grammar;
import jacc.grammar.Finitary;
import jacc.grammar.LookaheadMachine;
import jacc.grammar.Resolver;
import jacc.grammar.Tables;
import jacc.grammar.Parser;

/** Encapsulates the process of running a single job for the jacc
 *  parser generator, with some degree of independence from the
 *  actual user interface.
 */
public class JaccJob extends Phase {
    private Settings     settings;
    private JaccParser   parser;
    private JaccTables   tables;
    private JaccResolver resolver;
    private PrintWriter  out;

    public JaccJob(Handler handler, PrintWriter out, Settings settings) {
        super(handler);
        this.out      = out;
        this.settings = settings;
        this.parser   = new JaccParser(handler, settings);
    }

    /** Return the settings for this job.
     */
    Settings getSettings() {
        return settings;
    }

    /** Return the tables for this job.
     */
    JaccTables getTables() {
        return tables;
    }

    /** Return the resolver for this job.
     */
    JaccResolver getResolver() {
        return resolver;
    }

    /** Create a JaccLexer from an input file name.
     */
    private JaccLexer lexerFromFile(String inputFile) {
        try {
            Reader    input = new FileReader(inputFile);
            JaccLexer lexer = new JaccLexer(getHandler(),
                                new JavaSource(getHandler(), inputFile, input));
            lexer.nextToken(); // prime the token stream
            return lexer;
        } catch (FileNotFoundException e) {
            report(new Failure("Could not open file \"" + inputFile + "\""));
            return null;
        }
    }

    /** Parse a grammar file.
     */
    public void parseGrammarFile(String inputFile) {
        JaccLexer lexer = lexerFromFile(inputFile);
        if (lexer!=null) {
            parser.parse(lexer);
        }
    }

    /** Generate a machine and corresponding parse tables for the
     *  input grammar.
     */
    public void buildTables() {
        Grammar grammar = parser.getGrammar();

        if (grammar==null || !allDeriveFinite(grammar)) {
            return;
        }

        LookaheadMachine machine = settings.makeMachine(grammar);

        resolver = new JaccResolver(machine);
        tables   = new JaccTables(machine, resolver);

        if (tables.getProdUnused()>0) {
            report(new Warning(tables.getProdUnused()
                               + " rules never reduced"));
        }

        if (resolver.getNumSRConflicts()>0 || resolver.getNumRRConflicts()>0) {
            report(new Warning("conflicts: "
                               + resolver.getNumSRConflicts()
                               + " shift/reduce, "
                               + resolver.getNumRRConflicts()
                               + " reduce/reduce"));
        }
    }

    /** Check that all nonterminals in the input grammar derive a finite
     *  string.
     */
    private boolean allDeriveFinite(Grammar grammar) {
        Finitary finitary  = grammar.getFinitary();
        boolean  allFinite = true;
        for (int nt=0; nt<grammar.getNumNTs(); nt++) {
            if (!finitary.at(nt)) {
                allFinite = false;
                report(new Failure("No finite strings can be derived for "
                                  + grammar.getNonterminal(nt)));
            }
        }
        return allFinite;
    }

    /** Parse a file containing an example input.
     */
    public void readRunExample(String inputFile, boolean showState) {
        out.println("Running example from \"" + inputFile + "\"");
        JaccLexer lexer = lexerFromFile(inputFile);
        if (lexer!=null) {
            runExample(parser.parseSymbols(lexer), showState);
        }
    }

    /** Run a sample input through the generated parser and display the
     *  resulsts at each step.
     */
    public void runExample(int[] syms, boolean showState) {
        Grammar g = parser.getGrammar();
        Parser  p = new Parser(tables, syms);
        out.print("start ");
        for (;;) {
            out.print(" :  ");
            p.display(out, showState);
            switch (p.step()) {
                case Parser.ACCEPT:
                    out.println("Accept!");
                    return;
                case Parser.ERROR :
                    out.print("error in state ");
                    out.print(p.getState());
                    out.print(", next symbol ");
                    out.println(g.getSymbol(p.getNextSymbol()));
                    return;
                case Parser.GOTO  :
                    out.print("goto  ");
                    break;
                case Parser.SHIFT :
                    out.print("shift ");
                    break;
                case Parser.REDUCE:
                    out.print("reduce");
                    break;
            }
        }
    }

    /** Parse and process a file containing error examples.
     */
    public void readErrorExamples(String inputFile) {
        out.println("Reading error examples from \"" + inputFile + "\"");
        JaccLexer lexer = lexerFromFile(inputFile);
        if (lexer!=null) {
            parser.parseErrorExamples(lexer, this);
        }
    }

    /** Process a sequence of input symbols that is expected to result
     *  in an error described by a given tag.
     */
    public void errorExample(Position pos, String tag, int[] syms) {
        Parser p = new Parser(tables, syms);
        int    s;
        do { s = p.step(); } while (s!=Parser.ACCEPT && s!=Parser.ERROR);
        if (s==Parser.ACCEPT) {
            report(new Warning(pos, "Example for \""
                                    + tag + "\" does not produce an error"));
        } else {
            Grammar grammar = tables.getMachine().getGrammar();
            int     sym     = p.getNextSymbol();

            if (grammar.isNonterminal(sym)) {  // maybe could use first set?
                report(new Warning(pos, "Example for \"" + tag
                           + "\" reaches an error at the nonterminal "
                           + grammar.getSymbol(sym)));
            } else {                           // use single terminal
                int state = p.getState();
//              out.println("Error in state " + state
//                          + " on terminal " + grammar.getSymbol(sym)
//                          + " indicates: " + tag);
                if (!tables.errorAt(state, sym)) {
                    // This shouldn't occur because the parser wouldn't
                    // have got stuck here if a shift or reduce had been
                    // indicated!
                    report(new Failure(pos,
                             "Error example results in internal error"));
                } else {
                    String tag1 = tables.errorSet(state, sym, tag);
                    if (tag1!=null) {
                        report(new Warning(pos,
                             "Multiple errors are possible in state " + state
                             + " on terminal " + grammar.getSymbol(sym)
                             + ":\n - " + tag1
                             +  "\n - " + tag));
                    }
                }
            }
        }
    }
}
