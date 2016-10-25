// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc.util;

public class BitSet {

    private BitSet() {}

    private static final int LOG_BITS_PER_WORD = 5;
    private static final int BITS_PER_WORD     = 1 << LOG_BITS_PER_WORD;
    private static final int BIT_MASK          = BITS_PER_WORD - 1;

    public static int[] make(int size) {
        return new int[(size+BITS_PER_WORD-1) >> LOG_BITS_PER_WORD];
    }

    public static int[] copy(int[] set) {
        int[] ret = new int[set.length];
        for (int i=0; i<set.length; i++) {
            ret[i] = set[i];
        }
        return ret;
    }

    public static void clear(int[] s) {
        for (int i=0; i<s.length; i++) {
            s[i] = 0;
        }
    }

    public static boolean isEmpty(int[] set) {
        for (int i=0; i<set.length; i++) {
            if (set[i]!=0) {
                return false;
            }
        }
        return true;
    }

    public static boolean equal(int[] s1, int[] s2) {
        int i = 0;
        for (; i<s1.length && i<s2.length; i++) {
            if (s1[i]!=s2[i]) {
                return false;
            }
        }
        return i>=s1.length && i>=s2.length;
    }

    public static boolean disjoint(int[] s1, int[] s2) {
        int i = 0;
        for (; i<s1.length && i<s2.length; i++) {
            if ((s1[i] & s2[i]) != 0) {
                return false;
            }
        }
        return i>=s1.length && i>=s2.length;
    }

    public static void union(int[] s1, int[] s2) {
        for (int i=0; i<s1.length; i++) {
            s1[i] |= s2[i];
        }
    }

    public static void intersect(int[] s1, int[] s2) {
        for (int i=0; i<s1.length; i++) {
            s1[i] &= s2[i];
        }
    }

    public static boolean addTo(int[] oldBits, int[] newBits) {
        if (oldBits.length < newBits.length) {
            throw new Error("bitset arguments do not match");
        }
        int     i       = 0;
        boolean changed = false;
        for (; i<newBits.length; i++) {
            if (newBits[i]!=0) {
                int bits = oldBits[i] | newBits[i];
                if (bits != oldBits[i]) {
                    changed = true;
                }
                oldBits[i] = bits;
            }
        }
        return changed;
    }

    public static boolean addTo(int[] s, int n) {
        int mask = 1 << (n & BIT_MASK);
        int pos  = n >> LOG_BITS_PER_WORD;
        int val  = s[pos] | mask;
        if (val!=s[pos]) {
            s[pos] = val;
            return true;
        } else {
            return false;
        }
    }

    public static void set(int[] s, int n) {
        int mask = 1 << (n & BIT_MASK);
        int pos  = n >> LOG_BITS_PER_WORD;
        s[pos] |= mask;
    }

    public static boolean get(int[] s, int n) {
        int mask = 1 << (n & BIT_MASK);
        int pos  = n >> LOG_BITS_PER_WORD;
        return (s[pos] & mask)!=0;
    }

    public static int[] members(int[] s) {
        int count = 0;
        for (int i=0; i<s.length; i++) {
            if (s[i]!=0) {
                int val = s[i];
                for (int j=0; j<BITS_PER_WORD && val!=0; j++) {
                    if ((val & 1) != 0) {
                        count++;
                    }
                    val >>= 1;
                }
            }
        }

        int[] mems = new int[count];
        int out    = 0;
        for (int i=0; i<s.length && out<count; i++) {
            if (s[i]!=0) {
                int offset = i << LOG_BITS_PER_WORD;
                int val    = s[i];
                for (int j=0; j<BITS_PER_WORD && val!=0; j++) {
                    if ((val & 1) != 0) {
                        mems[out++] = offset+j;
                    }
                    val >>= 1;
                }
            }
        }
        return mems;
    }

    public static Interator interator(int[] set, int start) {
        return new BitSetInterator(set, start);
    }

    private static class BitSetInterator extends Interator {
        int[] set;
        int   pos;
        int   mask;
        int   num;
        int   bitCount;
        BitSetInterator(int[] set, int start) {
            this.set = set;
            this.num = start;
            pos      = 0;
            mask     = 1;
            bitCount = 0;
        }
        private void advance() {
            num++;
            if (++bitCount == BITS_PER_WORD) {
                pos++;
                bitCount = 0;
                mask     = 1;
            } else {
                mask <<= 1;
            }
        }
        public int next() {
            int value = num;
            advance();
            return value;
        }
        public boolean hasNext() {
            while (pos<set.length && ((set[pos]&mask)==0)) {
                advance();
            }
            return (pos<set.length);
        }
    }
}
