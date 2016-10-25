// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc;

import compiler.Phase;
import compiler.Failure;
import compiler.Warning;
import compiler.Handler;
import compiler.Position;
import jacc.grammar.Grammar;

/** Abstract parser for the jacc parser generator.  Implements lists
 *  of terminal and nonterminal symbols from which a grammar can be
 *  extracted.  These lists are populated by a concrete parser that
 *  extends this class with methods for reading the chosen concrete
 *  syntax.
 */
public abstract class JaccAbstractParser extends Phase {

    public JaccAbstractParser(Handler handler) {
        super(handler);
    }

    protected NamedJaccSymbols terminals = new NamedJaccSymbols();
    protected NamedJaccSymbols nonterms  = new NamedJaccSymbols();
    protected NumJaccSymbols   literals  = new NumJaccSymbols();
    protected JaccSymbol       start     = null;

    /** Return a Grammar object that represents the input grammar.
     */
    public Grammar getGrammar() {
        // Build an array of symbols, starting with the nonterminals,
        // then the terminal symbols, then any literals, and finally
        // the end marker $end.
        int numNTs = nonterms.getSize();
        int numTs  = terminals.getSize() + literals.getSize() + 1;
        if (numNTs==0 || start==null) {
            report(new Failure("No nonterminals defined"));
            return null;
        }
        JaccSymbol[] symbols = new JaccSymbol[numNTs+numTs];
        literals.fill(symbols,
                      terminals.fill(symbols,
                                     nonterms.fill(symbols,0)));
        symbols[numNTs+numTs-1] = new JaccSymbol("$end");

        // Assign each terminal a code to be used to represent that
        // particular token in lexer/parser communication.  The $end
        // symbol gets code 0, other numbers are allocated starting
        // from 1, but taking care to avoid clashes with codes already
        // assigned to literals.
        symbols[numNTs+numTs-1].setNum(0);
        int tokenCode = 1;
        for (int i=0; i<numTs-1; i++) {
            if (symbols[numNTs+i].getNum()<0) {
                while (literals.find(tokenCode)!=null) {
                    tokenCode++;
                }
                symbols[numNTs+i].setNum(tokenCode++);
            }
        }

        // Make sure that the start symbol is the first nonterminal in
        // the symbols array, swapping it with another element if
        // necessary.
        for (int i=0; i<numNTs; i++) {
            if (symbols[i]==start) {
                if (i>0) {
                    JaccSymbol temp = symbols[0];
                    symbols[0]      = symbols[i];
                    symbols[i]      = temp;
                }
                break;
            }
        }

        // Record the token number for each symbol.  These numbers will
        // be used to map the arrays of Symbol objects in each parsed
        // production into corresponding arrays of integers, as required
        // by the Grammar class.
        for (int i=0; i<symbols.length; i++) {
            symbols[i].setTokenNo(i);
        }

        // Build the productions for this grammar.
        JaccProd[][] prods  = new JaccProd[nonterms.getSize()][];
        boolean      failed = false;
        for (int i=0; i<prods.length; i++) {
            prods[i] = symbols[i].getProds();
            if (prods[i]==null || prods[i].length==0) {
                report(new Failure("No productions for " +
                                   symbols[i].getName()));
                failed = true;
            }
        }
        if (!failed) {
            try {
                return new Grammar(symbols, prods);
            } catch (Exception e) {
                report(new Failure("Internal problem " + e));
            }
        }
        return null;
    }
}

/** Stores a collection of JaccSymbol objects in a binary tree.
 *  This is an abstract class that does not provide any functions
 *  for constructing tree objects, nor does it mandate any particular
 *  ordering on data values, as might be expected in a binary search
 *  tree.  Individual subclasses are expected to provide the additional
 *  functionality that is needed in specific cases.
 */
abstract class JaccSymbols {
    protected Node root;
    protected int  size;

    protected JaccSymbols() {
        this.root = null;
        this.size = 0;
    }

    public int getSize() {
        return size;
    }

    protected static class Node {
        Node       left;
        JaccSymbol data;
        Node       right;
        Node(JaccSymbol data) {
            this.data = data;
        }
    }

    public int fill(JaccSymbol[] syms, int count) {
        return fill(syms,count,root);
    }

    private static int fill(JaccSymbol[] syms, int count, Node node) {
        if (node!=null) {
            count         = fill(syms, count, node.left);
            syms[count++] = node.data;
            count         = fill(syms, count, node.right);
        }
        return count;
    }
}

/** Represents a collection of symbols, extending JaccSymbols with
 *  the ability to add symbols using names as index values.
 */
class NamedJaccSymbols extends JaccSymbols {
    public JaccSymbol find(String name) {
        Node node = root;
        while (node!=null) {
            int cmp = name.compareTo(node.data.getName());
            if (cmp<0) {
                node = node.left;
            } else if (cmp>0) {
                node = node.right;
            } else {
                return node.data;
            }
        }
        return null;
    }

    public JaccSymbol findOrAdd(String name) {
        if (root==null) {
            JaccSymbol sym = new JaccSymbol(name);
            root           = new Node(sym);
            size++;
            return sym;
        } else {
            Node node = root;
            for (;;) {
                int cmp = name.compareTo(node.data.getName());
                if (cmp<0) {
                    if (node.left==null) {
                        JaccSymbol sym = new JaccSymbol(name);
                        node.left      = new Node(sym);
                        size++;
                        return sym;
                    } else {
                        node = node.left;
                    }
                } else if (cmp>0) {
                    if (node.right==null) {
                        JaccSymbol sym = new JaccSymbol(name);
                        node.right     = new Node(sym);
                        size++;
                        return sym;
                    } else {
                        node = node.right;
                    }
                } else {
                    return node.data;
                }
            }
        }
    }
}

/** Represents a collection of symbols, extending JaccSymbols with
 *  the ability to add symbols using numbers as index values.
 */
class NumJaccSymbols extends JaccSymbols {
    public JaccSymbol find(int num) {
        Node node = root;
        while (node!=null) {
            int key = node.data.getNum();
            if (num<key) {
                node = node.left;
            } else if (num>key) {
                node = node.right;
            } else {
                return node.data;
            }
        }
        return null;
    }

    public JaccSymbol findOrAdd(String name, int num) {
        if (root==null) {
            JaccSymbol sym = new JaccSymbol(name,num);
            root           = new Node(sym);
            size++;
            return sym;
        } else {
            Node node = root;
            for (;;) {
                int key = node.data.getNum();
                if (num<key) {
                    if (node.left==null) {
                        JaccSymbol sym = new JaccSymbol(name,num);
                        node.left      = new Node(sym);
                        size++;
                        return sym;
                    } else {
                        node = node.left;
                    }
                } else if (num>key) {
                    if (node.right==null) {
                        JaccSymbol sym = new JaccSymbol(name,num);
                        node.right     = new Node(sym);
                        size++;
                        return sym;
                    } else {
                        node = node.right;
                    }
                } else {
                    return node.data;
                }
            }
        }
    }
}
