// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc.grammar;

/** A framework for describing iterative analyses on the strongly
 *  connected components of a graph.  A typical analysis is described
 *  by:
 *  <ul>
 *    <li> Defining a constructor that invokes super(comps), sets up
 *         the initial data for the analysis, and then invokes either
 *         topDown() or bottomUp();
 *    </li>
 *    <li> Overides analyze() to do the appropriate calculations for the
 *         analysis at the point concerned.
 *    </li>
 *  </ul>
 */
public abstract class Analysis {
    /** Records the underlying set of components, starting with the ones
     *  that have no lower descendents.
     */
    private int[][] comps;

    /** Construct a component analysis instance by recording the
     *  component members.
     */
    protected Analysis(int[][] comps) {
        this.comps = comps;
    }

    /** Method used to run a bottom-up analysis.  This is an analysis
     *  in which each object passes data to the things that depend on it.
     */
    protected void bottomUp() {
        for (int i=0; i<comps.length; i++) {
            analyzeComponent(comps[i]);
        }
    }

    /** Method used to run a top-down analysis.  This is an analysis
     *  in which each object passes data to the things on which it depends.
     */
    protected void topDown() {
        for (int i=comps.length; i-- >0; ) {
            analyzeComponent(comps[i]);
        }
    }

    /** Method used to run an analysis over the elements of a component.
     *  The analysis is iterated until no changes are detected.
     */
    private void analyzeComponent(int[] comp) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int j=0; j<comp.length; j++) {
                changed |= analyze(comp[j]);
            }
        }
    }

    /** Run the analysis at a particular point.  Return a boolean true
     *  if this changed the current approximation at this point.
     */
    protected abstract boolean analyze(int i);
}
