/*******************************************************************************
 * ============LICENSE_START==================================================
 * * org.onap.dmaap
 * * ===========================================================================
 * * Copyright © 2017 AT&T Intellectual Property. All rights reserved.
 * * ===========================================================================
 * * Licensed under the Apache License, Version 2.0 (the "License");
 * * you may not use this file except in compliance with the License.
 * * You may obtain a copy of the License at
 * *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 * *
 *  * Unless required by applicable law or agreed to in writing, software
 * * distributed under the License is distributed on an "AS IS" BASIS,
 * * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * * See the License for the specific language governing permissions and
 * * limitations under the License.
 * * ============LICENSE_END====================================================
 * *
 * * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 * *
 ******************************************************************************/


package org.onap.dmaap.datarouter.provisioning.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This class provides operations similar to the standard Java {@link java.util.BitSet} class.
 * It is designed for bit sets where there are long runs of 1s and 0s; it is not appropriate
 * for sparsely populated bits sets.  In addition, this class uses <code>long</code>s rather
 * than <code>int</code>s to represent the indices of the bits.
 *
 * @author Robert Eby
 * @version $Id$
 */
public class RLEBitSet {
    /**
     * Used to represent a continues set of <i>nbits</i> 1 bits starting at <i>start</i>.
     */
    private class RLE implements Comparable<RLE> {
        private final long start;
        private long nbits;

        public RLE(long from, long nbits) {
            this.start = from;
            this.nbits = (nbits > 0) ? nbits : 0;
        }

        /**
         * Returns the index of the first set bit in this RLE.
         *
         * @return the index
         */
        public long firstBit() {
            return start;
        }

        /**
         * Returns the index of the last set bit in this RLE.
         *
         * @return the index
         */
        public long lastBit() {
            return start + nbits - 1;
        }


        public boolean intersects(RLE b2) {
            if (b2.lastBit() < this.firstBit()) {
                return false;
            }
            if (b2.firstBit() > this.lastBit()) {
                return false;
            }
            return true;
        }

        public boolean isSubset(RLE b2) {
            if (firstBit() < b2.firstBit()) {
                return false;
            }
            if (firstBit() > b2.lastBit()) {
                return false;
            }
            if (lastBit() < b2.firstBit()) {
                return false;
            }
            if (lastBit() > b2.lastBit()) {
                return false;
            }
            return true;
        }

        public RLE union(RLE b2) {
            RLE b1 = this;
            if (b1.firstBit() > b2.firstBit()) {
                b1 = b2;
                b2 = this;
            }
            long end = b1.lastBit();
            if (b2.lastBit() > b1.lastBit()) {
                end = b2.lastBit();
            }
            return new RLE(b1.firstBit(), end - b1.firstBit() + 1);
        }

        /**
         * Returns the number of bits set to {@code true} in this {@code RLE}.
         *
         * @return the number of bits set to {@code true} in this {@code RLE}.
         */
        public int cardinality() {
            return (int) nbits;
        }

        @Override
        public int compareTo(RLE rle) {
            if (this.equals(rle)) {
                return 0;
            }
            return (start < rle.start) ? -1 : 1;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof RLE) {
                RLE rle = (RLE) obj;
                return (start == rle.start) && (nbits == rle.nbits);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Long.valueOf(start ^ nbits).hashCode();
        }

        @Override
        public String toString() {
            return "[" + firstBit() + ".." + lastBit() + "]";
        }
    }

    private SortedSet<RLE> bitsets;

    /**
     * Creates a new bit set. All bits are initially <code>false</code>.
     */
    public RLEBitSet() {
        bitsets = new TreeSet<>();
    }

    /**
     * Creates a new bit set, with bits set according to the value of <code>s</code>.
     *
     * @param str the initialization String
     */
    public RLEBitSet(String str) {
        bitsets = new TreeSet<>();
        set(str);
    }

    /**
     * Returns the "logical size" of this {@code RLEBitSet}: the index of the highest set bit
     * in the {@code RLEBitSet} plus one. Returns zero if the {@code RLEBitSet} contains no set bits.
     *
     * @return the logical size of this {@code RLEBitSet}
     */
    public long length() {
        if (isEmpty()) {
            return 0;
        }
        return bitsets.last().lastBit() + 1;
    }

