/*
 * Decompiled with CFR 0_114.
 */
package org.ibex.nestedvm.util;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import org.ibex.nestedvm.util.Seekable;

public class ELF {
    private static final int ELF_MAGIC = 2135247942;
    public static final int ELFCLASSNONE = 0;
    public static final int ELFCLASS32 = 1;
    public static final int ELFCLASS64 = 2;
    public static final int ELFDATANONE = 0;
    public static final int ELFDATA2LSB = 1;
    public static final int ELFDATA2MSB = 2;
    public static final int SHT_SYMTAB = 2;
    public static final int SHT_STRTAB = 3;
    public static final int SHT_NOBITS = 8;
    public static final int SHF_WRITE = 1;
    public static final int SHF_ALLOC = 2;
    public static final int SHF_EXECINSTR = 4;
    public static final int PF_X = 1;
    public static final int PF_W = 2;
    public static final int PF_R = 4;
    public static final int PT_LOAD = 1;
    public static final short ET_EXEC = 2;
    public static final short EM_MIPS = 8;
    private Seekable data;
    public ELFIdent ident;
    public ELFHeader header;
    public PHeader[] pheaders;
    public SHeader[] sheaders;
    private byte[] stringTable;
    private boolean sectionReaderActive;
    private Symtab _symtab;

    private void readFully(byte[] arrby) throws IOException {
        int n;
        int n2 = 0;
        for (int i = arrby.length; i > 0; i -= n) {
            n = this.data.read(arrby, n2, i);
            if (n == -1) {
                throw new IOException("EOF");
            }
            n2 += n;
        }
    }

    private int readIntBE() throws IOException {
        byte[] arrby = new byte[4];
        this.readFully(arrby);
        return (arrby[0] & 255) << 24 | (arrby[1] & 255) << 16 | (arrby[2] & 255) << 8 | (arrby[3] & 255) << 0;
    }

    private int readInt() throws IOException {
        int n = this.readIntBE();
        if (this.ident != null && this.ident.data == 1) {
            n = n << 24 & -16777216 | n << 8 & 16711680 | n >>> 8 & 65280 | n >> 24 & 255;
        }
        return n;
    }

    private short readShortBE() throws IOException {
        byte[] arrby = new byte[2];
        this.readFully(arrby);
        return (short)((arrby[0] & 255) << 8 | (arrby[1] & 255) << 0);
    }

    private short readShort() throws IOException {
        short s = this.readShortBE();
        if (this.ident != null && this.ident.data == 1) {
            s = (short)((s << 8 & 65280 | s >> 8 & 255) & 65535);
        }
        return s;
    }

    private byte readByte() throws IOException {
        byte[] arrby = new byte[1];
        this.readFully(arrby);
        return arrby[0];
    }

    public ELF(String string) throws IOException, ELFException {
        this(new Seekable.File(string, false));
    }

    public ELF(Seekable seekable) throws IOException, ELFException {
        int n;
        this.data = seekable;
        this.ident = new ELFIdent();
        this.header = new ELFHeader();
        this.pheaders = new PHeader[this.header.phnum];
        for (n = 0; n < this.header.phnum; ++n) {
            seekable.seek(this.header.phoff + n * this.header.phentsize);
            this.pheaders[n] = new PHeader();
        }
        this.sheaders = new SHeader[this.header.shnum];
        for (n = 0; n < this.header.shnum; ++n) {
            seekable.seek(this.header.shoff + n * this.header.shentsize);
            this.sheaders[n] = new SHeader();
        }
        if (this.header.shstrndx < 0 || this.header.shstrndx >= this.header.shnum) {
            throw new ELFException("Bad shstrndx");
        }
        seekable.seek(this.sheaders[this.header.shstrndx].offset);
        this.stringTable = new byte[this.sheaders[this.header.shstrndx].size];
        this.readFully(this.stringTable);
        for (n = 0; n < this.header.shnum; ++n) {
            SHeader sHeader = this.sheaders[n];
            sHeader.name = this.getString(sHeader.nameidx);
        }
    }

    private String getString(int n) {
        return this.getString(n, this.stringTable);
    }

