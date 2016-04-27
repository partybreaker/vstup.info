/*
 * Decompiled with CFR 0_114.
 */
package org.ibex.nestedvm.util;

public class InodeCache {
    private static final Object PLACEHOLDER = new Object();
    private static final short SHORT_PLACEHOLDER = -2;
    private static final short SHORT_NULL = -1;
    private static final int LOAD_FACTOR = 2;
    private final int maxSize;
    private final int totalSlots;
    private final int maxUsedSlots;
    private final Object[] keys;
    private final short[] next;
    private final short[] prev;
    private final short[] inodes;
    private final short[] reverse;
    private int size;
    private int usedSlots;
    private short mru;
    private short lru;

    public InodeCache() {
        this(1024);
    }

    public InodeCache(int n) {
        this.maxSize = n;
        this.totalSlots = n * 2 * 2 + 3;
        this.maxUsedSlots = this.totalSlots / 2;
        if (this.totalSlots > 32767) {
            throw new IllegalArgumentException("cache size too large");
        }
        this.keys = new Object[this.totalSlots];
        this.next = new short[this.totalSlots];
        this.prev = new short[this.totalSlots];
        this.inodes = new short[this.totalSlots];
        this.reverse = new short[this.totalSlots];
        this.clear();
    }

    private static void fill(Object[] arrobject, Object object) {
        for (int i = 0; i < arrobject.length; ++i) {
            arrobject[i] = object;
        }
    }

    private static void fill(short[] arrs, short s) {
        for (int i = 0; i < arrs.length; ++i) {
            arrs[i] = s;
        }
    }

    public final void clear() {
        this.usedSlots = 0;
        this.size = 0;
        this.lru = -1;
        this.mru = -1;
        InodeCache.fill(this.keys, null);
        InodeCache.fill(this.inodes, -1);
        InodeCache.fill(this.reverse, -1);
    }

    /*
     * Enabled aggressive block sorting
     */
    public final short get(Object object) {
        Object object2;
        int n;
        int n2 = object.hashCode() & Integer.MAX_VALUE;
        int n3 = n = n2 % this.totalSlots;
        int n4 = 1;
        boolean bl = true;
        int n5 = -1;
        while ((object2 = this.keys[n]) != null) {
            if (object2 == PLACEHOLDER) {
                if (n5 == -1) {
                    n5 = n;
                }
            } else if (object2.equals(object)) {
                short s = this.inodes[n];
                if (n == this.mru) {
                    return s;
                }
                if (this.lru == n) {
                    this.lru = this.next[this.lru];
                } else {
                    short s2;
                    short s3 = this.prev[n];
                    this.next[s3] = s2 = this.next[n];
                    this.prev[s2] = s3;
                }
                this.prev[n] = this.mru;
                this.next[this.mru] = (short)n;
                this.mru = (short)n;
                return s;
            }
            n = Math.abs((n3 + (bl ? 1 : -1) * n4 * n4) % this.totalSlots);
            if (!bl) {
                ++n4;
            }
            bl = !bl;
        }
        int n6 = n;
        if (this.usedSlots == this.maxUsedSlots) {
            this.clear();
            return this.get(object);
        }
        ++this.usedSlots;
        if (this.size == this.maxSize) {
            this.keys[this.lru] = PLACEHOLDER;
            this.inodes[this.lru] = -2;
            this.lru = this.next[this.lru];
        } else {
            if (this.size == 0) {
                this.lru = (short)n6;
            }
            ++this.size;
        }
        int n7 = n2 & 32767;
        block1 : do {
            n3 = n = n7 % this.totalSlots;
            n4 = 1;
            bl = true;
            n5 = -1;
            short s;
            while ((s = this.reverse[n]) != -1) {
                short s4 = this.inodes[s];
                if (s4 == -2) {
                    if (n5 == -1) {
                        n5 = n;
                    }
                } else if (s4 == n7) {
                    ++n7;
                    continue block1;
                }
                n = Math.abs((n3 + (bl ? 1 : -1) * n4 * n4) % this.totalSlots);
                if (!bl) {
                    ++n4;
                }
                bl = !bl;
            }
            break block1;
            break;
        } while (true);
        this.keys[n6] = object;
        this.reverse[n] = (short)n6;
        this.inodes[n6] = (short)n7;
        if (this.mru != -1) {
            this.prev[n6] = this.mru;
            this.next[this.mru] = (short)n6;
        }
        this.mru = (short)n6;
        return (short)n7;
    }

    public Object reverse(short s) {
        int n;
        short s2;
        int n2 = n = s % this.totalSlots;
        int n3 = 1;
        boolean bl = true;
        while ((s2 = this.reverse[n]) != -1) {
            if (this.inodes[s2] == s) {
                return this.keys[s2];
            }
            n = Math.abs((n2 + (bl ? 1 : -1) * n3 * n3) % this.totalSlots);
            if (!bl) {
                ++n3;
            }
            bl = !bl;
        }
        return null;
    }
}

