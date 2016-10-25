// Copyright (c) Mark P Jones, OGI School of Science & Engineering
// Subject to conditions of distribution and use; see LICENSE for details
// April 24 2004 01:01 AM
// 

package jacc.util;

import java.io.PrintWriter;

/** Holds a small set of integers, arranged in increasing numerical order.
 */
public class IntSet {
    private int[] elems;
    private int   used;

    private static final int START_SIZE = 1; // must be strictly positive 

    private IntSet() {
        elems = new int[START_SIZE];
        used  = 0;
    }

    public static IntSet empty() {
        return new IntSet();
    }

    public static IntSet singleton(int val) {
        IntSet set = new IntSet();
        set.add(val);
        return set;
    }        

    public int size() {
        return used;
    }

    public boolean isEmpty() {
        return used==0;
    }

    public void clear() {
        used = 0;
    }

    public int at(int pos) {
        return elems[pos];
    }

    public int[] toArray() {
        int[] result = new int[used];
        for (int i=0; i<used; i++) {
            result[i] = elems[i];
        }
        return result;
    }

    public boolean contains(int val) {
        int lo = 0;                     // searching [lo..hi-1]
        int hi = used;
        while (lo<hi) {                 
            int mid = (lo+hi)/2;
            int elm = elems[mid];
            if (val==elm) {             // element found!
                return true;
            } else if (val < elm) {     // look in [lo..mid-1]
                hi = mid;
            } else {                    // look in [mid+1..hi-1]
                lo = mid + 1;
            }
        }
        return false;
    }

    public void add(int val) {
        int lo = 0;                     // searching [lo..hi-1]
        int hi = used;
        while (lo<hi) {                 
            int mid = (lo+hi)/2;
            int elm = elems[mid];
            if (val < elm) {            // look in [lo..mid-1]
                hi = mid;
            } else if (val==elm) {      // element found at mid
                return;
            } else {                    // look in [mid+1..hi-1]
                lo = mid + 1;
            }
        }
        // new value will fit in at position lo.
        if (used>=elems.length) {
            int[] newElems = new int[elems.length*2];
            for (int i=0; i<lo; i++) {
                newElems[i] = elems[i];
            }
            newElems[lo] = val;
            for (int i=lo; i<used; i++) {
                newElems[i+1] = elems[i];
            }
            elems = newElems;
        } else {
            for (int i=used; i>lo; i--) {
                elems[i] = elems[i-1];
            }
            elems[lo] = val;
        }
        used++;
    }

    public boolean equals(IntSet that) {
        if (this.used == that.used) {
            for (int i=0; i<used; i++) {
                if (this.elems[i] != that.elems[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public Interator interator() {
        return new ElemInterator(elems,0,used);
    }

    public static void main(String[] args) {
        PrintWriter out = new PrintWriter(System.out);
        IntSet set = empty();
        int[]  nums      = { 4, 3, 7, 3, 1, 6, 8, 7, 7, 9, 5, 5, 2, 0 };
        for (int i=0; i<nums.length; i++) {
            set.display(out);
            out.println("adding " + nums[i]);
            set.add(nums[i]);
        }
        set.display(out);
    }

    public void display(PrintWriter out) {
        Interator es = interator();
        out.print("{");
        for (int count = 0; es.hasNext(); count++) {
            if (count!=0) {
                out.print(", ");
            }
            out.print(es.next());
        }
        out.print("}");             
        out.println(": used = " + used + ", length = " + elems.length);
    }
}
