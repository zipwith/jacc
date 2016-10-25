// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc;

import java.io.File;
import java.io.PrintWriter;

import compiler.Handler;
import compiler.SimpleHandler;

/** A command line interface for the jacc parser generator.
 */
public class CommandLine {
    /** Main entry point: parse command line arguments, then create
     *  and run a jacc parser generator job.
     */
    public static void main(String[] args) {
        NameList    inputs     = null;
        String      suffix     = ".jacc";
        Settings    settings   = new Settings();
        boolean     wantParser = true;
        boolean     wantTokens = true;
        boolean     wantText   = false;
        boolean     wantHTML   = false;
        boolean     wantDot    = false;
        boolean     wantFirst  = false;
        NameList    errFiles   = null;
        NameList    runFiles   = null;
        boolean     wantStates = false;
        PrintWriter out        = new PrintWriter(System.out, true);

        for (int i=0; i<args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-")) {
                if (arg.length()==1) {
                    usage("Missing command line options");
                }
                for (int j=1; j<arg.length(); j++) {
                    switch (arg.charAt(j)) {
                        case 'f':
                            wantFirst  = true;
                            break;
                        case 'p':
                            wantParser = false;
                            break;
                        case 't':
                            wantTokens = false;
                            break;
                        case 'v':
                            wantText   = true;
                            break;
                        case 'h':
                            wantHTML   = true;
                            break;
                        case 'd':
                            wantDot    = true;
                            break;
                        case '0':
                            settings.setMachineType(Settings.LR0);
                            break;
                        case 's':
                            settings.setMachineType(Settings.SLR1);
                            break;
                        case 'a':
                            settings.setMachineType(Settings.LALR1);
                            break;
                        case 'e':
                            if (i+1>=args.length) {
                                usage("Missing filename for -e option");
                            }
                            errFiles = new NameList(args[++i], errFiles);
                            break;
                        case 'r':
                            if (i+1>=args.length) {
                                usage("Missing filename for -r option");
                            }
                            runFiles = new NameList(args[++i], runFiles);
                            break;
                        case 'n':
                            wantStates = true;
                            break;
                        default:
                            usage("Unrecognized command line option "+
                                  arg.charAt(j));
                    }
                }
            } else if (!arg.endsWith(suffix)) {
                usage("Input file must have \"" + suffix + "\" suffix");
            } else {
                inputs = new NameList(arg, inputs);
            }
        }

        if (inputs==null) {
            usage("No input file(s) specified");
        }

        Handler handler   = new SimpleHandler();
        String  firstName = inputs.getFirst();
        int     n         = 1 + Math.max(firstName.lastIndexOf('\\'),
                                         firstName.lastIndexOf('/'));
        String  prefix    = firstName.substring(0,n);
        String  name      = firstName
                             .substring(n, firstName.length()-suffix.length());
        final JaccJob job = new JaccJob(handler, out, settings);
        NameList.visit(inputs, new NameList.Visitor() {
            void visit(String name) { job.parseGrammarFile(name); }
        });
        job.buildTables();
        settings.fillBlanks(name);
        NameList.visit(errFiles, new NameList.Visitor() {
            void visit(String name) { job.readErrorExamples(name); }
        });

        if (handler.getNumFailures()>0) {
            return;
        }
        if (wantParser) {
            new ParserOutput(handler, job)
             .write(prefix + settings.getClassName() + ".java");
        }
        if (wantTokens) {
            new TokensOutput(handler, job)
             .write(prefix + settings.getInterfaceName() + ".java");
        }
        if (wantText) {
            new TextOutput(handler, job, wantFirst)
             .write(prefix + name + ".output");
        }
        if (wantHTML) {
            new HTMLOutput(handler, job, wantFirst)
             .write(prefix + name + "Machine.html");
        }
        if (wantDot) {
            new DotOutput(handler, job)
             .write(prefix + name + ".dot");
        }
        final boolean showState = wantStates;
        NameList.visit(runFiles, new NameList.Visitor() {
            void visit(String name) { job.readRunExample(name, showState); }
        });
    }

    /** Display message describing the format of command line arguments.
     */
    private static void usage(String msg) {
        System.err.println(msg);
        System.err.println("usage: jacc [options] file.jacc ...");
 
        System.err.println("options (individually, or in combination):");
        System.err.println(" -p        do not generate parser");
        System.err.println(" -t        do not generate token specification");
        System.err.println(" -v        output text description of machine");
        System.err.println(" -h        output HTML description of machine");
        System.err.println(" -d        output dot description of machine");
        System.err.println(" -f        show first/follow sets (with -h or -v)");
        System.err.println(" -a        treat as LALR(1) grammar (default)");
        System.err.println(" -s        treat as SLR(1) grammar");
        System.err.println(" -0        treat as LR(0) grammar");
        System.err.println(" -r file   run parser on input in file");
        System.err.println(" -n        show state numbers in parser output");
        System.err.println(" -e file   read error cases from file");
        System.exit(1);
    }

    /** A simple linked list data structure for holding lists of
     *  file names.  The associated visitor will visit the names
     *  in the order they were added from first to last.
     */
    private static class NameList {
        public String   name;
        public NameList names;
        public NameList(String name, NameList names) {
            this.name  = name;
            this.names = names;
        }

        public abstract static class Visitor {
            abstract void visit(String name);
        }

        public static void visit(NameList ns, Visitor visitor) {
            if (ns!=null) {
                visit(ns.names, visitor);
                visitor.visit(ns.name);
            }
        }

        public String getFirst() {
            NameList ns = this;
            while (ns.names!=null) {
                ns = ns.names;
            }
            return ns.name;
        }
    }
}
