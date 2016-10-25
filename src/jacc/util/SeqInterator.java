// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc.util;

public class SeqInterator extends Interator {
    private int count, limit;
    public SeqInterator(int count, int limit) {
        this.count = count;
        this.limit = limit;
    }
    public int next() {
        return count++;
    }
    public boolean hasNext() {
        return count < limit;
    }
}
