// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Date;
import jacc.grammar.Grammar;
import jacc.grammar.Machine;
import compiler.Phase;
import compiler.Handler;
import compiler.Failure;

/** Base class for Jacc output routines.
 */
public abstract class Output extends Phase {
    protected JaccJob      job;
    protected Grammar      grammar;
    protected int          numTs;
    protected int          numNTs;
    protected int          numSyms;
    protected Machine      machine;
    protected int          numStates;
    protected JaccTables   tables;
    protected JaccResolver resolver;
    protected Settings     settings;

    /** Construct a new output routine.
     */
    protected Output(Handler handler, JaccJob job) {
        super(handler);
        this.job       = job;
        this.tables    = job.getTables();
        this.machine   = tables.getMachine();
        this.grammar   = machine.getGrammar();
        this.numTs     = grammar.getNumTs();
        this.numNTs    = grammar.getNumNTs();
        this.numSyms   = grammar.getNumSyms();
        this.numStates = machine.getNumStates();
        this.resolver  = job.getResolver();
        this.settings  = job.getSettings();
    }

    /** Create a file and write output.
     */
    public void write(String filename) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(filename));
            write(writer);
        } catch (IOException e) {
            report(new Failure("Cannot write to file \"" + filename + "\""));
        }
        if (writer!=null) {
            writer.close();
        }
    }

    /** Generate the required output.
     */
    public abstract void write(PrintWriter out);

    // Miscellaneous helper functions: ---------------------------------------

    /** Print an array of strings.
     */
    protected static void indent(PrintWriter out, int ind, String[] strs) {
        for (int i=0; i<strs.length; i++) {
            indent(out, ind, strs[i]);
        }
    }

    /** Print an indent at the beginning of a line.
     */
    protected static void indent(PrintWriter out, int ind) {
        for (int i=0; i<ind; i++) {
            out.print("    ");
        }
    }

    /** Print an indent, a string, and a newline.
     */
    protected static void indent(PrintWriter out, int ind, String line) {
        indent(out, ind);
        out.println(line);
    }

    /** Output a time/date stamp message
     */
    protected static void datestamp(PrintWriter out) {
        out.println("// Output created by jacc on " + new Date());
        out.println();
    }
}
