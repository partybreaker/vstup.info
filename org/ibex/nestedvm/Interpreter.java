/*
 * Decompiled with CFR 0_114.
 */
package org.ibex.nestedvm;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.util.HashMap;
import org.ibex.nestedvm.Runtime;
import org.ibex.nestedvm.UnixRuntime;
import org.ibex.nestedvm.util.ELF;
import org.ibex.nestedvm.util.Seekable;

public class Interpreter
extends UnixRuntime
implements Cloneable {
    private int[] registers = new int[32];
    private int hi;
    private int lo;
    private int[] fpregs = new int[32];
    private int fcsr;
    private int pc;
    public String image;
    private ELF.Symtab symtab;
    private int gp;
    private ELF.Symbol userInfo;
    private int entryPoint;
    private int heapStart;
    private HashMap sourceLineCache;

    private final void setFC(boolean bl) {
        this.fcsr = this.fcsr & -8388609 | (bl ? 8388608 : 0);
    }

    private final int roundingMode() {
        return this.fcsr & 3;
    }

    private final double getDouble(int n) {
        return Double.longBitsToDouble(((long)this.fpregs[n + 1] & 0xFFFFFFFFL) << 32 | (long)this.fpregs[n] & 0xFFFFFFFFL);
    }

    private final void setDouble(int n, double d) {
        long l = Double.doubleToLongBits(d);
        this.fpregs[n + 1] = (int)(l >>> 32);
        this.fpregs[n] = (int)l;
    }

    private final float getFloat(int n) {
        return Float.intBitsToFloat(this.fpregs[n]);
    }

    private final void setFloat(int n, float f) {
        this.fpregs[n] = Float.floatToRawIntBits(f);
    }

    protected void _execute() throws Runtime.ExecutionException {
        try {
            this.runSome();
        }
        catch (Runtime.ExecutionException var1_1) {
            var1_1.setLocation(Interpreter.toHex(this.pc) + ": " + this.sourceLine(this.pc));
            throw var1_1;
        }
    }

    protected Object clone() throws CloneNotSupportedException {
        Interpreter interpreter = (Interpreter)super.clone();
        interpreter.registers = (int[])this.registers.clone();
        interpreter.fpregs = (int[])this.fpregs.clone();
        return interpreter;
    }

    private final int runSome() throws Runtime.FaultException, Runtime.ExecutionException {
        block222 : {
            int n = 1 << this.pageShift >> 2;
            int[] arrn = this.registers;
            int[] arrn2 = this.fpregs;
            int n2 = this.pc;
            int n3 = n2 + 4;
            try {
                block211 : do {
                    int n4;
                    int n5;
                    try {
                        n4 = this.readPages[n2 >>> this.pageShift][n2 >>> 2 & n - 1];
                    }
                    catch (RuntimeException var7_9) {
                        if (n2 == -559038737) {
                            throw new Error("fell off cpu: r2: " + arrn[2]);
                        }
                        n4 = this.memRead(n2);
                    }
                    int n6 = n4 >>> 26 & 255;
                    int n7 = n4 >>> 21 & 31;
                    int n8 = n4 >>> 16 & 31;
                    int n9 = n4 >>> 16 & 31;
                    int n10 = n4 >>> 11 & 31;
                    int n11 = n4 >>> 11 & 31;
                    int n12 = n4 >>> 6 & 31;
                    int n13 = n4 >>> 6 & 31;
                    int n14 = n4 & 63;
                    int n15 = n4 & 67108863;
                    int n16 = n4 & 65535;
                    int n17 = n5 = n4 << 16 >> 16;
                    arrn[0] = 0;
                    block15 : switch (n6) {
                        int n18;
                        int n19;
                        int n20;
                        case 0: {
                            switch (n14) {
                                case 0: {
                                    if (n4 == 0) break block15;
                                    arrn[n10] = arrn[n8] << n12;
                                    break block15;
                                }
                                case 2: {
                                    arrn[n10] = arrn[n8] >>> n12;
                                    break block15;
                                }
                                case 3: {
                                    arrn[n10] = arrn[n8] >> n12;
                                    break block15;
                                }
                                case 4: {
                                    arrn[n10] = arrn[n8] << (arrn[n7] & 31);
                                    break block15;
                                }
                                case 6: {
                                    arrn[n10] = arrn[n8] >>> (arrn[n7] & 31);
                                    break block15;
                                }
                                case 7: {
                                    arrn[n10] = arrn[n8] >> (arrn[n7] & 31);
                                    break block15;
                                }
                                case 8: {
                                    n20 = arrn[n7];
                                    n2 += 4;
                                    n3 = n20;
                                    continue block211;
                                }
                                case 9: {
                                    n20 = arrn[n7];
                                    arrn[n10] = (n2 += 4) + 4;
                                    n3 = n20;
                                    continue block211;
                                }
                                case 12: {
                                    this.pc = n2;
                                    arrn[2] = this.syscall(arrn[2], arrn[4], arrn[5], arrn[6], arrn[7], arrn[8], arrn[9]);
                                    if (this.state == 0) break block15;
                                    this.pc = n3;
                                    break block222;
                                }
                                case 13: {
                                    throw new Runtime.ExecutionException("Break");
                                }
                                case 16: {
                                    arrn[n10] = this.hi;
                                    break block15;
                                }
                                case 17: {
                                    this.hi = arrn[n7];
                                    break block15;
                                }
                                case 18: {
                                    arrn[n10] = this.lo;
                                    break block15;
                                }
                                case 19: {
                                    this.lo = arrn[n7];
                                    break block15;
                                }
                                case 24: {
                                    n19 = (long)arrn[n7] * (long)arrn[n8];
                                    this.hi = n19 >>> 32;
                                    this.lo = n19;
                                    break block15;
                                }
                                case 25: {
                                    long l = ((long)arrn[n7] & 0xFFFFFFFFL) * ((long)arrn[n8] & 0xFFFFFFFFL);
                                    this.hi = (int)(l >>> 32);
                                    this.lo = (int)l;
                                    break block15;
                                }
                                case 26: {
                                    this.hi = arrn[n7] % arrn[n8];
                                    this.lo = arrn[n7] / arrn[n8];
                                    break block15;
                                }
                                case 27: {
                                    if (n8 == 0) break block15;
                                    this.hi = (int)(((long)arrn[n7] & 0xFFFFFFFFL) % ((long)arrn[n8] & 0xFFFFFFFFL));
                                    this.lo = (int)(((long)arrn[n7] & 0xFFFFFFFFL) / ((long)arrn[n8] & 0xFFFFFFFFL));
                                    break block15;
                                }
                                case 32: {
                                    throw new Runtime.ExecutionException("ADD (add with oveflow trap) not suported");
                                }
                                case 33: {
                                    arrn[n10] = arrn[n7] + arrn[n8];
                                    break block15;
                                }
                                case 34: {
                                    throw new Runtime.ExecutionException("SUB (sub with oveflow trap) not suported");
                                }
                                case 35: {
                                    arrn[n10] = arrn[n7] - arrn[n8];
                                    break block15;
                                }
                                case 36: {
                                    arrn[n10] = arrn[n7] & arrn[n8];
                                    break block15;
                                }
                                case 37: {
                                    arrn[n10] = arrn[n7] | arrn[n8];
                                    break block15;
                                }
                                case 38: {
                                    arrn[n10] = arrn[n7] ^ arrn[n8];
                                    break block15;
                                }
                                case 39: {
                                    arrn[n10] = ~ (arrn[n7] | arrn[n8]);
                                    break block15;
                                }
                                case 42: {
                                    arrn[n10] = arrn[n7] < arrn[n8] ? 1 : 0;
                                    break block15;
                                }
                                case 43: {
                                    arrn[n10] = ((long)arrn[n7] & 0xFFFFFFFFL) < ((long)arrn[n8] & 0xFFFFFFFFL) ? 1 : 0;
                                    break block15;
                                }
                            }
                            throw new Runtime.ExecutionException("Illegal instruction 0/" + n14);
                        }
                        case 1: {
                            switch (n8) {
                                case 0: {
                                    if (arrn[n7] >= 0) break block15;
                                    n3 = n20 = (n2 += 4) + n17 * 4;
                                    continue block211;
                                }
                                case 1: {
                                    if (arrn[n7] < 0) break block15;
                                    n3 = n20 = (n2 += 4) + n17 * 4;
                                    continue block211;
                                }
                                case 16: {
                                    if (arrn[n7] >= 0) break block15;
                                    arrn[31] = (n2 += 4) + 4;
                                    n3 = n20 = n2 + n17 * 4;
                                    continue block211;
                                }
                                case 17: {
                                    if (arrn[n7] < 0) break block15;
                                    arrn[31] = (n2 += 4) + 4;
                                    n3 = n20 = n2 + n17 * 4;
                                    continue block211;
                                }
                            }
                            throw new Runtime.ExecutionException("Illegal Instruction");
                        }
                        case 2: {
                            n20 = n2 & -268435456 | n15 << 2;
                            n2 += 4;
                            n3 = n20;
                            continue block211;
                        }
                        case 3: {
                            n20 = n2 & -268435456 | n15 << 2;
                            arrn[31] = (n2 += 4) + 4;
                            n3 = n20;
                            continue block211;
                        }
                        case 4: {
                            if (arrn[n7] != arrn[n8]) break;
                            n3 = n20 = (n2 += 4) + n17 * 4;
                            continue block211;
                        }
                        case 5: {
                            if (arrn[n7] == arrn[n8]) break;
                            n3 = n20 = (n2 += 4) + n17 * 4;
                            continue block211;
                        }
                        case 6: {
                            if (arrn[n7] > 0) break;
                            n3 = n20 = (n2 += 4) + n17 * 4;
                            continue block211;
                        }
                        case 7: {
                            if (arrn[n7] <= 0) break;
                            n3 = n20 = (n2 += 4) + n17 * 4;
                            continue block211;
                        }
                        case 8: {
                            arrn[n8] = arrn[n7] + n5;
                            break;
                        }
                        case 9: {
                            arrn[n8] = arrn[n7] + n5;
                            break;
                        }
                        case 10: {
                            arrn[n8] = arrn[n7] < n5 ? 1 : 0;
                            break;
                        }
                        case 11: {
                            arrn[n8] = ((long)arrn[n7] & 0xFFFFFFFFL) < ((long)n5 & 0xFFFFFFFFL) ? 1 : 0;
                            break;
                        }
                        case 12: {
                            arrn[n8] = arrn[n7] & n16;
                            break;
                        }
                        case 13: {
                            arrn[n8] = arrn[n7] | n16;
                            break;
                        }
                        case 14: {
                            arrn[n8] = arrn[n7] ^ n16;
                            break;
                        }
                        case 15: {
                            arrn[n8] = n16 << 16;
                            break;
                        }
                        case 16: {
                            throw new Runtime.ExecutionException("TLB/Exception support not implemented");
                        }
                        case 17: {
                            boolean bl;
                            n19 = 0;
                            String string = n19 != 0 ? this.sourceLine(n2) : "";
                            boolean bl2 = bl = n19 != 0 && (string.indexOf("dtoa.c:51") >= 0 || string.indexOf("dtoa.c:52") >= 0 || string.indexOf("test.c") >= 0);
                            if (n7 > 8 && bl) {
                                System.out.println("               FP Op: " + n6 + "/" + n7 + "/" + n14 + " " + string);
                            }
                            if (this.roundingMode() != 0 && n7 != 6 && (n7 != 16 && n7 != 17 || n14 != 36)) {
                                throw new Runtime.ExecutionException("Non-cvt.w.z operation attempted with roundingMode != round to nearest");
                            }
                            switch (n7) {
                                case 0: {
                                    arrn[n8] = arrn2[n10];
                                    break block15;
                                }
                                case 2: {
                                    if (n11 != 31) {
                                        throw new Runtime.ExecutionException("FCR " + n11 + " unavailable");
                                    }
                                    arrn[n8] = this.fcsr;
                                    break block15;
                                }
                                case 4: {
                                    arrn2[n10] = arrn[n8];
                                    break block15;
                                }
                                case 6: {
                                    if (n11 != 31) {
                                        throw new Runtime.ExecutionException("FCR " + n11 + " unavailable");
                                    }
                                    this.fcsr = arrn[n8];
                                    break block15;
                                }
                                case 8: {
                                    if ((this.fcsr & 8388608) != 0 != ((n4 >>> 16 & 1) != 0)) break block15;
                                    n3 = n20 = (n2 += 4) + n17 * 4;
                                    continue block211;
                                }
                                case 16: {
                                    switch (n14) {
                                        case 0: {
                                            this.setFloat(n13, this.getFloat(n11) + this.getFloat(n9));
                                            break block15;
                                        }
                                        case 1: {
                                            this.setFloat(n13, this.getFloat(n11) - this.getFloat(n9));
                                            break block15;
                                        }
                                        case 2: {
                                            this.setFloat(n13, this.getFloat(n11) * this.getFloat(n9));
                                            break block15;
                                        }
                                        case 3: {
                                            this.setFloat(n13, this.getFloat(n11) / this.getFloat(n9));
                                            break block15;
                                        }
                                        case 5: {
                                            this.setFloat(n13, Math.abs(this.getFloat(n11)));
                                            break block15;
                                        }
                                        case 6: {
                                            arrn2[n13] = arrn2[n11];
                                            break block15;
                                        }
                                        case 7: {
                                            this.setFloat(n13, - this.getFloat(n11));
                                            break block15;
                                        }
                                        case 33: {
                                            this.setDouble(n13, this.getFloat(n11));
                                            break block15;
                                        }
                                        case 36: {
                                            switch (this.roundingMode()) {
                                                case 0: {
                                                    arrn2[n13] = (int)Math.floor(this.getFloat(n11) + 0.5f);
                                                    break block15;
                                                }
                                                case 1: {
                                                    arrn2[n13] = (int)this.getFloat(n11);
                                                    break block15;
                                                }
                                                case 2: {
                                                    arrn2[n13] = (int)Math.ceil(this.getFloat(n11));
                                                    break block15;
                                                }
                                                case 3: {
                                                    arrn2[n13] = (int)Math.floor(this.getFloat(n11));
                                                }
                                            }
                                            break block15;
                                        }
                                        case 50: {
                                            this.setFC(this.getFloat(n11) == this.getFloat(n9));
                                            break block15;
                                        }
                                        case 60: {
                                            this.setFC(this.getFloat(n11) < this.getFloat(n9));
                                            break block15;
                                        }
                                        case 62: {
                                            this.setFC(this.getFloat(n11) <= this.getFloat(n9));
                                            break block15;
                                        }
                                    }
                                    throw new Runtime.ExecutionException("Invalid Instruction 17/" + n7 + "/" + n14 + " at " + this.sourceLine(n2));
                                }
                                case 17: {
                                    switch (n14) {
                                        case 0: {
                                            this.setDouble(n13, this.getDouble(n11) + this.getDouble(n9));
                                            break block15;
                                        }
                                        case 1: {
                                            if (bl) {
                                                System.out.println("f" + n13 + " = f" + n11 + " (" + this.getDouble(n11) + ") - f" + n9 + " (" + this.getDouble(n9) + ")");
                                            }
                                            this.setDouble(n13, this.getDouble(n11) - this.getDouble(n9));
                                            break block15;
                                        }
                                        case 2: {
                                            if (bl) {
                                                System.out.println("f" + n13 + " = f" + n11 + " (" + this.getDouble(n11) + ") * f" + n9 + " (" + this.getDouble(n9) + ")");
                                            }
                                            this.setDouble(n13, this.getDouble(n11) * this.getDouble(n9));
                                            if (!bl) break block15;
                                            System.out.println("f" + n13 + " = " + this.getDouble(n13));
                                            break block15;
                                        }
                                        case 3: {
                                            this.setDouble(n13, this.getDouble(n11) / this.getDouble(n9));
                                            break block15;
                                        }
                                        case 5: {
                                            this.setDouble(n13, Math.abs(this.getDouble(n11)));
                                            break block15;
                                        }
                                        case 6: {
                                            arrn2[n13] = arrn2[n11];
                                            arrn2[n13 + 1] = arrn2[n11 + 1];
                                            break block15;
                                        }
                                        case 7: {
                                            this.setDouble(n13, - this.getDouble(n11));
                                            break block15;
                                        }
                                        case 32: {
                                            this.setFloat(n13, (float)this.getDouble(n11));
                                            break block15;
                                        }
                                        case 36: {
                                            if (bl) {
                                                System.out.println("CVT.W.D rm: " + this.roundingMode() + " f" + n11 + ":" + this.getDouble(n11));
                                            }
                                            switch (this.roundingMode()) {
                                                case 0: {
                                                    arrn2[n13] = (int)Math.floor(this.getDouble(n11) + 0.5);
                                                    break;
                                                }
                                                case 1: {
                                                    arrn2[n13] = (int)this.getDouble(n11);
                                                    break;
                                                }
                                                case 2: {
                                                    arrn2[n13] = (int)Math.ceil(this.getDouble(n11));
                                                    break;
                                                }
                                                case 3: {
                                                    arrn2[n13] = (int)Math.floor(this.getDouble(n11));
                                                }
                                            }
                                            if (!bl) break block15;
                                            System.out.println("CVT.W.D: f" + n13 + ":" + arrn2[n13]);
                                            break block15;
                                        }
                                        case 50: {
                                            this.setFC(this.getDouble(n11) == this.getDouble(n9));
                                            break block15;
                                        }
                                        case 60: {
                                            this.setFC(this.getDouble(n11) < this.getDouble(n9));
                                            break block15;
                                        }
                                        case 62: {
                                            this.setFC(this.getDouble(n11) <= this.getDouble(n9));
                                            break block15;
                                        }
                                    }
                                    throw new Runtime.ExecutionException("Invalid Instruction 17/" + n7 + "/" + n14 + " at " + this.sourceLine(n2));
                                }
                                case 20: {
                                    switch (n14) {
                                        case 32: {
                                            this.setFloat(n13, arrn2[n11]);
                                            break block15;
                                        }
                                        case 33: {
                                            this.setDouble(n13, arrn2[n11]);
                                            break block15;
                                        }
                                    }
                                    throw new Runtime.ExecutionException("Invalid Instruction 17/" + n7 + "/" + n14 + " at " + this.sourceLine(n2));
                                }
                            }
                            throw new Runtime.ExecutionException("Invalid Instruction 17/" + n7);
                        }
                        case 18: 
                        case 19: {
                            throw new Runtime.ExecutionException("No coprocessor installed");
                        }
                        case 32: {
                            n18 = arrn[n7] + n5;
                            try {
                                n20 = this.readPages[n18 >>> this.pageShift][n18 >>> 2 & n - 1];
                            }
                            catch (RuntimeException var22_26) {
                                n20 = this.memRead(n18 & -4);
                            }
                            switch (n18 & 3) {
                                case 0: {
                                    n20 = n20 >>> 24 & 255;
                                    break;
                                }
                                case 1: {
                                    n20 = n20 >>> 16 & 255;
                                    break;
                                }
                                case 2: {
                                    n20 = n20 >>> 8 & 255;
                                    break;
                                }
                                case 3: {
                                    n20 = n20 >>> 0 & 255;
                                }
                            }
                            if ((n20 & 128) != 0) {
                                n20 |= -256;
                            }
                            arrn[n8] = n20;
                            break;
                        }
                        case 33: {
                            n18 = arrn[n7] + n5;
                            try {
                                n20 = this.readPages[n18 >>> this.pageShift][n18 >>> 2 & n - 1];
                            }
                            catch (RuntimeException var22_27) {
                                n20 = this.memRead(n18 & -4);
                            }
                            switch (n18 & 3) {
                                case 0: {
                                    n20 = n20 >>> 16 & 65535;
                                    break;
                                }
                                case 2: {
                                    n20 = n20 >>> 0 & 65535;
                                    break;
                                }
                                default: {
                                    throw new Runtime.ReadFaultException(n18);
                                }
                            }
                            if ((n20 & 32768) != 0) {
                                n20 |= -65536;
                            }
                            arrn[n8] = n20;
                            break;
                        }
                        case 34: {
                            n18 = arrn[n7] + n5;
                            try {
                                n20 = this.readPages[n18 >>> this.pageShift][n18 >>> 2 & n - 1];
                            }
                            catch (RuntimeException var22_28) {
                                n20 = this.memRead(n18 & -4);
                            }
                            switch (n18 & 3) {
                                case 0: {
                                    arrn[n8] = arrn[n8] & 0 | n20 << 0;
                                    break block15;
                                }
                                case 1: {
                                    arrn[n8] = arrn[n8] & 255 | n20 << 8;
                                    break block15;
                                }
                                case 2: {
                                    arrn[n8] = arrn[n8] & 65535 | n20 << 16;
                                    break block15;
                                }
                                case 3: {
                                    arrn[n8] = arrn[n8] & 16777215 | n20 << 24;
                                }
                            }
                            break;
                        }
                        case 35: {
                            n18 = arrn[n7] + n5;
                            try {
                                arrn[n8] = this.readPages[n18 >>> this.pageShift][n18 >>> 2 & n - 1];
                            }
                            catch (RuntimeException var22_29) {
                                arrn[n8] = this.memRead(n18);
                            }
                            break;
                        }
                        case 36: {
                            n18 = arrn[n7] + n5;
                            try {
                                n20 = this.readPages[n18 >>> this.pageShift][n18 >>> 2 & n - 1];
                            }
                            catch (RuntimeException var22_30) {
                                n20 = this.memRead(n18);
                            }
                            switch (n18 & 3) {
                                case 0: {
                                    arrn[n8] = n20 >>> 24 & 255;
                                    break block15;
                                }
                                case 1: {
                                    arrn[n8] = n20 >>> 16 & 255;
                                    break block15;
                                }
                                case 2: {
                                    arrn[n8] = n20 >>> 8 & 255;
                                    break block15;
                                }
                                case 3: {
                                    arrn[n8] = n20 >>> 0 & 255;
                                }
                            }
                            break;
                        }
                        case 37: {
                            n18 = arrn[n7] + n5;
                            try {
                                n20 = this.readPages[n18 >>> this.pageShift][n18 >>> 2 & n - 1];
                            }
                            catch (RuntimeException var22_31) {
                                n20 = this.memRead(n18 & -4);
                            }
                            switch (n18 & 3) {
                                case 0: {
                                    arrn[n8] = n20 >>> 16 & 65535;
                                    break block15;
                                }
                                case 2: {
                                    arrn[n8] = n20 >>> 0 & 65535;
                                    break block15;
                                }
                            }
                            throw new Runtime.ReadFaultException(n18);
                        }
                        case 38: {
                            n18 = arrn[n7] + n5;
                            try {
                                n20 = this.readPages[n18 >>> this.pageShift][n18 >>> 2 & n - 1];
                            }
                            catch (RuntimeException var22_32) {
                                n20 = this.memRead(n18 & -4);
                            }
                            switch (n18 & 3) {
                                case 0: {
                                    arrn[n8] = arrn[n8] & -256 | n20 >>> 24;
                                    break block15;
                                }
                                case 1: {
                                    arrn[n8] = arrn[n8] & -65536 | n20 >>> 16;
                                    break block15;
                                }
                                case 2: {
                                    arrn[n8] = arrn[n8] & -16777216 | n20 >>> 8;
                                    break block15;
                                }
                                case 3: {
                                    arrn[n8] = arrn[n8] & 0 | n20 >>> 0;
                                }
                            }
                            break;
                        }
                        case 40: {
                            n18 = arrn[n7] + n5;
                            try {
                                n20 = this.readPages[n18 >>> this.pageShift][n18 >>> 2 & n - 1];
                            }
                            catch (RuntimeException var22_33) {
                                n20 = this.memRead(n18 & -4);
                            }
                            switch (n18 & 3) {
                                case 0: {
                                    n20 = n20 & 16777215 | (arrn[n8] & 255) << 24;
                                    break;
                                }
                                case 1: {
                                    n20 = n20 & -16711681 | (arrn[n8] & 255) << 16;
                                    break;
                                }
                                case 2: {
                                    n20 = n20 & -65281 | (arrn[n8] & 255) << 8;
                                    break;
                                }
                                case 3: {
                                    n20 = n20 & -256 | (arrn[n8] & 255) << 0;
                                }
                            }
                            try {
                                this.writePages[n18 >>> this.pageShift][n18 >>> 2 & n - 1] = n20;
                            }
                            catch (RuntimeException var22_34) {
                                this.memWrite(n18 & -4, n20);
                            }
                            break;
                        }
                        case 41: {
                            n18 = arrn[n7] + n5;
                            try {
                                n20 = this.readPages[n18 >>> this.pageShift][n18 >>> 2 & n - 1];
                            }
                            catch (RuntimeException var22_35) {
                                n20 = this.memRead(n18 & -4);
                            }
                            switch (n18 & 3) {
                                case 0: {
                                    n20 = n20 & 65535 | (arrn[n8] & 65535) << 16;
                                    break;
                                }
                                case 2: {
                                    n20 = n20 & -65536 | (arrn[n8] & 65535) << 0;
                                    break;
                                }
                                default: {
                                    throw new Runtime.WriteFaultException(n18);
                                }
                            }
                            try {
                                this.writePages[n18 >>> this.pageShift][n18 >>> 2 & n - 1] = n20;
                            }
                            catch (RuntimeException var22_36) {
                                this.memWrite(n18 & -4, n20);
                            }
                            break;
                        }
                        case 42: {
                            n18 = arrn[n7] + n5;
                            n20 = this.memRead(n18 & -4);
                            switch (n18 & 3) {
                                case 0: {
                                    n20 = n20 & 0 | arrn[n8] >>> 0;
                                    break;
                                }
                                case 1: {
                                    n20 = n20 & -16777216 | arrn[n8] >>> 8;
                                    break;
                                }
                                case 2: {
                                    n20 = n20 & -65536 | arrn[n8] >>> 16;
                                    break;
                                }
                                case 3: {
                                    n20 = n20 & -256 | arrn[n8] >>> 24;
                                }
                            }
                            try {
                                this.writePages[n18 >>> this.pageShift][n18 >>> 2 & n - 1] = n20;
                            }
                            catch (RuntimeException var22_37) {
                                this.memWrite(n18 & -4, n20);
                            }
                            break;
                        }
                        case 43: {
                            n18 = arrn[n7] + n5;
                            try {
                                this.writePages[n18 >>> this.pageShift][n18 >>> 2 & n - 1] = arrn[n8];
                            }
                            catch (RuntimeException var22_38) {
                                this.memWrite(n18 & -4, arrn[n8]);
                            }
                            break;
                        }
                        case 46: {
                            n18 = arrn[n7] + n5;
                            n20 = this.memRead(n18 & -4);
                            switch (n18 & 3) {
                                case 0: {
                                    n20 = n20 & 16777215 | arrn[n8] << 24;
                                    break;
                                }
                                case 1: {
                                    n20 = n20 & 65535 | arrn[n8] << 16;
                                    break;
                                }
                                case 2: {
                                    n20 = n20 & 255 | arrn[n8] << 8;
                                    break;
                                }
                                case 3: {
                                    n20 = n20 & 0 | arrn[n8] << 0;
                                }
                            }
                            this.memWrite(n18 & -4, n20);
                            break;
                        }
                        case 48: {
                            arrn[n8] = this.memRead(arrn[n7] + n5);
                            break;
                        }
                        case 49: {
                            arrn2[n8] = this.memRead(arrn[n7] + n5);
                            break;
                        }
                        case 56: {
                            this.memWrite(arrn[n7] + n5, arrn[n8]);
                            arrn[n8] = 1;
                            break;
                        }
                        case 57: {
                            this.memWrite(arrn[n7] + n5, arrn2[n8]);
                            break;
                        }
                        default: {
                            throw new Runtime.ExecutionException("Invalid Instruction: " + n6);
                        }
                    }
                    n2 = n3;
                    n3 = n2 + 4;
                } while (true);
            }
            catch (Runtime.ExecutionException var6_7) {
                this.pc = n2;
                throw var6_7;
            }
        }
        return 0;
    }

    public int lookupSymbol(String string) {
        ELF.Symbol symbol = this.symtab.getGlobalSymbol(string);
        return symbol == null ? -1 : symbol.addr;
    }

    protected int gp() {
        return this.gp;
    }

    protected int userInfoBae() {
        return this.userInfo == null ? 0 : this.userInfo.addr;
    }

    protected int userInfoSize() {
        return this.userInfo == null ? 0 : this.userInfo.size;
    }

    protected int entryPoint() {
        return this.entryPoint;
    }

    protected int heapStart() {
        return this.heapStart;
    }

    private void loadImage(Seekable seekable) throws IOException {
        ELF eLF = new ELF(seekable);
        this.symtab = eLF.getSymtab();
        if (eLF.header.type != 2) {
            throw new IOException("Binary is not an executable");
        }
        if (eLF.header.machine != 8) {
            throw new IOException("Binary is not for the MIPS I Architecture");
        }
        if (eLF.ident.data != 2) {
            throw new IOException("Binary is not big endian");
        }
        this.entryPoint = eLF.header.entry;
        ELF.Symtab symtab = eLF.getSymtab();
        if (symtab == null) {
            throw new IOException("No symtab in binary (did you strip it?)");
        }
        this.userInfo = symtab.getGlobalSymbol("user_info");
        ELF.Symbol symbol = symtab.getGlobalSymbol("_gp");
        if (symbol == null) {
            throw new IOException("NO _gp symbol!");
        }
        this.gp = symbol.addr;
        this.entryPoint = eLF.header.entry;
        ELF.PHeader[] arrpHeader = eLF.pheaders;
        int n = 0;
        int n2 = 1 << this.pageShift;
        int n3 = 1 << this.pageShift >> 2;
        for (int i = 0; i < arrpHeader.length; ++i) {
            ELF.PHeader pHeader = arrpHeader[i];
            if (pHeader.type != 1) continue;
            int n4 = pHeader.memsz;
            int n5 = pHeader.filesz;
            if (n4 == 0) continue;
            if (n4 < 0) {
                throw new IOException("pheader size too large");
            }
            int n6 = pHeader.vaddr;
            if (n6 == 0) {
                throw new IOException("pheader vaddr == 0x0");
            }
            n = Interpreter.max(n6 + n4, n);
            for (int j = 0; j < n4 + n2 - 1; j += n2) {
                int n7 = j + n6 >>> this.pageShift;
                if (this.readPages[n7] == null) {
                    this.readPages[n7] = new int[n3];
                }
                if (!pHeader.writable()) continue;
                this.writePages[n7] = this.readPages[n7];
            }
            if (n5 == 0) continue;
            n5 &= -4;
            DataInputStream dataInputStream = new DataInputStream(pHeader.getInputStream());
            do {
                this.readPages[n6 >>> this.pageShift][n6 >>> 2 & n3 - 1] = dataInputStream.readInt();
                n6 += 4;
            } while ((n5 -= 4) > 0);
            dataInputStream.close();
        }
        this.heapStart = n + n2 - 1 & ~ (n2 - 1);
    }

    protected void setCPUState(Runtime.CPUState cPUState) {
        int n;
        for (n = 1; n < 32; ++n) {
            this.registers[n] = cPUState.r[n];
        }
        for (n = 0; n < 32; ++n) {
            this.fpregs[n] = cPUState.f[n];
        }
        this.hi = cPUState.hi;
        this.lo = cPUState.lo;
        this.fcsr = cPUState.fcsr;
        this.pc = cPUState.pc;
    }

    protected void getCPUState(Runtime.CPUState cPUState) {
        int n;
        for (n = 1; n < 32; ++n) {
            cPUState.r[n] = this.registers[n];
        }
        for (n = 0; n < 32; ++n) {
            cPUState.f[n] = this.fpregs[n];
        }
        cPUState.hi = this.hi;
        cPUState.lo = this.lo;
        cPUState.fcsr = this.fcsr;
        cPUState.pc = this.pc;
    }

    public Interpreter(Seekable seekable) throws IOException {
        super(4096, 65536);
        this.loadImage(seekable);
    }

    public Interpreter(String string) throws IOException {
        this(new Seekable.File(string, false));
        this.image = string;
    }

    public Interpreter(InputStream inputStream) throws IOException {
        this(new Seekable.InputStream(inputStream));
    }

    public String sourceLine(int n) {
        String string = this.sourceLineCache == null ? null : this.sourceLineCache.get(new Integer(n));
        if (string != null) {
            return string;
        }
        if (this.image == null) {
            return null;
        }
        try {
            Process process = java.lang.Runtime.getRuntime().exec(new String[]{"mips-unknown-elf-addr2line", "-e", this.image, Interpreter.toHex(n)});
            string = new BufferedReader(new InputStreamReader(process.getInputStream())).readLine();
            if (string == null) {
                return null;
            }
            while (string.startsWith("../")) {
                string = string.substring(3);
            }
            if (this.sourceLineCache == null) {
                this.sourceLineCache = new HashMap();
            }
            this.sourceLineCache.put(new Integer(n), string);
            return string;
        }
        catch (IOException var4_4) {
            return null;
        }
    }

    public static void main(String[] arrstring) throws Exception {
        String string = arrstring[0];
        Interpreter interpreter = new Interpreter(string);
        java.lang.Runtime.getRuntime().addShutdownHook(new Thread(interpreter.new DebugShutdownHook()));
        int n = interpreter.run(arrstring);
        System.err.println("Exit status: " + n);
        System.exit(n);
    }

    public class DebugShutdownHook
    implements Runnable {
        public void run() {
            int n = Interpreter.this.pc;
            if (Interpreter.this.getState() == 0) {
                System.err.print("\nCPU Executing " + Runtime.toHex(n) + ": " + Interpreter.this.sourceLine(n) + "\n");
            }
        }
    }

}

