// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc;

import java.io.PrintWriter;
import compiler.Handler;

/** Used to generate an HTML description of the generated machine.
 */
public class HTMLOutput extends TextOutput {
    public HTMLOutput(Handler handler, JaccJob job, boolean wantFirst) {
        super(handler, job, wantFirst);
    }

    /** Output a textual description of the generated machine with HTML
     *  hyperlinks for ease of browsing.
     */
    public void write(PrintWriter out) {
        out.println("<html>");
        out.println("<title>Generated machine for " + settings.getClassName()
                                                    + "</title>");
        out.println("<body>");
        out.println("<h1>Generated machine for " + settings.getClassName()
                                                 + "</h1>");
        out.println("<pre>");
        super.write(out);
        out.println("</pre>");
        out.println("</body>");
        out.println("</html>");
    }

    /** Generate a description of the entry point to a state.
     */
    protected String describeEntry(int st) {
        return "<hr><a name=\"st" + st + "\"><b>" + super.describeEntry(st) + "</b></a>";
    }

    /** Generate a description of a shift.
     */
    protected String describeShift(int st) {
        return "<a href=\"#st" + st + "\">" + super.describeShift(st) + "</a>";
    }

    /** Generate a description of a goto.
     */
    protected String describeGoto(int st) {
        return "<a href=\"#st" + st + "\">" + super.describeGoto(st) + "</a>";
    }
}