    /**
     * Returns the value of the bit with the specified index. The value is {@code true} if the bit
     * with the index bit is currently set in this BitSet; otherwise, the result is {@code false}.
     *
     * @param bit the bit index
     * @return the value of the bit with the specified index
     */
    public boolean get(long bit) {
        synchronized (bitsets) {
            for (RLE bs : bitsets) {
                if (bit >= bs.firstBit() && bit <= bs.lastBit()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Set one or more bits to true, based on the value of <code>s</code>.
     *
     * @param str the initialization String, which consists of a comma or space separated list of
     *          non-negative numbers and ranges.  An individual number represents the bit index to set.
     *          A range (two numbers separated by a dash) causes all bit indexes between the two numbers
     *          (inclusive) to be set.
     * @throws NumberFormatException     - if a number is incorrectly formatted
     * @throws IndexOutOfBoundsException - if an index is negative
     */
    public void set(String str) {
        str = str.trim();
        if (!str.isEmpty()) {
            for (String s2 : str.split("[, \n]+")) {
                if (s2.indexOf('-') >= 0) {
                    String[] pp = s2.split("-");
                    long l1 = Long.parseLong(pp[0]);
                    long l2 = Long.parseLong(pp[1]);
                    set(l1, l2 + 1);
                } else {
                    set(Long.parseLong(s2));
                }
            }
        }
    }

    /**
     * Sets the bit at the specified index to {@code true}.
     *
     * @param bit a bit index
     */
    public void set(long bit) {
        set(bit, bit + 1);
    }

    /**
     * Sets the bits from the specified {@code from} (inclusive) to the
     * specified {@code to} (exclusive) to {@code true}.
     *
     * @param from index of the first bit to be set
     * @param to   index after the last bit to be set
     * @throws IndexOutOfBoundsException if {@code from} is negative,
     *                                   or {@code to} is negative,
     *                                   or {@code from} is larger than {@code to}
     */
    public void set(long from, long to) {
        checkRange(from, to);
        RLE newbits = new RLE(from, to - from);
        synchronized (bitsets) {
            for (RLE bs : bitsets) {
                if (bs.intersects(newbits)) {
                    if (!newbits.isSubset(bs)) {
                        bitsets.remove(bs);
                        bitsets.add(newbits.union(bs));
                        coalesce();
                    }
                    return;
                }
            }
            bitsets.add(newbits);
        }
        coalesce();
    }

    /**
     * Sets all of the bits in this BitSet to {@code false}.
     */
    public void clear() {
        synchronized (bitsets) {
            bitsets.clear();
        }
    }

    /**
     * Sets the bit specified by the index to {@code false}.
     *
     * @param bit the index of the bit to be cleared
     */
    public void clear(long bit) {
        clear(bit, bit + 1);
    }

    /**
     * Sets the bits from the specified {@code from} (inclusive) to the
     * specified {@code to} (exclusive) to {@code false}.
     *
     * @param from index of the first bit to be cleared
     * @param to   index after the last bit to be cleared
     * @throws IndexOutOfBoundsException if {@code from} is negative,
     *                                   or {@code to} is negative,
     *                                   or {@code from} is larger than {@code to}
     */
    public void clear(long from, long to) {
        checkRange(from, to);
        RLE newbits = new RLE(from, to - from);
        List<RLE> newranges = new ArrayList<>();
        synchronized (bitsets) {
            for (RLE bs : bitsets) {
                if (bs.intersects(newbits)) {
                    // preserve the bits that are not being cleared
                    long len = newbits.firstBit() - bs.firstBit();
                    if (len > 0) {
                        newranges.add(new RLE(bs.firstBit(), len));
                    }
                    len = bs.lastBit() - newbits.lastBit();
                    if (len > 0) {
                        newranges.add(new RLE(newbits.lastBit() + 1, len));
                    }
                    bs.nbits = 0;
                }
            }
            if (!newranges.isEmpty()) {
                for (RLE bs : newranges) {
                    bitsets.add(bs);
                }
            }
        }
        coalesce();
    }

    /**
     * Combine abutting RLEBitSets, and remove 0 length RLEBitSets.
     */
    private void coalesce() {
        RLE last = null;
        synchronized (bitsets) {
            Iterator<RLE> iter = bitsets.iterator();
            while (iter.hasNext()) {
                RLE bs = iter.next();
                if (last != null && (last.lastBit() + 1 == bs.firstBit())) {
                    last.nbits += bs.nbits;
                    iter.remove();
                } else if (bs.nbits == 0) {
                    iter.remove();
                } else {
                    last = bs;
                }
            }
        }
    }

    /**
     * Checks that fromIndex ... toIndex is a valid range of bit indices.
     */
    private static void checkRange(long from, long to) {
        if (from < 0) {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + from);
        }
        if (to < 0) {
            throw new IndexOutOfBoundsException("toIndex < 0: " + to);
        }
        if (from > to) {
            throw new IndexOutOfBoundsException("fromIndex: " + from + " > toIndex: " + to);
        }
    }

    /**
     * Performs a logical <b>AND</b> of this target bit set with the argument bit set.
     * This bit set is modified so that each bit in it has the value {@code true} if and only if
     * it both initially had the value {@code true} and the corresponding bit in the bit set
     * argument also had the value {@code true}.
     *
     * @param set a {@code RLEBitSet}
     */
    public void and(RLEBitSet set) {
        long last = 0;
        synchronized (set.bitsets) {
            for (RLE bs : set.bitsets) {
                clear(last, bs.start);
                last = bs.start + bs.nbits;
            }
        }
        clear(last, Long.MAX_VALUE);
    }

    /**
     * Clears all of the bits in this {@code RLEBitSet} whose corresponding bit is set in
     * the specified {@code RLEBitSet}.
     *
     * @param set the {@code RLEBitSet} with which to mask this {@code RLEBitSet}
     */
    public void andNot(RLEBitSet set) {
        synchronized (set.bitsets) {
            for (RLE bs : set.bitsets) {
                clear(bs.start, bs.start + bs.nbits);
            }
        }
    }

    /**
     * Returns true if this {@code RLEBitSet} contains no bits that are set
     * to {@code true}.
     *
     * @return boolean indicating whether this {@code BitSet} is empty
     */
    public boolean isEmpty() {
        return bitsets.isEmpty();
    }

    /**
     * Returns the number of bits set to {@code true} in this {@code RLEBitSet}.
     *
     * @return the number of bits set to {@code true} in this {@code RLEBitSet}.
     */
    public int cardinality() {
        int trueCount = 0;
        synchronized (bitsets) {
            for (RLE bs : bitsets) {
                trueCount += bs.cardinality();
            }
        }
        return trueCount;
    }

    /**
     * Cloning this RLEBitSet produces a new RLEBitSet that is equal to it. The clone of the
     * bit set is another bit set that has exactly the same bits set to true as this bit set.
     *
     * @return a clone of this bit set
     */
    public Object clone() {
        RLEBitSet rv = new RLEBitSet();
        synchronized (bitsets) {
            for (RLE bs : bitsets) {
                rv.bitsets.add(new RLE(bs.start, bs.nbits));
            }
        }
        return rv;
    }

    /**
     * Returns a string representation of this bit set, using the same notation as is required for
     * the String constructor. For every index for which this {@code RLEBitSet} contains a bit in
     * the set state, the decimal representation of that index is included in the result. Such
     * indices are listed in order from lowest to highest, separated by ",". Ranges of set bits are
     * indicated by <i>lobit</i>-<i>hibit</i>.
     *
     * @return the String
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String prefix = "";
        synchronized (bitsets) {
            for (RLE bs : bitsets) {
                sb.append(prefix);
                prefix = ",";
                long bit1 = bs.firstBit();
                long bit2 = bs.lastBit();
                sb.append(bit1);
                if (bit1 != bit2) {
                    sb.append('-').append(bit2);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Return an Iterator which provides pairs of {@code Long}s representing the beginning and
     * ending index of a range of set bits in this {@code RLEBitSet}.
     *
     * @return the Iterator
     */
    public Iterator<Long[]> getRangeIterator() {
        return new Iterator<Long[]>() {
            private Iterator<RLE> iterator = bitsets.iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Long[] next() {
                RLE bs = iterator.next();
                return new Long[]{bs.firstBit(), bs.lastBit()};
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
