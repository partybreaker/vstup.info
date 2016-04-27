/*
 * Decompiled with CFR 0_114.
 */
package org.ibex.nestedvm.util;

public final class Sort {
    private static final CompareFunc comparableCompareFunc = new CompareFunc(){

        public int compare(Object object, Object object2) {
            return ((Comparable)object).compareTo(object2);
        }
    };

    private Sort() {
    }

    public static void sort(Comparable[] arrcomparable) {
        Sort.sort(arrcomparable, comparableCompareFunc);
    }

    public static void sort(Object[] arrobject, CompareFunc compareFunc) {
        Sort.sort(arrobject, compareFunc, 0, arrobject.length - 1);
    }

    private static void sort(Object[] arrobject, CompareFunc compareFunc, int n, int n2) {
        Object object;
        if (n >= n2) {
            return;
        }
        if (n2 - n <= 6) {
            for (int i = n + 1; i <= n2; ++i) {
                Object object2 = arrobject[i];
                for (int j = i - 1; j >= n && compareFunc.compare(arrobject[j], object2) > 0; --j) {
                    arrobject[j + 1] = arrobject[j];
                }
                arrobject[j + 1] = object2;
            }
            return;
        }
        Object object3 = arrobject[n2];
        int n3 = n - 1;
        int n4 = n2;
        do {
            if (n3 < n4 && compareFunc.compare(arrobject[++n3], object3) < 0) {
                continue;
            }
            while (n4 > n3 && compareFunc.compare(arrobject[--n4], object3) > 0) {
            }
            object = arrobject[n3];
            arrobject[n3] = arrobject[n4];
            arrobject[n4] = object;
            if (n3 >= n4) break;
        } while (true);
        object = arrobject[n3];
        arrobject[n3] = arrobject[n2];
        arrobject[n2] = object;
        Sort.sort(arrobject, compareFunc, n, n3 - 1);
        Sort.sort(arrobject, compareFunc, n3 + 1, n2);
    }

    public static interface CompareFunc {
        public int compare(Object var1, Object var2);
    }

    public static interface Comparable {
        public int compareTo(Object var1);
    }

}

