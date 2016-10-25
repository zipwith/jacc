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

/** A parser for the jacc parser generator.  Constructs a Grammar and
 *  JaccOutputConfig object from the stream of tokens produced by a
 *  JaccLexer.
 */
public class JaccParser extends JaccAbstractParser implements JaccTokens {
    private Settings settings;

    public JaccParser(Handler handler, Settings settings) {
        super(handler);
        this.settings = settings;
    }

    private int seqNo = 1;
    private JaccLexer lexer;

    /** Top-level parser: Read a file and calculate a corresponding Grammar.
     */
    public void parse(JaccLexer lexer) {
        this.lexer = lexer;

        // Ensure that the "error" token is included in the terminal set.
        terminals.findOrAdd("error");

        // Parse the definition section at the beginning of the input.
        parseDefinitions();

        // Read the rules and any trailing parts of the input.
        if (lexer.getToken()!=MARK) {
            report(new Failure(lexer.getPos(),"Missing grammar"));
        } else {
            lexer.nextToken();
            parseGrammar();
            if (lexer.getToken()==MARK) {
                String line;
                while ((line=lexer.readWholeLine())!=null) {
                    settings.addPostText(line);
                    settings.addPostText("\n");
                }
            }
        }
        lexer.close();
    }

    /** Parse a sequence of symbols, both terminals and non-terminals.
     *  This method is intended to be used for reading "sample token"
     *  streams.
     */
    public int[] parseSymbols(JaccLexer lexer) {
        this.lexer   = lexer;
        SymList syms = null;
        for (;;) {
            JaccSymbol sym = parseDefinedSymbol();
            if (sym==null) {
                if (lexer.getToken()!=ENDINPUT) {
                    report(new Warning(lexer.getPos(),
                           "Ignoring extra tokens at end of input"));
                }
                lexer.close();
                return SymList.toIntArray(syms);
            }
            syms = new SymList(sym, syms);
            lexer.nextToken();
        }
    }

    /** Read a file of error examples, each comprising a string literal
     *  followed by a sequence of tokens.
     */
    public void parseErrorExamples(JaccLexer lexer, JaccJob job) {
        this.lexer = lexer;
        while (lexer.getToken()==STRLIT) {
            String tag = lexer.getLexeme();
            if (lexer.nextToken()==COLON) {
                lexer.nextToken();
            } else {
                report(new Warning(lexer.getPos(),
                                   "A colon was expected here"));
            }
            for (;;) {
                // parse and process an example
                Position   pos  = lexer.getPos();
                SymList    syms = null;
                JaccSymbol sym;
                while ((sym=parseDefinedSymbol())!=null) {
                    syms = new SymList(sym, syms);
                    lexer.nextToken();
                }
                int[] nums = SymList.toIntArray(syms);
                job.errorExample(pos, tag, nums);

                // look for next example (or the end of the input)
                int tok = lexer.getToken();
                if (tok==BAR) {
                    lexer.nextToken();
                } else if (tok==ENDINPUT) {
                    break;
                } else {
                    if (tok!=SEMI) {
                        report(new Failure(lexer.getPos(),
                          "Unexpected token; a semicolon was expected here"));
                        do {
                            tok = lexer.nextToken();
                        } while (tok!=SEMI && tok!=ENDINPUT);
                    }
                    if (tok==SEMI) {
                        lexer.nextToken();
                    }
                    break;
                }
            }
        }
        if (lexer.getToken()!=ENDINPUT) {
            report(new Failure(lexer.getPos(),
                     "Unexpected token; ignoring the rest of this file"));
        }
        lexer.close();
    }

    //---------------------------------------------------------------------
    // Read the definition part of an input file:

    private void parseDefinitions() {
        boolean errorReported = false;
        for (;;) {
            switch (lexer.getToken()) {
                case ENDINPUT :
                case MARK :
                    return;
                default:
                    if (parseDefinition()) {
                        errorReported = false;
                    } else {
                        if (!errorReported) {
                            errorReported = true;
                            report(new Failure(lexer.getPos(),
                                   "Syntax error in definition"));
                        }
                        lexer.nextToken();
                    }
            }
        }
    }

    private int precedence = 0;

