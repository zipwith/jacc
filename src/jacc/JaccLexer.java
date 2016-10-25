// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc;

import compiler.Source;
import compiler.SourceLexer;
import compiler.Handler;
import compiler.Warning;
import compiler.Failure;

/** A lexical analyzer for the jacc parser generator.
 */
public class JaccLexer extends SourceLexer implements JaccTokens {
    /** Construct a lexical analyzer for a jacc input source.
     */
    public JaccLexer(Handler handler, Source source) {
        super(handler, source);
    }

    /** Read the next token and return the corresponding integer code.
     */
    public int nextToken() {
        for (;;) {
            skipWhitespace();
            markPosition();
            lexemeText = null;
            switch (c) {
                case EOF  : return token=ENDINPUT;
                case ':'  : nextChar();
                            return token=COLON;
                case ';'  : nextChar();
                            return token=SEMI;
                case '|'  : nextChar();
                            return token=BAR;
                case '<'  : nextChar();
                            return token=TOPEN;
                case '>'  : nextChar();
                            return token=TCLOSE;
                case '['  : nextChar();
                            return token=BOPEN;
                case ']'  : nextChar();
                            return token=BCLOSE;
                case '.'  : nextChar();
                            return token=DOT;
                case '%'  : if (directive()!=ERROR) {
                                return token;
                            }
                            break;
                case '\"' : if (string()!=ERROR) {
                                return token;
                            }
                            break;
                case '\'' : if (literal()!=ERROR) {
                                return token;
                            }
                            break;
                case '{'  : if (action()!=ERROR) {
                                return token;
                            }
                            break;
                case '/'  : skipComment();
                            break;
                default   : if (Character.isJavaIdentifierStart((char)c)) {
                                return identifier();
                            } else if (Character.isDigit((char)c)) {
                                return number();
                            } else {
                                illegalCharacter();
                                nextChar();
                            }
            }
        }
    }

    /** Read a whole line from the current position to the end of the
     *  current line.  This method is used to read code that appears
     *  after the second %% marker in an input file.
     */
    public String readWholeLine() {
        if (line==null) {
            return null;
        } else {
            String result = line;
            if (col>0) {
                result = result.substring(col);
            }
            nextLine();
            return result;
        }
    }

    /** Read the rest of the current line, skipping leading whitespace.
     *  A subsequent call to nextToken() will, of course, be required
     *  to read the following token.
     */
    public String readCodeLine() {
        while (isWhitespace(c)) {
            nextChar();
        }
        return readWholeLine();
    }

    //---------------------------------------------------------------------
    // Details of lexical analysis follow:

    private boolean isWhitespace(int c) {
        return (c==' ') || (c=='\f');
    }

    private void skipWhitespace() {
        while (isWhitespace(c)) {
            nextChar();
        }
        while (c==EOL) {
            nextLine();
            while (isWhitespace(c)) {
                nextChar();
            }
        }
    }

    private void skipComment() {        // Assumes c=='/'
        nextChar();
        if (c=='/') {                   // Skip one line comment
            nextLine();
        } else if (c=='*') {            // Skip bracketed comment
            nextChar();
            for (;;) {
                if (c=='*') {
                    do {
                        nextChar();
                    } while (c=='*');
                    if (c=='/') {
                        nextChar();
                        return;
                    }
                }
                if (c==EOF) {
                    report(new Failure(getPos(), "Unterminated comment"));
                    return;
                }
                if (c==EOL) {
                    nextLine();
                } else {
                    nextChar();
                }
            }
        } else {
            report(new Failure(getPos(), "Illegal comment format"));
        }
    }

    private int identifier() {          // Assumes isJavaIdentifierStart(c)
        int start = col;
        do {
            nextChar();
        } while (c!=EOF && Character.isJavaIdentifierPart((char)c));
        lexemeText = line.substring(start, col);
        return token=IDENT;
    }

    private int directive() {           // Assumes c=='%'
        nextChar();
        if (c=='%') {
            nextChar();
            return token=MARK;
        } else if (Character.isJavaIdentifierStart((char)c)) {
            identifier();
            if (lexemeText.equals("token")) {
                return token=TOKEN;
            } else if (lexemeText.equals("type")) {
                return token=TYPE;
            } else if (lexemeText.equals("prec")) {
                return token=PREC;
            } else if (lexemeText.equals("left")) {
                return token=LEFT;
            } else if (lexemeText.equals("right")) {
                return token=RIGHT;
            } else if (lexemeText.equals("nonassoc")) {
                return token=NONASSOC;
            } else if (lexemeText.equals("start")) {
                return token=START;
            } else if (lexemeText.equals("package")) {
                return token=PACKAGE;
            } else if (lexemeText.equals("extends")) {
                return token=EXTENDS;
            } else if (lexemeText.equals("implements")) {
                return token=IMPLEMENTS;
            } else if (lexemeText.equals("semantic")) {
                return token=SEMANTIC;
            } else if (lexemeText.equals("get")) {
                return token=GETTOKEN;
            } else if (lexemeText.equals("next")) {
                return token=NEXTTOKEN;
            } else if (lexemeText.equals("class")) {
                return token=CLASS;
            } else if (lexemeText.equals("interface")) {
                return token=INTERFACE;
            } else {
                report(new Failure(getPos(), "Unrecognized directive"));
                return ERROR;
            }
        } else if (c=='{') {
            nextChar();
            return code();
        } else {
            report(new Failure(getPos(), "Illegal directive syntax"));
            return ERROR;
        }
    }

