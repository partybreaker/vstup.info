/*
 * Decompiled with CFR 0_114.
 */
package org.ibex.nestedvm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import org.ibex.nestedvm.Registers;
import org.ibex.nestedvm.UsermodeConstants;
import org.ibex.nestedvm.util.Platform;
import org.ibex.nestedvm.util.Seekable;

public abstract class Runtime
implements UsermodeConstants,
Registers,
Cloneable {
    public static final String VERSION = "1.0";
    static final boolean STDERR_DIAG = true;
    protected final int pageShift;
    private final int stackBottom;
    protected int[][] readPages;
    protected int[][] writePages;
    private int heapEnd;
    private static final int STACK_GUARD_PAGES = 4;
    private long startTime;
    public static final int RUNNING = 0;
    public static final int STOPPED = 1;
    public static final int PAUSED = 2;
    public static final int CALLJAVA = 3;
    public static final int EXITED = 4;
    public static final int EXECED = 5;
    protected int state = 1;
    private int exitStatus;
    public ExecutionException exitException;
    FD[] fds;
    boolean[] closeOnExec;
    SecurityManager sm;
    private CallJavaCB callJavaCB;
    private byte[] _byteBuf;
    static final int MAX_CHUNK = 16776192;
    static final boolean win32Hacks;
    public static final int RD_ONLY = 0;
    public static final int WR_ONLY = 1;
    public static final int RDWR = 2;
    public static final int O_CREAT = 512;
    public static final int O_EXCL = 2048;
    public static final int O_APPEND = 8;
    public static final int O_TRUNC = 1024;
    public static final int O_NONBLOCK = 16384;
    public static final int O_NOCTTY = 32768;

    protected abstract int heapStart();

    protected abstract int entryPoint();

    protected int userInfoBase() {
        return 0;
    }

    protected int userInfoSize() {
        return 0;
    }

    protected abstract int gp();

    public final int getState() {
        return this.state;
    }

    public void setSecurityManager(SecurityManager securityManager) {
        this.sm = securityManager;
    }

    public void setCallJavaCB(CallJavaCB callJavaCB) {
        this.callJavaCB = callJavaCB;
    }

    protected abstract void _execute() throws ExecutionException;

    public int lookupSymbol(String string) {
        return -1;
    }

    protected abstract void getCPUState(CPUState var1);

    protected abstract void setCPUState(CPUState var1);

    protected Object clone() throws CloneNotSupportedException {
        int n;
        Runtime runtime = (Runtime)super.clone();
        runtime._byteBuf = null;
        runtime.startTime = 0;
        runtime.fds = new FD[64];
        for (n = 0; n < 64; ++n) {
            if (this.fds[n] == null) continue;
            runtime.fds[n] = this.fds[n].dup();
        }
        n = this.writePages.length;
        runtime.readPages = new int[n][];
        runtime.writePages = new int[n][];
        for (int i = 0; i < n; ++i) {
            if (this.readPages[i] == null) continue;
            runtime.readPages[i] = this.writePages[i] == null ? this.readPages[i] : (runtime.writePages[i] = (int[])this.writePages[i].clone());
        }
        return runtime;
    }

    protected Runtime(int n, int n2) {
        this(n, n2, false);
    }

    protected Runtime(int n, int n2, boolean bl) {
        if (n <= 0) {
            throw new IllegalArgumentException("pageSize <= 0");
        }
        if (n2 <= 0) {
            throw new IllegalArgumentException("totalPages <= 0");
        }
        if ((n & n - 1) != 0) {
            throw new IllegalArgumentException("pageSize not a power of two");
        }
        int n3 = 0;
        while (n >>> n3 != 1) {
            ++n3;
        }
        this.pageShift = n3;
        int n4 = this.heapStart();
        int n5 = n2 * n;
        int n6 = Runtime.max(n5 / 512, 131072);
        int n7 = 0;
        if (n2 > 1) {
            n6 = Runtime.max(n6, n);
            n7 = (n6 = n6 + n - 1 & ~ (n - 1)) >>> this.pageShift;
            if (n7 + 4 + ((n4 = n4 + n - 1 & ~ (n - 1)) >>> this.pageShift) >= n2) {
                throw new IllegalArgumentException("total pages too small");
            }
        } else {
            if (n < n4 + n6) {
                throw new IllegalArgumentException("total memory too small");
            }
            n4 = n4 + 4095 & -4097;
        }
        this.stackBottom = n5 - n6;
        this.heapEnd = n4;
        this.readPages = new int[n2][];
        this.writePages = new int[n2][];
        if (n2 == 1) {
            this.readPages[0] = this.writePages[0] = new int[n >> 2];
        } else {
            for (int i = this.stackBottom >>> this.pageShift; i < this.writePages.length; ++i) {
                this.readPages[i] = this.writePages[i] = new int[n >> 2];
            }
        }
        if (!bl) {
            this.fds = new FD[64];
            this.closeOnExec = new boolean[64];
            InputStream inputStream = win32Hacks ? new Win32ConsoleIS(System.in) : System.in;
            this.addFD(new TerminalFD(inputStream));
            this.addFD(new TerminalFD(System.out));
            this.addFD(new TerminalFD(System.err));
        }
    }

    protected final void initPages(int[] arrn, int n, boolean bl) {
        int n2 = 1 << this.pageShift >>> 2;
        int n3 = (1 << this.pageShift) - 1;
        int n4 = 0;
        while (n4 < arrn.length) {
            int n5 = n >>> this.pageShift;
            int n6 = (n & n3) >> 2;
            int n7 = Runtime.min(n2 - n6, arrn.length - n4);
            if (this.readPages[n5] == null) {
                this.initPage(n5, bl);
            } else if (!bl && this.writePages[n5] == null) {
                this.writePages[n5] = this.readPages[n5];
            }
            System.arraycopy(arrn, n4, this.readPages[n5], n6, n7);
            n4 += n7;
            n += n7 * 4;
        }
    }

    protected final void clearPages(int n, int n2) {
        int n3 = 1 << this.pageShift >>> 2;
        int n4 = (1 << this.pageShift) - 1;
        int n5 = 0;
        while (n5 < n2) {
            int n6 = n >>> this.pageShift;
            int n7 = (n & n4) >> 2;
            int n8 = Runtime.min(n3 - n7, n2 - n5);
            if (this.readPages[n6] == null) {
                this.readPages[n6] = this.writePages[n6] = new int[n3];
            } else {
                if (this.writePages[n6] == null) {
                    this.writePages[n6] = this.readPages[n6];
                }
                for (int i = n7; i < n7 + n8; ++i) {
                    this.writePages[n6][i] = 0;
                }
            }
            n5 += n8;
            n += n8 * 4;
        }
    }

    public final void copyin(int n, byte[] arrby, int n2) throws ReadFaultException {
        int n3;
        int n4 = 1 << this.pageShift >>> 2;
        int n5 = n4 - 1;
        int n6 = 0;
        if (n2 == 0) {
            return;
        }
        if ((n & 3) != 0) {
            n3 = this.memRead(n & -4);
            switch (n & 3) {
                case 1: {
                    arrby[n6++] = (byte)(n3 >>> 16 & 255);
                    if (--n2 == 0) break;
                }
                case 2: {
                    arrby[n6++] = (byte)(n3 >>> 8 & 255);
                    if (--n2 == 0) break;
                }
                case 3: {
                    arrby[n6++] = (byte)(n3 >>> 0 & 255);
                    if (--n2 == 0) {
                        // empty if block
                    } else {
                        break;
                    }
                }
            }
            n = (n & -4) + 4;
        }
        if ((n2 & -4) != 0) {
            int n7;
            int n8 = n >>> 2;
            for (n3 = n2 >>> 2; n3 != 0; n3 -= n7) {
                int[] arrn = this.readPages[n8 >>> this.pageShift - 2];
                if (arrn == null) {
                    throw new ReadFaultException(n8 << 2);
                }
                int n9 = n8 & n5;
                n7 = Runtime.min(n3, n4 - n9);
                int n10 = 0;
                while (n10 < n7) {
                    int n11 = arrn[n9 + n10];
                    arrby[n6 + 0] = (byte)(n11 >>> 24 & 255);
                    arrby[n6 + 1] = (byte)(n11 >>> 16 & 255);
                    arrby[n6 + 2] = (byte)(n11 >>> 8 & 255);
                    arrby[n6 + 3] = (byte)(n11 >>> 0 & 255);
                    ++n10;
                    n6 += 4;
                }
                n8 += n7;
            }
            n = n8 << 2;
            n2 &= 3;
        }
        if (n2 != 0) {
            n3 = this.memRead(n);
            switch (n2) {
                case 3: {
                    arrby[n6 + 2] = (byte)(n3 >>> 8 & 255);
                }
                case 2: {
                    arrby[n6 + 1] = (byte)(n3 >>> 16 & 255);
                }
                case 1: {
                    arrby[n6 + 0] = (byte)(n3 >>> 24 & 255);
                }
            }
        }
    }

    public final void copyout(byte[] arrby, int n, int n2) throws FaultException {
        int n3;
        int n4 = 1 << this.pageShift >>> 2;
        int n5 = n4 - 1;
        int n6 = 0;
        if (n2 == 0) {
            return;
        }
        if ((n & 3) != 0) {
            n3 = this.memRead(n & -4);
            switch (n & 3) {
                case 1: {
                    n3 = n3 & -16711681 | (arrby[n6++] & 255) << 16;
                    if (--n2 == 0) break;
                }
                case 2: {
                    n3 = n3 & -65281 | (arrby[n6++] & 255) << 8;
                    if (--n2 == 0) break;
                }
                case 3: {
                    n3 = n3 & -256 | (arrby[n6++] & 255) << 0;
                    if (--n2 == 0) {
                        // empty if block
                    } else {
                        break;
                    }
                }
            }
            this.memWrite(n & -4, n3);
            n += n6;
        }
        if ((n2 & -4) != 0) {
            int n7;
            int n8 = n >>> 2;
            for (n3 = n2 >>> 2; n3 != 0; n3 -= n7) {
                int[] arrn = this.writePages[n8 >>> this.pageShift - 2];
                if (arrn == null) {
                    throw new WriteFaultException(n8 << 2);
                }
                int n9 = n8 & n5;
                n7 = Runtime.min(n3, n4 - n9);
                int n10 = 0;
                while (n10 < n7) {
                    arrn[n9 + n10] = (arrby[n6 + 0] & 255) << 24 | (arrby[n6 + 1] & 255) << 16 | (arrby[n6 + 2] & 255) << 8 | (arrby[n6 + 3] & 255) << 0;
                    ++n10;
                    n6 += 4;
                }
                n8 += n7;
            }
            n = n8 << 2;
            n2 &= 3;
        }
        if (n2 != 0) {
            n3 = this.memRead(n);
            switch (n2) {
                case 1: {
                    n3 = n3 & 16777215 | (arrby[n6 + 0] & 255) << 24;
                    break;
                }
                case 2: {
                    n3 = n3 & 65535 | (arrby[n6 + 0] & 255) << 24 | (arrby[n6 + 1] & 255) << 16;
                    break;
                }
                case 3: {
                    n3 = n3 & 255 | (arrby[n6 + 0] & 255) << 24 | (arrby[n6 + 1] & 255) << 16 | (arrby[n6 + 2] & 255) << 8;
                }
            }
            this.memWrite(n, n3);
        }
    }

    public final void memcpy(int n, int n2, int n3) throws FaultException {
        int n4 = 1 << this.pageShift >>> 2;
        int n5 = n4 - 1;
        if ((n & 3) == 0 && (n2 & 3) == 0) {
            int n6;
            int n7;
            if ((n3 & -4) != 0) {
                int n8;
                n6 = n2 >>> 2;
                int n9 = n >>> 2;
                for (n7 = n3 >> 2; n7 != 0; n7 -= n8) {
                    int[] arrn = this.readPages[n6 >>> this.pageShift - 2];
                    if (arrn == null) {
                        throw new ReadFaultException(n6 << 2);
                    }
                    int[] arrn2 = this.writePages[n9 >>> this.pageShift - 2];
                    if (arrn2 == null) {
                        throw new WriteFaultException(n9 << 2);
                    }
                    int n10 = n6 & n5;
                    int n11 = n9 & n5;
                    n8 = Runtime.min(n7, n4 - Runtime.max(n10, n11));
                    System.arraycopy(arrn, n10, arrn2, n11, n8);
                    n6 += n8;
                    n9 += n8;
                }
                n2 = n6 << 2;
                n = n9 << 2;
                n3 &= 3;
            }
            if (n3 != 0) {
                n7 = this.memRead(n2);
                n6 = this.memRead(n);
                switch (n3) {
                    case 1: {
                        this.memWrite(n, n7 & -16777216 | n6 & 16777215);
                        break;
                    }
                    case 2: {
                        this.memWrite(n, n7 & -65536 | n6 & 65535);
                        break;
                    }
                    case 3: {
                        this.memWrite(n, n7 & -256 | n6 & 255);
                    }
                }
            }
        } else {
            while (n3 > 0) {
                int n12 = Runtime.min(n3, 16776192);
                byte[] arrby = this.byteBuf(n12);
                this.copyin(n2, arrby, n12);
                this.copyout(arrby, n, n12);
                n3 -= n12;
                n2 += n12;
                n += n12;
            }
        }
    }

    public final void memset(int n, int n2, int n3) throws FaultException {
        int n4;
        int n5 = 1 << this.pageShift >>> 2;
        int n6 = n5 - 1;
        int n7 = (n2 & 255) << 24 | (n2 & 255) << 16 | (n2 & 255) << 8 | (n2 & 255) << 0;
        if ((n & 3) != 0) {
            n4 = this.memRead(n & -4);
            switch (n & 3) {
                case 1: {
                    n4 = n4 & -16711681 | (n2 & 255) << 16;
                    if (--n3 == 0) break;
                }
                case 2: {
                    n4 = n4 & -65281 | (n2 & 255) << 8;
                    if (--n3 == 0) break;
                }
                case 3: {
                    n4 = n4 & -256 | (n2 & 255) << 0;
                    if (--n3 == 0) {
                        // empty if block
                    } else {
                        break;
                    }
                }
            }
            this.memWrite(n & -4, n4);
            n = (n & -4) + 4;
        }
        if ((n3 & -4) != 0) {
            int n8;
            int n9 = n >>> 2;
            for (n4 = n3 >> 2; n4 != 0; n4 -= n8) {
                int[] arrn = this.readPages[n9 >>> this.pageShift - 2];
                if (arrn == null) {
                    throw new WriteFaultException(n9 << 2);
                }
                int n10 = n9 & n6;
                n8 = Runtime.min(n4, n5 - n10);
                for (int i = n10; i < n10 + n8; ++i) {
                    arrn[i] = n7;
                }
                n9 += n8;
            }
            n = n9 << 2;
            n3 &= 3;
        }
        if (n3 != 0) {
            n4 = this.memRead(n);
            switch (n3) {
                case 1: {
                    n4 = n4 & 16777215 | n7 & -16777216;
                    break;
                }
                case 2: {
                    n4 = n4 & 65535 | n7 & -65536;
                    break;
                }
                case 3: {
                    n4 = n4 & 255 | n7 & -256;
                }
            }
            this.memWrite(n, n4);
        }
    }

    public final int memRead(int n) throws ReadFaultException {
        if ((n & 3) != 0) {
            throw new ReadFaultException(n);
        }
        return this.unsafeMemRead(n);
    }

    protected final int unsafeMemRead(int n) throws ReadFaultException {
        int n2 = n >>> this.pageShift;
        int n3 = (n & (1 << this.pageShift) - 1) >> 2;
        try {
            return this.readPages[n2][n3];
        }
        catch (ArrayIndexOutOfBoundsException var4_4) {
            if (n2 < 0 || n2 >= this.readPages.length) {
                throw new ReadFaultException(n);
            }
            throw var4_4;
        }
        catch (NullPointerException var4_5) {
            throw new ReadFaultException(n);
        }
    }

    public final void memWrite(int n, int n2) throws WriteFaultException {
        if ((n & 3) != 0) {
            throw new WriteFaultException(n);
        }
        this.unsafeMemWrite(n, n2);
    }

    protected final void unsafeMemWrite(int n, int n2) throws WriteFaultException {
        int n3 = n >>> this.pageShift;
        int n4 = (n & (1 << this.pageShift) - 1) >> 2;
        try {
            this.writePages[n3][n4] = n2;
        }
        catch (ArrayIndexOutOfBoundsException var5_5) {
            if (n3 < 0 || n3 >= this.writePages.length) {
                throw new WriteFaultException(n);
            }
            throw var5_5;
        }
        catch (NullPointerException var5_6) {
            throw new WriteFaultException(n);
        }
    }

    private final int[] initPage(int n) {
        return this.initPage(n, false);
    }

    private final int[] initPage(int n, boolean bl) {
        int[] arrn = new int[1 << this.pageShift >>> 2];
        this.writePages[n] = bl ? null : arrn;
        this.readPages[n] = arrn;
        return arrn;
    }

    public final int exitStatus() {
        if (this.state != 4) {
            throw new IllegalStateException("exitStatus() called in an inappropriate state");
        }
        return this.exitStatus;
    }

    private int addStringArray(String[] arrstring, int n) throws FaultException {
        int n2;
        int n3 = arrstring.length;
        int n4 = 0;
        for (n2 = 0; n2 < n3; ++n2) {
            n4 += arrstring[n2].length() + 1;
        }
        n2 = n - (n4 += (n3 + 1) * 4) & -4;
        int n5 = n2 + (n3 + 1) * 4;
        int[] arrn = new int[n3 + 1];
        try {
            int n6;
            for (n6 = 0; n6 < n3; ++n6) {
                byte[] arrby = Runtime.getBytes(arrstring[n6]);
                arrn[n6] = n5;
                this.copyout(arrby, n5, arrby.length);
                this.memset(n5 + arrby.length, 0, 1);
                n5 += arrby.length + 1;
            }
            n5 = n2;
            for (n6 = 0; n6 < n3 + 1; ++n6) {
                this.memWrite(n5, arrn[n6]);
                n5 += 4;
            }
        }
        catch (FaultException var8_9) {
            throw new RuntimeException(var8_9.toString());
        }
        return n2;
    }

    String[] createEnv(String[] arrstring) {
        if (arrstring == null) {
            arrstring = new String[]{};
        }
        return arrstring;
    }

    public void setUserInfo(int n, int n2) {
        if (n < 0 || n >= this.userInfoSize() / 4) {
            throw new IndexOutOfBoundsException("setUserInfo called with index >= " + this.userInfoSize() / 4);
        }
        try {
            this.memWrite(this.userInfoBase() + n * 4, n2);
        }
        catch (FaultException var3_3) {
            throw new RuntimeException(var3_3.toString());
        }
    }

    public int getUserInfo(int n) {
        if (n < 0 || n >= this.userInfoSize() / 4) {
            throw new IndexOutOfBoundsException("setUserInfo called with index >= " + this.userInfoSize() / 4);
        }
        try {
            return this.memRead(this.userInfoBase() + n * 4);
        }
        catch (FaultException var2_2) {
            throw new RuntimeException(var2_2.toString());
        }
    }

    private void __execute() {
        try {
            this._execute();
        }
        catch (FaultException var1_1) {
            var1_1.printStackTrace();
            this.exit(139, true);
            this.exitException = var1_1;
        }
        catch (ExecutionException var1_2) {
            var1_2.printStackTrace();
            this.exit(132, true);
            this.exitException = var1_2;
        }
    }

    public final boolean execute() {
        if (this.state != 2) {
            throw new IllegalStateException("execute() called in inappropriate state");
        }
        if (this.startTime == 0) {
            this.startTime = System.currentTimeMillis();
        }
        this.state = 0;
        this.__execute();
        if (this.state != 2 && this.state != 4 && this.state != 5) {
            throw new IllegalStateException("execute() ended up in an inappropriate state (" + this.state + ")");
        }
        return this.state != 2;
    }

    static String[] concatArgv(String string, String[] arrstring) {
        String[] arrstring2 = new String[arrstring.length + 1];
        System.arraycopy(arrstring, 0, arrstring2, 1, arrstring.length);
        arrstring2[0] = string;
        return arrstring2;
    }

    public final int run() {
        return this.run(null);
    }

    public final int run(String string, String[] arrstring) {
        return this.run(Runtime.concatArgv(string, arrstring));
    }

    public final int run(String[] arrstring) {
        return this.run(arrstring, null);
    }

    public final int run(String[] arrstring, String[] arrstring2) {
        this.start(arrstring, arrstring2);
        while (!this.execute()) {
            System.err.println("WARNING: Pause requested while executing run()");
        }
        if (this.state == 5) {
            System.err.println("WARNING: Process exec()ed while being run under run()");
        }
        return this.state == 4 ? this.exitStatus() : 0;
    }

    public final void start() {
        this.start(null);
    }

    public final void start(String[] arrstring) {
        this.start(arrstring, null);
    }

    public final void start(String[] arrstring, String[] arrstring2) {
        int n;
        int n2;
        int n3;
        if (this.state != 1) {
            throw new IllegalStateException("start() called in inappropriate state");
        }
        if (arrstring == null) {
            arrstring = new String[]{this.getClass().getName()};
        }
        int n4 = n = this.writePages.length * (1 << this.pageShift);
        try {
            n4 = n3 = this.addStringArray(arrstring, n4);
            n4 = n2 = this.addStringArray(this.createEnv(arrstring2), n4);
        }
        catch (FaultException var7_7) {
            throw new IllegalArgumentException("args/environ too big");
        }
        if (n - (n4 &= -16) > 65536) {
            throw new IllegalArgumentException("args/environ too big");
        }
        if (this.heapEnd == 0) {
            this.heapEnd = this.heapStart();
            if (this.heapEnd == 0) {
                throw new Error("heapEnd == 0");
            }
            int n5 = this.writePages.length == 1 ? 4096 : 1 << this.pageShift;
            this.heapEnd = this.heapEnd + n5 - 1 & ~ (n5 - 1);
        }
        CPUState cPUState = new CPUState();
        cPUState.r[4] = n3;
        cPUState.r[5] = n2;
        cPUState.r[29] = n4;
        cPUState.r[31] = -559038737;
        cPUState.r[28] = this.gp();
        cPUState.pc = this.entryPoint();
        this.setCPUState(cPUState);
        this.state = 2;
        this._started();
    }

    public final void stop() {
        if (this.state != 0 && this.state != 2) {
            throw new IllegalStateException("stop() called in inappropriate state");
        }
        this.exit(0, false);
    }

    void _started() {
    }

    public final int call(String string, Object[] arrobject) throws CallException, FaultException {
        int n;
        if (this.state != 2 && this.state != 3) {
            throw new IllegalStateException("call() called in inappropriate state");
        }
        if (arrobject.length > 7) {
            throw new IllegalArgumentException("args.length > 7");
        }
        CPUState cPUState = new CPUState();
        this.getCPUState(cPUState);
        int n2 = cPUState.r[29];
        int[] arrn = new int[arrobject.length];
        for (n = 0; n < arrobject.length; ++n) {
            Object object = arrobject[n];
            byte[] arrby = null;
            if (object instanceof String) {
                arrby = Runtime.getBytes((String)object);
            } else if (object instanceof byte[]) {
                arrby = (byte[])object;
            } else if (object instanceof Number) {
                arrn[n] = ((Number)object).intValue();
            }
            if (arrby == null) continue;
            this.copyout(arrby, n2 -= arrby.length, arrby.length);
            arrn[n] = n2;
        }
        n = cPUState.r[29];
        if (n == n2) {
            return this.call(string, arrn);
        }
        cPUState.r[29] = n2;
        this.setCPUState(cPUState);
        int n3 = this.call(string, arrn);
        cPUState.r[29] = n;
        this.setCPUState(cPUState);
        return n3;
    }

    public final int call(String string) throws CallException {
        return this.call(string, new int[0]);
    }

    public final int call(String string, int n) throws CallException {
        return this.call(string, new int[]{n});
    }

    public final int call(String string, int n, int n2) throws CallException {
        return this.call(string, new int[]{n, n2});
    }

    public final int call(String string, int[] arrn) throws CallException {
        int n = this.lookupSymbol(string);
        if (n == -1) {
            throw new CallException(string + " not found");
        }
        int n2 = this.lookupSymbol("_call_helper");
        if (n2 == -1) {
            throw new CallException("_call_helper not found");
        }
        return this.call(n2, n, arrn);
    }

    public final int call(int n, int n2, int[] arrn) throws CallException {
        if (arrn.length > 7) {
            throw new IllegalArgumentException("rest.length > 7");
        }
        if (this.state != 2 && this.state != 3) {
            throw new IllegalStateException("call() called in inappropriate state");
        }
        int n3 = this.state;
        CPUState cPUState = new CPUState();
        this.getCPUState(cPUState);
        CPUState cPUState2 = cPUState.dup();
        cPUState2.r[29] = cPUState2.r[29] & -16;
        cPUState2.r[31] = -559038737;
        cPUState2.r[4] = n2;
        switch (arrn.length) {
            case 7: {
                cPUState2.r[19] = arrn[6];
            }
            case 6: {
                cPUState2.r[18] = arrn[5];
            }
            case 5: {
                cPUState2.r[17] = arrn[4];
            }
            case 4: {
                cPUState2.r[16] = arrn[3];
            }
            case 3: {
                cPUState2.r[7] = arrn[2];
            }
            case 2: {
                cPUState2.r[6] = arrn[1];
            }
            case 1: {
                cPUState2.r[5] = arrn[0];
            }
        }
        cPUState2.pc = n;
        this.state = 0;
        this.setCPUState(cPUState2);
        this.__execute();
        this.getCPUState(cPUState2);
        this.setCPUState(cPUState);
        if (this.state != 2) {
            throw new CallException("Process exit()ed while servicing a call() request");
        }
        this.state = n3;
        return cPUState2.r[3];
    }

    public final int addFD(FD fD) {
        int n;
        if (this.state == 4 || this.state == 5) {
            throw new IllegalStateException("addFD called in inappropriate state");
        }
        for (n = 0; n < 64 && this.fds[n] != null; ++n) {
        }
        if (n == 64) {
            return -1;
        }
        this.fds[n] = fD;
        this.closeOnExec[n] = false;
        return n;
    }

    void _preCloseFD(FD fD) {
    }

    void _postCloseFD(FD fD) {
    }

    public final boolean closeFD(int n) {
        if (this.state == 4 || this.state == 5) {
            throw new IllegalStateException("closeFD called in inappropriate state");
        }
        if (n < 0 || n >= 64) {
            return false;
        }
        if (this.fds[n] == null) {
            return false;
        }
        this._preCloseFD(this.fds[n]);
        this.fds[n].close();
        this._postCloseFD(this.fds[n]);
        this.fds[n] = null;
        return true;
    }

    public final int dupFD(int n) {
        int n2;
        if (n < 0 || n >= 64) {
            return -1;
        }
        if (this.fds[n] == null) {
            return -1;
        }
        for (n2 = 0; n2 < 64 && this.fds[n2] != null; ++n2) {
        }
        if (n2 == 64) {
            return -1;
        }
        this.fds[n2] = this.fds[n].dup();
        return n2;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    FD hostFSOpen(final File file, int n, int n2, final Object object) throws ErrnoException {
        boolean bl;
        block16 : {
            if ((n & -3596) != 0) {
                System.err.println("WARNING: Unsupported flags passed to open(\"" + file + "\"): " + Runtime.toHex(n & -3596));
                throw new ErrnoException(134);
            }
            boolean bl2 = bl = (n & 3) != 0;
            if (this.sm != null) {
                if (bl) {
                    if (!this.sm.allowWrite(file)) throw new ErrnoException(13);
                } else if (!this.sm.allowRead(file)) {
                    throw new ErrnoException(13);
                }
            }
            if ((n & 2560) == 2560) {
                try {
                    if (!Platform.atomicCreateFile(file)) {
                        throw new ErrnoException(17);
                    }
                    break block16;
                }
                catch (IOException var6_6) {
                    throw new ErrnoException(5);
                }
            }
            if (!file.exists()) {
                if ((n & 512) == 0) {
                    return null;
                }
            } else if (file.isDirectory()) {
                return this.hostFSDirFD(file, object);
            }
        }
        try {
            final Seekable.File file2 = new Seekable.File(file, bl, (n & 1024) != 0);
            return new SeekableFD(file2, n){

                protected FStat _fstat() {
                    return Runtime.this.hostFStat(file, file2, object);
                }
            };
        }
        catch (FileNotFoundException var7_8) {
            if (var7_8.getMessage() == null || var7_8.getMessage().indexOf("Permission denied") < 0) return null;
            throw new ErrnoException(13);
        }
        catch (IOException var7_9) {
            throw new ErrnoException(5);
        }
    }

    FStat hostFStat(File file, Seekable.File file2, Object object) {
        return new HostFStat(file, file2);
    }

    FD hostFSDirFD(File file, Object object) {
        return null;
    }

    FD _open(String string, int n, int n2) throws ErrnoException {
        return this.hostFSOpen(new File(string), n, n2, null);
    }

    private int sys_open(int n, int n2, int n3) throws ErrnoException, FaultException {
        FD fD;
        String string = this.cstring(n);
        if (string.length() == 1024 && this.getClass().getName().equals("tests.TeX")) {
            string = string.trim();
        }
        if ((fD = this._open(string, n2 &= -32769, n3)) == null) {
            return -2;
        }
        int n4 = this.addFD(fD);
        if (n4 == -1) {
            fD.close();
            return -23;
        }
        return n4;
    }

    private int sys_write(int n, int n2, int n3) throws FaultException, ErrnoException {
        n3 = Math.min(n3, 16776192);
        if (n < 0 || n >= 64) {
            return -81;
        }
        if (this.fds[n] == null) {
            return -81;
        }
        byte[] arrby = this.byteBuf(n3);
        this.copyin(n2, arrby, n3);
        try {
            return this.fds[n].write(arrby, 0, n3);
        }
        catch (ErrnoException var5_5) {
            if (var5_5.errno == 32) {
                this.sys_exit(141);
            }
            throw var5_5;
        }
    }

    private int sys_read(int n, int n2, int n3) throws FaultException, ErrnoException {
        n3 = Math.min(n3, 16776192);
        if (n < 0 || n >= 64) {
            return -81;
        }
        if (this.fds[n] == null) {
            return -81;
        }
        byte[] arrby = this.byteBuf(n3);
        int n4 = this.fds[n].read(arrby, 0, n3);
        this.copyout(arrby, n2, n4);
        return n4;
    }

    private int sys_ftruncate(int n, long l) {
        if (n < 0 || n >= 64) {
            return -81;
        }
        if (this.fds[n] == null) {
            return -81;
        }
        Seekable seekable = this.fds[n].seekable();
        if (l < 0 || seekable == null) {
            return -22;
        }
        try {
            seekable.resize(l);
        }
        catch (IOException var5_4) {
            return -5;
        }
        return 0;
    }

    private int sys_close(int n) {
        return this.closeFD(n) ? 0 : -81;
    }

    private int sys_lseek(int n, int n2, int n3) throws ErrnoException {
        if (n < 0 || n >= 64) {
            return -81;
        }
        if (this.fds[n] == null) {
            return -81;
        }
        if (n3 != 0 && n3 != 1 && n3 != 2) {
            return -22;
        }
        int n4 = this.fds[n].seek(n2, n3);
        return n4 < 0 ? -29 : n4;
    }

    int stat(FStat fStat, int n) throws FaultException {
        this.memWrite(n + 0, fStat.dev() << 16 | fStat.inode() & 65535);
        this.memWrite(n + 4, fStat.type() & 61440 | fStat.mode() & 4095);
        this.memWrite(n + 8, fStat.nlink() << 16 | fStat.uid() & 65535);
        this.memWrite(n + 12, fStat.gid() << 16 | 0);
        this.memWrite(n + 16, fStat.size());
        this.memWrite(n + 20, fStat.atime());
        this.memWrite(n + 28, fStat.mtime());
        this.memWrite(n + 36, fStat.ctime());
        this.memWrite(n + 44, fStat.blksize());
        this.memWrite(n + 48, fStat.blocks());
        return 0;
    }

    private int sys_fstat(int n, int n2) throws FaultException {
        if (n < 0 || n >= 64) {
            return -81;
        }
        if (this.fds[n] == null) {
            return -81;
        }
        return this.stat(this.fds[n].fstat(), n2);
    }

    private int sys_gettimeofday(int n, int n2) throws FaultException {
        long l = System.currentTimeMillis();
        int n3 = (int)(l / 1000);
        int n4 = (int)(l % 1000 * 1000);
        this.memWrite(n + 0, n3);
        this.memWrite(n + 4, n4);
        return 0;
    }

    private int sys_sleep(int n) {
        if (n < 0) {
            n = Integer.MAX_VALUE;
        }
        try {
            Thread.sleep((long)n * 1000);
            return 0;
        }
        catch (InterruptedException var2_2) {
            return -1;
        }
    }

    private int sys_times(int n) {
        long l = System.currentTimeMillis();
        int n2 = (int)((l - this.startTime) / 16);
        int n3 = (int)((l - this.startTime) / 16);
        try {
            if (n != 0) {
                this.memWrite(n + 0, n2);
                this.memWrite(n + 4, n3);
                this.memWrite(n + 8, n2);
                this.memWrite(n + 12, n3);
            }
        }
        catch (FaultException var6_5) {
            return -14;
        }
        return (int)l;
    }

    private int sys_sysconf(int n) {
        switch (n) {
            case 2: {
                return 1000;
            }
            case 8: {
                return this.writePages.length == 1 ? 4096 : 1 << this.pageShift;
            }
            case 11: {
                return this.writePages.length == 1 ? (1 << this.pageShift) / 4096 : this.writePages.length;
            }
        }
        System.err.println("WARNING: Attempted to use unknown sysconf key: " + n);
        return -22;
    }

    public final int sbrk(int n) {
        if (n < 0) {
            return -12;
        }
        if (n == 0) {
            return this.heapEnd;
        }
        int n2 = this.heapEnd;
        int n3 = n2 + (n = n + 3 & -4);
        if (n3 >= this.stackBottom) {
            return -12;
        }
        if (this.writePages.length > 1) {
            int n4 = (1 << this.pageShift) - 1;
            int n5 = 1 << this.pageShift >>> 2;
            int n6 = n2 + n4 >>> this.pageShift;
            int n7 = n3 + n4 >>> this.pageShift;
            try {
                for (int i = n6; i < n7; ++i) {
                    this.readPages[i] = this.writePages[i] = new int[n5];
                }
            }
            catch (OutOfMemoryError var8_9) {
                System.err.println("WARNING: Caught OOM Exception in sbrk: " + var8_9);
                return -12;
            }
        }
        this.heapEnd = n3;
        return n2;
    }

    private int sys_getpid() {
        return this.getPid();
    }

    int getPid() {
        return 1;
    }

    private int sys_calljava(int n, int n2, int n3, int n4) {
        if (this.state != 0) {
            throw new IllegalStateException("wound up calling sys_calljava while not in RUNNING");
        }
        if (this.callJavaCB != null) {
            int n5;
            this.state = 3;
            try {
                n5 = this.callJavaCB.call(n, n2, n3, n4);
            }
            catch (RuntimeException var6_6) {
                System.err.println("Error while executing callJavaCB");
                var6_6.printStackTrace();
                n5 = 0;
            }
            this.state = 0;
            return n5;
        }
        System.err.println("WARNING: calljava syscall invoked without a calljava callback set");
        return 0;
    }

    private int sys_pause() {
        this.state = 2;
        return 0;
    }

    private int sys_getpagesize() {
        return this.writePages.length == 1 ? 4096 : 1 << this.pageShift;
    }

    void _exited() {
    }

    void exit(int n, boolean bl) {
        if (bl && this.fds[2] != null) {
            try {
                byte[] arrby = Runtime.getBytes("Process exited on signal " + (n - 128) + "\n");
                this.fds[2].write(arrby, 0, arrby.length);
            }
            catch (ErrnoException var3_4) {
                // empty catch block
            }
        }
        this.exitStatus = n;
        for (int i = 0; i < this.fds.length; ++i) {
            if (this.fds[i] == null) continue;
            this.closeFD(i);
        }
        this.state = 4;
        this._exited();
    }

    private int sys_exit(int n) {
        this.exit(n, false);
        return 0;
    }

    final int sys_fcntl(int n, int n2, int n3) throws FaultException {
        if (n < 0 || n >= 64) {
            return -81;
        }
        if (this.fds[n] == null) {
            return -81;
        }
        FD fD = this.fds[n];
        switch (n2) {
            case 0: {
                int n4;
                if (n3 < 0 || n3 >= 64) {
                    return -22;
                }
                for (n4 = n3; n4 < 64 && this.fds[n4] != null; ++n4) {
                }
                if (n4 == 64) {
                    return -24;
                }
                this.fds[n4] = fD.dup();
                return n4;
            }
            case 3: {
                return fD.flags();
            }
            case 2: {
                this.closeOnExec[n] = n3 != 0;
                return 0;
            }
            case 1: {
                return this.closeOnExec[n] ? 1 : 0;
            }
            case 7: 
            case 8: {
                System.err.println("WARNING: file locking requires UnixRuntime");
                return -88;
            }
        }
        System.err.println("WARNING: Unknown fcntl command: " + n2);
        return -88;
    }

    final int fsync(int n) {
        if (n < 0 || n >= 64) {
            return -81;
        }
        if (this.fds[n] == null) {
            return -81;
        }
        FD fD = this.fds[n];
        Seekable seekable = fD.seekable();
        if (seekable == null) {
            return -22;
        }
        try {
            seekable.sync();
            return 0;
        }
        catch (IOException var4_4) {
            return -5;
        }
    }

    protected final int syscall(int n, int n2, int n3, int n4, int n5, int n6, int n7) {
        try {
            int n8 = this._syscall(n, n2, n3, n4, n5, n6, n7);
            return n8;
        }
        catch (ErrnoException var8_9) {
            return - var8_9.errno;
        }
        catch (FaultException var8_10) {
            return -14;
        }
        catch (RuntimeException var8_11) {
            var8_11.printStackTrace();
            throw new Error("Internal Error in _syscall()");
        }
    }

    int _syscall(int n, int n2, int n3, int n4, int n5, int n6, int n7) throws ErrnoException, FaultException {
        switch (n) {
            case 0: {
                return 0;
            }
            case 1: {
                return this.sys_exit(n2);
            }
            case 2: {
                return this.sys_pause();
            }
            case 6: {
                return this.sys_write(n2, n3, n4);
            }
            case 8: {
                return this.sys_fstat(n2, n3);
            }
            case 7: {
                return this.sbrk(n2);
            }
            case 3: {
                return this.sys_open(n2, n3, n4);
            }
            case 4: {
                return this.sys_close(n2);
            }
            case 5: {
                return this.sys_read(n2, n3, n4);
            }
            case 10: {
                return this.sys_lseek(n2, n3, n4);
            }
            case 44: {
                return this.sys_ftruncate(n2, n3);
            }
            case 12: {
                return this.sys_getpid();
            }
            case 13: {
                return this.sys_calljava(n2, n3, n4, n5);
            }
            case 15: {
                return this.sys_gettimeofday(n2, n3);
            }
            case 16: {
                return this.sys_sleep(n2);
            }
            case 17: {
                return this.sys_times(n2);
            }
            case 19: {
                return this.sys_getpagesize();
            }
            case 29: {
                return this.sys_fcntl(n2, n3, n4);
            }
            case 31: {
                return this.sys_sysconf(n2);
            }
            case 68: {
                return this.sys_getuid();
            }
            case 70: {
                return this.sys_geteuid();
            }
            case 69: {
                return this.sys_getgid();
            }
            case 71: {
                return this.sys_getegid();
            }
            case 91: {
                return this.fsync(n2);
            }
            case 37: {
                this.memcpy(n2, n3, n4);
                return n2;
            }
            case 38: {
                this.memset(n2, n3, n4);
                return n2;
            }
            case 11: 
            case 14: 
            case 18: 
            case 22: 
            case 23: 
            case 24: 
            case 25: 
            case 26: 
            case 27: {
                System.err.println("Attempted to use a UnixRuntime syscall in Runtime (" + n + ")");
                return -88;
            }
        }
        System.err.println("Attempted to use unknown syscall: " + n);
        return -88;
    }

    private int sys_getuid() {
        return 0;
    }

    private int sys_geteuid() {
        return 0;
    }

    private int sys_getgid() {
        return 0;
    }

    private int sys_getegid() {
        return 0;
    }

    public int xmalloc(int n) {
        int n2 = this.malloc(n);
        if (n2 == 0) {
            throw new RuntimeException("malloc() failed");
        }
        return n2;
    }

    public int xrealloc(int n, int n2) {
        int n3 = this.realloc(n, n2);
        if (n3 == 0) {
            throw new RuntimeException("realloc() failed");
        }
        return n3;
    }

    public int realloc(int n, int n2) {
        try {
            return this.call("realloc", n, n2);
        }
        catch (CallException var3_3) {
            return 0;
        }
    }

    public int malloc(int n) {
        try {
            return this.call("malloc", n);
        }
        catch (CallException var2_2) {
            return 0;
        }
    }

    public void free(int n) {
        try {
            if (n != 0) {
                this.call("free", n);
            }
        }
        catch (CallException var2_2) {
            // empty catch block
        }
    }

    public int strdup(String string) {
        if (string == null) {
            string = "(null)";
        }
        byte[] arrby = Runtime.getBytes(string);
        byte[] arrby2 = new byte[arrby.length + 1];
        System.arraycopy(arrby, 0, arrby2, 0, arrby.length);
        int n = this.malloc(arrby2.length);
        if (n == 0) {
            return 0;
        }
        try {
            this.copyout(arrby2, n, arrby2.length);
        }
        catch (FaultException var5_5) {
            this.free(n);
            return 0;
        }
        return n;
    }

    public final String utfstring(int n) throws ReadFaultException {
        if (n == 0) {
            return null;
        }
        int n2 = n;
        int n3 = 1;
        while (n3 != 0) {
            n3 = this.memRead(n2 & -4);
            switch (n2 & 3) {
                case 0: {
                    n3 = n3 >>> 24 & 255;
                    break;
                }
                case 1: {
                    n3 = n3 >>> 16 & 255;
                    break;
                }
                case 2: {
                    n3 = n3 >>> 8 & 255;
                    break;
                }
                case 3: {
                    n3 = n3 >>> 0 & 255;
                }
            }
            ++n2;
        }
        if (n2 > n) {
            --n2;
        }
        byte[] arrby = new byte[n2 - n];
        this.copyin(n, arrby, arrby.length);
        try {
            return new String(arrby, "UTF-8");
        }
        catch (UnsupportedEncodingException var4_5) {
            throw new RuntimeException(var4_5);
        }
    }

    public final String cstring(int n) throws ReadFaultException {
        if (n == 0) {
            return null;
        }
        StringBuffer stringBuffer = new StringBuffer();
        do {
            int n2 = this.memRead(n & -4);
            switch (n & 3) {
                case 0: {
                    if ((n2 >>> 24 & 255) == 0) {
                        return stringBuffer.toString();
                    }
                    stringBuffer.append((char)(n2 >>> 24 & 255));
                    ++n;
                }
                case 1: {
                    if ((n2 >>> 16 & 255) == 0) {
                        return stringBuffer.toString();
                    }
                    stringBuffer.append((char)(n2 >>> 16 & 255));
                    ++n;
                }
                case 2: {
                    if ((n2 >>> 8 & 255) == 0) {
                        return stringBuffer.toString();
                    }
                    stringBuffer.append((char)(n2 >>> 8 & 255));
                    ++n;
                }
                case 3: {
                    if ((n2 >>> 0 & 255) == 0) {
                        return stringBuffer.toString();
                    }
                    stringBuffer.append((char)(n2 >>> 0 & 255));
                    ++n;
                }
            }
        } while (true);
    }

    protected final void nullPointerCheck(int n) throws ExecutionException {
        if (n < 65536) {
            throw new ExecutionException("Attempted to dereference a null pointer " + Runtime.toHex(n));
        }
    }

    byte[] byteBuf(int n) {
        if (this._byteBuf == null) {
            this._byteBuf = new byte[n];
        } else if (this._byteBuf.length < n) {
            this._byteBuf = new byte[Runtime.min(Runtime.max(this._byteBuf.length * 2, n), 16776192)];
        }
        return this._byteBuf;
    }

    protected static final int[] decodeData(String string, int n) {
        if (string.length() % 8 != 0) {
            throw new IllegalArgumentException("string length must be a multiple of 8");
        }
        if (string.length() / 8 * 7 < n * 4) {
            throw new IllegalArgumentException("string isn't big enough");
        }
        int[] arrn = new int[n];
        int n2 = 0;
        int n3 = 0;
        int n4 = 0;
        int n5 = 0;
        while (n5 < n) {
            long l = 0;
            for (int i = 0; i < 8; ++i) {
                l <<= 7;
                l |= (long)(string.charAt(n4 + i) & 127);
            }
            if (n3 > 0) {
                arrn[n5++] = n2 | (int)(l >>> 56 - n3);
            }
            if (n5 < n) {
                arrn[n5++] = (int)(l >>> 24 - n3);
            }
            n3 = n3 + 8 & 31;
            n2 = (int)(l << n3);
            n4 += 8;
        }
        return arrn;
    }

    static byte[] getBytes(String string) {
        try {
            return string.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException var1_1) {
            return null;
        }
    }

    static byte[] getNullTerminatedBytes(String string) {
        byte[] arrby = Runtime.getBytes(string);
        byte[] arrby2 = new byte[arrby.length + 1];
        System.arraycopy(arrby, 0, arrby2, 0, arrby.length);
        return arrby2;
    }

    static final String toHex(int n) {
        return "0x" + Long.toString((long)n & 0xFFFFFFFFL, 16);
    }

    static final int min(int n, int n2) {
        return n < n2 ? n : n2;
    }

    static final int max(int n, int n2) {
        return n > n2 ? n : n2;
    }

    static {
        String string = Platform.getProperty("os.name");
        String string2 = Platform.getProperty("nestedvm.win32hacks");
        win32Hacks = string2 != null ? Boolean.valueOf(string2) : string != null && string.toLowerCase().indexOf("windows") != -1;
    }

    public static class SecurityManager {
        public boolean allowRead(File file) {
            return true;
        }

        public boolean allowWrite(File file) {
            return true;
        }

        public boolean allowStat(File file) {
            return true;
        }

        public boolean allowUnlink(File file) {
            return true;
        }
    }

    protected static class CPUState {
        public int[] r = new int[32];
        public int[] f = new int[32];
        public int hi;
        public int lo;
        public int fcsr;
        public int pc;

        public CPUState dup() {
            CPUState cPUState = new CPUState();
            cPUState.hi = this.hi;
            cPUState.lo = this.lo;
            cPUState.fcsr = this.fcsr;
            cPUState.pc = this.pc;
            for (int i = 0; i < 32; ++i) {
                cPUState.r[i] = this.r[i];
                cPUState.f[i] = this.f[i];
            }
            return cPUState;
        }
    }

    protected static class ErrnoException
    extends Exception {
        public int errno;

        public ErrnoException(int n) {
            super("Errno: " + n);
            this.errno = n;
        }
    }

    public static class CallException
    extends Exception {
        public CallException(String string) {
            super(string);
        }
    }

    public static class ExecutionException
    extends Exception {
        private String message = "(null)";
        private String location = "(unknown)";

        public ExecutionException() {
        }

        public ExecutionException(String string) {
            if (string != null) {
                this.message = string;
            }
        }

        void setLocation(String string) {
            this.location = string == null ? "(unknown)" : string;
        }

        public final String getMessage() {
            return this.message + " at " + this.location;
        }
    }

    public static class FaultException
    extends ExecutionException {
        public final int addr;
        public final RuntimeException cause;

        public FaultException(int n) {
            super("fault at: " + Runtime.toHex(n));
            this.addr = n;
            this.cause = null;
        }

        public FaultException(RuntimeException runtimeException) {
            super(runtimeException.toString());
            this.addr = -1;
            this.cause = runtimeException;
        }
    }

    public static class WriteFaultException
    extends FaultException {
        public WriteFaultException(int n) {
            super(n);
        }
    }

    public static class ReadFaultException
    extends FaultException {
        public ReadFaultException(int n) {
            super(n);
        }
    }

    static class HostFStat
    extends FStat {
        private final File f;
        private final Seekable.File sf;
        private final boolean executable;

        public HostFStat(File file, Seekable.File file2) {
            this(file, file2, false);
        }

        public HostFStat(File file, boolean bl) {
            this(file, null, bl);
        }

        public HostFStat(File file, Seekable.File file2, boolean bl) {
            this.f = file;
            this.sf = file2;
            this.executable = bl;
        }

        public int dev() {
            return 1;
        }

        public int inode() {
            return this.f.getAbsolutePath().hashCode() & 32767;
        }

        public int type() {
            return this.f.isDirectory() ? 16384 : 32768;
        }

        public int nlink() {
            return 1;
        }

        public int mode() {
            int n = 0;
            boolean bl = this.f.canRead();
            if (bl && (this.executable || this.f.isDirectory())) {
                n |= 73;
            }
            if (bl) {
                n |= 292;
            }
            if (this.f.canWrite()) {
                n |= 146;
            }
            return n;
        }

        public int size() {
            try {
                return this.sf != null ? this.sf.length() : (int)this.f.length();
            }
            catch (Exception var1_1) {
                return (int)this.f.length();
            }
        }

        public int mtime() {
            return (int)(this.f.lastModified() / 1000);
        }
    }

    public static class SocketFStat
    extends FStat {
        public int dev() {
            return -1;
        }

        public int type() {
            return 49152;
        }

        public int inode() {
            return this.hashCode() & 32767;
        }
    }

    public static abstract class FStat {
        public static final int S_IFIFO = 4096;
        public static final int S_IFCHR = 8192;
        public static final int S_IFDIR = 16384;
        public static final int S_IFREG = 32768;
        public static final int S_IFSOCK = 49152;

        public int mode() {
            return 0;
        }

        public int nlink() {
            return 0;
        }

        public int uid() {
            return 0;
        }

        public int gid() {
            return 0;
        }

        public int size() {
            return 0;
        }

        public int atime() {
            return 0;
        }

        public int mtime() {
            return 0;
        }

        public int ctime() {
            return 0;
        }

        public int blksize() {
            return 512;
        }

        public int blocks() {
            return (this.size() + this.blksize() - 1) / this.blksize();
        }

        public abstract int dev();

        public abstract int type();

        public abstract int inode();
    }

    static class Win32ConsoleIS
    extends InputStream {
        private int pushedBack = -1;
        private final InputStream parent;

        public Win32ConsoleIS(InputStream inputStream) {
            this.parent = inputStream;
        }

        public int read() throws IOException {
            if (this.pushedBack != -1) {
                int n = this.pushedBack;
                this.pushedBack = -1;
                return n;
            }
            int n = this.parent.read();
            if (n == 13 && (n = this.parent.read()) != 10) {
                this.pushedBack = n;
                return 13;
            }
            return n;
        }

        public int read(byte[] arrby, int n, int n2) throws IOException {
            int n3;
            boolean bl = false;
            if (this.pushedBack != -1 && n2 > 0) {
                arrby[0] = (byte)this.pushedBack;
                this.pushedBack = -1;
                ++n;
                --n2;
                bl = true;
            }
            if ((n3 = this.parent.read(arrby, n, n2)) == -1) {
                return bl ? 1 : -1;
            }
            for (int i = 0; i < n3; ++i) {
                if (arrby[n + i] != 13) continue;
                if (i == n3 - 1) {
                    int n4 = this.parent.read();
                    if (n4 == 10) {
                        arrby[n + i] = 10;
                        continue;
                    }
                    this.pushedBack = n4;
                    continue;
                }
                if (arrby[n + i + 1] != 10) continue;
                System.arraycopy(arrby, n + i + 1, arrby, n + i, n2 - i - 1);
                --n3;
            }
            return n3 + (bl ? 1 : 0);
        }
    }

    static class TerminalFD
    extends InputOutputStreamFD {
        public TerminalFD(InputStream inputStream) {
            this(inputStream, null);
        }

        public TerminalFD(OutputStream outputStream) {
            this(null, outputStream);
        }

        public TerminalFD(InputStream inputStream, OutputStream outputStream) {
            super(inputStream, outputStream);
        }

        public void _close() {
        }

        public FStat _fstat() {
            return new SocketFStat(){

                public int type() {
                    return 8192;
                }

                public int mode() {
                    return 384;
                }
            };
        }

    }

    public static class InputOutputStreamFD
    extends FD {
        private final InputStream is;
        private final OutputStream os;

        public InputOutputStreamFD(InputStream inputStream) {
            this(inputStream, null);
        }

        public InputOutputStreamFD(OutputStream outputStream) {
            this(null, outputStream);
        }

        public InputOutputStreamFD(InputStream inputStream, OutputStream outputStream) {
            this.is = inputStream;
            this.os = outputStream;
            if (inputStream == null && outputStream == null) {
                throw new IllegalArgumentException("at least one stream must be supplied");
            }
        }

        public int flags() {
            if (this.is != null && this.os != null) {
                return 2;
            }
            if (this.is != null) {
                return 0;
            }
            if (this.os != null) {
                return 1;
            }
            throw new Error("should never happen");
        }

        public void _close() {
            if (this.is != null) {
                try {
                    this.is.close();
                }
                catch (IOException var1_1) {
                    // empty catch block
                }
            }
            if (this.os != null) {
                try {
                    this.os.close();
                }
                catch (IOException var1_2) {
                    // empty catch block
                }
            }
        }

        public int read(byte[] arrby, int n, int n2) throws ErrnoException {
            if (this.is == null) {
                return super.read(arrby, n, n2);
            }
            try {
                int n3 = this.is.read(arrby, n, n2);
                return n3 < 0 ? 0 : n3;
            }
            catch (IOException var4_5) {
                throw new ErrnoException(5);
            }
        }

        public int write(byte[] arrby, int n, int n2) throws ErrnoException {
            if (this.os == null) {
                return super.write(arrby, n, n2);
            }
            try {
                this.os.write(arrby, n, n2);
                return n2;
            }
            catch (IOException var4_4) {
                throw new ErrnoException(5);
            }
        }

        public FStat _fstat() {
            return new SocketFStat();
        }
    }

    public static abstract class SeekableFD
    extends FD {
        private final int flags;
        private final Seekable data;

        SeekableFD(Seekable seekable, int n) {
            this.data = seekable;
            this.flags = n;
        }

        protected abstract FStat _fstat();

        public int flags() {
            return this.flags;
        }

        Seekable seekable() {
            return this.data;
        }

        public int seek(int n, int n2) throws ErrnoException {
            try {
                switch (n2) {
                    case 0: {
                        break;
                    }
                    case 1: {
                        n += this.data.pos();
                        break;
                    }
                    case 2: {
                        n += this.data.length();
                        break;
                    }
                    default: {
                        return -1;
                    }
                }
                this.data.seek(n);
                return n;
            }
            catch (IOException var3_3) {
                throw new ErrnoException(29);
            }
        }

        public int write(byte[] arrby, int n, int n2) throws ErrnoException {
            if ((this.flags & 3) == 0) {
                throw new ErrnoException(81);
            }
            if ((this.flags & 8) != 0) {
                this.seek(0, 2);
            }
            try {
                return this.data.write(arrby, n, n2);
            }
            catch (IOException var4_4) {
                throw new ErrnoException(5);
            }
        }

        public int read(byte[] arrby, int n, int n2) throws ErrnoException {
            if ((this.flags & 3) == 1) {
                throw new ErrnoException(81);
            }
            try {
                int n3 = this.data.read(arrby, n, n2);
                return n3 < 0 ? 0 : n3;
            }
            catch (IOException var4_5) {
                throw new ErrnoException(5);
            }
        }

        protected void _close() {
            try {
                this.data.close();
            }
            catch (IOException var1_1) {
                // empty catch block
            }
        }
    }

    public static abstract class FD {
        private int refCount = 1;
        private String normalizedPath = null;
        private boolean deleteOnClose = false;
        private FStat cachedFStat = null;

        public void setNormalizedPath(String string) {
            this.normalizedPath = string;
        }

        public String getNormalizedPath() {
            return this.normalizedPath;
        }

        public void markDeleteOnClose() {
            this.deleteOnClose = true;
        }

        public boolean isMarkedForDeleteOnClose() {
            return this.deleteOnClose;
        }

        public int read(byte[] arrby, int n, int n2) throws ErrnoException {
            throw new ErrnoException(81);
        }

        public int write(byte[] arrby, int n, int n2) throws ErrnoException {
            throw new ErrnoException(81);
        }

        public int seek(int n, int n2) throws ErrnoException {
            return -1;
        }

        public int getdents(byte[] arrby, int n, int n2) throws ErrnoException {
            throw new ErrnoException(81);
        }

        Seekable seekable() {
            return null;
        }

        public final FStat fstat() {
            if (this.cachedFStat == null) {
                this.cachedFStat = this._fstat();
            }
            return this.cachedFStat;
        }

        protected abstract FStat _fstat();

        public abstract int flags();

        public final void close() {
            if (--this.refCount == 0) {
                this._close();
            }
        }

        protected void _close() {
        }

        FD dup() {
            ++this.refCount;
            return this;
        }
    }

    public static interface CallJavaCB {
        public int call(int var1, int var2, int var3, int var4);
    }

}

