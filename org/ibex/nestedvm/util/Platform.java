/*
 * Decompiled with CFR 0_114.
 */
package org.ibex.nestedvm.util;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.text.DateFormatSymbols;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.TimeZone;
import org.ibex.nestedvm.util.Seekable;

public abstract class Platform {
    private static final Platform p;
    static /* synthetic */ Class class$org$ibex$nestedvm$util$Platform;

    Platform() {
    }

    public static String getProperty(String string) {
        try {
            return System.getProperty(string);
        }
        catch (SecurityException var1_1) {
            return null;
        }
    }

    abstract boolean _atomicCreateFile(File var1) throws IOException;

    public static boolean atomicCreateFile(File file) throws IOException {
        return p._atomicCreateFile(file);
    }

    abstract Seekable.Lock _lockFile(Seekable var1, RandomAccessFile var2, long var3, long var5, boolean var7) throws IOException;

    public static Seekable.Lock lockFile(Seekable seekable, RandomAccessFile randomAccessFile, long l, long l2, boolean bl) throws IOException {
        return p._lockFile(seekable, randomAccessFile, l, l2, bl);
    }

    abstract void _socketHalfClose(Socket var1, boolean var2) throws IOException;

    public static void socketHalfClose(Socket socket, boolean bl) throws IOException {
        p._socketHalfClose(socket, bl);
    }

    abstract void _socketSetKeepAlive(Socket var1, boolean var2) throws SocketException;

    public static void socketSetKeepAlive(Socket socket, boolean bl) throws SocketException {
        p._socketSetKeepAlive(socket, bl);
    }

    abstract InetAddress _inetAddressFromBytes(byte[] var1) throws UnknownHostException;

    public static InetAddress inetAddressFromBytes(byte[] arrby) throws UnknownHostException {
        return p._inetAddressFromBytes(arrby);
    }

    abstract String _timeZoneGetDisplayName(TimeZone var1, boolean var2, boolean var3, Locale var4);

    public static String timeZoneGetDisplayName(TimeZone timeZone, boolean bl, boolean bl2, Locale locale) {
        return p._timeZoneGetDisplayName(timeZone, bl, bl2, locale);
    }

    public static String timeZoneGetDisplayName(TimeZone timeZone, boolean bl, boolean bl2) {
        return Platform.timeZoneGetDisplayName(timeZone, bl, bl2, Locale.getDefault());
    }

    abstract void _setFileLength(RandomAccessFile var1, int var2) throws IOException;

    public static void setFileLength(RandomAccessFile randomAccessFile, int n) throws IOException {
        p._setFileLength(randomAccessFile, n);
    }

    abstract File[] _listRoots();

    public static File[] listRoots() {
        return p._listRoots();
    }

    abstract File _getRoot(File var1);

    public static File getRoot(File file) {
        return p._getRoot(file);
    }

    static /* synthetic */ Class class$(String string) {
        try {
            return Class.forName(string);
        }
        catch (ClassNotFoundException var1_1) {
            throw new NoClassDefFoundError(var1_1.getMessage());
        }
    }

    static {
        float f;
        String string;
        try {
            f = Platform.getProperty("java.vm.name").equals("SableVM") ? 1.2f : Float.valueOf(Platform.getProperty("java.specification.version")).floatValue();
        }
        catch (Exception var1_1) {
            System.err.println("WARNING: " + var1_1 + " while trying to find jvm version -  assuming 1.1");
            f = 1.1f;
        }
        if (f >= 1.4f) {
            string = "Jdk14";
        } else if (f >= 1.3f) {
            string = "Jdk13";
        } else if (f >= 1.2f) {
            string = "Jdk12";
        } else if (f >= 1.1f) {
            string = "Jdk11";
        } else {
            throw new Error("JVM Specification version: " + f + " is too old. (see org.ibex.util.Platform to add support)");
        }
        try {
            Class class_ = class$org$ibex$nestedvm$util$Platform == null ? (Platform.class$org$ibex$nestedvm$util$Platform = Platform.class$("org.ibex.nestedvm.util.Platform")) : class$org$ibex$nestedvm$util$Platform;
            p = (Platform)Class.forName(class_.getName() + "$" + string).newInstance();
        }
        catch (Exception var2_3) {
            var2_3.printStackTrace();
            throw new Error("Error instansiating platform class");
        }
    }

    private static final class Jdk14FileLock
    extends Seekable.Lock {
        private final Seekable s;
        private final FileLock l;

        Jdk14FileLock(Seekable seekable, FileLock fileLock) {
            this.s = seekable;
            this.l = fileLock;
        }

        public Seekable seekable() {
            return this.s;
        }

        public boolean isShared() {
            return this.l.isShared();
        }

        public boolean isValid() {
            return this.l.isValid();
        }

        public void release() throws IOException {
            this.l.release();
        }

        public long position() {
            return this.l.position();
        }

        public long size() {
            return this.l.size();
        }

        public String toString() {
            return this.l.toString();
        }
    }

    static class Jdk14
    extends Jdk13 {
        Jdk14() {
        }

        InetAddress _inetAddressFromBytes(byte[] arrby) throws UnknownHostException {
            return InetAddress.getByAddress(arrby);
        }

        Seekable.Lock _lockFile(Seekable seekable, RandomAccessFile randomAccessFile, long l, long l2, boolean bl) throws IOException {
            FileLock fileLock;
            try {
                fileLock = l == 0 && l2 == 0 ? randomAccessFile.getChannel().lock() : randomAccessFile.getChannel().tryLock(l, l2, bl);
            }
            catch (OverlappingFileLockException var9_7) {
                fileLock = null;
            }
            if (fileLock == null) {
                return null;
            }
            return new Jdk14FileLock(seekable, fileLock);
        }
    }