    private int code() {                // Assumes c is first char after %{
        int start        = col;
        StringBuffer buf = null;
        for (;;) {
            if (c=='%') {
                do {
                    nextChar();
                } while (c=='%');
                if (c=='}') {
                    lexemeText = endBuffer(buf,start,col-1);
                    nextChar();
                    return token=CODE;
                }
            }
            if (c==EOF) {
                report(new Failure(getPos(),
                                   "Code fragment terminator %} not found"));
                lexemeText = endBuffer(buf, start, col);
                return token = CODE;
            }
            if (c==EOL) {
                if (buf==null) {
                    buf = new StringBuffer(line.substring(start,col));
                } else {
                    buf.append('\n');
                    buf.append(line);
                }
                nextLine();
            } else {
                nextChar();
            }
        }
    }

    private String endBuffer(StringBuffer buf, int start, int end) {
        if (buf==null) {
            return line.substring(start, end);
        } else {
            buf.append('\n');
            if (line!=null) {
                buf.append(line.substring(0,end));
            }
            return buf.toString();
        }
    }

    private int lastLiteral;

    public int getLastLiteral() {
        return lastLiteral;
    }

    private int number() {              // Assumes c is a digit
        int start = col;
        int n     = 0;
        int d     = Character.digit((char)c, 10);
        do {
            n = 10*n + d;
            nextChar();
            d = Character.digit((char)c, 10);
        } while (d>=0);
        lexemeText  = line.substring(start, col);
        lastLiteral = n;
        return token=INTLIT;
    }

    private int string() {              // Assumes c=='\"'
        nextChar();
        int start = col;
        while (c!='\"' && c!=EOL && c!=EOF) {
            if (c=='\\') {
                escapeChar();
            } else {
                nextChar();
            }
        }
        lexemeText = line.substring(start,col);
        if (c=='\"') {
            nextChar();
        } else {
            report(new Warning(getPos(), "Missing \" on string literal"));
        }
        return token=STRLIT;
    }

    private int literal() {             // Assumes c=='\''
        int start = col;
        nextChar();
        if (c=='\\') {
            escapeChar();
        } else if (c!='\'' && c!=EOL && c!=EOF) {
            lastLiteral = c;
            nextChar();
        } else {
            report(new Failure(getPos(), "Illegal character literal syntax"));
            return ERROR;
        }
        if (c=='\'') {
            nextChar();
        } else {
            report(new Warning(getPos(), "Missing \' on character literal"));
        }
        lexemeText = line.substring(start,col);
        return token=CHARLIT;
    }

    private void escapeChar() {         // Assumes c=='\\'
        nextChar();
        switch (c) {
            case 'b'  :  case 't'  :  case 'n'  :  case 'f'  :
            case 'r'  :  case '\"' :  case '\'' :  case '\\' :
                lastLiteral = c;
                nextChar();
                return;
            default : {
                int d = Character.digit((char)c,8);
                if (d>=0) {
                    lastLiteral = 0;
                    int len = (d<4) ? 3 : 2;
                    do {
                        lastLiteral = (lastLiteral<<3) + d;
                        nextChar();
                        d = Character.digit((char)c,8);
                    } while (d>=0 && --len>0);
                    return;
                }
            }
        }
        report(new Failure(getPos(), "Syntax error in escape sequence"));
    }

    private int action() {              // Assumes c=='{'
        int start        = col;
        int nesting      = 0;
        StringBuffer buf = null;
        for (;;) {
            if (c=='}') {
                nesting--;
                if (nesting==0) {
                    nextChar();
                    lexemeText = endBuffer(buf,start,col);
                    return token=ACTION;
                }
            } else if (c=='{') {
                nesting++;
            }
            if (c==EOF) {
                report(new Failure(getPos(), "Unterminated action"));
                lexemeText = endBuffer(buf, start, col);
                return token = ACTION;
            } else if (c==EOL) {
                if (buf==null) {
                    buf = new StringBuffer(line.substring(start,col));
                } else {
                    buf.append('\n');
                    buf.append(line);
                }
                nextLine();
            } else {
                nextChar();
            }
        }
    }

    private void illegalCharacter() {
        report(new Warning(getPos(), "Ignoring illegal character"));
    }
}
