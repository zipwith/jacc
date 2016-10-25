// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package compiler;

/** A base class for building lexical analyzers that use a Source
 *  object as input.
 */
public abstract class SourceLexer extends Lexer {
    /** The Source object for this lexical analyzer.
     */
    protected Source source;

    /** Holds the text of the current line.
     */
    protected String line;

    /** Holds the position in the current line; a zero index
     *  indicates the first character in the line, while an
     *  index of line.length() indicates a "virtual EOL" at
     *  the end of the line.
     */
    protected int col = (-1);

    private SourcePosition pos;

    protected final static int EOF = -1;
    protected final static int EOL = '\n';
    protected int   c;

    public SourceLexer(Handler handler, Source source) {
        super(handler);
        this.source = source;
        this.pos    = new SourcePosition(source);
        this.line   = source.readLine();
        nextChar();
    }

    public Position getPos() {
        return pos.copy();
    }

    protected void markPosition() {
        pos.updateCoords(source.getLineNo(), col);
    }

    protected void nextLine() {
        line = source.readLine();
        col  = (-1);
        nextChar();
    }

    protected int nextChar() {
        if (line==null) {
            c   = EOF;
            col = 0;  // EOF is always at column 0
        } else if (++col>=line.length()) {
            c   = EOL;
        } else {
            c   = line.charAt(col);
        }
        return c;
    }

    public void close() {
        if (source != null) {
            source.close();
            source = null;
        }
    }
}
