// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc;

public class Fixity {
    /** Specifies a left associative operator.  If <code>+</code> is
     *  a left associative operator, then <code>a+b+c</code> should
     *  be parsed as <code>(a+b)+c</code>.
     */
    public static final int LEFT   = 1;

    /** Specifies a nonassociative operator.  If <code>+</code> is
     *  a nonassociative operator, then <code>a+b+c</code> should
     *  be treated as an error (ambiguous use of operators).
     */
    public static final int NONASS = 2;

    /** Specifies a right associative operator.  If <code>+</code> is
     *  a right associative operator, then <code>a+b+c</code> should
     *  be parsed as <code>a+(b+c)</code>.
     */
    public static final int RIGHT  = 3;

    /** Records an associativity value (LEFT, NONASS, or RIGHT).
     */
    private int assoc;

    /** Records a precedence value.
     */
    private int prec;

    /** Construct a fixity object with specified precedence and
     *  associativity.  The constructor is made private to ensure
     *  that the only ways of constructing a Fixity are by using one
     *  of the factory methods, <code>left</code>, <code>right</code>,
     *  or <code>nonass</code>.  As a result, we ensure that the
     *  <code>assoc</code> field will always be valid 
     *
     *  @param assoc should be one of LEFT, NONASS, or RIGHT.
     *  @param prec  precedence level; any integer values will
     *               do, but higher integer values always indicate
     *               higher precedences.
     */
    private Fixity(int assoc, int prec) {
        this.assoc = assoc;
        this.prec  = prec;
    }

    /** Construct a fixity for a left associative operator.
     */
    public static Fixity left(int prec) {
        return new Fixity(LEFT, prec);
    }

    /** Construct a fixity for a nonassociative operator.
     */
    public static Fixity nonass(int prec) {
        return new Fixity(NONASS,prec);
    }

    /** Construct a fixity for a right associative operator.
     */
    public static Fixity right(int prec) {
        return new Fixity(RIGHT,prec);
    }

    /** Return the associativity property of this fixity object.
     */
    public int getAssoc() {
        return assoc;
    }

    /** Return the precedence of this fixity object.
     */
    public int getPrec() {
        return prec;
    }

    /** Use precedences to decide which of two operators should be
     *  applied first.  If possible, we apply the operator with the
     *  highest precedence first.  If the two operators have the
     *  same precedence, and are both left assoc (resp. right assoc)
     *  then we choose the left (resp. right) one first.  If all else
     *  fails, we determine that the use of the operators together is
     *  ambiguous.
     *
     *  @param  l the fixity of the left operator.
     *  @param  r the fixity of the right operator.
     *  @return one of:
     *          <ul>
     *          <li> <code>LEFT</code>, meaning that the left operator
     *               should be applied first.
     *          <li> <code>RIGHT</code>, meaning that the right operator
     *               should be applied first.
     *          <li> <code>NONASS</code>, meaning that the expression is
     *               ambiguous.
     *          </ul>
     */
    public static int which(Fixity l, Fixity r) {
        if (l!=null && r!=null) {
            if (l.prec > r.prec) {
                return LEFT;
            } else if (l.prec < r.prec) {
                return RIGHT;
            } else if (l.assoc==LEFT && r.assoc==LEFT) {
                return LEFT;
            } else if (l.assoc==RIGHT && r.assoc==RIGHT) {
                return RIGHT;
            }
        }
        return NONASS;
    }

    /** Test to see whether fixity objects are the same.
     */
    public boolean equalsFixity(Fixity that) {
        return (this.assoc==that.assoc) && (this.prec==that.prec);
    }

    /** Standard equality test, comparing this fixity against
     *  another object.
     */
    public boolean equals(Object o) {
        if (o instanceof Fixity) {
            return equalsFixity((Fixity)o);
        }
        return false;
    }
}
