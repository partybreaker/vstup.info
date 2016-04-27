/*
 * Decompiled with CFR 0_114.
 */
package org.ibex.nestedvm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TimeZone;
import java.util.Vector;
import org.ibex.nestedvm.Runtime;
import org.ibex.nestedvm.util.InodeCache;
import org.ibex.nestedvm.util.Platform;
import org.ibex.nestedvm.util.Seekable;
import org.ibex.nestedvm.util.Sort;

public abstract class UnixRuntime
extends Runtime
implements Cloneable {
    private int pid;
    private UnixRuntime parent;
    private static final GlobalState defaultGS;
    private GlobalState gs;
    private String cwd;
    private UnixRuntime execedRuntime;
    private Object children;
    private Vector activeChildren;
    private Vector exitedChildren;
    private static final Method runtimeCompilerCompile;
    static /* synthetic */ Class class$org$ibex$nestedvm$util$Seekable;
    static /* synthetic */ Class class$java$lang$String;

    public final int getPid() {
        return this.pid;
    }

    public void setGlobalState(GlobalState globalState) {
        if (this.state != 1) {
            throw new IllegalStateException("can't change GlobalState when running");
        }
        if (globalState == null) {
            throw new NullPointerException("gs is null");
        }
        this.gs = globalState;
    }

    protected UnixRuntime(int n, int n2) {
        this(n, n2, false);
    }

    protected UnixRuntime(int n, int n2, boolean bl) {
        super(n, n2, bl);
        if (!bl) {
            this.gs = defaultGS;
            String string = Platform.getProperty("user.dir");
            String string2 = this.cwd = string == null ? null : this.gs.mapHostPath(string);
            if (this.cwd == null) {
                this.cwd = "/";
            }
            this.cwd = this.cwd.substring(1);
        }
    }

    private static String posixTZ() {
        StringBuffer stringBuffer = new StringBuffer();
        TimeZone timeZone = TimeZone.getDefault();
        int n = timeZone.getRawOffset() / 1000;
        stringBuffer.append(Platform.timeZoneGetDisplayName(timeZone, false, false));
        if (n > 0) {
            stringBuffer.append("-");
        } else {
            n = - n;
        }
        stringBuffer.append(n / 3600);
        if ((n %= 3600) > 0) {
            stringBuffer.append(":").append(n / 60);
        }
        if ((n %= 60) > 0) {
            stringBuffer.append(":").append(n);
        }
        if (timeZone.useDaylightTime()) {
            stringBuffer.append(Platform.timeZoneGetDisplayName(timeZone, true, false));
        }
        return stringBuffer.toString();
    }

    private static boolean envHas(String string, String[] arrstring) {
        for (int i = 0; i < arrstring.length; ++i) {
            if (arrstring[i] == null || !arrstring[i].startsWith(string + "=")) continue;
            return true;
        }
        return false;
    }

    String[] createEnv(String[] arrstring) {
        int n;
        String string;
        String[] arrstring2 = new String[7];
        int n2 = 0;
        if (arrstring == null) {
            arrstring = new String[]{};
        }
        if (!UnixRuntime.envHas("USER", arrstring) && Platform.getProperty("user.name") != null) {
            arrstring2[n2++] = "USER=" + Platform.getProperty("user.name");
        }
        if (!UnixRuntime.envHas("HOME", arrstring) && (string = Platform.getProperty("user.home")) != null && (string = this.gs.mapHostPath(string)) != null) {
            arrstring2[n2++] = "HOME=" + string;
        }
        if (!UnixRuntime.envHas("TMPDIR", arrstring) && (string = Platform.getProperty("java.io.tmpdir")) != null && (string = this.gs.mapHostPath(string)) != null) {
            arrstring2[n2++] = "TMPDIR=" + string;
        }
        if (!UnixRuntime.envHas("SHELL", arrstring)) {
            arrstring2[n2++] = "SHELL=/bin/sh";
        }
        if (!UnixRuntime.envHas("TERM", arrstring) && !win32Hacks) {
            arrstring2[n2++] = "TERM=vt100";
        }
        if (!UnixRuntime.envHas("TZ", arrstring)) {
            arrstring2[n2++] = "TZ=" + UnixRuntime.posixTZ();
        }
        if (!UnixRuntime.envHas("PATH", arrstring)) {
            arrstring2[n2++] = "PATH=/usr/local/bin:/usr/bin:/bin:/usr/local/sbin:/usr/sbin:/sbin";
        }
        String[] arrstring3 = new String[arrstring.length + n2];
        for (n = 0; n < n2; ++n) {
            arrstring3[n] = arrstring2[n];
        }
        for (n = 0; n < arrstring.length; ++n) {
            arrstring3[n2++] = arrstring[n];
        }
        return arrstring3;
    }

    void _started() {
        UnixRuntime[] arrunixRuntime = this.gs.tasks;
        GlobalState globalState = this.gs;
        synchronized (globalState) {
            if (this.pid != 0) {
                UnixRuntime unixRuntime = arrunixRuntime[this.pid];
                if (unixRuntime == null || unixRuntime == this || unixRuntime.pid != this.pid || unixRuntime.parent != this.parent) {
                    throw new Error("should never happen");
                }
                Object object = this.parent.children;
                synchronized (object) {
                    int n = this.parent.activeChildren.indexOf(unixRuntime);
                    if (n == -1) {
                        throw new Error("should never happen");
                    }
                    this.parent.activeChildren.setElementAt(this, n);
                }
            } else {
                int n;
                int n2 = -1;
                for (n = n3 = this.gs.nextPID; n < arrunixRuntime.length; ++n) {
                    if (arrunixRuntime[n] != null) continue;
                    n2 = n;
                    break;
                }
                if (n2 == -1) {
                    int n3;
                    for (n = 1; n < n3; ++n) {
                        if (arrunixRuntime[n] != null) continue;
                        n2 = n;
                        break;
                    }
                }
                if (n2 == -1) {
                    throw new ProcessTableFullExn();
                }
                this.pid = n2;
                this.gs.nextPID = n2 + 1;
            }
            arrunixRuntime[this.pid] = this;
        }
    }

    int _syscall(int n, int n2, int n3, int n4, int n5, int n6, int n7) throws Runtime.ErrnoException, Runtime.FaultException {
        switch (n) {
            case 11: {
                return this.sys_kill(n2, n3);
            }
            case 25: {
                return this.sys_fork();
            }
            case 23: {
                return this.sys_pipe(n2);
            }
            case 24: {
                return this.sys_dup2(n2, n3);
            }
            case 39: {
                return this.sys_dup(n2);
            }
            case 26: {
                return this.sys_waitpid(n2, n3, n4);
            }
            case 14: {
                return this.sys_stat(n2, n3);
            }
            case 33: {
                return this.sys_lstat(n2, n3);
            }
            case 18: {
                return this.sys_mkdir(n2, n3);
            }
            case 27: {
                return this.sys_getcwd(n2, n3);
            }
            case 22: {
                return this.sys_chdir(n2);
            }
            case 28: {
                return this.sys_exec(n2, n3, n4);
            }
            case 36: {
                return this.sys_getdents(n2, n3, n4, n5);
            }
            case 20: {
                return this.sys_unlink(n2);
            }
            case 46: {
                return this.sys_getppid();
            }
            case 56: {
                return this.sys_socket(n2, n3, n4);
            }
            case 57: {
                return this.sys_connect(n2, n3, n4);
            }
            case 58: {
                return this.sys_resolve_hostname(n2, n3, n4);
            }
            case 60: {
                return this.sys_setsockopt(n2, n3, n4, n5, n6);
            }
            case 61: {
                return this.sys_getsockopt(n2, n3, n4, n5, n6);
            }
            case 63: {
                return this.sys_bind(n2, n3, n4);
            }
            case 62: {
                return this.sys_listen(n2, n3);
            }
            case 59: {
                return this.sys_accept(n2, n3, n4);
            }
            case 64: {
                return this.sys_shutdown(n2, n3);
            }
            case 53: {
                return this.sys_sysctl(n2, n3, n4, n5, n6, n7);
            }
            case 65: {
                return this.sys_sendto(n2, n3, n4, n5, n6, n7);
            }
            case 66: {
                return this.sys_recvfrom(n2, n3, n4, n5, n6, n7);
            }
            case 67: {
                return this.sys_select(n2, n3, n4, n5, n6);
            }
            case 78: {
                return this.sys_access(n2, n3);
            }
            case 52: {
                return this.sys_realpath(n2, n3);
            }
            case 76: {
                return this.sys_chown(n2, n3, n4);
            }
            case 43: {
                return this.sys_chown(n2, n3, n4);
            }
            case 77: {
                return this.sys_fchown(n2, n3, n4);
            }
            case 74: {
                return this.sys_chmod(n2, n3, n4);
            }
            case 75: {
                return this.sys_fchmod(n2, n3, n4);
            }
            case 29: {
                return this.sys_fcntl_lock(n2, n3, n4);
            }
            case 73: {
                return this.sys_umask(n2);
            }
        }
        return super._syscall(n, n2, n3, n4, n5, n6, n7);
    }

    Runtime.FD _open(String string, int n, int n2) throws Runtime.ErrnoException {
        Runtime.FD fD = this.gs.open(this, string = this.normalizePath(string), n, n2);
        if (fD != null && string != null) {
            fD.setNormalizedPath(string);
        }
        return fD;
    }

    private int sys_getppid() {
        return this.parent == null ? 1 : this.parent.pid;
    }

    private int sys_chown(int n, int n2, int n3) {
        return 0;
    }

    private int sys_lchown(int n, int n2, int n3) {
        return 0;
    }

    private int sys_fchown(int n, int n2, int n3) {
        return 0;
    }

    private int sys_chmod(int n, int n2, int n3) {
        return 0;
    }

    private int sys_fchmod(int n, int n2, int n3) {
        return 0;
    }

    private int sys_umask(int n) {
        return 0;
    }

    private int sys_access(int n, int n2) throws Runtime.ErrnoException, Runtime.ReadFaultException {
        return this.gs.stat(this, this.cstring(n)) == null ? -2 : 0;
    }

    private int sys_realpath(int n, int n2) throws Runtime.FaultException {
        String string = this.normalizePath(this.cstring(n));
        byte[] arrby = UnixRuntime.getNullTerminatedBytes(string);
        if (arrby.length > 1024) {
            return -34;
        }
        this.copyout(arrby, n2, arrby.length);
        return 0;
    }

    private int sys_kill(int n, int n2) {
        if (n != n) {
            return -3;
        }
        if (n2 < 0 || n2 >= 32) {
            return -22;
        }
        switch (n2) {
            case 0: {
                return 0;
            }
            case 17: 
            case 18: 
            case 19: 
            case 20: 
            case 21: 
            case 22: 
            case 23: 
            case 28: {
                break;
            }
            default: {
                this.exit(128 + n2, true);
            }
        }
        return 0;
    }

    private int sys_waitpid(int n, int n2, int n3) throws Runtime.FaultException, Runtime.ErrnoException {
        boolean bl;
        if ((n3 & -2) != 0) {
            return -22;
        }
        if (n == 0 || n < -1) {
            System.err.println("WARNING: waitpid called with a pid of " + n);
            return -10;
        }
        boolean bl2 = bl = (n3 & 1) == 0;
        if (n != -1 && (n <= 0 || n >= this.gs.tasks.length)) {
            return -10;
        }
        if (this.children == null) {
            return bl ? -10 : 0;
        }
        UnixRuntime unixRuntime = null;
        Object object = this.children;
        synchronized (object) {
            do {
                if (n == -1) {
                    if (this.exitedChildren.size() > 0) {
                        unixRuntime = (UnixRuntime)this.exitedChildren.elementAt(this.exitedChildren.size() - 1);
                        this.exitedChildren.removeElementAt(this.exitedChildren.size() - 1);
                    }
                } else if (n > 0) {
                    if (n >= this.gs.tasks.length) {
                        return -10;
                    }
                    UnixRuntime unixRuntime2 = this.gs.tasks[n];
                    if (unixRuntime2.parent != this) {
                        return -10;
                    }
                    if (unixRuntime2.state == 4) {
                        if (!this.exitedChildren.removeElement(unixRuntime2)) {
                            throw new Error("should never happen");
                        }
                        unixRuntime = unixRuntime2;
                    }
                } else {
                    throw new Error("should never happen");
                }
                if (unixRuntime != null) break;
                if (!bl) {
                    return 0;
                }
                try {
                    this.children.wait();
                }
                catch (InterruptedException var8_8) {}
            } while (true);
            this.gs.tasks[unixRuntime.pid] = null;
        }
        if (n2 != 0) {
            this.memWrite(n2, unixRuntime.exitStatus() << 8);
        }
        return unixRuntime.pid;
    }

    void _exited() {
        Object object;
        Enumeration enumeration;
        if (this.children != null) {
            object = this.children;
            synchronized (object) {
                UnixRuntime unixRuntime;
                enumeration = this.exitedChildren.elements();
                while (enumeration.hasMoreElements()) {
                    unixRuntime = (UnixRuntime)enumeration.nextElement();
                    this.gs.tasks[unixRuntime.pid] = null;
                }
                this.exitedChildren.removeAllElements();
                enumeration = this.activeChildren.elements();
                while (enumeration.hasMoreElements()) {
                    unixRuntime = (UnixRuntime)enumeration.nextElement();
                    unixRuntime.parent = null;
                }
                this.activeChildren.removeAllElements();
            }
        }
        if ((object = this.parent) == null) {
            this.gs.tasks[this.pid] = null;
        } else {
            enumeration = object.children;
            synchronized (enumeration) {
                if (this.parent == null) {
                    this.gs.tasks[this.pid] = null;
                } else {
                    if (!this.parent.activeChildren.removeElement(this)) {
                        throw new Error("should never happen _exited: pid: " + this.pid);
                    }
                    this.parent.exitedChildren.addElement(this);
                    this.parent.children.notify();
                }
            }
        }
    }

    protected Object clone() throws CloneNotSupportedException {
        UnixRuntime unixRuntime = (UnixRuntime)super.clone();
        unixRuntime.pid = 0;
        unixRuntime.parent = null;
        unixRuntime.children = null;
        unixRuntime.exitedChildren = null;
        unixRuntime.activeChildren = null;
        return unixRuntime;
    }

    private int sys_fork() {
        UnixRuntime unixRuntime;
        try {
            unixRuntime = (UnixRuntime)this.clone();
        }
        catch (Exception var2_2) {
            var2_2.printStackTrace();
            return -12;
        }
        unixRuntime.parent = this;
        try {
            unixRuntime._started();
        }
        catch (ProcessTableFullExn var2_3) {
            return -12;
        }
        if (this.children == null) {
            this.children = new Object();
            this.activeChildren = new Vector();
            this.exitedChildren = new Vector();
        }
        this.activeChildren.addElement(unixRuntime);
        Runtime.CPUState cPUState = new Runtime.CPUState();
        this.getCPUState(cPUState);
        cPUState.r[2] = 0;
        cPUState.pc += 4;
        unixRuntime.setCPUState(cPUState);
        unixRuntime.state = 2;
        new ForkedProcess(unixRuntime);
        return unixRuntime.pid;
    }

    public static int runAndExec(UnixRuntime unixRuntime, String string, String[] arrstring) {
        return UnixRuntime.runAndExec(unixRuntime, UnixRuntime.concatArgv(string, arrstring));
    }

    public static int runAndExec(UnixRuntime unixRuntime, String[] arrstring) {
        unixRuntime.start(arrstring);
        return UnixRuntime.executeAndExec(unixRuntime);
    }

    public static int executeAndExec(UnixRuntime unixRuntime) {
        do {
            if (!unixRuntime.execute()) {
                System.err.println("WARNING: Pause requested while executing runAndExec()");
                continue;
            }
            if (unixRuntime.state != 5) {
                return unixRuntime.exitStatus();
            }
            unixRuntime = unixRuntime.execedRuntime;
        } while (true);
    }

    private String[] readStringArray(int n) throws Runtime.ReadFaultException {
        int n2 = 0;
        int n3 = n;
        while (this.memRead(n3) != 0) {
            ++n2;
            n3 += 4;
        }
        String[] arrstring = new String[n2];
        int n4 = 0;
        int n5 = n;
        while (n4 < n2) {
            arrstring[n4] = this.cstring(this.memRead(n5));
            ++n4;
            n5 += 4;
        }
        return arrstring;
    }

    private int sys_exec(int n, int n2, int n3) throws Runtime.ErrnoException, Runtime.FaultException {
        return this.exec(this.normalizePath(this.cstring(n)), this.readStringArray(n2), this.readStringArray(n3));
    }

    public Class runtimeCompile(Seekable seekable, String string) throws IOException {
        if (runtimeCompilerCompile == null) {
            System.err.println("WARNING: Exec attempted but RuntimeCompiler not found!");
            return null;
        }
        try {
            return (Class)runtimeCompilerCompile.invoke(null, seekable, "unixruntime,maxinsnpermethod=256,lessconstants", string);
        }
        catch (IllegalAccessException var3_3) {
            var3_3.printStackTrace();
            return null;
        }
        catch (InvocationTargetException var3_4) {
            Throwable throwable = var3_4.getTargetException();
            if (throwable instanceof IOException) {
                throw (IOException)throwable;
            }
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            }
            if (throwable instanceof Error) {
                throw (Error)throwable;
            }
            throwable.printStackTrace();
            return null;
        }
    }

    private int exec(String string, String[] arrstring, String[] arrstring2) throws Runtime.ErrnoException {
        Runtime.FD fD;
        if (arrstring.length == 0) {
            arrstring = new String[]{""};
        }
        if (string.equals("bin/busybox") && this.getClass().getName().endsWith("BusyBox")) {
            return this.execClass(this.getClass(), arrstring, arrstring2);
        }
        Runtime.FStat fStat = this.gs.stat(this, string);
        if (fStat == null) {
            return -2;
        }
        GlobalState.CacheEnt cacheEnt = (GlobalState.CacheEnt)this.gs.execCache.get(string);
        long l = fStat.mtime();
        long l2 = fStat.size();
        if (cacheEnt != null) {
            if (cacheEnt.time == l && cacheEnt.size == l2) {
                if (cacheEnt.o instanceof Class) {
                    return this.execClass((Class)cacheEnt.o, arrstring, arrstring2);
                }
                if (cacheEnt.o instanceof String[]) {
                    return this.execScript(string, (String[])cacheEnt.o, arrstring, arrstring2);
                }
                throw new Error("should never happen");
            }
            this.gs.execCache.remove(string);
        }
        if ((fD = this.gs.open(this, string, 0, 0)) == null) {
            throw new Runtime.ErrnoException(2);
        }
        Seekable seekable = fD.seekable();
        if (seekable == null) {
            throw new Runtime.ErrnoException(13);
        }
        byte[] arrby = new byte[4096];
        try {
            int n = seekable.read(arrby, 0, arrby.length);
            if (n == -1) {
                throw new Runtime.ErrnoException(8);
            }
            switch (arrby[0]) {
                case 127: {
                    if (n < 4) {
                        seekable.tryReadFully(arrby, n, 4 - n);
                    }
                    if (arrby[1] != 69 || arrby[2] != 76 || arrby[3] != 70) {
                        int n2 = -8;
                        return n2;
                    }
                    seekable.seek(0);
                    System.err.println("Running RuntimeCompiler for " + string);
                    Class class_ = this.runtimeCompile(seekable, string);
                    System.err.println("RuntimeCompiler finished for " + string);
                    if (class_ == null) {
                        throw new Runtime.ErrnoException(8);
                    }
                    this.gs.execCache.put(string, new GlobalState.CacheEnt(l, l2, class_));
                    int n3 = this.execClass(class_, arrstring, arrstring2);
                    return n3;
                }
                case 35: {
                    int n4;
                    int n5;
                    int n6;
                    if (n == 1) {
                        n4 = seekable.read(arrby, 1, arrby.length - 1);
                        if (n4 == -1) {
                            int n7 = -8;
                            return n7;
                        }
                        n += n4;
                    }
                    if (arrby[1] != 33) {
                        n4 = -8;
                        return n4;
                    }
                    n4 = 2;
                    n -= 2;
                    block14 : do {
                        for (n6 = n4; n6 < n4 + n; ++n6) {
                            if (arrby[n6] != 10) continue;
                            n4 = n6;
                            break block14;
                        }
                        if ((n4 += n) == arrby.length) break;
                        n = seekable.read(arrby, n4, arrby.length - n4);
                    } while (true);
                    for (n6 = 2; n6 < n4 && arrby[n6] == 32; ++n6) {
                    }
                    if (n6 == n4) {
                        throw new Runtime.ErrnoException(8);
                    }
                    for (n5 = n6; n5 < n4 && arrby[n5] != 32; ++n5) {
                    }
                    int n8 = n5;
                    while (n5 < n4 && arrby[n5] == 32) {
                        ++n5;
                    }
                    String[] arrstring3 = new String[2];
                    arrstring3[0] = new String(arrby, n6, n8 - n6);
                    arrstring3[1] = n5 < n4 ? new String(arrby, n5, n4 - n5) : null;
                    String[] arrstring4 = arrstring3;
                    this.gs.execCache.put(string, new GlobalState.CacheEnt(l, l2, arrstring4));
                    int n9 = this.execScript(string, arrstring4, arrstring, arrstring2);
                    return n9;
                }
            }
            int n10 = -8;
            return n10;
        }
        catch (IOException var13_12) {
            int n = -5;
            return n;
        }
        finally {
            fD.close();
        }
    }

    public int execScript(String string, String[] arrstring, String[] arrstring2, String[] arrstring3) throws Runtime.ErrnoException {
        int n;
        String[] arrstring4 = new String[arrstring2.length - 1 + (arrstring[1] != null ? 3 : 2)];
        int n2 = arrstring[0].lastIndexOf(47);
        arrstring4[0] = n2 == -1 ? arrstring[0] : arrstring[0].substring(n2 + 1);
        arrstring4[1] = "/" + string;
        n2 = 2;
        if (arrstring[1] != null) {
            arrstring4[n2++] = arrstring[1];
        }
        for (n = 1; n < arrstring2.length; ++n) {
            arrstring4[n2++] = arrstring2[n];
        }
        if (n2 != arrstring4.length) {
            throw new Error("p != newArgv.length");
        }
        System.err.println("Execing: " + arrstring[0]);
        for (n = 0; n < arrstring4.length; ++n) {
            System.err.println("execing [" + n + "] " + arrstring4[n]);
        }
        return this.exec(arrstring[0], arrstring4, arrstring3);
    }

    public int execClass(Class class_, String[] arrstring, String[] arrstring2) {
        try {
            UnixRuntime unixRuntime = (UnixRuntime)class_.getDeclaredConstructor(Boolean.TYPE).newInstance(Boolean.TRUE);
            return this.exec(unixRuntime, arrstring, arrstring2);
        }
        catch (Exception var4_5) {
            var4_5.printStackTrace();
            return -8;
        }
    }

    private int exec(UnixRuntime unixRuntime, String[] arrstring, String[] arrstring2) {
        for (int i = 0; i < 64; ++i) {
            if (!this.closeOnExec[i]) continue;
            this.closeFD(i);
        }
        unixRuntime.fds = this.fds;
        unixRuntime.closeOnExec = this.closeOnExec;
        this.fds = null;
        this.closeOnExec = null;
        unixRuntime.gs = this.gs;
        unixRuntime.sm = this.sm;
        unixRuntime.cwd = this.cwd;
        unixRuntime.pid = this.pid;
        unixRuntime.parent = this.parent;
        unixRuntime.start(arrstring, arrstring2);
        this.state = 5;
        this.execedRuntime = unixRuntime;
        return 0;
    }

    private int sys_pipe(int n) {
        Pipe pipe = new Pipe();
        int n2 = this.addFD(pipe.reader);
        if (n2 < 0) {
            return -23;
        }
        int n3 = this.addFD(pipe.writer);
        if (n3 < 0) {
            this.closeFD(n2);
            return -23;
        }
        try {
            this.memWrite(n, n2);
            this.memWrite(n + 4, n3);
        }
        catch (Runtime.FaultException var5_5) {
            this.closeFD(n2);
            this.closeFD(n3);
            return -14;
        }
        return 0;
    }

    private int sys_dup2(int n, int n2) {
        if (n == n2) {
            return 0;
        }
        if (n < 0 || n >= 64) {
            return -81;
        }
        if (n2 < 0 || n2 >= 64) {
            return -81;
        }
        if (this.fds[n] == null) {
            return -81;
        }
        if (this.fds[n2] != null) {
            this.fds[n2].close();
        }
        this.fds[n2] = this.fds[n].dup();
        return 0;
    }

    private int sys_dup(int n) {
        if (n < 0 || n >= 64) {
            return -81;
        }
        if (this.fds[n] == null) {
            return -81;
        }
        Runtime.FD fD = this.fds[n].dup();
        int n2 = this.addFD(fD);
        if (n2 < 0) {
            fD.close();
            return -23;
        }
        return n2;
    }

    private int sys_stat(int n, int n2) throws Runtime.FaultException, Runtime.ErrnoException {
        Runtime.FStat fStat = this.gs.stat(this, this.normalizePath(this.cstring(n)));
        if (fStat == null) {
            return -2;
        }
        return this.stat(fStat, n2);
    }

    private int sys_lstat(int n, int n2) throws Runtime.FaultException, Runtime.ErrnoException {
        Runtime.FStat fStat = this.gs.lstat(this, this.normalizePath(this.cstring(n)));
        if (fStat == null) {
            return -2;
        }
        return this.stat(fStat, n2);
    }

    private int sys_mkdir(int n, int n2) throws Runtime.FaultException, Runtime.ErrnoException {
        this.gs.mkdir(this, this.normalizePath(this.cstring(n)), n2);
        return 0;
    }

    private int sys_unlink(int n) throws Runtime.FaultException, Runtime.ErrnoException {
        this.gs.unlink(this, this.normalizePath(this.cstring(n)));
        return 0;
    }

    private int sys_getcwd(int n, int n2) throws Runtime.FaultException, Runtime.ErrnoException {
        byte[] arrby = UnixRuntime.getBytes(this.cwd);
        if (n2 == 0) {
            return -22;
        }
        if (n2 < arrby.length + 2) {
            return -34;
        }
        this.memset(n, 47, 1);
        this.copyout(arrby, n + 1, arrby.length);
        this.memset(n + arrby.length + 1, 0, 1);
        return n;
    }

    private int sys_chdir(int n) throws Runtime.ErrnoException, Runtime.FaultException {
        String string = this.normalizePath(this.cstring(n));
        Runtime.FStat fStat = this.gs.stat(this, string);
        if (fStat == null) {
            return -2;
        }
        if (fStat.type() != 16384) {
            return -20;
        }
        this.cwd = string;
        return 0;
    }

    private int sys_getdents(int n, int n2, int n3, int n4) throws Runtime.FaultException, Runtime.ErrnoException {
        n3 = Math.min(n3, 16776192);
        if (n < 0 || n >= 64) {
            return -81;
        }
        if (this.fds[n] == null) {
            return -81;
        }
        byte[] arrby = this.byteBuf(n3);
        int n5 = this.fds[n].getdents(arrby, 0, n3);
        this.copyout(arrby, n2, n5);
        return n5;
    }

    void _preCloseFD(Runtime.FD fD) {
        Seekable seekable = fD.seekable();
        if (seekable == null) {
            return;
        }
        try {
            for (int i = 0; i < this.gs.locks.length; ++i) {
                Seekable.Lock lock = this.gs.locks[i];
                if (lock == null || !seekable.equals(lock.seekable()) || lock.getOwner() != this) continue;
                lock.release();
                this.gs.locks[i] = null;
            }
        }
        catch (IOException var3_4) {
            throw new RuntimeException(var3_4);
        }
    }

    void _postCloseFD(Runtime.FD fD) {
        if (fD.isMarkedForDeleteOnClose()) {
            try {
                this.gs.unlink(this, fD.getNormalizedPath());
            }
            catch (Throwable var2_2) {
                // empty catch block
            }
        }
    }

    private int sys_fcntl_lock(int n, int n2, int n3) throws Runtime.FaultException {
        if (n2 != 7 && n2 != 8) {
            return this.sys_fcntl(n, n2, n3);
        }
        if (n < 0 || n >= 64) {
            return -81;
        }
        if (this.fds[n] == null) {
            return -81;
        }
        Runtime.FD fD = this.fds[n];
        if (n3 == 0) {
            return -22;
        }
        int n4 = this.memRead(n3);
        int n5 = this.memRead(n3 + 4);
        int n6 = this.memRead(n3 + 8);
        int n7 = n4 >> 16;
        int n8 = n4 & 255;
        Seekable.Lock[] arrlock = this.gs.locks;
        Seekable seekable = fD.seekable();
        if (seekable == null) {
            return -22;
        }
        try {
            switch (n8) {
                case 0: {
                    break;
                }
                case 1: {
                    n5 += seekable.pos();
                    break;
                }
                case 2: {
                    n5 += seekable.length();
                    break;
                }
                default: {
                    return -1;
                }
            }
            if (n2 == 7) {
                for (int i = 0; i < arrlock.length; ++i) {
                    if (arrlock[i] == null || !seekable.equals(arrlock[i].seekable()) || !arrlock[i].overlaps(n5, n6) || arrlock[i].getOwner() == this || arrlock[i].isShared() && n7 == 1) continue;
                    return 0;
                }
                Seekable.Lock lock = seekable.lock(n5, n6, n7 == 1);
                if (lock != null) {
                    this.memWrite(n3, 196608);
                    lock.release();
                }
                return 0;
            }
            if (n2 != 8) {
                return -22;
            }
            if (n7 == 3) {
                for (int i = 0; i < arrlock.length; ++i) {
                    int n9;
                    if (arrlock[i] == null || !seekable.equals(arrlock[i].seekable()) || arrlock[i].getOwner() != this || (n9 = (int)arrlock[i].position()) < n5 || n5 != 0 && n6 != 0 && (long)n9 + arrlock[i].size() > (long)(n5 + n6)) continue;
                    arrlock[i].release();
                    arrlock[i] = null;
                }
                return 0;
            }
            if (n7 == 1 || n7 == 2) {
                int n10;
                for (int i = 0; i < arrlock.length; ++i) {
                    if (arrlock[i] == null || !seekable.equals(arrlock[i].seekable())) continue;
                    if (arrlock[i].getOwner() == this) {
                        if (arrlock[i].contained(n5, n6)) {
                            arrlock[i].release();
                            arrlock[i] = null;
                            continue;
                        }
                        if (!arrlock[i].contains(n5, n6)) continue;
                        if (arrlock[i].isShared() == (n7 == 1)) {
                            this.memWrite(n3 + 4, (int)arrlock[i].position());
                            this.memWrite(n3 + 8, (int)arrlock[i].size());
                            return 0;
                        }
                        arrlock[i].release();
                        arrlock[i] = null;
                        continue;
                    }
                    if (!arrlock[i].overlaps(n5, n6) || arrlock[i].isShared() && n7 != 2) continue;
                    return -11;
                }
                Seekable.Lock lock = seekable.lock(n5, n6, n7 == 1);
                if (lock == null) {
                    return -11;
                }
                lock.setOwner(this);
                for (n10 = 0; n10 < arrlock.length && arrlock[n10] != null; ++n10) {
                }
                if (n10 == arrlock.length) {
                    return -46;
                }
                arrlock[n10] = lock;
                return 0;
            }
            return -22;
        }
        catch (IOException var12_17) {
            throw new RuntimeException(var12_17);
        }
    }

    private int sys_socket(int n, int n2, int n3) {
        if (n != 2 || n2 != 1 && n2 != 2) {
            return -123;
        }
        return this.addFD(new SocketFD(n2 == 1 ? 0 : 1));
    }

    private SocketFD getSocketFD(int n) throws Runtime.ErrnoException {
        if (n < 0 || n >= 64) {
            throw new Runtime.ErrnoException(81);
        }
        if (this.fds[n] == null) {
            throw new Runtime.ErrnoException(81);
        }
        if (!(this.fds[n] instanceof SocketFD)) {
            throw new Runtime.ErrnoException(108);
        }
        return (SocketFD)this.fds[n];
    }

    private int sys_connect(int n, int n2, int n3) throws Runtime.ErrnoException, Runtime.FaultException {
        InetAddress inetAddress;
        SocketFD socketFD = this.getSocketFD(n);
        if (socketFD.type() == 0 && (socketFD.s != null || socketFD.ss != null)) {
            return -127;
        }
        int n4 = this.memRead(n2);
        if ((n4 >>> 16 & 255) != 2) {
            return -106;
        }
        int n5 = n4 & 65535;
        byte[] arrby = new byte[4];
        this.copyin(n2 + 4, arrby, 4);
        try {
            inetAddress = Platform.inetAddressFromBytes(arrby);
        }
        catch (UnknownHostException var9_9) {
            return -125;
        }
        socketFD.connectAddr = inetAddress;
        socketFD.connectPort = n5;
        try {
            switch (socketFD.type()) {
                case 0: {
                    Socket socket;
                    socketFD.s = socket = new Socket(inetAddress, n5);
                    socketFD.setOptions();
                    socketFD.is = socket.getInputStream();
                    socketFD.os = socket.getOutputStream();
                    break;
                }
                case 1: {
                    break;
                }
                default: {
                    throw new Error("should never happen");
                }
            }
        }
        catch (IOException var9_11) {
            return -111;
        }
        return 0;
    }

    private int sys_resolve_hostname(int n, int n2, int n3) throws Runtime.FaultException {
        InetAddress[] arrinetAddress;
        String string = this.cstring(n);
        int n4 = this.memRead(n3);
        try {
            arrinetAddress = InetAddress.getAllByName(string);
        }
        catch (UnknownHostException var7_7) {
            return 1;
        }
        int n5 = UnixRuntime.min(n4 / 4, arrinetAddress.length);
        int n6 = 0;
        while (n6 < n5) {
            byte[] arrby = arrinetAddress[n6].getAddress();
            this.copyout(arrby, n2, 4);
            ++n6;
            n2 += 4;
        }
        this.memWrite(n3, n5 * 4);
        return 0;
    }

    private int sys_setsockopt(int n, int n2, int n3, int n4, int n5) throws Runtime.ReadFaultException, Runtime.ErrnoException {
        SocketFD socketFD = this.getSocketFD(n);
        switch (n2) {
            case 65535: {
                switch (n3) {
                    case 4: 
                    case 8: {
                        if (n5 != 4) {
                            return -22;
                        }
                        int n6 = this.memRead(n4);
                        socketFD.options = n6 != 0 ? (socketFD.options |= n3) : (socketFD.options &= ~ n3);
                        socketFD.setOptions();
                        return 0;
                    }
                }
                System.err.println("Unknown setsockopt name passed: " + n3);
                return -109;
            }
        }
        System.err.println("Unknown setsockopt leve passed: " + n2);
        return -109;
    }

    private int sys_getsockopt(int n, int n2, int n3, int n4, int n5) throws Runtime.ErrnoException, Runtime.FaultException {
        SocketFD socketFD = this.getSocketFD(n);
        switch (n2) {
            case 65535: {
                switch (n3) {
                    case 4: 
                    case 8: {
                        int n6 = this.memRead(n5);
                        if (n6 < 4) {
                            return -22;
                        }
                        int n7 = (socketFD.options & n3) != 0 ? 1 : 0;
                        this.memWrite(n4, n7);
                        this.memWrite(n5, 4);
                        return 0;
                    }
                }
                System.err.println("Unknown setsockopt name passed: " + n3);
                return -109;
            }
        }
        System.err.println("Unknown setsockopt leve passed: " + n2);
        return -109;
    }

    private int sys_bind(int n, int n2, int n3) throws Runtime.FaultException, Runtime.ErrnoException {
        SocketFD socketFD = this.getSocketFD(n);
        if (socketFD.type() == 0 && (socketFD.s != null || socketFD.ss != null)) {
            return -127;
        }
        int n4 = this.memRead(n2);
        if ((n4 >>> 16 & 255) != 2) {
            return -106;
        }
        int n5 = n4 & 65535;
        InetAddress inetAddress = null;
        if (this.memRead(n2 + 4) != 0) {
            byte[] arrby = new byte[4];
            this.copyin(n2 + 4, arrby, 4);
            try {
                inetAddress = Platform.inetAddressFromBytes(arrby);
            }
            catch (UnknownHostException var9_10) {
                return -125;
            }
        }
        switch (socketFD.type()) {
            case 0: {
                socketFD.bindAddr = inetAddress;
                socketFD.bindPort = n5;
                return 0;
            }
            case 1: {
                if (socketFD.ds != null) {
                    socketFD.ds.close();
                }
                try {
                    socketFD.ds = inetAddress != null ? new DatagramSocket(n5, inetAddress) : new DatagramSocket(n5);
                }
                catch (IOException var8_9) {
                    return -112;
                }
                return 0;
            }
        }
        throw new Error("should never happen");
    }

    private int sys_listen(int n, int n2) throws Runtime.ErrnoException {
        SocketFD socketFD = this.getSocketFD(n);
        if (socketFD.type() != 0) {
            return -95;
        }
        if (socketFD.ss != null || socketFD.s != null) {
            return -127;
        }
        if (socketFD.bindPort < 0) {
            return -95;
        }
        try {
            socketFD.ss = new ServerSocket(socketFD.bindPort, n2, socketFD.bindAddr);
            socketFD.flags |= 2;
            return 0;
        }
        catch (IOException var4_4) {
            return -112;
        }
    }

    private int sys_accept(int n, int n2, int n3) throws Runtime.ErrnoException, Runtime.FaultException {
        byte[] arrby;
        Socket socket;
        SocketFD socketFD = this.getSocketFD(n);
        if (socketFD.type() != 0) {
            return -95;
        }
        if (!socketFD.listen()) {
            return -95;
        }
        int n4 = this.memRead(n3);
        ServerSocket serverSocket = socketFD.ss;
        try {
            socket = serverSocket.accept();
        }
        catch (IOException var8_8) {
            return -5;
        }
        if (n4 >= 8) {
            this.memWrite(n2, 100794368 | socket.getPort());
            arrby = socket.getInetAddress().getAddress();
            this.copyout(arrby, n2 + 4, 4);
            this.memWrite(n3, 8);
        }
        arrby = new byte[](0);
        arrby.s = socket;
        try {
            arrby.is = socket.getInputStream();
            arrby.os = socket.getOutputStream();
        }
        catch (IOException var9_10) {
            return -5;
        }
        int n5 = this.addFD((Runtime.FD)arrby);
        if (n5 == -1) {
            arrby.close();
            return -23;
        }
        return n5;
    }

    private int sys_shutdown(int n, int n2) throws Runtime.ErrnoException {
        SocketFD socketFD = this.getSocketFD(n);
        if (socketFD.type() != 0 || socketFD.listen()) {
            return -95;
        }
        if (socketFD.s == null) {
            return -128;
        }
        Socket socket = socketFD.s;
        try {
            if (n2 == 0 || n2 == 2) {
                Platform.socketHalfClose(socket, false);
            }
            if (n2 == 1 || n2 == 2) {
                Platform.socketHalfClose(socket, true);
            }
        }
        catch (IOException var5_5) {
            return -5;
        }
        return 0;
    }

    private int sys_sendto(int n, int n2, int n3, int n4, int n5, int n6) throws Runtime.ErrnoException, Runtime.ReadFaultException {
        InetAddress inetAddress;
        SocketFD socketFD = this.getSocketFD(n);
        if (n4 != 0) {
            throw new Runtime.ErrnoException(22);
        }
        int n7 = this.memRead(n5);
        if ((n7 >>> 16 & 255) != 2) {
            return -106;
        }
        int n8 = n7 & 65535;
        byte[] arrby = new byte[4];
        this.copyin(n5 + 4, arrby, 4);
        try {
            inetAddress = Platform.inetAddressFromBytes(arrby);
        }
        catch (UnknownHostException var12_12) {
            return -125;
        }
        n3 = Math.min(n3, 16776192);
        byte[] arrby2 = this.byteBuf(n3);
        this.copyin(n2, arrby2, n3);
        try {
            return socketFD.sendto(arrby2, 0, n3, inetAddress, n8);
        }
        catch (Runtime.ErrnoException var13_14) {
            if (var13_14.errno == 32) {
                this.exit(141, true);
            }
            throw var13_14;
        }
    }

    private int sys_recvfrom(int n, int n2, int n3, int n4, int n5, int n6) throws Runtime.ErrnoException, Runtime.FaultException {
        SocketFD socketFD = this.getSocketFD(n);
        if (n4 != 0) {
            throw new Runtime.ErrnoException(22);
        }
        InetAddress[] arrinetAddress = n5 == 0 ? null : new InetAddress[1];
        int[] arrn = n5 == 0 ? null : new int[1];
        n3 = Math.min(n3, 16776192);
        byte[] arrby = this.byteBuf(n3);
        int n7 = socketFD.recvfrom(arrby, 0, n3, arrinetAddress, arrn);
        this.copyout(arrby, n2, n7);
        if (n5 != 0) {
            this.memWrite(n5, 131072 | arrn[0]);
            byte[] arrby2 = arrinetAddress[0].getAddress();
            this.copyout(arrby2, n5 + 4, 4);
        }
        return n7;
    }

    private int sys_select(int n, int n2, int n3, int n4, int n5) throws Runtime.ReadFaultException, Runtime.ErrnoException {
        return -88;
    }

    private static String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException var0) {
            return "darkstar";
        }
    }

    private int sys_sysctl(int n, int n2, int n3, int n4, int n5, int n6) throws Runtime.FaultException {
        if (n5 != 0) {
            return -1;
        }
        if (n2 == 0) {
            return -2;
        }
        if (n3 == 0) {
            return 0;
        }
        String string = null;
        block0 : switch (this.memRead(n)) {
            case 1: {
                if (n2 != 2) break;
                switch (this.memRead(n + 4)) {
                    case 1: {
                        string = "NestedVM";
                        break block0;
                    }
                    case 10: {
                        string = UnixRuntime.hostName();
                        break block0;
                    }
                    case 2: {
                        string = "1.0";
                        break block0;
                    }
                    case 4: {
                        string = "NestedVM Kernel Version 1.0";
                    }
                }
                break;
            }
            case 6: {
                if (n2 != 2) break;
                switch (this.memRead(n + 4)) {
                    case 1: {
                        string = "NestedVM Virtual Machine";
                    }
                }
            }
        }
        if (string == null) {
            return -2;
        }
        int n7 = this.memRead(n4);
        if (string instanceof String) {
            byte[] arrby = UnixRuntime.getNullTerminatedBytes(string);
            if (n7 < arrby.length) {
                return -12;
            }
            n7 = arrby.length;
            this.copyout(arrby, n3, n7);
            this.memWrite(n4, n7);
        } else if (string instanceof Integer) {
            if (n7 < 4) {
                return -12;
            }
            this.memWrite(n3, (Integer)((Object)string));
        } else {
            throw new Error("should never happen");
        }
        return 0;
    }

    private String normalizePath(String string) {
        boolean bl = string.startsWith("/");
        int n = this.cwd.length();
        if (!string.startsWith(".") && string.indexOf("./") == -1 && string.indexOf("//") == -1 && !string.endsWith(".")) {
            return bl ? string.substring(1) : (n == 0 ? string : (string.length() == 0 ? this.cwd : this.cwd + "/" + string));
        }
        char[] arrc = new char[string.length() + 1];
        char[] arrc2 = new char[arrc.length + (bl ? -1 : this.cwd.length())];
        string.getChars(0, string.length(), arrc, 0);
        int n2 = 0;
        int n3 = 0;
        if (bl) {
            while (arrc[++n2] == '/') {
            }
        } else if (n != 0) {
            this.cwd.getChars(0, n, arrc2, 0);
            n3 = n;
        }
        while (arrc[n2] != '\u0000') {
            if (n2 != 0) {
                while (arrc[n2] != '\u0000' && arrc[n2] != '/') {
                    arrc2[n3++] = arrc[n2++];
                }
                if (arrc[n2] == '\u0000') break;
                while (arrc[n2] == '/') {
                    ++n2;
                }
            }
            if (arrc[n2] == '\u0000') break;
            if (arrc[n2] != '.') {
                arrc2[n3++] = 47;
                arrc2[n3++] = arrc[n2++];
                continue;
            }
            if (arrc[n2 + 1] == '\u0000' || arrc[n2 + 1] == '/') {
                ++n2;
                continue;
            }
            if (arrc[n2 + 1] == '.' && (arrc[n2 + 2] == '\u0000' || arrc[n2 + 2] == '/')) {
                n2 += 2;
                if (n3 > 0) {
                    --n3;
                }
                while (n3 > 0 && arrc2[n3] != '/') {
                    --n3;
                }
                continue;
            }
            ++n2;
            arrc2[n3++] = 47;
            arrc2[n3++] = 46;
        }
        if (n3 > 0 && arrc2[n3 - 1] == '/') {
            --n3;
        }
        int n4 = arrc2[0] == '/' ? 1 : 0;
        return new String(arrc2, n4, n3 - n4);
    }

    Runtime.FStat hostFStat(File file, Object object) {
        Object object2;
        boolean bl = false;
        try {
            object2 = new FileInputStream(file);
            switch (object2.read()) {
                case 127: {
                    bl = object2.read() == 69 && object2.read() == 76 && object2.read() == 70;
                    break;
                }
                case 35: {
                    bl = object2.read() == 33;
                }
            }
            object2.close();
        }
        catch (IOException var4_5) {
            // empty catch block
        }
        object2 = (HostFS)object;
        final short s = object2.inodes.get(file.getAbsolutePath());
        final int n = object2.devno;
        return new Runtime.HostFStat(file, bl){

            public int inode() {
                return s;
            }

            public int dev() {
                return n;
            }
        };
    }

    Runtime.FD hostFSDirFD(File file, Object object) {
        HostFS hostFS;
        HostFS hostFS2 = hostFS = (HostFS)object;
        hostFS2.getClass();
        return hostFS2.new HostFS.HostDirFD(file);
    }

    private static void putInt(byte[] arrby, int n, int n2) {
        arrby[n + 0] = (byte)(n2 >>> 24 & 255);
        arrby[n + 1] = (byte)(n2 >>> 16 & 255);
        arrby[n + 2] = (byte)(n2 >>> 8 & 255);
        arrby[n + 3] = (byte)(n2 >>> 0 & 255);
    }

    static /* synthetic */ Class class$(String string) {
        try {
            return Class.forName(string);
        }
        catch (ClassNotFoundException var1_1) {
            throw new NoClassDefFoundError(var1_1.getMessage());
        }
    }

    static /* synthetic */ void access$500(byte[] arrby, int n, int n2) {
        UnixRuntime.putInt(arrby, n, n2);
    }

    static {
        Method method;
        defaultGS = new GlobalState();
        try {
            Class[] arrclass = new Class[3];
            Class class_ = class$org$ibex$nestedvm$util$Seekable == null ? (UnixRuntime.class$org$ibex$nestedvm$util$Seekable = UnixRuntime.class$("org.ibex.nestedvm.util.Seekable")) : class$org$ibex$nestedvm$util$Seekable;
            arrclass[0] = class_;
            Class class_2 = class$java$lang$String == null ? (UnixRuntime.class$java$lang$String = UnixRuntime.class$("java.lang.String")) : class$java$lang$String;
            arrclass[1] = class_2;
            arrclass[2] = class$java$lang$String == null ? (UnixRuntime.class$java$lang$String = UnixRuntime.class$("java.lang.String")) : class$java$lang$String;
            method = Class.forName("org.ibex.nestedvm.RuntimeCompiler").getMethod("compile", arrclass);
        }
        catch (NoSuchMethodException var1_1) {
            method = null;
        }
        catch (ClassNotFoundException var1_2) {
            method = null;
        }
        runtimeCompilerCompile = method;
    }

    public static class ResourceFS
    extends FS {
        final InodeCache inodes = new InodeCache(500);

        public Runtime.FStat lstat(UnixRuntime unixRuntime, String string) throws Runtime.ErrnoException {
            return this.stat(unixRuntime, string);
        }

        public void mkdir(UnixRuntime unixRuntime, String string, int n) throws Runtime.ErrnoException {
            throw new Runtime.ErrnoException(30);
        }

        public void unlink(UnixRuntime unixRuntime, String string) throws Runtime.ErrnoException {
            throw new Runtime.ErrnoException(30);
        }

        Runtime.FStat connFStat(final URLConnection uRLConnection) {
            return new Runtime.FStat(){

                public int type() {
                    return 32768;
                }

                public int nlink() {
                    return 1;
                }

                public int mode() {
                    return 292;
                }

                public int size() {
                    return uRLConnection.getContentLength();
                }

                public int mtime() {
                    return (int)(uRLConnection.getDate() / 1000);
                }

                public int inode() {
                    return ResourceFS.this.inodes.get(uRLConnection.getURL().toString());
                }

                public int dev() {
                    return ResourceFS.this.devno;
                }
            };
        }

        public Runtime.FStat stat(UnixRuntime unixRuntime, String string) throws Runtime.ErrnoException {
            URL uRL = unixRuntime.getClass().getResource("/" + string);
            if (uRL == null) {
                return null;
            }
            try {
                return this.connFStat(uRL.openConnection());
            }
            catch (IOException var4_4) {
                throw new Runtime.ErrnoException(5);
            }
        }

        public Runtime.FD open(UnixRuntime unixRuntime, String string, int n, int n2) throws Runtime.ErrnoException {
            if ((n & -4) != 0) {
                System.err.println("WARNING: Unsupported flags passed to ResourceFS.open(\"" + string + "\"): " + Runtime.toHex(n & -4));
                throw new Runtime.ErrnoException(134);
            }
            if ((n & 3) != 0) {
                throw new Runtime.ErrnoException(30);
            }
            URL uRL = unixRuntime.getClass().getResource("/" + string);
            if (uRL == null) {
                return null;
            }
            try {
                final URLConnection uRLConnection = uRL.openConnection();
                Seekable.InputStream inputStream = new Seekable.InputStream(uRLConnection.getInputStream());
                return new Runtime.SeekableFD(inputStream, n){

                    protected Runtime.FStat _fstat() {
                        return ResourceFS.this.connFStat(uRLConnection);
                    }
                };
            }
            catch (FileNotFoundException var6_7) {
                if (var6_7.getMessage() != null && var6_7.getMessage().indexOf("Permission denied") >= 0) {
                    throw new Runtime.ErrnoException(13);
                }
                return null;
            }
            catch (IOException var6_8) {
                throw new Runtime.ErrnoException(5);
            }
        }

    }

    public static class DevFS
    extends FS {
        private static final int ROOT_INODE = 1;
        private static final int NULL_INODE = 2;
        private static final int ZERO_INODE = 3;
        private static final int FD_INODE = 4;
        private static final int FD_INODES = 32;
        private Runtime.FD devZeroFD;
        private Runtime.FD devNullFD;

        public DevFS() {
            this.devZeroFD = new Runtime.FD(){

                public int read(byte[] arrby, int n, int n2) {
                    for (int i = n; i < n + n2; ++i) {
                        arrby[i] = 0;
                    }
                    return n2;
                }

                public int write(byte[] arrby, int n, int n2) {
                    return n2;
                }

                public int seek(int n, int n2) {
                    return 0;
                }

                public Runtime.FStat _fstat() {
                    return new DevFStat(){

                        public int inode() {
                            return 3;
                        }
                    };
                }

                public int flags() {
                    return 2;
                }

                static /* synthetic */ DevFS access$700( var0) {
                    return var0.DevFS.this;
                }

            };
            this.devNullFD = new Runtime.FD(){

                public int read(byte[] arrby, int n, int n2) {
                    return 0;
                }

                public int write(byte[] arrby, int n, int n2) {
                    return n2;
                }

                public int seek(int n, int n2) {
                    return 0;
                }

                public Runtime.FStat _fstat() {
                    return new DevFStat(){

                        public int inode() {
                            return 2;
                        }
                    };
                }

                public int flags() {
                    return 2;
                }

                static /* synthetic */ DevFS access$800(DevFS var0) {
                    return var0.DevFS.this;
                }

            };
        }

        public Runtime.FD open(UnixRuntime unixRuntime, String string, int n, int n2) throws Runtime.ErrnoException {
            if (string.equals("null")) {
                return this.devNullFD;
            }
            if (string.equals("zero")) {
                return this.devZeroFD;
            }
            if (string.startsWith("fd/")) {
                int n3;
                try {
                    n3 = Integer.parseInt(string.substring(4));
                }
                catch (NumberFormatException var6_7) {
                    return null;
                }
                if (n3 < 0 || n3 >= 64) {
                    return null;
                }
                if (unixRuntime.fds[n3] == null) {
                    return null;
                }
                return unixRuntime.fds[n3].dup();
            }
            if (string.equals("fd")) {
                int n4 = 0;
                for (int i = 0; i < 64; ++i) {
                    if (unixRuntime.fds[i] == null) continue;
                    ++n4;
                }
                final int[] arrn = new int[n4];
                n4 = 0;
                for (int j = 0; j < 64; ++j) {
                    if (unixRuntime.fds[j] == null) continue;
                    arrn[n4++] = j;
                }
                return new DevDirFD(){

                    public int myInode() {
                        return 4;
                    }

                    public int parentInode() {
                        return 1;
                    }

                    public int inode(int n) {
                        return 32 + n;
                    }

                    public String name(int n) {
                        return Integer.toString(arrn[n]);
                    }

                    public int size() {
                        return arrn.length;
                    }
                };
            }
            if (string.equals("")) {
                return new DevDirFD(){

                    public int myInode() {
                        return 1;
                    }

                    public int parentInode() {
                        return 1;
                    }

                    public int inode(int n) {
                        switch (n) {
                            case 0: {
                                return 2;
                            }
                            case 1: {
                                return 3;
                            }
                            case 2: {
                                return 4;
                            }
                        }
                        return -1;
                    }

                    public String name(int n) {
                        switch (n) {
                            case 0: {
                                return "null";
                            }
                            case 1: {
                                return "zero";
                            }
                            case 2: {
                                return "fd";
                            }
                        }
                        return null;
                    }

                    public int size() {
                        return 3;
                    }
                };
            }
            return null;
        }

        public Runtime.FStat stat(UnixRuntime unixRuntime, String string) throws Runtime.ErrnoException {
            if (string.equals("null")) {
                return this.devNullFD.fstat();
            }
            if (string.equals("zero")) {
                return this.devZeroFD.fstat();
            }
            if (string.startsWith("fd/")) {
                int n;
                try {
                    n = Integer.parseInt(string.substring(3));
                }
                catch (NumberFormatException var4_4) {
                    return null;
                }
                if (n < 0 || n >= 64) {
                    return null;
                }
                if (unixRuntime.fds[n] == null) {
                    return null;
                }
                return unixRuntime.fds[n].fstat();
            }
            if (string.equals("fd")) {
                return new Runtime.FStat(){

                    public int inode() {
                        return 4;
                    }

                    public int dev() {
                        return DevFS.this.devno;
                    }

                    public int type() {
                        return 16384;
                    }

                    public int mode() {
                        return 292;
                    }
                };
            }
            if (string.equals("")) {
                return new Runtime.FStat(){

                    public int inode() {
                        return 1;
                    }

                    public int dev() {
                        return DevFS.this.devno;
                    }

                    public int type() {
                        return 16384;
                    }

                    public int mode() {
                        return 292;
                    }
                };
            }
            return null;
        }

        public void mkdir(UnixRuntime unixRuntime, String string, int n) throws Runtime.ErrnoException {
            throw new Runtime.ErrnoException(30);
        }

        public void unlink(UnixRuntime unixRuntime, String string) throws Runtime.ErrnoException {
            throw new Runtime.ErrnoException(30);
        }

        private abstract class DevDirFD
        extends DirFD {
            private DevDirFD() {
            }

            public int myDev() {
                return DevFS.this.devno;
            }
        }

        private abstract class DevFStat
        extends Runtime.FStat {
            private DevFStat() {
            }

            public int dev() {
                return DevFS.this.devno;
            }

            public int mode() {
                return 438;
            }

            public int type() {
                return 8192;
            }

            public int nlink() {
                return 1;
            }

            public abstract int inode();
        }

    }

    public static abstract class DirFD
    extends Runtime.FD {
        private int pos = -2;

        protected abstract int size();

        protected abstract String name(int var1);

        protected abstract int inode(int var1);

        protected abstract int myDev();

        protected abstract int parentInode();

        protected abstract int myInode();

        public int flags() {
            return 0;
        }

        /*
         * Unable to fully structure code
         * Enabled aggressive block sorting
         * Lifted jumps to return sites
         */
        public int getdents(byte[] var1_1, int var2_2, int var3_3) {
            var4_4 = var2_2;
            while (var3_3 > 0) {
                if (this.pos >= this.size()) return var2_2 - var4_4;
                switch (this.pos) {
                    case -2: 
                    case -1: {
                        v0 = var5_5 = this.pos == -1 ? this.parentInode() : this.myInode();
                        if (var5_5 != -1) {
                            var6_6 = 9 + (this.pos == -1 ? 2 : 1);
                            if (var6_6 > var3_3) {
                                return var2_2 - var4_4;
                            }
                            var1_1[var2_2 + 8] = 46;
                            if (this.pos != -1) break;
                            var1_1[var2_2 + 9] = 46;
                            break;
                        }
                        ** GOTO lbl30
                    }
                    default: {
                        var7_7 = this.name(this.pos);
                        var8_8 = Runtime.getBytes(var7_7);
                        var6_6 = var8_8.length + 9;
                        if (var6_6 > var3_3) {
                            return var2_2 - var4_4;
                        }
                        var5_5 = this.inode(this.pos);
                        System.arraycopy(var8_8, 0, var1_1, var2_2 + 8, var8_8.length);
                    }
                }
                var1_1[var2_2 + var6_6 - 1] = 0;
                var6_6 = var6_6 + 3 & -4;
                UnixRuntime.access$500(var1_1, var2_2, var6_6);
                UnixRuntime.access$500(var1_1, var2_2 + 4, var5_5);
                var2_2 += var6_6;
                var3_3 -= var6_6;
lbl30: // 2 sources:
                ++this.pos;
            }
            return var2_2 - var4_4;
        }

        protected Runtime.FStat _fstat() {
            return new Runtime.FStat(){

                public int type() {
                    return 16384;
                }

                public int inode() {
                    return DirFD.this.myInode();
                }

                public int dev() {
                    return DirFD.this.myDev();
                }
            };
        }

    }

    public static class CygdriveFS
    extends HostFS {
        protected File hostFile(String string) {
            char c = string.charAt(0);
            if (c < 'a' || c > 'z' || string.charAt(1) != '/') {
                return null;
            }
            string = "" + c + ":" + string.substring(1).replace('/', '\\');
            return new File(string);
        }

        public CygdriveFS() {
            super("/");
        }
    }

    public static class HostFS
    extends FS {
        InodeCache inodes = new InodeCache(4000);
        protected File root;

        public File getRoot() {
            return this.root;
        }

        protected File hostFile(String string) {
            char c = File.separatorChar;
            if (c != '/') {
                char[] arrc = string.toCharArray();
                for (int i = 0; i < arrc.length; ++i) {
                    char c2 = arrc[i];
                    if (c2 == '/') {
                        arrc[i] = c;
                        continue;
                    }
                    if (c2 != c) continue;
                    arrc[i] = 47;
                }
                string = new String(arrc);
            }
            return new File(this.root, string);
        }

        public HostFS(String string) {
            this(new File(string));
        }

        public HostFS(File file) {
            this.root = file;
        }

        public Runtime.FD open(UnixRuntime unixRuntime, String string, int n, int n2) throws Runtime.ErrnoException {
            File file = this.hostFile(string);
            return unixRuntime.hostFSOpen(file, n, n2, this);
        }

        public void unlink(UnixRuntime unixRuntime, String string) throws Runtime.ErrnoException {
            File file = this.hostFile(string);
            if (unixRuntime.sm != null && !unixRuntime.sm.allowUnlink(file)) {
                throw new Runtime.ErrnoException(1);
            }
            if (!file.exists()) {
                throw new Runtime.ErrnoException(2);
            }
            if (!file.delete()) {
                boolean bl = false;
                for (int i = 0; i < 64; ++i) {
                    String string2;
                    if (unixRuntime.fds[i] == null || (string2 = unixRuntime.fds[i].getNormalizedPath()) == null || !string2.equals(string)) continue;
                    unixRuntime.fds[i].markDeleteOnClose();
                    bl = true;
                }
                if (!bl) {
                    throw new Runtime.ErrnoException(1);
                }
            }
        }

        public Runtime.FStat stat(UnixRuntime unixRuntime, String string) throws Runtime.ErrnoException {
            File file = this.hostFile(string);
            if (unixRuntime.sm != null && !unixRuntime.sm.allowStat(file)) {
                throw new Runtime.ErrnoException(13);
            }
            if (!file.exists()) {
                return null;
            }
            return unixRuntime.hostFStat(file, this);
        }

        public void mkdir(UnixRuntime unixRuntime, String string, int n) throws Runtime.ErrnoException {
            File file = this.hostFile(string);
            if (unixRuntime.sm != null && !unixRuntime.sm.allowWrite(file)) {
                throw new Runtime.ErrnoException(13);
            }
            if (file.exists() && file.isDirectory()) {
                throw new Runtime.ErrnoException(17);
            }
            if (file.exists()) {
                throw new Runtime.ErrnoException(20);
            }
            File file2 = HostFS.getParentFile(file);
            if (!(file2 == null || file2.exists() && file2.isDirectory())) {
                throw new Runtime.ErrnoException(20);
            }
            if (!file.mkdir()) {
                throw new Runtime.ErrnoException(5);
            }
        }

        private static File getParentFile(File file) {
            String string = file.getParent();
            return string == null ? null : new File(string);
        }

        public class HostDirFD
        extends DirFD {
            private final File f;
            private final File[] children;

            public HostDirFD(File file) {
                this.f = file;
                String[] arrstring = file.list();
                this.children = new File[arrstring.length];
                for (int i = 0; i < arrstring.length; ++i) {
                    this.children[i] = new File(file, arrstring[i]);
                }
            }

            public int size() {
                return this.children.length;
            }

            public String name(int n) {
                return this.children[n].getName();
            }

            public int inode(int n) {
                return HostFS.this.inodes.get(this.children[n].getAbsolutePath());
            }

            public int parentInode() {
                File file = HostFS.getParentFile(this.f);
                return file == null ? this.myInode() : (int)HostFS.this.inodes.get(file.getAbsolutePath());
            }

            public int myInode() {
                return HostFS.this.inodes.get(this.f.getAbsolutePath());
            }

            public int myDev() {
                return HostFS.this.devno;
            }
        }

    }

    public static abstract class FS {
        static final int OPEN = 1;
        static final int STAT = 2;
        static final int LSTAT = 3;
        static final int MKDIR = 4;
        static final int UNLINK = 5;
        GlobalState owner;
        int devno;

        Object dispatch(int n, UnixRuntime unixRuntime, String string, int n2, int n3) throws Runtime.ErrnoException {
            switch (n) {
                case 1: {
                    return this.open(unixRuntime, string, n2, n3);
                }
                case 2: {
                    return this.stat(unixRuntime, string);
                }
                case 3: {
                    return this.lstat(unixRuntime, string);
                }
                case 4: {
                    this.mkdir(unixRuntime, string, n2);
                    return null;
                }
                case 5: {
                    this.unlink(unixRuntime, string);
                    return null;
                }
            }
            throw new Error("should never happen");
        }

        public Runtime.FStat lstat(UnixRuntime unixRuntime, String string) throws Runtime.ErrnoException {
            return this.stat(unixRuntime, string);
        }

        public abstract Runtime.FD open(UnixRuntime var1, String var2, int var3, int var4) throws Runtime.ErrnoException;

        public abstract Runtime.FStat stat(UnixRuntime var1, String var2) throws Runtime.ErrnoException;

        public abstract void mkdir(UnixRuntime var1, String var2, int var3) throws Runtime.ErrnoException;

        public abstract void unlink(UnixRuntime var1, String var2) throws Runtime.ErrnoException;
    }

    public static final class GlobalState {
        Hashtable execCache = new Hashtable();
        final UnixRuntime[] tasks;
        int nextPID = 1;
        Seekable.Lock[] locks = new Seekable.Lock[16];
        private MP[] mps = new MP[0];
        private FS root;

        public GlobalState() {
            this(255);
        }

        public GlobalState(int n) {
            this(n, true);
        }

        public GlobalState(int n, boolean bl) {
            this.tasks = new UnixRuntime[n + 1];
            if (bl) {
                File[] arrfile;
                File file = null;
                if (Platform.getProperty("nestedvm.root") != null) {
                    file = new File(Platform.getProperty("nestedvm.root"));
                    if (!file.isDirectory()) {
                        throw new IllegalArgumentException("nestedvm.root is not a directory");
                    }
                } else {
                    arrfile = Platform.getProperty("user.dir");
                    file = Platform.getRoot(new File((String)(arrfile != null ? arrfile : ".")));
                }
                this.addMount("/", new HostFS(file));
                if (Platform.getProperty("nestedvm.root") == null) {
                    arrfile = Platform.listRoots();
                    for (int i = 0; i < arrfile.length; ++i) {
                        String string = arrfile[i].getPath();
                        if (string.endsWith(File.separator)) {
                            string = string.substring(0, string.length() - 1);
                        }
                        if (string.length() == 0 || string.indexOf(47) != -1) continue;
                        this.addMount("/" + string.toLowerCase(), new HostFS(arrfile[i]));
                    }
                }
                this.addMount("/dev", new DevFS());
                this.addMount("/resource", new ResourceFS());
                this.addMount("/cygdrive", new CygdriveFS());
            }
        }

        public String mapHostPath(String string) {
            return this.mapHostPath(new File(string));
        }

        public String mapHostPath(File file) {
            FS fS;
            GlobalState globalState = this;
            synchronized (globalState) {
                fS = this.root;
            }
            if (!file.isAbsolute()) {
                file = new File(file.getAbsolutePath());
            }
            for (int i = this.mps.length; i >= 0; --i) {
                Object object;
                String string;
                FS fS2 = i == this.mps.length ? fS : this.mps[i].fs;
                String string2 = string = i == this.mps.length ? "" : this.mps[i].path;
                if (!(fS2 instanceof HostFS)) continue;
                File file2 = ((HostFS)fS2).getRoot();
                if (!file2.isAbsolute()) {
                    file2 = new File(file2.getAbsolutePath());
                }
                if (!file.getPath().startsWith(file2.getPath())) continue;
                char c = File.separatorChar;
                String string3 = file.getPath().substring(file2.getPath().length());
                if (c != '/') {
                    object = string3.toCharArray();
                    for (int j = 0; j < object.length; ++j) {
                        if (object[j] == '/') {
                            object[j] = c;
                            continue;
                        }
                        if (object[j] != c) continue;
                        object[j] = 47;
                    }
                    string3 = new String((char[])object);
                }
                object = "/" + (string.length() == 0 ? "" : new StringBuffer().append(string).append("/").toString()) + string3;
                return object;
            }
            return null;
        }

        public synchronized FS getMount(String string) {
            if (!string.startsWith("/")) {
                throw new IllegalArgumentException("Mount point doesn't start with a /");
            }
            if (string.equals("/")) {
                return this.root;
            }
            string = string.substring(1);
            for (int i = 0; i < this.mps.length; ++i) {
                if (!this.mps[i].path.equals(string)) continue;
                return this.mps[i].fs;
            }
            return null;
        }

        public synchronized void addMount(String string, FS fS) {
            if (this.getMount(string) != null) {
                throw new IllegalArgumentException("mount point already exists");
            }
            if (!string.startsWith("/")) {
                throw new IllegalArgumentException("Mount point doesn't start with a /");
            }
            if (fS.owner != null) {
                fS.owner.removeMount(fS);
            }
            fS.owner = this;
            if (string.equals("/")) {
                this.root = fS;
                fS.devno = 1;
                return;
            }
            string = string.substring(1);
            int n = this.mps.length;
            Sort.Comparable[] arrcomparable = new MP[n + 1];
            if (n != 0) {
                System.arraycopy(this.mps, 0, arrcomparable, 0, n);
            }
            arrcomparable[n] = new MP(string, fS);
            Sort.sort(arrcomparable);
            this.mps = arrcomparable;
            int n2 = 0;
            for (int i = 0; i < this.mps.length; ++i) {
                n2 = Runtime.max(n2, this.mps[i].fs.devno);
            }
            fS.devno = n2 + 2;
        }

        public synchronized void removeMount(FS fS) {
            for (int i = 0; i < this.mps.length; ++i) {
                if (this.mps[i].fs != fS) continue;
                this.removeMount(i);
                return;
            }
            throw new IllegalArgumentException("mount point doesn't exist");
        }

        public synchronized void removeMount(String string) {
            if (!string.startsWith("/")) {
                throw new IllegalArgumentException("Mount point doesn't start with a /");
            }
            if (string.equals("/")) {
                this.removeMount(-1);
            } else {
                int n;
                string = string.substring(1);
                for (n = 0; n < this.mps.length && !this.mps[n].path.equals(string); ++n) {
                }
                if (n == this.mps.length) {
                    throw new IllegalArgumentException("mount point doesn't exist");
                }
                this.removeMount(n);
            }
        }

        private void removeMount(int n) {
            if (n == -1) {
                this.root.owner = null;
                this.root = null;
                return;
            }
            MP[] arrmP = new MP[this.mps.length - 1];
            System.arraycopy(this.mps, 0, arrmP, 0, n);
            System.arraycopy(this.mps, 0, arrmP, n, this.mps.length - n - 1);
            this.mps = arrmP;
        }

        private Object fsop(int n, UnixRuntime unixRuntime, String string, int n2, int n3) throws Runtime.ErrnoException {
            int n4 = string.length();
            if (n4 != 0) {
                MP[] arrmP;
                GlobalState globalState = this;
                synchronized (globalState) {
                    arrmP = this.mps;
                }
                for (int i = 0; i < arrmP.length; ++i) {
                    MP mP = arrmP[i];
                    int n5 = mP.path.length();
                    if (!string.startsWith(mP.path) || n4 != n5 && string.charAt(n5) != '/') continue;
                    return mP.fs.dispatch(n, unixRuntime, n4 == n5 ? "" : string.substring(n5 + 1), n2, n3);
                }
            }
            return this.root.dispatch(n, unixRuntime, string, n2, n3);
        }

        public final Runtime.FD open(UnixRuntime unixRuntime, String string, int n, int n2) throws Runtime.ErrnoException {
            return (Runtime.FD)this.fsop(1, unixRuntime, string, n, n2);
        }

        public final Runtime.FStat stat(UnixRuntime unixRuntime, String string) throws Runtime.ErrnoException {
            return (Runtime.FStat)this.fsop(2, unixRuntime, string, 0, 0);
        }

        public final Runtime.FStat lstat(UnixRuntime unixRuntime, String string) throws Runtime.ErrnoException {
            return (Runtime.FStat)this.fsop(3, unixRuntime, string, 0, 0);
        }

        public final void mkdir(UnixRuntime unixRuntime, String string, int n) throws Runtime.ErrnoException {
            this.fsop(4, unixRuntime, string, n, 0);
        }

        public final void unlink(UnixRuntime unixRuntime, String string) throws Runtime.ErrnoException {
            this.fsop(5, unixRuntime, string, 0, 0);
        }

        private static class CacheEnt {
            public final long time;
            public final long size;
            public final Object o;

            public CacheEnt(long l, long l2, Object object) {
                this.time = l;
                this.size = l2;
                this.o = object;
            }
        }

        static class MP
        implements Sort.Comparable {
            public String path;
            public FS fs;

            public MP(String string, FS fS) {
                this.path = string;
                this.fs = fS;
            }

            public int compareTo(Object object) {
                if (!(object instanceof MP)) {
                    return 1;
                }
                return - this.path.compareTo(((MP)object).path);
            }
        }

    }

    static class SocketFD
    extends Runtime.FD {
        public static final int TYPE_STREAM = 0;
        public static final int TYPE_DGRAM = 1;
        public static final int LISTEN = 2;
        int flags;
        int options;
        Socket s;
        ServerSocket ss;
        DatagramSocket ds;
        InetAddress bindAddr;
        int bindPort = -1;
        InetAddress connectAddr;
        int connectPort = -1;
        DatagramPacket dp;
        InputStream is;
        OutputStream os;
        private static final byte[] EMPTY = new byte[0];

        public int type() {
            return this.flags & 1;
        }

        public boolean listen() {
            return (this.flags & 2) != 0;
        }

        public SocketFD(int n) {
            this.flags = n;
            if (n == 1) {
                this.dp = new DatagramPacket(EMPTY, 0);
            }
        }

        public void setOptions() {
            try {
                if (this.s != null && this.type() == 0 && !this.listen()) {
                    Platform.socketSetKeepAlive(this.s, (this.options & 8) != 0);
                }
            }
            catch (SocketException var1_1) {
                var1_1.printStackTrace();
            }
        }

        public void _close() {
            try {
                if (this.s != null) {
                    this.s.close();
                }
                if (this.ss != null) {
                    this.ss.close();
                }
                if (this.ds != null) {
                    this.ds.close();
                }
            }
            catch (IOException var1_1) {
                // empty catch block
            }
        }

        public int read(byte[] arrby, int n, int n2) throws Runtime.ErrnoException {
            if (this.type() == 1) {
                return this.recvfrom(arrby, n, n2, null, null);
            }
            if (this.is == null) {
                throw new Runtime.ErrnoException(32);
            }
            try {
                int n3 = this.is.read(arrby, n, n2);
                return n3 < 0 ? 0 : n3;
            }
            catch (IOException var4_5) {
                throw new Runtime.ErrnoException(5);
            }
        }

        public int recvfrom(byte[] arrby, int n, int n2, InetAddress[] arrinetAddress, int[] arrn) throws Runtime.ErrnoException {
            if (this.type() == 0) {
                return this.read(arrby, n, n2);
            }
            if (n != 0) {
                throw new IllegalArgumentException("off must be 0");
            }
            this.dp.setData(arrby);
            this.dp.setLength(n2);
            try {
                if (this.ds == null) {
                    this.ds = new DatagramSocket();
                }
                this.ds.receive(this.dp);
            }
            catch (IOException var6_6) {
                var6_6.printStackTrace();
                throw new Runtime.ErrnoException(5);
            }
            if (arrinetAddress != null) {
                arrinetAddress[0] = this.dp.getAddress();
                arrn[0] = this.dp.getPort();
            }
            return this.dp.getLength();
        }

        public int write(byte[] arrby, int n, int n2) throws Runtime.ErrnoException {
            if (this.type() == 1) {
                return this.sendto(arrby, n, n2, null, -1);
            }
            if (this.os == null) {
                throw new Runtime.ErrnoException(32);
            }
            try {
                this.os.write(arrby, n, n2);
                return n2;
            }
            catch (IOException var4_4) {
                throw new Runtime.ErrnoException(5);
            }
        }

        public int sendto(byte[] arrby, int n, int n2, InetAddress inetAddress, int n3) throws Runtime.ErrnoException {
            if (n != 0) {
                throw new IllegalArgumentException("off must be 0");
            }
            if (this.type() == 0) {
                return this.write(arrby, n, n2);
            }
            if (inetAddress == null) {
                inetAddress = this.connectAddr;
                n3 = this.connectPort;
                if (inetAddress == null) {
                    throw new Runtime.ErrnoException(128);
                }
            }
            this.dp.setAddress(inetAddress);
            this.dp.setPort(n3);
            this.dp.setData(arrby);
            this.dp.setLength(n2);
            try {
                if (this.ds == null) {
                    this.ds = new DatagramSocket();
                }
                this.ds.send(this.dp);
            }
            catch (IOException var6_6) {
                var6_6.printStackTrace();
                if ("Network is unreachable".equals(var6_6.getMessage())) {
                    throw new Runtime.ErrnoException(118);
                }
                throw new Runtime.ErrnoException(5);
            }
            return this.dp.getLength();
        }

        public int flags() {
            return 2;
        }

        public Runtime.FStat _fstat() {
            return new Runtime.SocketFStat();
        }
    }

    static class Pipe {
        private final byte[] pipebuf = new byte[2048];
        private int readPos;
        private int writePos;
        public final Runtime.FD reader;
        public final Runtime.FD writer;

        Pipe() {
            this.reader = new Reader();
            this.writer = new Writer();
        }

        static /* synthetic */ int access$212(Pipe pipe, int n) {
            return pipe.readPos += n;
        }

        static /* synthetic */ int access$112(Pipe pipe, int n) {
            return pipe.writePos += n;
        }

        public class Writer
        extends Runtime.FD {
            protected Runtime.FStat _fstat() {
                return new Runtime.SocketFStat();
            }

            public int write(byte[] arrby, int n, int n2) throws Runtime.ErrnoException {
                if (n2 == 0) {
                    return 0;
                }
                Pipe pipe = Pipe.this;
                synchronized (pipe) {
                    if (Pipe.this.readPos == -1) {
                        throw new Runtime.ErrnoException(32);
                    }
                    if (Pipe.this.pipebuf.length - Pipe.this.writePos < Math.min(n2, 512)) {
                        while (Pipe.this.readPos != -1 && Pipe.this.readPos != Pipe.this.writePos) {
                            try {
                                Pipe.this.wait();
                            }
                            catch (InterruptedException var5_5) {}
                        }
                        if (Pipe.this.readPos == -1) {
                            throw new Runtime.ErrnoException(32);
                        }
                        Pipe.this.readPos = (Pipe.this.writePos = 0);
                    }
                    n2 = Math.min(n2, Pipe.this.pipebuf.length - Pipe.this.writePos);
                    System.arraycopy(arrby, n, Pipe.this.pipebuf, Pipe.this.writePos, n2);
                    if (Pipe.this.readPos == Pipe.this.writePos) {
                        Pipe.this.notify();
                    }
                    Pipe.access$112(Pipe.this, n2);
                    return n2;
                }
            }

            public int flags() {
                return 1;
            }

            public void _close() {
                Pipe pipe = Pipe.this;
                synchronized (pipe) {
                    Pipe.this.writePos = -1;
                    Pipe.this.notify();
                }
            }
        }

        public class Reader
        extends Runtime.FD {
            protected Runtime.FStat _fstat() {
                return new Runtime.SocketFStat();
            }

            public int read(byte[] arrby, int n, int n2) throws Runtime.ErrnoException {
                if (n2 == 0) {
                    return 0;
                }
                Pipe pipe = Pipe.this;
                synchronized (pipe) {
                    while (Pipe.this.writePos != -1 && Pipe.this.readPos == Pipe.this.writePos) {
                        try {
                            Pipe.this.wait();
                        }
                        catch (InterruptedException var5_5) {}
                    }
                    if (Pipe.this.writePos == -1) {
                        return 0;
                    }
                    n2 = Math.min(n2, Pipe.this.writePos - Pipe.this.readPos);
                    System.arraycopy(Pipe.this.pipebuf, Pipe.this.readPos, arrby, n, n2);
                    Pipe.access$212(Pipe.this, n2);
                    if (Pipe.this.readPos == Pipe.this.writePos) {
                        Pipe.this.notify();
                    }
                    return n2;
                }
            }

            public int flags() {
                return 0;
            }

            public void _close() {
                Pipe pipe = Pipe.this;
                synchronized (pipe) {
                    Pipe.this.readPos = -1;
                    Pipe.this.notify();
                }
            }
        }

    }

    public static final class ForkedProcess
    extends Thread {
        private final UnixRuntime initial;

        public ForkedProcess(UnixRuntime unixRuntime) {
            this.initial = unixRuntime;
            this.start();
        }

        public void run() {
            UnixRuntime.executeAndExec(this.initial);
        }
    }

    private static class ProcessTableFullExn
    extends RuntimeException {
        private ProcessTableFullExn() {
        }
    }

}

