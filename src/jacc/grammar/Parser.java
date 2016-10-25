// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
//

package jacc.grammar;

/** Provides a parsing engine for a given grammar/machine/parse table.
 */
public class Parser {
    /** Parse tables for this machine.
     */
    private Tables tables;

    /** A stream of input tokens.
     */
    private int[] input;

    /** The underlying machine for this parser.
     */
    private Machine machine;

    /** The underlying grammar for this parser.
     */
    private Grammar grammar;

    /** Construct a machine for a given grammar.
     */
    public Parser(Tables tables, int[] input) {
        this.tables     = tables;
        this.input      = input;
        this.machine    = tables.getMachine();
        this.grammar    = machine.getGrammar();
    }

    /** Index of next token in input stream.
     */
    private int position = 0;

    /** Holds the most recently "read" token (if there is one).
     */
    private int currSymbol = (-1);

    /** A non-negative value indicates that we have just executed
     *  a reduce action on the corresponding non-terminal.
     */
    private int reducedNT = (-1);

    /** Holds the current parser stack.
     */
    private Stack stack = new Stack();

    /** Holds the current parser state.
     */
    private int state = 0;

    /** Return the current parser state.
     */
    public int getState() {
        return state;
    }

    /** Return the next input symbol.
     */
    public int getNextSymbol() {
        return (reducedNT>=0) ? reducedNT : currSymbol;
    }

    /** Define constants that are returned by the step() method to
     *  indicate the action that has been taken.  The parser will
     *  typically be invoked by calling step repeatedly until either
     *  either Parser.ACCEPT or Parser.ERROR are returned.
     */
    public final static int ACCEPT = 0;
    public final static int ERROR  = 1;
    public final static int SHIFT  = 2;
    public final static int GOTO   = 3;
    public final static int REDUCE = 4;

    /** Advance the parser by one step, returning a tag to indicate
     *  the type of action that was taken.
     */
    public int step() {
        if (state<0) {
            return ACCEPT;
        }

        if (reducedNT>=0) {
            shift(reducedNT);
            if (!gotoState(reducedNT)) {
                return ERROR; // shouldn't occur if machine is correct
            }
            reducedNT = (-1);
            return GOTO;
        }

        if (currSymbol<0) {
            currSymbol = (position>=input.length) ? grammar.getNumSyms()-1
                                                  : input[position++];
        }

        if (grammar.isNonterminal(currSymbol)) {
            shift(currSymbol);
            if (!gotoState(currSymbol)) {
                return ERROR; // could occur for nonterminal in input stream
            }
            currSymbol = (-1);
            return GOTO;
        } else {
            byte[] action = tables.getActionAt(state);
            int[]  arg    = tables.getArgAt(state);
            int    t      = currSymbol - grammar.getNumNTs();
            switch (action[t]) {
                case Tables.SHIFT: {
                    if (arg[t]<0) {
                        return ACCEPT;
                    }
                    shift(currSymbol);
                    currSymbol = (-1);
                    state      = arg[t];
                    return SHIFT;
                }
 
                case Tables.REDUCE: {
                    reduce(arg[t]);
                    return REDUCE;
                }
            }
        }
        return ERROR;
    }

    /** Shift a particular symbol onto the parse stack.
     */
    private void shift(int symbol) {
        stack = stack.push(state, symbol);
    }

    /** Perform a reduce action, matching items on the top of the
     *  stack with the right hand of a particular item, and then
     *  preparing to execute a GOTO for the corresponding left hand
     *  side on the next step.
     */
    private void reduce(int arg) {
        LR0Items.Item it = machine.reduceItem(state, arg);
        int           n  = it.getProd().getRhs().length;
        if (n>0) {
            for (; n>1; n--) {
                stack = stack.pop();
            }
            state = stack.getState();
            stack = stack.pop();
        }
        reducedNT = it.getLhs();
    }

    /** Attempt to execute a goto action on a particular symbol
     *  in the current state.  Return a boolean to indicate if a
     *  suitable goto was found.
     */
    private boolean gotoState(int symbol) {
        int[] ts = machine.getGotosAt(state);
        for (int i=0; i<ts.length; i++) {
            if (symbol==machine.getEntry(ts[i])) {
                state = ts[i];
                return true;
            }
        }
        return false;
    }

    /** Display the current parser state.
     */
    public void display(java.io.PrintWriter out, boolean showState) {
        this.stack.display(out, grammar, showState);
        if (showState) {
            out.print(state);
            out.print(" ");
        }
        out.print("_ ");
        if (reducedNT>=0) {
            out.print(grammar.getSymbol(reducedNT).toString());
            out.print(" ");
        }
        if (currSymbol>=0) {
            out.print(grammar.getSymbol(currSymbol).toString());
            if (position<input.length) {
                out.print(" ...");
            }
        } else if (position<input.length) {
            out.print(grammar.getSymbol(input[position]).toString());
            out.print(" ...");
        }
        out.println();
    }

    /** Provides a representation for the parser's stack.
     */
    public static class Stack {
        private int   state;
        private int   symbol;
        private Stack up, down;

        public Stack() {
            this(null);
        }

        private Stack(Stack down) {
            this.down = down;
            this.up   = null;
        }

        public boolean empty() {
            return (down==null);
        }

        public int getState() {
            return state;
        }

        public int getSymbol() {
            return symbol;
        }

        public Stack pop() {
            return down;
        }

        Stack push(int state, int symbol) {
            Stack s = this.up;
            if (s==null) {
                s = this.up = new Stack(this);
            }
            s.state  = state;
            s.symbol = symbol;
            return s;
        }

        public void display(java.io.PrintWriter out,
                            Grammar grammar, boolean showState) {
            if (this.down!=null) {
                this.down.display(out, grammar, showState);
                if (showState) {
                    out.print(this.state);
                    out.print(" ");
                }
                out.print(grammar.getSymbol(this.symbol).toString());
                out.print(" ");
            }
        }
    }
}