    private boolean parseDefinition() {
        switch (lexer.getToken()) {
            case CODE :
                settings.addPreText(lexer.getLexeme());
                lexer.nextToken();
                return true;

            case TOKEN :
                parseTokenDefn();
                return true;

            case TYPE :
                parseTypeDefn();
                return true;

            case LEFT :
                parseFixityDefn(Fixity.left(precedence++));
                return true;

            case NONASSOC :
                parseFixityDefn(Fixity.nonass(precedence++));
                return true;

            case RIGHT :
                parseFixityDefn(Fixity.right(precedence++));
                return true;

            case START :
                parseStart();
                return true;

            case CLASS :
                settings.setClassName(
                    parseIdent(lexer.getLexeme(),
                        settings.getClassName()));
                return true;

            case INTERFACE :
                settings.setInterfaceName(
                    parseIdent(lexer.getLexeme(),
                        settings.getInterfaceName()));
                return true;

            case PACKAGE :
                settings.setPackageName(
                    parseDefnQualName(lexer.getLexeme(),
                        settings.getPackageName()));
                return true;

            case EXTENDS :
                settings.setExtendsName(
                    parseDefnQualName(lexer.getLexeme(),
                        settings.getExtendsName()));
                return true;

            case IMPLEMENTS : {
                    lexer.nextToken();
                    String qn = parseQualName();
                    if (qn!=null) {
                        settings.addImplementsNames(qn);
                    }
                }
                return true;

            case SEMANTIC :
                settings.setTypeName(
                    parseDefnQualName(lexer.getLexeme(),
                        settings.getTypeName()));
                if (lexer.getToken()==':') {
                    settings.setGetSemantic(lexer.readCodeLine());
                    lexer.nextToken();
                }
                return true;

            case GETTOKEN :
                settings.setGetToken(lexer.readCodeLine());
                lexer.nextToken();
                return true;

            case NEXTTOKEN :
                settings.setNextToken(lexer.readCodeLine());
                lexer.nextToken();
                return true;

            default:
                return false;
        }
    }

    // The repetition in parseStart(), parseIdent(), and parseDefnQualName()
    // is very annoying, but trying to avoid it by abstracting out a common
    // pattern introduces too many distractions.

    private void parseStart() {     // assumes token is %start
        Position pos = lexer.getPos();
        lexer.nextToken();
        JaccSymbol sym = parseNonterminal();
        if (sym==null) {
            report(new Failure(pos, "Missing start symbol"));
        } else {
            if (start==null) {
                start = sym;
            } else {
                report(new Failure(pos,
                       "Multiple %start definitions are not permitted"));
            }
            lexer.nextToken();
        }
    }

    private String parseIdent(String tag, String old) {
        Position pos = lexer.getPos();
        if (lexer.nextToken()!=IDENT) {
            report(new Failure(lexer.getPos(),
                   "Syntax error in %" + tag
                        + " directive; identifier expected"));
            return old;
        }
        String s = lexer.getLexeme();
        lexer.nextToken();
        if (s!=null && old!=null) {
            report(new Failure(pos,
                   "Multiple %" + tag + " definitions are not permitted"));
            s = old;
        }
        return s;
    }

    private String parseDefnQualName(String tag, String old) {
        Position pos = lexer.getPos();
        lexer.nextToken();
        String qn = parseQualName();
        if (qn!=null && old!=null) {
            report(new Failure(pos,
                   "Multiple %" + tag + " definitions are not permitted"));
            qn = old;
        }
        return qn;
    }

    // The repetition in parseTokenDefn(), parseTypeDefn(), and
    // parseFixityDefn() is very annoying, but trying to avoid it
    // by abstracting out a common pattern introduces too many
    // other distractions.

    private void parseTokenDefn() {
        Position pos  = lexer.getPos();
        String   type = optionalType();
        for (int count=0;; count++) {
            JaccSymbol sym = parseTerminal();
            if (sym==null) {
                if (count==0) {
                    report(new Failure(pos,
                           "Missing symbols in %token definition"));
                }
                return;
            }
            addType(sym, type);
            lexer.nextToken();
        }
    }

    private void parseTypeDefn() {
        Position pos  = lexer.getPos();
        String   type = optionalType();
        for (int count=0;; count++) {
            JaccSymbol sym = parseSymbol();
            if (sym==null) {
                if (count==0) {
                    report(new Failure(pos,
                           "Missing symbols in %type definition"));
                }
                return;
            }
            addType(sym, type);
            lexer.nextToken();
        }
    }

    private void parseFixityDefn(Fixity fixity) {
        Position pos  = lexer.getPos();
        String   type = optionalType();
        for (int count=0;; count++) {
            JaccSymbol sym = parseTerminal();
            if (sym==null) {
                if (count==0) {
                    report(new Failure(pos,
                           "Missing symbols in fixity definition"));
                }
                return;
            }
            addFixity(sym, fixity);
            addType(sym, type);
            lexer.nextToken();
        }
    }

