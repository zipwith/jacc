// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc;

import jacc.grammar.Grammar;

/** Represents a symbol in a jacc grammar.
 */
public class JaccSymbol extends Grammar.Symbol {
    /** Construct a JaccSymbol object for a literal.
     */
    public JaccSymbol(String name, int num) {
        super(name);
        this.num = num;
    }

    /** Construct a JaccSymbol object for an identifier.
     */
    public JaccSymbol(String name) {
        this(name, -1);
    }

    //---------------------------------------------------------------------
    // Properties dealing with symbol number.  (i.e., the code used by the
    // lexer to represent this particular symbol).

    private int num;

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        if (this.num<0) {
            this.num = num;
        }
    }

    //---------------------------------------------------------------------
    // Properties dealing with token number.  (i.e., the number of this
    // token within the list of all Jacc tokens.)

    private int tokenNo = (-1);

    public int getTokenNo() {
        return tokenNo;
    }

    public void setTokenNo(int tokenNo) {
        if (this.tokenNo<0) {
            this.tokenNo = tokenNo;
        }
    }

    //---------------------------------------------------------------------
    // Properties dealing with productions.

    private JaccProd[] jaccProds = null;
    private int        pused     = 0;

    public void addProduction(JaccProd prod) {
        if (jaccProds==null) {
            jaccProds = new JaccProd[1];
        } else if (pused>=jaccProds.length) {
            JaccProd[] newprods = new JaccProd[2*jaccProds.length];
            for (int i=0; i<jaccProds.length; i++) {
                newprods[i] = jaccProds[i];
            }
            jaccProds = newprods;
        }
        jaccProds[pused++] = prod;
    }

    public JaccProd[] getProds() {
        JaccProd[] prods = new JaccProd[pused];
        for (int i=0; i<pused; i++) {
            prods[i] = jaccProds[i];
            prods[i].fixup();
        }
        return prods;
    }

    //---------------------------------------------------------------------
    // Properties dealing with fixity:

    /** Holds the fixity of this symbol.
     */
    public Fixity fixity;

    /** Set the fixity details for this symbol.
     *
     * @param  fixity the intended fixity for this symbol; must not be
     *         null.
     * @return true if the fixity was set.  A false indicates that
     *         the value conflicts with a previous setting, which was
     *         not changed.
     */
    public boolean setFixity(Fixity fixity) {
        if (this.fixity==null) {
            this.fixity = fixity;
            return true;
        } else {
            return fixity.equalsFixity(this.fixity);
        }
    }

    /** Get the fixity (if any) of this symbol.
     */
    public Fixity getFixity() {
        return fixity;
    }

    //---------------------------------------------------------------------
    // Properties dealing with type:

    /** Holds the name of the type of semantic values corresponding to
     *  this symbol.
     */
    private String type;

    /** Set the type details for this symbol.
     * 
     * @param  type the required type for this symbol; must not be null.
     * @return true if the type was set.  A false indicates that
     *         the value conflicts with a previous setting, which
     *         was not changed.
     */
    public boolean setType(String type) {
        if (this.type==null) {
            this.type = type;
            return true;
        } else {
            return type.compareTo(this.type)==0;
        }
    }

    /** Return the type (if any) that has been set for this symbol.
     */
    public String getType() {
        return type;
    }
}