    private String getString(int n, byte[] arrby) {
        StringBuffer stringBuffer = new StringBuffer();
        if (n < 0 || n >= arrby.length) {
            return "<invalid strtab entry>";
        }
        while (n >= 0 && n < arrby.length && arrby[n] != 0) {
            stringBuffer.append(arrby[n++]);
        }
        return stringBuffer.toString();
    }

    public SHeader sectionWithName(String string) {
        for (int i = 0; i < this.sheaders.length; ++i) {
            if (!this.sheaders[i].name.equals(string)) continue;
            return this.sheaders[i];
        }
        return null;
    }

    public Symtab getSymtab() throws IOException {
        if (this._symtab != null) {
            return this._symtab;
        }
        if (this.sectionReaderActive) {
            throw new ELFException("Can't read the symtab while a section reader is active");
        }
        SHeader sHeader = this.sectionWithName(".symtab");
        if (sHeader == null || sHeader.type != 2) {
            return null;
        }
        SHeader sHeader2 = this.sectionWithName(".strtab");
        if (sHeader2 == null || sHeader2.type != 3) {
            return null;
        }
        byte[] arrby = new byte[sHeader2.size];
        DataInputStream dataInputStream = new DataInputStream(sHeader2.getInputStream());
        dataInputStream.readFully(arrby);
        dataInputStream.close();
        this._symtab = new Symtab(sHeader.offset, sHeader.size, arrby);
        return this._symtab;
    }

    private static String toHex(int n) {
        return "0x" + Long.toString((long)n & 0xFFFFFFFFL, 16);
    }

    public class Symbol {
        public String name;
        public int addr;
        public int size;
        public byte info;
        public byte type;
        public byte binding;
        public byte other;
        public short shndx;
        public SHeader sheader;
        public static final int STT_FUNC = 2;
        public static final int STB_GLOBAL = 1;

        Symbol(byte[] arrby) throws IOException {
            this.name = ELF.this.getString(ELF.this.readInt(), arrby);
            this.addr = ELF.this.readInt();
            this.size = ELF.this.readInt();
            this.info = ELF.this.readByte();
            this.type = (byte)(this.info & 15);
            this.binding = (byte)(this.info >> 4);
            this.other = ELF.this.readByte();
            this.shndx = ELF.this.readShort();
        }
    }

    public class Symtab {
        public Symbol[] symbols;

        Symtab(int n, int n2, byte[] arrby) throws IOException {
            ELF.this.data.seek(n);
            int n3 = n2 / 16;
            this.symbols = new Symbol[n3];
            for (int i = 0; i < n3; ++i) {
                this.symbols[i] = new Symbol(arrby);
            }
        }

        public Symbol getSymbol(String string) {
            Symbol symbol = null;
            for (int i = 0; i < this.symbols.length; ++i) {
                if (!this.symbols[i].name.equals(string)) continue;
                if (symbol == null) {
                    symbol = this.symbols[i];
                    continue;
                }
                System.err.println("WARNING: Multiple symbol matches for " + string);
            }
            return symbol;
        }

        public Symbol getGlobalSymbol(String string) {
            for (int i = 0; i < this.symbols.length; ++i) {
                if (this.symbols[i].binding != 1 || !this.symbols[i].name.equals(string)) continue;
                return this.symbols[i];
            }
            return null;
        }
    }

    private class SectionInputStream
    extends InputStream {
        private int pos;
        private int maxpos;

        SectionInputStream(int n, int n2) throws IOException {
            if (ELF.this.sectionReaderActive) {
                throw new IOException("Section reader already active");
            }
            ELF.this.sectionReaderActive = true;
            this.pos = n;
            ELF.this.data.seek(this.pos);
            this.maxpos = n2;
        }

        private int bytesLeft() {
            return this.maxpos - this.pos;
        }

        public int read() throws IOException {
            byte[] arrby = new byte[1];
            return this.read(arrby, 0, 1) == -1 ? -1 : arrby[0] & 255;
        }

        public int read(byte[] arrby, int n, int n2) throws IOException {
            int n3 = ELF.this.data.read(arrby, n, Math.min(n2, this.bytesLeft()));
            if (n3 > 0) {
                this.pos += n3;
            }
            return n3;
        }

        public void close() {
            ELF.this.sectionReaderActive = false;
        }
    }