    /** Parse an optional type argument, wrapped in angle brackets.
     */
    private String optionalType() {
        if (lexer.nextToken()==TOPEN) {
            lexer.nextToken();
            String name = parseQualName();
            while (lexer.getToken()==BOPEN) {
                if (lexer.nextToken()==BCLOSE) {
                    lexer.nextToken();
                    name += "[]";
                } else {
                    report(new Failure(lexer.getPos(),
                                       "Missing ']' in array type"));
                    break;
                }
            }
            if (lexer.getToken()==TCLOSE) {
                lexer.nextToken();
            } else if (name!=null) {
                report(new Failure(lexer.getPos(),
                       "Missing `>' in type specification"));
            }
            return name;
        }
        return null;
    }

    private void addFixity(JaccSymbol sym, Fixity fixity) {
        if (!sym.setFixity(fixity)) {
            report(new Warning(lexer.getPos(),
                   "Cannot change fixity for " + sym.getName()));
        }
    }

    private void addType(JaccSymbol sym, String type) {
        if (type!=null && !sym.setType(type)) {
            report(new Warning(lexer.getPos(),
                   "Cannot change type for " + sym.getName()));
        }
    }

    /** Parse the grammar part of a Jacc input file.
     */
    private void parseGrammar() {
        JaccSymbol lhs;
        while ((lhs=parseLhs())!=null) {
            if (start==null) {
                start = lhs;
            }
            lhs.addProduction(parseRhs());
            while (lexer.getToken()==BAR) {
                lexer.nextToken();
                lhs.addProduction(parseRhs());
            }
            if (lexer.getToken()==SEMI) {
                lexer.nextToken();
            } else {
                report(new Warning(lexer.getPos(),
                       "Missing ';' at end of rule"));
            }
        }
    }

    /** Parse the left hand side of a rule.  This function will return
     *  only when one of the following conditions occurs:
     *  <ul>
     *  <li> A nonterminal followed by a colon is read.  When parseLhs()
     *       returns, we will know that the lexer has been advanced past
     *       the colon, and that the return value specifies the JaccSymbol
     *       object for the nonterminal.
     *  <li> The MARK token (%%) is read, in which case parseLhs()
     *       returns null, without advancing past the MARK token.
     *  <li> The ENDINPUT token is read, in which case parseLhs()
     *       returns null.
     *  </ul>
     *  If none of these conditions hold at the beginning of the token
     *  stream that is passed to parseLhs(), then it skips tokens until
     *  one of the conditions does hold.  An error message will be
     *  produced, but we do not produce more than one error diagnostic
     *  for each call to parseLhs() to avoid cascading error messages.
     */
    private JaccSymbol parseLhs() {
        boolean errorReported = false;
        int     tok           = lexer.getToken();
        while (tok!=MARK && tok!=ENDINPUT) {
            JaccSymbol lhs = parseNonterminal();
            if (lhs==null) {
                if (!errorReported) {
                    if (parseTerminal()!=null) {
                        report(new Failure(lexer.getPos(),
                               "Terminal symbol used on left "+
                               "hand side of rule"));
                    } else {
                        report(new Failure(lexer.getPos(),
                               "Missing left hand side in rule"));
                    }
                    errorReported = true;
                }
                tok = lexer.nextToken();
            } else {
                tok = lexer.nextToken();
                if (tok!=COLON) {
                    if (!errorReported) {
                        report(new Failure(lexer.getPos(),
                               "Missing colon after left hand "+
                               "side of rule"));
                    }
                    errorReported = true;
                } else {
                    lexer.nextToken();
                    return lhs;
                }
            }
        }
        return null;
    }

    /** Parse the right hand side of a production, accepting precedence
     *  annotations and a final action.
     */
    private JaccProd parseRhs() {
        Fixity  fixity = null;
        SymList syms   = null;
        for (;;) {
            if (lexer.getToken()==PREC) {
                lexer.nextToken();
                JaccSymbol sym = parseSymbol();
                if (sym==null) {
                    report(new Failure(lexer.getPos(),
                           "Missing token for %prec directive"));
                } else if (sym.getFixity()==null) {
                    report(new Failure(lexer.getPos(),
                           "Ignoring %prec annotation because " +
                           "no fixity has been specified for "  +
                           sym.getName()));
                    lexer.nextToken();
                } else {
                    if (fixity!=null) {
                        report(new Warning(lexer.getPos(),
                               "Multiple %prec annotations in production"));
                    }
                    fixity = sym.getFixity();
                    lexer.nextToken();
                }
            } else {
                JaccSymbol sym = parseSymbol();
                if (sym==null) {
                    break;
                } else {
                    syms = new SymList(sym, syms);
                    lexer.nextToken();
                }
            }
        }

        String   action = null;
        Position actPos = null;
        if (lexer.getToken()==ACTION) {
            action = lexer.getLexeme();
            actPos = lexer.getPos();
            lexer.nextToken();
        }

        JaccSymbol[] prodSyms = SymList.toArray(syms);
        return new JaccProd(fixity, prodSyms, actPos, action, seqNo++);
    }

