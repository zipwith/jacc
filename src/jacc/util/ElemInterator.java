// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc.util;

public class ElemInterator extends Interator {
    private int count;
    private int limit;
    private int a[];
    public ElemInterator(int[] a, int lo, int hi) {
        this.a     = a;
        this.count = lo;
        this.limit = hi;
    }
    public ElemInterator(int[] a) {
        this (a, 0, a.length);
    }
    public int next() {
        return a[count++];
    }
    public boolean hasNext() {
        return count < limit;
    }
}