    static class Jdk13
    extends Jdk12 {
        Jdk13() {
        }

        void _socketHalfClose(Socket socket, boolean bl) throws IOException {
            if (bl) {
                socket.shutdownOutput();
            } else {
                socket.shutdownInput();
            }
        }

        void _socketSetKeepAlive(Socket socket, boolean bl) throws SocketException {
            socket.setKeepAlive(bl);
        }
    }

    static class Jdk12
    extends Jdk11 {
        Jdk12() {
        }

        boolean _atomicCreateFile(File file) throws IOException {
            return file.createNewFile();
        }

        String _timeZoneGetDisplayName(TimeZone timeZone, boolean bl, boolean bl2, Locale locale) {
            return timeZone.getDisplayName(bl, bl2 ? 1 : 0, locale);
        }

        void _setFileLength(RandomAccessFile randomAccessFile, int n) throws IOException {
            randomAccessFile.setLength(n);
        }

        File[] _listRoots() {
            return File.listRoots();
        }
    }

    static class Jdk11
    extends Platform {
        Jdk11() {
        }

        boolean _atomicCreateFile(File file) throws IOException {
            if (file.exists()) {
                return false;
            }
            new FileOutputStream(file).close();
            return true;
        }

        Seekable.Lock _lockFile(Seekable seekable, RandomAccessFile randomAccessFile, long l, long l2, boolean bl) throws IOException {
            throw new IOException("file locking requires jdk 1.4+");
        }

        void _socketHalfClose(Socket socket, boolean bl) throws IOException {
            throw new IOException("half closing sockets not supported");
        }

        InetAddress _inetAddressFromBytes(byte[] arrby) throws UnknownHostException {
            if (arrby.length != 4) {
                throw new UnknownHostException("only ipv4 addrs supported");
            }
            return InetAddress.getByName("" + (arrby[0] & 255) + "." + (arrby[1] & 255) + "." + (arrby[2] & 255) + "." + (arrby[3] & 255));
        }

        void _socketSetKeepAlive(Socket socket, boolean bl) throws SocketException {
            if (bl) {
                throw new SocketException("keepalive not supported");
            }
        }

        String _timeZoneGetDisplayName(TimeZone timeZone, boolean bl, boolean bl2, Locale locale) {
            String[][] arrstring = new DateFormatSymbols(locale).getZoneStrings();
            String string = timeZone.getID();
            for (int i = 0; i < arrstring.length; ++i) {
                if (!arrstring[i][0].equals(string)) continue;
                return arrstring[i][bl ? (bl2 ? 3 : 4) : (bl2 ? 1 : 2)];
            }
            StringBuffer stringBuffer = new StringBuffer("GMT");
            int n = timeZone.getRawOffset() / 1000;
            if (n < 0) {
                stringBuffer.append("-");
                n = - n;
            } else {
                stringBuffer.append("+");
            }
            stringBuffer.append(n / 3600);
            if ((n %= 3600) > 0) {
                stringBuffer.append(":").append(n / 60);
            }
            if ((n %= 60) > 0) {
                stringBuffer.append(":").append(n);
            }
            return stringBuffer.toString();
        }

        void _setFileLength(RandomAccessFile randomAccessFile, int n) throws IOException {
            int n2;
            FileInputStream fileInputStream = new FileInputStream(randomAccessFile.getFD());
            FileOutputStream fileOutputStream = new FileOutputStream(randomAccessFile.getFD());
            byte[] arrby = new byte[1024];
            while (n > 0 && (n2 = fileInputStream.read(arrby, 0, Math.min(n, arrby.length))) != -1) {
                fileOutputStream.write(arrby, 0, n2);
                n -= n2;
            }
            if (n == 0) {
                return;
            }
            for (n2 = 0; n2 < arrby.length; ++n2) {
                arrby[n2] = 0;
            }
            while (n > 0) {
                fileOutputStream.write(arrby, 0, Math.min(n, arrby.length));
                n -= arrby.length;
            }
        }

        RandomAccessFile _truncatedRandomAccessFile(File file, String string) throws IOException {
            new FileOutputStream(file).close();
            return new RandomAccessFile(file, string);
        }

        File[] _listRoots() {
            Object object;
            String[] arrstring = new String[]{"java.home", "java.class.path", "java.library.path", "java.io.tmpdir", "java.ext.dirs", "user.home", "user.dir"};
            Hashtable<File, Boolean> hashtable = new Hashtable<File, Boolean>();
            for (int i = 0; i < arrstring.length; ++i) {
                int n;
                String string = Jdk11.getProperty(arrstring[i]);
                if (string == null) continue;
                do {
                    object = string;
                    n = string.indexOf(File.pathSeparatorChar);
                    if (n != -1) {
                        object = string.substring(0, n);
                        string = string.substring(n + 1);
                    }
                    File file = Jdk11.getRoot(new File((String)object));
                    hashtable.put(file, Boolean.TRUE);
                } while (n != -1);
            }
            File[] arrfile = new File[hashtable.size()];
            int n = 0;
            object = hashtable.keys();
            while (object.hasMoreElements()) {
                arrfile[n++] = (File)object.nextElement();
            }
            return arrfile;
        }

        File _getRoot(File file) {
            String string;
            if (!file.isAbsolute()) {
                file = new File(file.getAbsolutePath());
            }
            while ((string = file.getParent()) != null) {
                file = new File(string);
            }
            if (file.getPath().length() == 0) {
                file = new File("/");
            }
            return file;
        }
    }

}

