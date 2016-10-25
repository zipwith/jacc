// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc;

import compiler.Diagnostic;

import jacc.grammar.Grammar;
import jacc.grammar.LookaheadMachine;
import jacc.grammar.Resolver;
import jacc.grammar.Tables;

/** Represents parse tables used by jacc.  Extends the basic
 *  Tables class from the grammar package with calculation of
 *  extra information that is useful for printing/optimizing
 *  the generated machine.
 */
public class JaccTables extends Tables {
    /** Construct a set of parse tables using lookahead information for
     *  the given machine.
     */
    public JaccTables(LookaheadMachine machine, Resolver resolver) {
        super(machine, resolver);
    }

    /** Holds a table of error message strings that have been defined
     *  for this job.
     */
    private String[] errors    = null;
    private int      numErrors = 0;

    /** Return the number of custom error strings that are
     *  defined in these tables.
     */
    public int getNumErrors() {
        return numErrors;
    }

    /** Return a particular entry from the table of error
     *  strings that are associated with this state.  We
     *  assume/require that 0 <= i < numErrors.
     */
    public String getError(int i) {
        return errors[i];
    }

    /** Return a boolean to indicate whether there is an error entry
     *  in the tables for a specified state and symbol.
     */
    public boolean errorAt(int state, int sym) {
        return action[state][sym-numNTs]==NONE;
    }

    /** Assign a string to the error entry in the table at the specified
     *  state and symbol.  We assume that this method will only be called
     *  if errorAt(state, sym) is true.  If a string has already been
     *  assigned to this entry, then the old string is returned and no
     *  further action is taken.  Otherwise, we return null, indicating
     *  that the operation was successful.
     *
     *  Arg values stored in the table are one greater than the
     *  corresponding index in the errors[] array.  This allows us to
     *  use strictly positive integers to indicate a special error
     *  code, leaving 0 to represtent the default.
     */
    public String errorSet(int state, int sym, String tag) {
        if (arg[state][sym-numNTs]!=0) {
            return errors[arg[state][sym-numNTs]-1];
        } else {
            arg[state][sym-numNTs] = errorNo(tag) + 1;
            return null;
        }
    }

    /** Look up (and possibly) add an error message string to the table
     *  of error messages that have been defined for this job.
     */
    private int errorNo(String tag) {
        for (int i=0; i<numErrors; i++) {
            if (errors[i].equals(tag)) {
                return i;
            }
        }
        String[] newErrors = new String[(numErrors==0) ? 1 : (2*numErrors)];
        for (int j=0; j<numErrors; j++) {
            newErrors[j] = errors[j];
        }
        errors = newErrors;
        errors[numErrors] = tag;
        return numErrors++;
    }

    /** Analyze the rows in the tables to determine a suitable
     *  index and defaults.
     */
    public void analyzeRows() {
        if (index==null) {
            RowAnalysis r   = new RowAnalysis();
            int numStates   = machine.getNumStates();
            this.index      = new int[numStates][];
            this.defaultRow = new int[numStates];
            for (int i=0; i<numStates; i++) {
                r.analyze(i);
            }
        }
    }

    /** Holds an index for the rows in each state.  More
     *  specifically, index[st][i] is the index of the ith
     *  row in the (sorted) set of rows for parser state st.
     */
    private int[][] index;

    /** Return the preferred index for the rows in state st.
     */
    public int[] indexAt(int st) {
        return index[st];
    }

    /** Holds the number of a row that could be used as a default
     *  (or -1 if no suitable default is found).  The row containing
     *  the most commonly appearing reduction is used as the default.
     */
    private int[] defaultRow;

    /** Return the row to be used as a default (or -1 if no suitable
     *  default can be found).
     */
    public int getDefaultRowAt(int st) {
        return defaultRow[st];
    }

    /** Display the tables for debugging or inspection.
     */
    public void display(java.io.PrintWriter out) {
        int numStates = machine.getNumStates();
        for (int st=0; st<numStates; st++) {
            System.out.print("state " + st + ": ");
            for (int i=0; i<numTs; i++) {
                switch (action[st][i]) {
                    case NONE   : out.print(" E");  break;
                    case SHIFT  : out.print(" S");  break;
                    case REDUCE : out.print(" R"); break;
                }
                out.print(arg[st][i]);
            }
            out.println();
        }
    }

    /** Ordering objects are used to determine a suitable index
     *  for the rows in a given state of the machine.  This requires
     *  us to sort the rows so that all occurences of any given
     *  action are grouped together.
     */
    private class RowAnalysis {
        private byte[] a;
        private int[]  b;
        private int    size;
        private int[]  idx;
 
        public void analyze(int state) {
            this.a    = action[state];
            this.b    = arg[state];
            this.size = numTs;
            this.idx  = new int[size];
            
            // Initialize the index array
            for (int i=0; i<numTs; i++) {
                idx[i] = i;
            }

            // Heap sort the rows ...
            for (int i=size/2; i>=0; i--) {
                heapify(i);
            }
            for (int i=size-1; i>0; i--) {
                int t  = idx[i];
                idx[i] = idx[0];
                idx[0] = t;
                size--;
                heapify(0);
            }
            index[state] = idx;

            // Determine which row to use as default
            defaultRow[state] = findDefault();
        }

        private void heapify(int i) {
            int m  = i;
            int im = idx[m];
            for (;;) {
                int l = 2*i+1;
                int r = l + 1;
                if (l<size) {
                    int il = idx[l];
                    if (a[il]>a[im] || (a[il]==a[im] && b[il]>b[im])) {
                        m  = l;
                        im = il;
                    }
                    if (r<size) {
                        int ir = idx[r];
                        if (a[ir]>a[im] || (a[ir]==a[im] && b[ir]>b[im])) {
                            m  = r;
                            im = ir;
                        }
                    }
                }
                if (m==i) {
                    return;
                }
                idx[m] = idx[i];
                idx[i] = im;
                i      = m;
                im     = idx[m];
            }
        }

        public int findDefault() {
            int best = 1;    // must repeat >=2 times to be used as default
            int def  = (-1); // negative value indicates no default in use
            for (int i=0; i<a.length; ) {
                int  ii = idx[i];
                int  ai = a[ii];
                if (ai==Tables.SHIFT) {  // skip shift entries
                    i++;
                } else {
                    int first = ii;
                    int count = 1;
                    int bi    = b[ii];
                    while (++i<a.length && a[idx[i]]==ai && b[idx[i]]==bi) {
                        count++;
                    }
                    if (count>best) {
                        def  = ii;
                        best = count;
                    }
                }
            }
            return def;
        }

        private void display() {
            for (int i=0; i<numTs; i++) {
                display(idx[i]);
            }
            System.out.println();
        }
        private void display(int i) {
            switch (a[i]) {
                case NONE   : System.out.print(" E");  break;
                case SHIFT  : System.out.print(" S");  break;
                case REDUCE : System.out.print(" R"); break;
            }
            System.out.print(b[i]);
        }
    }
}
