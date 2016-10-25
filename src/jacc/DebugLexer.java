// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc;

import compiler.Handler;
import compiler.Source;

/** A simple extension of the JaccLexer that can be used for debugging.
 *  Replace calls of new JaccLexer(...) with calls to new DebugLexer
 *  and the lexer will display token codes as they are generated.
 */
public class DebugLexer extends JaccLexer {
    public DebugLexer(Handler handler, Source source) {
        super(handler, source);
    }

    public int nextToken() {
        int tok = super.nextToken();
        System.out.println("Token " + tok + " >" + getLexeme() + "<");
        return tok;
    }
}
