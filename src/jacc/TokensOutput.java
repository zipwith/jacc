// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc;

import java.io.PrintWriter;
import jacc.grammar.Grammar;
import compiler.Handler;

/** Used to generate the text of a Java interface that defines a numeric
 *  code for each token.
 */
public class TokensOutput extends Output {
    public TokensOutput(Handler handler, JaccJob job) {
        super(handler, job);
    }

    /** Output a Java interface definition with codes for tokens.
     */
    public void write(PrintWriter out) {
        datestamp(out);
        String pkg = settings.getPackageName();
        if (pkg!=null) {
            out.println("package " + pkg +";");
            out.println();
        }
        out.println("interface " + settings.getInterfaceName() + " {");
        indent(out, 1);
        out.println("int ENDINPUT = 0;");
        for (int i=0; i<numTs-1; i++) {
            Grammar.Symbol sym = grammar.getTerminal(i);
            if (sym instanceof JaccSymbol) {
                JaccSymbol jsym = (JaccSymbol)sym;
                String     name = jsym.getName();
                indent(out, 1);
                if (name.startsWith("'")) {
                    out.println("// " + name + " (code=" + jsym.getNum() + ")");
                } else {
                    out.println("int " + name + " = " + jsym.getNum() + ";");
                }
            }
        }
        out.println("}");
    }
}