    /** Linked list of symbols, used to parse productions in parseRhs().
     */
    private static class SymList {
        JaccSymbol head;
        SymList    tail;
        SymList(JaccSymbol head, SymList tail) {
            this.head = head;
            this.tail = tail;
        }

        static int length(SymList syms) {
            int count = 0;
            for (; syms!=null; syms=syms.tail) {
                 count++;
            }
            return count;
        }

        static JaccSymbol[] toArray(SymList syms) {
            int          count    = length(syms);
            JaccSymbol[] symArray = new JaccSymbol[count];
            while (count>0) {
                symArray[--count] = syms.head;
                syms              = syms.tail;
            }
            return symArray;
        } 

        static int[] toIntArray(SymList syms) {
            int   count    = length(syms);
            int[] intArray = new int[count];
            while (count>0) {
                intArray[--count] = syms.head.getTokenNo();
                syms              = syms.tail;
            }
            return intArray;
        } 
    }

    /** Parse a qualified name.
     */
    private String parseQualName() {    // expects token to be IDENT
        if (lexer.getToken()!=IDENT) {
            report(new Failure(lexer.getPos(),
                   "Syntax error in qualified name; identifier expected"));
            return null;
        }

        StringBuffer buf = new StringBuffer();
        for (;;) {
            buf.append(lexer.getLexeme());
            if (lexer.nextToken()==DOT) {
                if (lexer.nextToken()!=IDENT) {
                    report(new Failure(lexer.getPos(),
                                       "Syntax error in qualified name"));
                    break;
                }
                buf.append('.');
            } else {
                break;
            }
        }
        return buf.toString();
    }

    /** Parse a symbol where a terminal is expected.  The symbol
     *  that we read must not have been defined previously as a
     *  nonterminal.
     *
     *  @return The terminal parsed, or null if none found.  Does
     *          not advance past the symbol that was read.
     */
    private JaccSymbol parseTerminal() {
        String lexeme = lexer.getLexeme();
        switch (lexer.getToken()) {
            case IDENT:
                if (nonterms.find(lexeme)!=null) {
                    return null;
                } else {
                    return terminals.findOrAdd(lexeme);
                }
            case CHARLIT:
                return literals.findOrAdd(lexeme, lexer.getLastLiteral());
            default:
                return null;
        }
    }

    /** Parse a symbol where a nonterminal is expected.  The symbol
     *  that we read must not have been defined previously as a terminal.
     *
     *  @return The nonterminal parsed, or null if none found.  Does
     *          not advance past the symbol that was read.
     */
    private JaccSymbol parseNonterminal() {
        String lexeme = lexer.getLexeme();
        switch (lexer.getToken()) {
            case IDENT:
                if (terminals.find(lexeme)!=null) {
                    return null;
                } else {
                    return nonterms.findOrAdd(lexeme);
                }
            default:
                return null;
        }
    }

    /** Parse a symbol on the rhs of a production.  Symbols may not have
     *  been mentioned previously, in which case they will be treated as
     *  nonterminals.
     *
     *  @return The symbol parsed, or null if no symbol found.  Does not
     *          advance past the symbol that was read.
     */
    private JaccSymbol parseSymbol() {
        String lexeme = lexer.getLexeme();
        switch (lexer.getToken()) {
            case IDENT: {
                JaccSymbol sym = null;
                sym = terminals.find(lexeme);
                if (sym==null) {
                    sym = nonterms.findOrAdd(lexeme);
                }
                return sym;
            }
            case CHARLIT:
                return literals.findOrAdd(lexeme, lexer.getLastLiteral());
            default:
                return null;
        }
    }

    /** Parse a single (predefined) symbol.  Only symbols that have
     *  already been recorded (in the set of terminals, the set of
     *  nonterminals, or the set of literals) will be accepted here.
     */
    private JaccSymbol parseDefinedSymbol() {
        String lexeme = lexer.getLexeme();
        switch (lexer.getToken()) {
            case IDENT: {
                JaccSymbol sym = nonterms.find(lexeme);
                return (sym!=null) ? sym : terminals.find(lexeme);
            }
            case CHARLIT:
                return literals.find(lexer.getLastLiteral());
            default:
                return null;
        }
    }
}
