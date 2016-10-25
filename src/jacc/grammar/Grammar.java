// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc.grammar;

import jacc.util.SCC;
import jacc.util.BitSet;
import jacc.util.Interator;

/** A representation for context free grammars.
 */
public class Grammar {
    /** Representation for symbols.  Subclass to add types, precedences,
     *  line nos, etc.
     */
    public static class Symbol {
        protected String name;
        public Symbol(String name) {
            this.name = name;
        }
        public String getName() {
            return name;
        }
        public String toString() {
            return name;
        }
    }

    /** Representation for productions.  Subclass to add actions,
     *  precedences, etc.
     */
    public static class Prod {
        protected int[] rhs;
        private   int   seqNo;
        public Prod(int[] rhs, int seqNo) {
            this.rhs   = rhs;
            this.seqNo = seqNo;
        }
        public int[] getRhs() {
            return rhs;
        }
        public int getSeqNo() {
            return seqNo;
        }
        public String getLabel() {
            return null;
        }
    }

    /** The set of symbols in this grammar.  The 0th entry is the start
     *  symbol, and the last entry is the endmarker.  Nonterminals occupy
     *  the initial portion of the array, while terminals go in the tail
     *  portion.
     */
    private Symbol[]  symbols;

    /** The set of productions for this grammar.
     */
    private Prod[][]  prods;

    /** Constructor for a grammar object; raises an exception if invalid
     *  parameter values are passed in.
     */
    public Grammar(Symbol[] symbols, Prod[][] prods)
      throws Exception {
        validate(symbols, prods);
        this.symbols = symbols;
        numSyms      = symbols.length;
        this.prods   = prods;
        numNTs       = prods.length;
        numTs        = numSyms - numNTs;
        calcDepends();
        comps        = SCC.get(depends, revdeps,numNTs);
    }

    /** Records the total number of symbols for this grammar.
     */
    private int numSyms;

    /** Records the number of nonterminals for this grammar.
     */
    private int numNTs;

    /** Records the number of terminals for this grammar.
     */
    private int numTs;

    /** Records the strongly connected components that are induced by
     *  dependency relation on nonterminals in this grammar.
     */
    private int[][]   comps;

    /** Get the total number of symbols in this grammar including
     *  both terminals and nonterminals.
     */
    public int getNumSyms() {
        return numSyms;
    }

    /** Get the total number of nonterminal symbols in this grammar.
     */
    public int getNumNTs() {
        return numNTs;
    }

    /** Get the total number of terminal symbols in this grammar.
     */
    public int getNumTs() {
        return numTs;
    }

    /** Get the symbol object corresponding to a particular index.
     */
    public Symbol getSymbol(int i) {
        return symbols[i];
    }

    /** Get the start symbol for this grammar.
     */
    public Symbol getStart() {
        return symbols[0];
    }

    /** Get the end symbol for this grammar.
     */
    public Symbol getEnd() {
        return symbols[numSyms-1];
    }

    /** Get the nonterminal object corresponding to a particular index.
     */
    public Symbol getNonterminal(int i) {
        return symbols[i];
    }

    /** Get the terminal object corresponding to a particular index.
     */
    public Symbol getTerminal(int i) {
        return symbols[numNTs+i];
    }

    /** Determine whether a given index represents a nonterminal symbol.
     */
    public boolean isNonterminal(int n) {
        return 0<=n && n<numNTs;
    }

    /** Determine whether a given index represents a terminal symbol.
     */
    public boolean isTerminal(int n) {
        return numNTs<=n && n<numSyms;
    }

    /** Return the total number of productions in this grammar.
     */
    public int getNumProds() {
        int tot = 0;
        for (int i=0; i<prods.length; i++) {
            tot += prods[i].length;
        }
        return tot;
    }

    /** Get productions for a given nonterminal index in this grammar.
     */
    public Prod[] getProds(int i) {
        return prods[i];
    }

    /** Return a description of the strongly connected components in
     *  this grammar, as determined by dependencies between nonterminal
     *  productions.
     */
    public int[][] getComponents() {
        return comps;
    }

    /** Validate a given set of symbols and productions.  This function
     *  allows a user to test a potential set of arguments for the Grammar
     *  constructor without actually attempting to build the grammar.
     */
    public static void validate(Symbol[] symbols, Prod[][] prods)
      throws Exception {
        //-----------------------------------------------------------------
        // Validate symbols:

        if (symbols==null || symbols.length==0) {
            throw new Exception("No symbols specified");
        }
        for (int i=0; i<symbols.length; i++) {
            if (symbols[i]==null) {
                throw new Exception("Symbol " + i + " is null");
            }
        }
        int numSyms = symbols.length;

        //-----------------------------------------------------------------
        // Validate productions:

        if (prods==null || prods.length==0) {
            throw new Exception("No nonterminals specified");
        }
        if (prods.length>numSyms) {
            throw new Exception("To many nonterminals specified");
        }
        if (prods.length==numSyms) {
            throw new Exception("No terminals specified");
        }
        for (int i=0; i<prods.length; i++) {
            if (prods[i]==null || prods[i].length==0) {
                throw new Exception("Nonterminal " + symbols[i] +
                                    " (number " + i + ") has no productions");
            }
            for (int j=0; j<prods[i].length; j++) {
                int[] rhs = prods[i][j].getRhs();
                if (rhs==null) {
                    throw new Exception("Production " +
                                        j + " for symbol " + symbols[i] +
                                        " (number " + i + ") is null");
                }
                for (int k=0; k<rhs.length; k++) {
                    if (rhs[k]<0 || rhs[k]>=numSyms-1) {
                        throw new Exception("Out of range symbol " + rhs[k] +
                                            " in production " + j +
                                            " for symbol " + symbols[i] +
                                            " (number " + i + ")");
                    }
                }
            }
        }
    }

