// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc.util;

import java.io.PrintWriter;

/** An implementation of the strongly connected components algorithm.
 */
public class SCC {
    public static int[][] get(int[][] depends, int[][] revdeps, int size) {
        return new GetComponents(depends,size,
                 new ArrangeByFinish(revdeps,size)
                     .getFinishOrder())
                   .getComponents();
    }

    public static int[][] get(int[][] depends) {
        return get(depends, invert(depends), depends.length);
    }

    public static int[][] get(int[][] depends, int len) {
        return get(depends, invert(depends, len), len);
    }

    // A Depth-first search that returns an array of index values,
    // arranged in decreasing order of finish time.

    private static class ArrangeByFinish extends DepthFirst {
        private int   dfsNum;
        private int[] order;

        ArrangeByFinish(int [][] dependencies, int size) {
            super(new SeqInterator(0,size), dependencies);
            dfsNum = size;
            order  = new int[dfsNum];
        }
        void doneVisit(int i) {
            order[--dfsNum] = i;
        }
        int[] getFinishOrder() {
            search();
            return order;
        }
    }

    // A depth first search that builds components

    private static class GetComponents extends DepthFirst {
        private int   numComps;
        private int[] compNo;

        GetComponents(int [][] dependencies, int size, int[] order) {
            super(new ElemInterator(order), dependencies);
            numComps = 0;
            compNo   = new int[size];
        }
        void doneVisit(int i) {
            compNo[i] = numComps;
        }
        void doneTree() {
            numComps++;
        }
        int[][] getComponents() {
            search();
            int[] compSize = new int[numComps];
            for (int i=0; i<compNo.length; i++) {
                compSize[compNo[i]]++;
            }
            int[][] comps = new int[numComps][];
            for (int j=0; j<numComps; j++) {
                comps[j] = new int[compSize[j]];
            }
            for (int i=0; i<compNo.length; i++) {
                int j = compNo[i];
                comps[j][--compSize[j]] = i;
            }
            return comps;
        }
    }

    public static int[][] invert(int[][] adj) {
        return invert(adj, adj.length);
    }

    public static int[][] invert(int[][] adj, int len) {
        int[] counts = new int[len];
        for (int i=0; i<len; i++) {
            for (int j=0; j<adj[i].length; j++) {
                counts[adj[i][j]]++;
            }
        }
        int[][] rev = new int[len][];
        for (int i=0; i<len; i++) {
            rev[i] = new int[counts[i]];
        }
        for (int i=0; i<len; i++) {
            for (int j=0; j<adj[i].length; j++) {
                int n = adj[i][j];
                counts[n]--;
                rev[n][counts[n]] = i;
            }
        }
        return rev;
    }

    public static void displayComponents(PrintWriter out, int[][] comps) {
        out.println("Components (" + comps.length + " in total):");
        for (int i=0; i<comps.length; i++) {
            out.print(" Component " + i + ": {");
            for (int j=0; j<comps[i].length; j++) {
                if (j>0) {
                    out.print(", ");
                }
                out.print(comps[i][j]);
            }
            out.println("}");
        }
    }
}
