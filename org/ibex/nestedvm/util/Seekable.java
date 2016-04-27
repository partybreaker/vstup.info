/*
 * Decompiled with CFR 0_114.
 */
package org.ibex.nestedvm.util;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import org.ibex.nestedvm.util.Platform;

public abstract class Seekable {
    public abstract int read(byte[] var1, int var2, int var3) throws IOException;

    public abstract int write(byte[] var1, int var2, int var3) throws IOException;

    public abstract int length() throws IOException;

    public abstract void seek(int var1) throws IOException;

    public abstract void close() throws IOException;

    public abstract int pos() throws IOException;

    public void sync() throws IOException {
        throw new IOException("sync not implemented for " + this.getClass());
    }

    public void resize(long l) throws IOException {
        throw new IOException("resize not implemented for " + this.getClass());
    }

    public Lock lock(long l, long l2, boolean bl) throws IOException {
        throw new IOException("lock not implemented for " + this.getClass());
    }

    public int read() throws IOException {
        byte[] arrby = new byte[1];
        int n = this.read(arrby, 0, 1);
        return n == -1 ? -1 : arrby[0] & 255;
    }

    public int tryReadFully(byte[] arrby, int n, int n2) throws IOException {
        int n3;
        int n4 = 0;
        while (n2 > 0 && (n3 = this.read(arrby, n, n2)) != -1) {
            n += n3;
            n2 -= n3;
            n4 += n3;
        }
        return n4 == 0 ? -1 : n4;
    }

    public static abstract class Lock {
        private Object owner = null;

        public abstract Seekable seekable();

        public abstract boolean isShared();

        public abstract boolean isValid();

        public abstract void release() throws IOException;

        public abstract long position();

        public abstract long size();

        public void setOwner(Object object) {
            this.owner = object;
        }

        public Object getOwner() {
            return this.owner;
        }

        public final boolean contains(int n, int n2) {
            return (long)n >= this.position() && this.position() + this.size() >= (long)(n + n2);
        }

        public final boolean contained(int n, int n2) {
            return (long)n < this.position() && this.position() + this.size() < (long)(n + n2);
        }

        public final boolean overlaps(int n, int n2) {
            return this.contains(n, n2) || this.contained(n, n2);
        }
    }

    public static class InputStream
    extends Seekable {
        private byte[] buffer = new byte[4096];
        private int bytesRead = 0;
        private boolean eof = false;
        private int pos;
        private java.io.InputStream is;

        public InputStream(java.io.InputStream inputStream) {
            this.is = inputStream;
        }

        public int read(byte[] arrby, int n, int n2) throws IOException {
            if (this.pos >= this.bytesRead && !this.eof) {
                this.readTo(this.pos + 1);
            }
            if ((n2 = Math.min(n2, this.bytesRead - this.pos)) <= 0) {
                return -1;
            }
            System.arraycopy(this.buffer, this.pos, arrby, n, n2);
            this.pos += n2;
            return n2;
        }

        private void readTo(int n) throws IOException {
            if (n >= this.buffer.length) {
                byte[] arrby = new byte[Math.max(this.buffer.length + Math.min(this.buffer.length, 65536), n)];
                System.arraycopy(this.buffer, 0, arrby, 0, this.bytesRead);
                this.buffer = arrby;
            }
            while (this.bytesRead < n) {
                int n2 = this.is.read(this.buffer, this.bytesRead, this.buffer.length - this.bytesRead);
                if (n2 == -1) {
                    this.eof = true;
                    break;
                }
                this.bytesRead += n2;
            }
        }

        public int length() throws IOException {
            while (!this.eof) {
                this.readTo(this.bytesRead + 4096);
            }
            return this.bytesRead;
        }

        public int write(byte[] arrby, int n, int n2) throws IOException {
            throw new IOException("read-only");
        }

        public void seek(int n) {
            this.pos = n;
        }

        public int pos() {
            return this.pos;
        }

        public void close() throws IOException {
            this.is.close();
        }
    }

    public static class File
    extends Seekable {
        private final java.io.File file;
        private final RandomAccessFile raf;

        public File(String string) throws IOException {
            this(string, false);
        }

        public File(String string, boolean bl) throws IOException {
            this(new java.io.File(string), bl, false);
        }

        public File(java.io.File file, boolean bl, boolean bl2) throws IOException {
            this.file = file;
            String string = bl ? "rw" : "r";
            this.raf = new RandomAccessFile(file, string);
            if (bl2) {
                Platform.setFileLength(this.raf, 0);
            }
        }

        public int read(byte[] arrby, int n, int n2) throws IOException {
            return this.raf.read(arrby, n, n2);
        }

        public int write(byte[] arrby, int n, int n2) throws IOException {
            this.raf.write(arrby, n, n2);
            return n2;
        }

        public void sync() throws IOException {
            this.raf.getFD().sync();
        }

        public void seek(int n) throws IOException {
            this.raf.seek(n);
        }

        public int pos() throws IOException {
            return (int)this.raf.getFilePointer();
        }

        public int length() throws IOException {
            return (int)this.raf.length();
        }

        public void close() throws IOException {
            this.raf.close();
        }

        public void resize(long l) throws IOException {
            Platform.setFileLength(this.raf, (int)l);
        }

        public boolean equals(Object object) {
            return object != null && object instanceof File && this.file.equals(((File)object).file);
        }

        public Lock lock(long l, long l2, boolean bl) throws IOException {
            return Platform.lockFile(this, this.raf, l, l2, bl);
        }
    }

    public static class ByteArray
    extends Seekable {
        protected byte[] data;
        protected int pos;
        private final boolean writable;

        public ByteArray(byte[] arrby, boolean bl) {
            this.data = arrby;
            this.pos = 0;
            this.writable = bl;
        }

        public int read(byte[] arrby, int n, int n2) {
            if ((n2 = Math.min(n2, this.data.length - this.pos)) <= 0) {
                return -1;
            }
            System.arraycopy(this.data, this.pos, arrby, n, n2);
            this.pos += n2;
            return n2;
        }

        public int write(byte[] arrby, int n, int n2) throws IOException {
            if (!this.writable) {
                throw new IOException("read-only data");
            }
            if ((n2 = Math.min(n2, this.data.length - this.pos)) <= 0) {
                throw new IOException("no space");
            }
            System.arraycopy(arrby, n, this.data, this.pos, n2);
            this.pos += n2;
            return n2;
        }

        public int length() {
            return this.data.length;
        }

        public int pos() {
            return this.pos;
        }

        public void seek(int n) {
            this.pos = n;
        }

        public void close() {
        }
    }

}