    //---------------------------------------------------------------------
    // Dependency calculations:

    /** depends[i] is an array of the NTs that appear in rhs for NT i.
     */
    private int[][] depends;

    /** revdeps[i] is an array of the NTs whose rhs contain a ref to NT i.
     */
    private int[][] revdeps;

    /** Calculate the dependencies between nonterminal symbols in the
     *  grammar.
     */
    private void calcDepends() {
        int[][] deps = new int[numNTs][];
        int[]   nts  = BitSet.make(numNTs);
        depends      = new int[numNTs][];

        for (int i=0; i<numNTs; i++) {
            deps[i] = BitSet.make(numNTs);
        }
        for (int i=0; i<numNTs; i++) {
            BitSet.clear(nts);
            for (int j=0; j<prods[i].length; j++) {
                int[] rhs = prods[i][j].getRhs();
                for (int k=0; k<rhs.length; k++) {
                    if (isNonterminal(rhs[k])) {
                        BitSet.set(deps[rhs[k]], i);
                        BitSet.set(nts, rhs[k]);
                    }
                }
            }
            depends[i] = BitSet.members(nts);
        }

        revdeps = new int[numNTs][];
        for (int i=0; i<numNTs; i++) {
            revdeps[i] = BitSet.members(deps[i]);
        }
    }

    //---------------------------------------------------------------------
    // Cache points for standard analyses:

    /** Holds a nullable analyis, if one has been requested.
     */
    private Nullable nullable;

    /** Return a nullable analysis for this grammar.
     */
    public Nullable getNullable() {
        if (nullable==null) {
            nullable = new Nullable(this);
        }
        return nullable;
    }

    /** Holds a finitary analyis, if one has been requested.
     */
    private Finitary finitary;

    /** Return a finitary analysis for this grammar.
     */
    public Finitary getFinitary() {
        if (finitary==null) {
            finitary = new Finitary(this);
        }
        return finitary;
    }

    /** Holds a left analyis, if one has been requested.
     */
    private Left left;

    /** Return a left set analysis for this grammar.
     */
    public Left getLeft() {
        if (left==null) {
            left = new Left(this);
        }
        return left;
    }

    /** Holds a first set analyis, if one has been requested.
     */
    private First first;

    /** Return a first set analysis for this grammar.
     */
    public First getFirst() {
        if (first==null) {
            first = new First(this, getNullable());
        }
        return first;
    }

    /** Holds a follow set analyis, if one has been requested.
     */
    private Follow follow;

    /** Return a follow set analysis for this grammar.
     */
    public Follow getFollow() {
        if (follow==null) {
            follow = new Follow(this, getNullable(), getFirst());
        }
        return follow;
    }

    //---------------------------------------------------------------------
    // Display utilities:

    /** Output the grammar for the purposes of debugging and inspection.
     */
    public void display(java.io.PrintWriter out) {
        for (int i=0; i<numNTs; i++) {
            out.println(symbols[i].getName());
            String punc = " = ";
            for (int j=0; j<prods[i].length; j++) {
                int[] rhs = prods[i][j].getRhs();
                out.print(punc);
                out.print(displaySymbols(rhs, "/* empty */", " "));
                out.println();
                punc = " | ";
            }
            out.println(" ;");
        }
    }

    /** Output the results of any analyses that have been performed on
     *  this grammar for debugging or inspection.
     */
    public void displayAnalyses(java.io.PrintWriter out) {
        if (nullable==null) {
            out.println("No nullable analysis");
        } else {
            nullable.display(out);
        }
        if (finitary==null) {
            out.println("No finitary analysis");
        } else {
            finitary.display(out);
        }
        if (left==null) {
            out.println("No left analysis");
        } else {
            left.display(out);
        }
        if (first==null) {
            out.println("No first analysis");
        } else {
            first.display(out);
        }
        if (follow==null) {
            out.println("No follow analysis");
        } else {
            follow.display(out);
        }
    }

    /** Output dependency information for this grammar for debugging and
     *  inspection.
     */
    public void displayDepends(java.io.PrintWriter out) {
        out.println("Dependency information:");
        for (int i=0; i<numNTs; i++) {
            out.print(" " + symbols[i] + ": calls {");
            out.print(displaySymbols(depends[i],"",", "));
            out.print("}, called from {");
            out.print(displaySymbols(revdeps[i],"",", "));
            out.println("}");
        }
    }

    /** Output a sequence of symbols from an array.
     */
    public String displaySymbols(int[] syms, String empty, String between) {
        return displaySymbols(syms, 0, syms.length, empty, between);
    }

    /** Output a sequence of symbols from a section of an array.
     */
    public String displaySymbols(int[] syms, int lo, int hi,
                                 String empty, String between) {
        if (syms==null || lo>=hi) {
            return empty;
        } else {
            StringBuffer buf = new StringBuffer();
            buf.append(symbols[syms[lo]].getName());
            for (int k=lo+1; k<hi; k++) {
                buf.append(between);
                buf.append(symbols[syms[k]].getName());
            }
            return buf.toString();
        }
    }

    /** Output a set of symbols from a bitset.
     */
    public String displaySymbolSet(int[] s, int offset) {
        StringBuffer buf = new StringBuffer();
        int count        = 0;
        Interator mems   = BitSet.interator(s, offset);
        while (mems.hasNext()) {
            if (count++ != 0) {
                buf.append(", ");
            }
            buf.append(symbols[mems.next()].getName());
        }
        return buf.toString();
    }
}
