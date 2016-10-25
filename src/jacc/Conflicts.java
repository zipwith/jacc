// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc;

import jacc.grammar.Grammar;
import jacc.grammar.Machine;

/** A linked list structure for storing conflicts.
 */
public class Conflicts {
    /** Represents a shift/reduce error.
     */
    private final static int SR = 0;

    /** Represents a reduce/reduce error.
     */
    private final static int RR = 1;

    /** The type of this conflict, either SR or RR.
     */
    private int type;

    /** First argument for conflict.  If this is a SR conflict,
     *  then this value holds the state number of the shift,
     *  If this is an RR conflict, then this value holds the
     *  offset of the first reduction.
     */
    private int arg1;

    /** Second argument for conflict, holds the offset of another
     *  reduction.
     */
    private int arg2;

    /** Token symbol on which this conflict occurs.
     */
    private Grammar.Symbol sym;

    /** Link to rest of the conflicts in this list.
     */
    private Conflicts next;

    /** Construct a new conflict item.
     */
    private Conflicts(int type, int arg1, int arg2,
                      Grammar.Symbol sym, Conflicts next) {
        this.type = type;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.sym  = sym;
        this.next = next;
    }

    /** Construct a shift/reduce conflict.
     */
    public static Conflicts sr(int arg1, int arg2, Grammar.Symbol sym, Conflicts cs) {
        return append(cs, new Conflicts(SR, arg1, arg2, sym, null));
    }

    /** Construct a reduce/reduce conflict.
     */
    public static Conflicts rr(int arg1, int arg2, Grammar.Symbol sym, Conflicts cs) {
        return append(cs, new Conflicts(RR, arg1, arg2, sym, null));
    }

    /** Destructive append on two lists of conflicts.
     */
    private static Conflicts append(Conflicts cs, Conflicts cs1) {
        if (cs==null) {
            return cs1;
        } else {
            Conflicts prev = cs;
            while (prev.next!=null) {
                prev = prev.next;
            }
            prev.next = cs1;
            return cs;
        }
    }

    /** Produce a string that describes all the conflicts in a list.
     */
    public static String describe(Machine machine, int st, Conflicts cs) {
        if (cs==null) {
            return "";
        } else {
            StringBuffer buf = new StringBuffer();
            String       nl  = System.getProperty("line.separator","\n");
            for (; cs!=null; cs=cs.next) {
                buf.append(st);
                buf.append(": ");
                if (cs.type==SR) {
                    buf.append("shift/reduce conflict (");
                    if (cs.arg1<0) {
                        buf.append("$end");
                    } else {
                        buf.append("shift ");
                        buf.append(cs.arg1);
                    }
                    buf.append(" and red'n ");
                    buf.append(machine.reduceItem(st, cs.arg2).getSeqNo());
                } else {
                    buf.append("reduce/reduce conflict (red'ns ");
                    buf.append(machine.reduceItem(st, cs.arg1).getSeqNo());
                    buf.append(" and ");
                    buf.append(machine.reduceItem(st, cs.arg2).getSeqNo());
                }
                buf.append(") on ");
                buf.append(cs.sym.getName());
                buf.append(nl);
            }
            return buf.toString();
        }
    }
}
