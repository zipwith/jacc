// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc.grammar;

/** Used to build a set of LR0 items for a grammar, including two extra
 *  items that are used to describe the corresponding augmented grammar.
 */
public class LR0Items {
    /** Records the grammar for this set of items.
     */
    private Grammar grammar;

    /** Construct a set of items for a given grammar.
     */
    public LR0Items(Grammar grammar) {
        this.grammar = grammar;
        int numNTs   = grammar.getNumNTs();
        numItems     = 2;               // Number of special items
        firstKernel  = new int[numNTs][];
        for (int i=0; i<numNTs; i++) {
            Grammar.Prod[] prods = grammar.getProds(i);
            firstKernel[i]       = new int[prods.length];
            for (int j=0; j<prods.length; j++) {
                int len           = prods[j].getRhs().length;
                firstKernel[i][j] = numItems;
                numItems         += (len==0 ? 1 : len);
            }
        }
        items       = new Item[numItems];
        numItems    = 0;
        new Item(-1,0,0);                   // Represents S' -> _ S $
        new Item(-1,0,1);                   // Represents S' -> S _ $
        for (int i=0; i<numNTs; i++) {
            Grammar.Prod[] prods = grammar.getProds(i);
            for (int j=0; j<prods.length; j++) {
                int[] rhs = prods[j].getRhs();
                for (int k=1; k<rhs.length; k++) {
                    new Item(i, j, k);
                }
                new Item(i, j, rhs.length);
            }
        }
    }

    /** The total number of items in this set of items.
     */
    private int numItems;

    /** An array of item objects, one for each of the items in this set.
     */
    private Item[] items;

    /** Pointers to the first kernel item for each production in the
     *  grammar.
     */
    private int[][] firstKernel;

    /** Return the total number of items in this set.
     */
    public int getNumItems() {
        return numItems;
    }

    /** Return the item for a particular index value.
     */
    public Item getItem(int i) {
        return items[i];
    }

    /** Return the index of the item in which the parser should begin.
     */
    public int getStartItem() {
        return 0;
    }

    /** Return the index of the item in which we are ready to see $end.
     */
    public int getEndItem() {
        return 1;
    }

    /** Return the index of the first kernel item for a given production.
     */
    public int getFirstKernel(int symNo, int prodNo) {
        return firstKernel[symNo][prodNo];
    }

    /** Display all the items in this set for debugging or inspection.
     */
    public void displayAllItems(java.io.PrintWriter out) {
        out.println("Items:");
        for (int i=0; i<items.length; i++) {
            out.print(i + ": ");
            items[i].display(out);
            out.println();
        }
    }

    /** Provides a representation for the individual items in this set.
     */
    public class Item {
        private int itemNo;
        private int lhs;
        private int prodNo;
        private int pos;

        /** Construct an item.
         */
        private Item(int lhs, int prodNo, int pos) {
            this.itemNo       = numItems;
            this.lhs          = lhs;
            this.prodNo       = prodNo;
            this.pos          = pos;
            items[numItems++] = this;
        }

        /** Return the number for this item (that is, its index in the set).
         */
        public int getNo() {
            return itemNo;
        }

        /** Return the number of the nonterminal on the left hand side of
         *  the underlying production.
         */
        public int getLhs() {
            return lhs;
        }

        /** Return the index for this production in the array of
         *  productions that are associated with the corresponding
         *  left hand side.
         */
        public int getProdNo() {
            return prodNo;
        }

        /** Return the sequence number (with respect to the full grammar)
         *  for the production in this item.
         */
        public int getSeqNo() {
            return getProd().getSeqNo();
        }

        /** Return the body of the underlying production.
         */
        public Grammar.Prod getProd() {
            return grammar.getProds(lhs)[prodNo];
        }

        /** Return the position of the marker in the item.
         */
        public int getPos() {
            return pos;
        }

        /** Determine whether we can advance from this item by recognizing
         *  a symbol.  This occurs in the middle of a regular production
         *  or in the start production, S' -> _ S $ when we recognize S.
         *  We don't add a goto for the other special production (i.e.,
         *  S' -> S _ $) because we don't have a state to transition too.
         *  Nevertheless, we do need to account for the $ symbol elsewhere
         *  when we calculate lookaheads.
         */
        public boolean canGoto() {
            if (lhs<0) {
                return (pos==0);
            } else {
                return (pos!=getProd().getRhs().length);
            }
        }

        /** Determine if this is an item on which we can reduce.
         *  This is not quite a negation of canGoto() because of
         *  the special case treatment of S' -> S _ $.
         */
        public boolean canReduce() {
            return (lhs>=0) && (pos==getProd().getRhs().length);
        }

        /** Determine if this item can shift the $ end marker to accept.
         */
        public boolean canAccept() {
            return (lhs<0) && (pos==1);
        }

        /** Return the number of the next item that we can goto by
         *  advancing this item.  This method should only be called on
         *  items for which <code>canGoto()</code> returns
         *  <code>true</code>.
         */
        public int getNextItem() {
            if (lhs>=0) {
                return itemNo+1;
            } else {
                return 1;
            }
        }

        /** Return the number of the symbol that we must pass to
         *  advance this item.  This method should only be called on
         *  items for which <code>canGoto()</code> returns
         *  <code>true</code>.
         */
        public int getNextSym() {
            if (lhs>=0) {
                return grammar.getProds(lhs)[prodNo].getRhs()[pos];
            } else {
                return 0;                   // the start symbol
            }
        }

        /** Return a printable representation of this item.
         */
        public void display(java.io.PrintWriter out) {
            if (lhs<0) {
                if (pos==0) {
                    out.print("$accept : _" + grammar.getStart() +
                              " " + grammar.getEnd());
                } else {
                    out.print("$accept : " + grammar.getStart() +
                              "_" + grammar.getEnd());
                }
                return;
            }
            out.print(grammar.getSymbol(lhs));
            out.print(" : ");
            Grammar.Prod prod = grammar.getProds(lhs)[prodNo];
            int[]        rhs  = prod.getRhs();
            out.print(grammar.displaySymbols(rhs, 0, pos, "", " "));
            out.print("_");
            if (pos<rhs.length) {
                out.print(grammar.displaySymbols(rhs, pos, rhs.length, "", " "));
            }
            String label = prod.getLabel();
            if (label!=null) {
                out.print("    (");
                out.print(label);
                out.print(')');
            }
        }
    }
}
