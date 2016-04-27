/*
 * Decompiled with CFR 0_114.
 */
package org.ibex.nestedvm;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.Hashtable;
import java.util.StringTokenizer;
import org.ibex.nestedvm.ClassFileCompiler;
import org.ibex.nestedvm.JavaSourceCompiler;
import org.ibex.nestedvm.Registers;
import org.ibex.nestedvm.util.ELF;
import org.ibex.nestedvm.util.Seekable;

public abstract class Compiler
implements Registers {
    ELF elf;
    final String fullClassName;
    String source = "unknown.mips.binary";
    boolean fastMem = true;
    int maxInsnPerMethod = 128;
    int maxBytesPerMethod;
    int methodMask;
    int methodShift;
    boolean pruneCases = true;
    boolean assumeTailCalls = true;
    boolean debugCompiler = false;
    boolean printStats = false;
    boolean runtimeStats = false;
    boolean supportCall = true;
    boolean nullPointerCheck = false;
    String runtimeClass = "org.ibex.nestedvm.Runtime";
    String hashClass = "java.util.Hashtable";
    boolean unixRuntime;
    boolean lessConstants;
    boolean singleFloat;
    int pageSize = 4096;
    int totalPages = 65536;
    int pageShift;
    boolean onePage;
    Hashtable jumpableAddresses;
    ELF.Symbol userInfo;
    ELF.Symbol gp;
    private boolean used;
    private static String[] options = new String[]{"fastMem", "Enable fast memory access - RuntimeExceptions will be thrown on faults", "nullPointerCheck", "Enables checking at runtime for null pointer accessses (slows things down a bit, only applicable with fastMem)", "maxInsnPerMethod", "Maximum number of MIPS instructions per java method (128 is optimal with Hotspot)", "pruneCases", "Remove unnecessary case 0xAABCCDD blocks from methods - may break some weird code", "assumeTailCalls", "Assume the JIT optimizes tail calls", "optimizedMemcpy", "Use an optimized java version of memcpy where possible", "debugCompiler", "Output information in the generated code for debugging the compiler - will slow down generated code significantly", "printStats", "Output some useful statistics about the compilation", "runtimeStats", "Keep track of some statistics at runtime in the generated code - will slow down generated code significantly", "supportCall", "Keep a stripped down version of the symbol table in the generated code to support the call() method", "runtimeClass", "Full classname of the Runtime class (default: Runtime) - use this is you put Runtime in a package", "hashClass", "Full classname of a Hashtable class (default: java.util.HashMap) - this must support get() and put()", "unixRuntime", "Use the UnixRuntime (has support for fork, wai, du, pipe, etc)", "pageSize", "The page size (must be a power of two)", "totalPages", "Total number of pages (total mem = pageSize*totalPages, must be a power of two)", "onePage", "One page hack (FIXME: document this better)", "lessConstants", "Use less constants at the cost of speed (FIXME: document this better)", "singleFloat", "Support single precision (32-bit) FP ops only"};
    static /* synthetic */ Class class$org$ibex$nestedvm$Compiler;
    static /* synthetic */ Class class$java$lang$String;

    public void setSource(String string) {
        this.source = string;
    }

    void maxInsnPerMethodInit() throws Exn {
        if ((this.maxInsnPerMethod & this.maxInsnPerMethod - 1) != 0) {
            throw new Exn("maxBytesPerMethod is not a power of two");
        }
        this.maxBytesPerMethod = this.maxInsnPerMethod * 4;
        this.methodMask = ~ (this.maxBytesPerMethod - 1);
        while (this.maxBytesPerMethod >>> this.methodShift != 1) {
            ++this.methodShift;
        }
    }

    void pageSizeInit() throws Exn {
        if ((this.pageSize & this.pageSize - 1) != 0) {
            throw new Exn("pageSize not a multiple of two");
        }
        if ((this.totalPages & this.totalPages - 1) != 0) {
            throw new Exn("totalPages not a multiple of two");
        }
        while (this.pageSize >>> this.pageShift != 1) {
            ++this.pageShift;
        }
    }

    private static void usage() {
        System.err.println("Usage: java Compiler [-outfile output.java] [-o options] [-dumpoptions] <classname> <binary.mips>");
        System.err.println("-o takes mount(8) like options and can be specified multiple times");
        System.err.println("Available options:");
        for (int i = 0; i < options.length; i += 2) {
            System.err.print(options[i] + ": " + Compiler.wrapAndIndent(options[i + 1], 16 - options[i].length(), 18, 62));
        }
        System.exit(1);
    }

    public static void main(String[] arrstring) throws IOException {
        String string = null;
        String string2 = null;
        String string3 = null;
        String string4 = null;
        String string5 = null;
        String string6 = null;
        boolean bl = false;
        int n = 0;
        while (arrstring.length - n > 0) {
            if (arrstring[n].equals("-outfile")) {
                if (++n == arrstring.length) {
                    Compiler.usage();
                }
                string = arrstring[n];
            } else if (arrstring[n].equals("-d")) {
                if (++n == arrstring.length) {
                    Compiler.usage();
                }
                string2 = arrstring[n];
            } else if (arrstring[n].equals("-outformat")) {
                if (++n == arrstring.length) {
                    Compiler.usage();
                }
                string6 = arrstring[n];
            } else if (arrstring[n].equals("-o")) {
                if (++n == arrstring.length) {
                    Compiler.usage();
                }
                if (string3 == null || string3.length() == 0) {
                    string3 = arrstring[n];
                } else if (arrstring[n].length() != 0) {
                    string3 = string3 + "," + arrstring[n];
                }
            } else if (arrstring[n].equals("-dumpoptions")) {
                bl = true;
            } else if (string4 == null) {
                string4 = arrstring[n];
            } else if (string5 == null) {
                string5 = arrstring[n];
            } else {
                Compiler.usage();
            }
            ++n;
        }
        if (string4 == null || string5 == null) {
            Compiler.usage();
        }
        Seekable.File file = new Seekable.File(string5);
        OutputStreamWriter outputStreamWriter = null;
        FileOutputStream fileOutputStream = null;
        Compiler compiler = null;
        if (string6 == null || string6.equals("class")) {
            if (string != null) {
                fileOutputStream = new FileOutputStream(string);
                compiler = new ClassFileCompiler((Seekable)file, string4, (OutputStream)fileOutputStream);
            } else if (string2 != null) {
                File file2 = new File(string2);
                if (!file2.isDirectory()) {
                    System.err.println(string2 + " doesn't exist or is not a directory");
                    System.exit(1);
                }
                compiler = new ClassFileCompiler((Seekable)file, string4, file2);
            } else {
                System.err.println("Refusing to write a classfile to stdout - use -outfile foo.class");
                System.exit(1);
            }
        } else if (string6.equals("javasource") || string6.equals("java")) {
            outputStreamWriter = string == null ? new OutputStreamWriter(System.out) : new FileWriter(string);
            compiler = new JavaSourceCompiler(file, string4, outputStreamWriter);
        } else {
            System.err.println("Unknown output format: " + string6);
            System.exit(1);
        }
        compiler.parseOptions(string3);
        compiler.setSource(string5);
        if (bl) {
            System.err.println("== Options ==");
            for (int i = 0; i < options.length; i += 2) {
                System.err.println(options[i] + ": " + compiler.getOption(options[i]).get());
            }
            System.err.println("== End Options ==");
        }
        try {
            compiler.go();
        }
        catch (Exn var13_15) {
            System.err.println("Compiler Error: " + var13_15.getMessage());
            System.exit(1);
        }
        finally {
            if (outputStreamWriter != null) {
                outputStreamWriter.close();
            }
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        }
    }

    public Compiler(Seekable seekable, String string) throws IOException {
        this.fullClassName = string;
        this.elf = new ELF(seekable);
        if (this.elf.header.type != 2) {
            throw new IOException("Binary is not an executable");
        }
        if (this.elf.header.machine != 8) {
            throw new IOException("Binary is not for the MIPS I Architecture");
        }
        if (this.elf.ident.data != 2) {
            throw new IOException("Binary is not big endian");
        }
    }

    abstract void _go() throws Exn, IOException;

    public void go() throws Exn, IOException {
        if (this.used) {
            throw new RuntimeException("Compiler instances are good for one shot only");
        }
        this.used = true;
        if (this.onePage && this.pageSize <= 4096) {
            this.pageSize = 4194304;
        }
        if (this.nullPointerCheck && !this.fastMem) {
            throw new Exn("fastMem must be enabled for nullPointerCheck to be of any use");
        }
        if (this.onePage && !this.fastMem) {
            throw new Exn("fastMem must be enabled for onePage to be of any use");
        }
        if (this.totalPages == 1 && !this.onePage) {
            throw new Exn("totalPages == 1 and onePage is not set");
        }
        if (this.onePage) {
            this.totalPages = 1;
        }
        this.maxInsnPerMethodInit();
        this.pageSizeInit();
        ELF.Symtab symtab = this.elf.getSymtab();
        if (symtab == null) {
            throw new Exn("Binary has no symtab (did you strip it?)");
        }
        this.userInfo = symtab.getGlobalSymbol("user_info");
        this.gp = symtab.getGlobalSymbol("_gp");
        if (this.gp == null) {
            throw new Exn("no _gp symbol (did you strip the binary?)");
        }
        if (this.pruneCases) {
            this.jumpableAddresses = new Hashtable<K, V>();
            this.jumpableAddresses.put(new Integer(this.elf.header.entry), Boolean.TRUE);
            ELF.SHeader sHeader = this.elf.sectionWithName(".text");
            if (sHeader == null) {
                throw new Exn("No .text segment");
            }
            this.findBranchesInSymtab(symtab, this.jumpableAddresses);
            for (int i = 0; i < this.elf.sheaders.length; ++i) {
                ELF.SHeader sHeader2 = this.elf.sheaders[i];
                String string = sHeader2.name;
                if (sHeader2.addr == 0 || !string.equals(".data") && !string.equals(".sdata") && !string.equals(".rodata") && !string.equals(".ctors") && !string.equals(".dtors")) continue;
                this.findBranchesInData(new DataInputStream(sHeader2.getInputStream()), sHeader2.size, this.jumpableAddresses, sHeader.addr, sHeader.addr + sHeader.size);
            }
            this.findBranchesInText(sHeader.addr, new DataInputStream(sHeader.getInputStream()), sHeader.size, this.jumpableAddresses);
        }
        if (this.unixRuntime && this.runtimeClass.startsWith("org.ibex.nestedvm.")) {
            this.runtimeClass = "org.ibex.nestedvm.UnixRuntime";
        }
        for (int i = 0; i < this.elf.sheaders.length; ++i) {
            String string = this.elf.sheaders[i].name;
            if ((this.elf.sheaders[i].flags & 2) == 0 || string.equals(".text") || string.equals(".data") || string.equals(".sdata") || string.equals(".rodata") || string.equals(".ctors") || string.equals(".dtors") || string.equals(".bss") || string.equals(".sbss")) continue;
            throw new Exn("Unknown section: " + string);
        }
        this._go();
    }

    private void findBranchesInSymtab(ELF.Symtab symtab, Hashtable hashtable) {
        ELF.Symbol[] arrsymbol = symtab.symbols;
        int n = 0;
        for (int i = 0; i < arrsymbol.length; ++i) {
            ELF.Symbol symbol = arrsymbol[i];
            if (symbol.type != 2 || hashtable.put(new Integer(symbol.addr), Boolean.TRUE) != null) continue;
            ++n;
        }
        if (this.printStats) {
            System.err.println("Found " + n + " additional possible branch targets in Symtab");
        }
    }

    private void findBranchesInText(int n, DataInputStream dataInputStream, int n2, Hashtable hashtable) throws IOException {
        int n3 = n2 / 4;
        int n4 = n;
        int n5 = 0;
        int[] arrn = new int[32];
        int[] arrn2 = new int[32];
        int n6 = 0;
        while (n6 < n3) {
            int n7 = dataInputStream.readInt();
            int n8 = n7 >>> 26 & 255;
            int n9 = n7 >>> 21 & 31;
            int n10 = n7 >>> 16 & 31;
            int n11 = n7 << 16 >> 16;
            int n12 = n7 & 65535;
            int n13 = n11;
            int n14 = n7 & 67108863;
            int n15 = n7 & 63;
            block0 : switch (n8) {
                case 0: {
                    switch (n15) {
                        case 9: {
                            if (hashtable.put(new Integer(n4 + 8), Boolean.TRUE) != null) break;
                            ++n5;
                            break block0;
                        }
                        case 12: {
                            if (hashtable.put(new Integer(n4 + 4), Boolean.TRUE) != null) break;
                            ++n5;
                        }
                    }
                    break;
                }
                case 1: {
                    switch (n10) {
                        case 16: 
                        case 17: {
                            if (hashtable.put(new Integer(n4 + 8), Boolean.TRUE) == null) {
                                ++n5;
                            }
                        }
                        case 0: 
                        case 1: {
                            if (hashtable.put(new Integer(n4 + n13 * 4 + 4), Boolean.TRUE) != null) break;
                            ++n5;
                        }
                    }
                    break;
                }
                case 3: {
                    if (hashtable.put(new Integer(n4 + 8), Boolean.TRUE) == null) {
                        ++n5;
                    }
                }
                case 2: {
                    if (hashtable.put(new Integer(n4 & -268435456 | n14 << 2), Boolean.TRUE) != null) break;
                    ++n5;
                    break;
                }
                case 4: 
                case 5: 
                case 6: 
                case 7: {
                    if (hashtable.put(new Integer(n4 + n13 * 4 + 4), Boolean.TRUE) != null) break;
                    ++n5;
                    break;
                }
                case 9: {
                    if (n4 - arrn2[n9] > 128) break;
                    int n16 = (arrn[n9] << 16) + n11;
                    if ((n16 & 3) == 0 && n16 >= n && n16 < n + n2 && hashtable.put(new Integer(n16), Boolean.TRUE) == null) {
                        ++n5;
                    }
                    if (n10 != n9) break;
                    arrn2[n9] = 0;
                    break;
                }
                case 15: {
                    arrn[n10] = n12;
                    arrn2[n10] = n4;
                    break;
                }
                case 17: {
                    switch (n9) {
                        case 8: {
                            if (hashtable.put(new Integer(n4 + n13 * 4 + 4), Boolean.TRUE) != null) break block0;
                            ++n5;
                        }
                    }
                }
            }
            ++n6;
            n4 += 4;
        }
        dataInputStream.close();
        if (this.printStats) {
            System.err.println("Found " + n5 + " additional possible branch targets in Text segment");
        }
    }

    private void findBranchesInData(DataInputStream dataInputStream, int n, Hashtable hashtable, int n2, int n3) throws IOException {
        int n4 = n / 4;
        int n5 = 0;
        for (int i = 0; i < n4; ++i) {
            int n6 = dataInputStream.readInt();
            if ((n6 & 3) != 0 || n6 < n2 || n6 >= n3 || hashtable.put(new Integer(n6), Boolean.TRUE) != null) continue;
            ++n5;
        }
        dataInputStream.close();
        if (n5 > 0 && this.printStats) {
            System.err.println("Found " + n5 + " additional possible branch targets in Data segment");
        }
    }

    static final String toHex(int n) {
        return "0x" + Long.toString((long)n & 0xFFFFFFFFL, 16);
    }

    static final String toHex8(int n) {
        String string = Long.toString((long)n & 0xFFFFFFFFL, 16);
        StringBuffer stringBuffer = new StringBuffer("0x");
        for (int i = 8 - string.length(); i > 0; --i) {
            stringBuffer.append('0');
        }
        stringBuffer.append(string);
        return stringBuffer.toString();
    }

    static final String toOctal3(int n) {
        char[] arrc = new char[3];
        for (int i = 2; i >= 0; --i) {
            arrc[i] = (char)(48 + (n & 7));
            n >>= 3;
        }
        return new String(arrc);
    }

    private Option getOption(String string) {
        string = string.toLowerCase();
        try {
            for (int i = 0; i < options.length; i += 2) {
                if (!options[i].toLowerCase().equals(string)) continue;
                return new Option(options[i]);
            }
            return null;
        }
        catch (NoSuchFieldException var2_3) {
            return null;
        }
    }

    public void parseOptions(String string) {
        if (string == null || string.length() == 0) {
            return;
        }
        StringTokenizer stringTokenizer = new StringTokenizer(string, ",");
        while (stringTokenizer.hasMoreElements()) {
            String string2;
            String string3;
            String string4 = stringTokenizer.nextToken();
            if (string4.indexOf("=") != -1) {
                string2 = string4.substring(0, string4.indexOf("="));
                string3 = string4.substring(string4.indexOf("=") + 1);
            } else if (string4.startsWith("no")) {
                string2 = string4.substring(2);
                string3 = "false";
            } else {
                string2 = string4;
                string3 = "true";
            }
            Option option = this.getOption(string2);
            if (option == null) {
                System.err.println("WARNING: No such option: " + string2);
                continue;
            }
            if (option.getType() == (class$java$lang$String == null ? Compiler.class$("java.lang.String") : class$java$lang$String)) {
                option.set(string3);
                continue;
            }
            if (option.getType() == Integer.TYPE) {
                try {
                    option.set(Compiler.parseInt(string3));
                }
                catch (NumberFormatException var7_7) {
                    System.err.println("WARNING: " + string3 + " is not an integer");
                }
                continue;
            }
            if (option.getType() == Boolean.TYPE) {
                option.set(new Boolean(string3.toLowerCase().equals("true") || string3.toLowerCase().equals("yes")));
                continue;
            }
            throw new Error("Unknown type: " + option.getType());
        }
    }

    private static Integer parseInt(String string) {
        int n = 1;
        if (!(string = string.toLowerCase()).startsWith("0x") && string.endsWith("m")) {
            string = string.substring(0, string.length() - 1);
            n = 1048576;
        } else if (!string.startsWith("0x") && string.endsWith("k")) {
            string = string.substring(0, string.length() - 1);
            n = 1024;
        }
        int n2 = string.length() > 2 && string.startsWith("0x") ? Integer.parseInt(string.substring(2), 16) : Integer.parseInt(string);
        return new Integer(n2 * n);
    }

    private static String wrapAndIndent(String string, int n, int n2, int n3) {
        int n4;
        StringTokenizer stringTokenizer = new StringTokenizer(string, " ");
        StringBuffer stringBuffer = new StringBuffer();
        for (n4 = 0; n4 < n; ++n4) {
            stringBuffer.append(' ');
        }
        n4 = 0;
        while (stringTokenizer.hasMoreTokens()) {
            String string2 = stringTokenizer.nextToken();
            if (string2.length() + n4 + 1 > n3 && n4 > 0) {
                stringBuffer.append('\n');
                for (int i = 0; i < n2; ++i) {
                    stringBuffer.append(' ');
                }
                n4 = 0;
            } else if (n4 > 0) {
                stringBuffer.append(' ');
                ++n4;
            }
            stringBuffer.append(string2);
            n4 += string2.length();
        }
        stringBuffer.append('\n');
        return stringBuffer.toString();
    }

    static String dateTime() {
        try {
            return new Date().toString();
        }
        catch (RuntimeException var0) {
            return "<unknown>";
        }
    }

    static /* synthetic */ Class class$(String string) {
        try {
            return Class.forName(string);
        }
        catch (ClassNotFoundException var1_1) {
            throw new NoClassDefFoundError(var1_1.getMessage());
        }
    }

    private class Option {
        private Field field;

        public Option(String string) throws NoSuchFieldException {
            Field field;
            if (string == null) {
                field = null;
            } else {
                Class class_ = Compiler.class$org$ibex$nestedvm$Compiler == null ? (Compiler.class$org$ibex$nestedvm$Compiler = Compiler.class$("org.ibex.nestedvm.Compiler")) : Compiler.class$org$ibex$nestedvm$Compiler;
                field = class_.getDeclaredField(string);
            }
            this.field = field;
        }

        public void set(Object object) {
            if (this.field == null) {
                return;
            }
            try {
                this.field.set(Compiler.this, object);
            }
            catch (IllegalAccessException var2_2) {
                System.err.println(var2_2);
            }
        }

        public Object get() {
            if (this.field == null) {
                return null;
            }
            try {
                return this.field.get(Compiler.this);
            }
            catch (IllegalAccessException var1_1) {
                System.err.println(var1_1);
                return null;
            }
        }

        public Class getType() {
            return this.field == null ? null : this.field.getType();
        }
    }

    static class Exn
    extends Exception {
        public Exn(String string) {
            super(string);
        }
    }

}

