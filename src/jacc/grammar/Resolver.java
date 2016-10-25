// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc.grammar;

/** A base class for objects that specify policies for resolving
 *  shift/reduce and reduce/reduce conflicts.
 */
public abstract class Resolver {
    /** Resolve a shift/reduce conflict.
     */
    public abstract void srResolve(Tables tables, int st, int tok, int redNo);

    /** Resolve a reduce/reduce conflict.
     */
    public abstract void rrResolve(Tables tables, int st, int tok, int redNo);
}