    public class ELFException
    extends IOException {
        ELFException(String string) {
            super(string);
        }
    }

    public class SHeader {
        int nameidx;
        public String name;
        public int type;
        public int flags;
        public int addr;
        public int offset;
        public int size;
        public int link;
        public int info;
        public int addralign;
        public int entsize;

        SHeader() throws IOException {
            this.nameidx = ELF.this.readInt();
            this.type = ELF.this.readInt();
            this.flags = ELF.this.readInt();
            this.addr = ELF.this.readInt();
            this.offset = ELF.this.readInt();
            this.size = ELF.this.readInt();
            this.link = ELF.this.readInt();
            this.info = ELF.this.readInt();
            this.addralign = ELF.this.readInt();
            this.entsize = ELF.this.readInt();
        }

        public InputStream getInputStream() throws IOException {
            return new BufferedInputStream(new SectionInputStream(this.offset, this.type == 8 ? 0 : this.offset + this.size));
        }

        public boolean isText() {
            return this.name.equals(".text");
        }

        public boolean isData() {
            return this.name.equals(".data") || this.name.equals(".sdata") || this.name.equals(".rodata") || this.name.equals(".ctors") || this.name.equals(".dtors");
        }

        public boolean isBSS() {
            return this.name.equals(".bss") || this.name.equals(".sbss");
        }
    }

    public class PHeader {
        public int type;
        public int offset;
        public int vaddr;
        public int paddr;
        public int filesz;
        public int memsz;
        public int flags;
        public int align;

        PHeader() throws IOException {
            this.type = ELF.this.readInt();
            this.offset = ELF.this.readInt();
            this.vaddr = ELF.this.readInt();
            this.paddr = ELF.this.readInt();
            this.filesz = ELF.this.readInt();
            this.memsz = ELF.this.readInt();
            this.flags = ELF.this.readInt();
            this.align = ELF.this.readInt();
            if (this.filesz > this.memsz) {
                throw new ELFException("ELF inconsistency: filesz > memsz (" + ELF.toHex(this.filesz) + " > " + ELF.toHex(this.memsz) + ")");
            }
        }

        public boolean writable() {
            return (this.flags & 2) != 0;
        }

        public InputStream getInputStream() throws IOException {
            return new BufferedInputStream(new SectionInputStream(this.offset, this.offset + this.filesz));
        }
    }

    public class ELFHeader {
        public short type;
        public short machine;
        public int version;
        public int entry;
        public int phoff;
        public int shoff;
        public int flags;
        public short ehsize;
        public short phentsize;
        public short phnum;
        public short shentsize;
        public short shnum;
        public short shstrndx;

        ELFHeader() throws IOException {
            this.type = ELF.this.readShort();
            this.machine = ELF.this.readShort();
            this.version = ELF.this.readInt();
            if (this.version != 1) {
                throw new ELFException("version != 1");
            }
            this.entry = ELF.this.readInt();
            this.phoff = ELF.this.readInt();
            this.shoff = ELF.this.readInt();
            this.flags = ELF.this.readInt();
            this.ehsize = ELF.this.readShort();
            this.phentsize = ELF.this.readShort();
            this.phnum = ELF.this.readShort();
            this.shentsize = ELF.this.readShort();
            this.shnum = ELF.this.readShort();
            this.shstrndx = ELF.this.readShort();
        }
    }

    public class ELFIdent {
        public byte klass;
        public byte data;
        public byte osabi;
        public byte abiversion;

        ELFIdent() throws IOException {
            if (ELF.this.readIntBE() != 2135247942) {
                throw new ELFException("Bad Magic");
            }
            this.klass = ELF.this.readByte();
            if (this.klass != 1) {
                throw new ELFException("org.ibex.nestedvm.util.ELF does not suport 64-bit binaries");
            }
            this.data = ELF.this.readByte();
            if (this.data != 1 && this.data != 2) {
                throw new ELFException("Unknown byte order");
            }
            ELF.this.readByte();
            this.osabi = ELF.this.readByte();
            this.abiversion = ELF.this.readByte();
            for (int i = 0; i < 7; ++i) {
                ELF.this.readByte();
            }
        }
    }

}

