/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  org.ibex.classgen.CGConst
 *  org.ibex.classgen.ClassFile
 *  org.ibex.classgen.ClassFile$Exn
 *  org.ibex.classgen.FieldGen
 *  org.ibex.classgen.MethodGen
 *  org.ibex.classgen.MethodGen$Pair
 *  org.ibex.classgen.MethodGen$PhantomTarget
 *  org.ibex.classgen.MethodGen$Switch
 *  org.ibex.classgen.MethodGen$Switch$Lookup
 *  org.ibex.classgen.MethodGen$Switch$Table
 *  org.ibex.classgen.Type
 *  org.ibex.classgen.Type$Array
 *  org.ibex.classgen.Type$Class
 *  org.ibex.classgen.Type$Class$Field
 *  org.ibex.classgen.Type$Class$Method
 */
package org.ibex.nestedvm;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Hashtable;
import org.ibex.classgen.CGConst;
import org.ibex.classgen.ClassFile;
import org.ibex.classgen.FieldGen;
import org.ibex.classgen.MethodGen;
import org.ibex.classgen.Type;
import org.ibex.nestedvm.Compiler;
import org.ibex.nestedvm.util.ELF;
import org.ibex.nestedvm.util.Seekable;

public class ClassFileCompiler
extends Compiler
implements CGConst {
    private static final boolean OPTIMIZE_CP = true;
    private OutputStream os;
    private File outDir;
    private PrintStream warn = System.err;
    private final Type.Class me;
    private ClassFile cg;
    private MethodGen clinit;
    private MethodGen init;
    private static int initDataCount;
    private int startOfMethod = 0;
    private int endOfMethod = 0;
    private MethodGen.PhantomTarget returnTarget;
    private MethodGen.PhantomTarget defaultTarget;
    private MethodGen.PhantomTarget[] insnTargets;
    private MethodGen mg;
    private static final int UNREACHABLE = 1;
    private static final int SKIP_NEXT = 2;
    private boolean textDone;
    private static final Float POINT_5_F;
    private static final Double POINT_5_D;
    private static final Long FFFFFFFF;
    private static final int R = 0;
    private static final int F = 32;
    private static final int HI = 64;
    private static final int LO = 65;
    private static final int FCSR = 66;
    private static final int REG_COUNT = 67;
    private static final String[] regField;
    private static final int MAX_LOCALS = 4;
    private static final int LOAD_LENGTH = 3;
    private int[] regLocalMapping = new int[67];
    private boolean[] regLocalWritten = new boolean[67];
    private int nextAvailLocal;
    private int loadsStart;
    private int preSetRegStackPos;
    private int[] preSetRegStack = new int[8];
    private int memWriteStage;
    private boolean didPreMemRead;
    private boolean preMemReadDoPreWrite;

    public ClassFileCompiler(String string, String string2, OutputStream outputStream) throws IOException {
        this((Seekable)new Seekable.File(string), string2, outputStream);
    }

    public ClassFileCompiler(Seekable seekable, String string, OutputStream outputStream) throws IOException {
        this(seekable, string);
        if (outputStream == null) {
            throw new NullPointerException();
        }
        this.os = outputStream;
    }

    public ClassFileCompiler(Seekable seekable, String string, File file) throws IOException {
        this(seekable, string);
        if (file == null) {
            throw new NullPointerException();
        }
        this.outDir = file;
    }

    private ClassFileCompiler(Seekable seekable, String string) throws IOException {
        super(seekable, string);
        this.me = Type.Class.instance((String)this.fullClassName);
    }

    public void setWarnWriter(PrintStream printStream) {
        this.warn = printStream;
    }

    protected void _go() throws Compiler.Exn, IOException {
        try {
            this.__go();
        }
        catch (ClassFile.Exn var1_1) {
            var1_1.printStackTrace(this.warn);
            throw new Compiler.Exn("Class generation exception: " + var1_1.toString());
        }
    }

    private void __go() throws Compiler.Exn, IOException {
        int n;
        ELF.Symbol[] arrsymbol;
        String string;
        int n2;
        MethodGen methodGen;
        if (!this.pruneCases) {
            throw new Compiler.Exn("-o prunecases MUST be enabled for ClassFileCompiler");
        }
        Type.Class class_ = Type.Class.instance((String)this.runtimeClass);
        this.cg = new ClassFile(this.me, class_, 49);
        if (this.source != null) {
            this.cg.setSourceFile(this.source);
        }
        this.cg.addField("pc", Type.INT, 2);
        this.cg.addField("hi", Type.INT, 2);
        this.cg.addField("lo", Type.INT, 2);
        this.cg.addField("fcsr", Type.INT, 2);
        for (n = 1; n < 32; ++n) {
            this.cg.addField("r" + n, Type.INT, 2);
        }
        for (n = 0; n < 32; ++n) {
            this.cg.addField("f" + n, this.singleFloat ? Type.FLOAT : Type.INT, 2);
        }
        this.clinit = this.cg.addMethod("<clinit>", Type.VOID, Type.NO_ARGS, 10);
        this.init = this.cg.addMethod("<init>", Type.VOID, Type.NO_ARGS, 1);
        this.init.add(42);
        this.init.add(18, this.pageSize);
        this.init.add(18, this.totalPages);
        this.init.add(-73, (Object)this.me.method("<init>", Type.VOID, new Type[]{Type.INT, Type.INT}));
        this.init.add(-79);
        this.init = this.cg.addMethod("<init>", Type.VOID, new Type[]{Type.BOOLEAN}, 1);
        this.init.add(42);
        this.init.add(18, this.pageSize);
        this.init.add(18, this.totalPages);
        this.init.add(27);
        this.init.add(-73, (Object)this.me.method("<init>", Type.VOID, new Type[]{Type.INT, Type.INT, Type.BOOLEAN}));
        this.init.add(-79);
        this.init = this.cg.addMethod("<init>", Type.VOID, new Type[]{Type.INT, Type.INT}, 1);
        this.init.add(42);
        this.init.add(27);
        this.init.add(28);
        this.init.add(3);
        this.init.add(-73, (Object)this.me.method("<init>", Type.VOID, new Type[]{Type.INT, Type.INT, Type.BOOLEAN}));
        this.init.add(-79);
        this.init = this.cg.addMethod("<init>", Type.VOID, new Type[]{Type.INT, Type.INT, Type.BOOLEAN}, 1);
        this.init.add(42);
        this.init.add(27);
        this.init.add(28);
        this.init.add(29);
        this.init.add(-73, (Object)class_.method("<init>", Type.VOID, new Type[]{Type.INT, Type.INT, Type.BOOLEAN}));
        if (this.onePage) {
            this.cg.addField("page", (Type)Type.INT.makeArray(), 18);
            this.init.add(42);
            this.init.add(89);
            this.init.add(-76, (Object)this.me.field("readPages", (Type)Type.INT.makeArray(2)));
            this.init.add(18, 0);
            this.init.add(50);
            this.init.add(-75, (Object)this.me.field("page", (Type)Type.INT.makeArray()));
        }
        if (this.supportCall) {
            this.cg.addField("symbols", (Type)Type.Class.instance((String)this.hashClass), 26);
        }
        n = 0;
        for (int i = 0; i < this.elf.sheaders.length; ++i) {
            arrsymbol = this.elf.sheaders[i];
            string = arrsymbol.name;
            if (arrsymbol.addr == 0) continue;
            n = Math.max(n, arrsymbol.addr + arrsymbol.size);
            if (string.equals(".text")) {
                this.emitText(arrsymbol.addr, new DataInputStream(arrsymbol.getInputStream()), arrsymbol.size);
                continue;
            }
            if (string.equals(".data") || string.equals(".sdata") || string.equals(".rodata") || string.equals(".ctors") || string.equals(".dtors")) {
                this.emitData(arrsymbol.addr, new DataInputStream(arrsymbol.getInputStream()), arrsymbol.size, string.equals(".rodata"));
                continue;
            }
            if (string.equals(".bss") || string.equals(".sbss")) {
                this.emitBSS(arrsymbol.addr, arrsymbol.size);
                continue;
            }
            throw new Compiler.Exn("Unknown segment: " + string);
        }
        this.init.add(-79);
        if (this.supportCall) {
            Type.Class class_2 = Type.Class.instance((String)this.hashClass);
            this.clinit.add(-69, (Object)class_2);
            this.clinit.add(89);
            this.clinit.add(89);
            this.clinit.add(-73, (Object)class_2.method("<init>", Type.VOID, Type.NO_ARGS));
            this.clinit.add(-77, (Object)this.me.field("symbols", (Type)class_2));
            arrsymbol = this.elf.getSymtab().symbols;
            for (int j = 0; j < arrsymbol.length; ++j) {
                ELF.Symbol symbol = arrsymbol[j];
                if (symbol.type != 2 || symbol.binding != 1 || !symbol.name.equals("_call_helper") && symbol.name.startsWith("_")) continue;
                this.clinit.add(89);
                this.clinit.add(18, (Object)symbol.name);
                this.clinit.add(-69, (Object)Type.INTEGER_OBJECT);
                this.clinit.add(89);
                this.clinit.add(18, symbol.addr);
                this.clinit.add(-73, (Object)Type.INTEGER_OBJECT.method("<init>", Type.VOID, new Type[]{Type.INT}));
                this.clinit.add(-74, (Object)class_2.method("put", (Type)Type.OBJECT, new Type[]{Type.OBJECT, Type.OBJECT}));
                this.clinit.add(87);
            }
            this.clinit.add(87);
        }
        this.clinit.add(-79);
        ELF.SHeader sHeader = this.elf.sectionWithName(".text");
        arrsymbol = this.cg.addMethod("trampoline", Type.VOID, Type.NO_ARGS, 2);
        string = (String)arrsymbol.size();
        arrsymbol.add(42);
        arrsymbol.add(-76, (Object)this.me.field("state", Type.INT));
        arrsymbol.add(-103, arrsymbol.size() + 2);
        arrsymbol.add(-79);
        arrsymbol.add(42);
        arrsymbol.add(42);
        arrsymbol.add(-76, (Object)this.me.field("pc", Type.INT));
        arrsymbol.add(18, this.methodShift);
        arrsymbol.add(124);
        int n3 = sHeader.addr >>> this.methodShift;
        int n4 = sHeader.addr + sHeader.size + this.maxBytesPerMethod - 1 >>> this.methodShift;
        MethodGen.Switch.Table table = new MethodGen.Switch.Table(n3, n4 - 1);
        arrsymbol.add(-86, (Object)table);
        for (int j = n3; j < n4; ++j) {
            table.setTargetForVal(j, arrsymbol.size());
            arrsymbol.add(-73, (Object)this.me.method("run_" + ClassFileCompiler.toHex(j << this.methodShift), Type.VOID, Type.NO_ARGS));
            arrsymbol.add(-89, (int)string);
        }
        table.setDefaultTarget(arrsymbol.size());
        arrsymbol.add(87);
        arrsymbol.add(-69, (Object)Type.Class.instance((String)"org.ibex.nestedvm.Runtime$ExecutionException"));
        arrsymbol.add(89);
        arrsymbol.add(-69, (Object)Type.STRINGBUFFER);
        arrsymbol.add(89);
        arrsymbol.add(18, (Object)"Jumped to invalid address in trampoline (r2: ");
        arrsymbol.add(-73, (Object)Type.STRINGBUFFER.method("<init>", Type.VOID, new Type[]{Type.STRING}));
        arrsymbol.add(42);
        arrsymbol.add(-76, (Object)this.me.field("r2", Type.INT));
        arrsymbol.add(-74, (Object)Type.STRINGBUFFER.method("append", (Type)Type.STRINGBUFFER, new Type[]{Type.INT}));
        arrsymbol.add(18, (Object)" pc: ");
        arrsymbol.add(-74, (Object)Type.STRINGBUFFER.method("append", (Type)Type.STRINGBUFFER, new Type[]{Type.STRING}));
        arrsymbol.add(42);
        arrsymbol.add(-76, (Object)this.me.field("pc", Type.INT));
        arrsymbol.add(-74, (Object)Type.STRINGBUFFER.method("append", (Type)Type.STRINGBUFFER, new Type[]{Type.INT}));
        arrsymbol.add(18, (Object)")");
        arrsymbol.add(-74, (Object)Type.STRINGBUFFER.method("append", (Type)Type.STRINGBUFFER, new Type[]{Type.STRING}));
        arrsymbol.add(-74, (Object)Type.STRINGBUFFER.method("toString", (Type)Type.STRING, Type.NO_ARGS));
        arrsymbol.add(-73, (Object)Type.Class.instance((String)"org.ibex.nestedvm.Runtime$ExecutionException").method("<init>", Type.VOID, new Type[]{Type.STRING}));
        arrsymbol.add(-65);
        this.addConstReturnMethod("gp", this.gp.addr);
        this.addConstReturnMethod("entryPoint", this.elf.header.entry);
        this.addConstReturnMethod("heapStart", n);
        if (this.userInfo != null) {
            this.addConstReturnMethod("userInfoBase", this.userInfo.addr);
            this.addConstReturnMethod("userInfoSize", this.userInfo.size);
        }
        if (this.supportCall) {
            Type.Class class_3 = Type.Class.instance((String)this.hashClass);
            methodGen = this.cg.addMethod("lookupSymbol", Type.INT, new Type[]{Type.STRING}, 4);
            methodGen.add(-78, (Object)this.me.field("symbols", (Type)class_3));
            methodGen.add(43);
            methodGen.add(-74, (Object)class_3.method("get", (Type)Type.OBJECT, new Type[]{Type.OBJECT}));
            methodGen.add(89);
            int n5 = methodGen.add(-58);
            methodGen.add(-64, (Object)Type.INTEGER_OBJECT);
            methodGen.add(-74, (Object)Type.INTEGER_OBJECT.method("intValue", Type.INT, Type.NO_ARGS));
            methodGen.add(-84);
            methodGen.setArg(n5, methodGen.size());
            methodGen.add(87);
            methodGen.add(2);
            methodGen.add(-84);
        }
        Type.Class class_4 = Type.Class.instance((String)"org.ibex.nestedvm.Runtime$CPUState");
        methodGen = this.cg.addMethod("setCPUState", Type.VOID, new Type[]{class_4}, 4);
        MethodGen methodGen2 = this.cg.addMethod("getCPUState", Type.VOID, new Type[]{class_4}, 4);
        methodGen.add(43);
        methodGen2.add(43);
        methodGen.add(-76, (Object)class_4.field("r", (Type)Type.INT.makeArray()));
        methodGen2.add(-76, (Object)class_4.field("r", (Type)Type.INT.makeArray()));
        methodGen.add(77);
        methodGen2.add(77);
        for (n2 = 1; n2 < 32; ++n2) {
            methodGen.add(42);
            methodGen.add(44);
            methodGen.add(18, n2);
            methodGen.add(46);
            methodGen.add(-75, (Object)this.me.field("r" + n2, Type.INT));
            methodGen2.add(44);
            methodGen2.add(18, n2);
            methodGen2.add(42);
            methodGen2.add(-76, (Object)this.me.field("r" + n2, Type.INT));
            methodGen2.add(79);
        }
        methodGen.add(43);
        methodGen2.add(43);
        methodGen.add(-76, (Object)class_4.field("f", (Type)Type.INT.makeArray()));
        methodGen2.add(-76, (Object)class_4.field("f", (Type)Type.INT.makeArray()));
        methodGen.add(77);
        methodGen2.add(77);
        for (n2 = 0; n2 < 32; ++n2) {
            methodGen.add(42);
            methodGen.add(44);
            methodGen.add(18, n2);
            methodGen.add(46);
            if (this.singleFloat) {
                methodGen.add(-72, (Object)Type.FLOAT_OBJECT.method("intBitsToFloat", Type.FLOAT, new Type[]{Type.INT}));
            }
            methodGen.add(-75, (Object)this.me.field("f" + n2, this.singleFloat ? Type.FLOAT : Type.INT));
            methodGen2.add(44);
            methodGen2.add(18, n2);
            methodGen2.add(42);
            methodGen2.add(-76, (Object)this.me.field("f" + n2, this.singleFloat ? Type.FLOAT : Type.INT));
            if (this.singleFloat) {
                methodGen2.add(-72, (Object)Type.FLOAT_OBJECT.method("floatToIntBits", Type.INT, new Type[]{Type.FLOAT}));
            }
            methodGen2.add(79);
        }
        String[] arrstring = new String[]{"hi", "lo", "fcsr", "pc"};
        for (int k = 0; k < arrstring.length; ++k) {
            methodGen.add(42);
            methodGen.add(43);
            methodGen.add(-76, (Object)class_4.field(arrstring[k], Type.INT));
            methodGen.add(-75, (Object)this.me.field(arrstring[k], Type.INT));
            methodGen2.add(43);
            methodGen2.add(42);
            methodGen2.add(-76, (Object)this.me.field(arrstring[k], Type.INT));
            methodGen2.add(-75, (Object)class_4.field(arrstring[k], Type.INT));
        }
        methodGen.add(-79);
        methodGen2.add(-79);
        MethodGen methodGen3 = this.cg.addMethod("_execute", Type.VOID, Type.NO_ARGS, 4);
        int n6 = methodGen3.size();
        methodGen3.add(42);
        methodGen3.add(-73, (Object)this.me.method("trampoline", Type.VOID, Type.NO_ARGS));
        int n7 = methodGen3.size();
        methodGen3.add(-79);
        int n8 = methodGen3.size();
        methodGen3.add(76);
        methodGen3.add(-69, (Object)Type.Class.instance((String)"org.ibex.nestedvm.Runtime$FaultException"));
        methodGen3.add(89);
        methodGen3.add(43);
        methodGen3.add(-73, (Object)Type.Class.instance((String)"org.ibex.nestedvm.Runtime$FaultException").method("<init>", Type.VOID, new Type[]{Type.Class.instance((String)"java.lang.RuntimeException")}));
        methodGen3.add(-65);
        methodGen3.addExceptionHandler(n6, n7, n8, Type.Class.instance((String)"java.lang.RuntimeException"));
        methodGen3.addThrow(Type.Class.instance((String)"org.ibex.nestedvm.Runtime$ExecutionException"));
        MethodGen methodGen4 = this.cg.addMethod("main", Type.VOID, new Type[]{Type.STRING.makeArray()}, 9);
        methodGen4.add(-69, (Object)this.me);
        methodGen4.add(89);
        methodGen4.add(-73, (Object)this.me.method("<init>", Type.VOID, Type.NO_ARGS));
        methodGen4.add(18, (Object)this.fullClassName);
        methodGen4.add(42);
        if (this.unixRuntime) {
            Type.Class class_5 = Type.Class.instance((String)"org.ibex.nestedvm.UnixRuntime");
            methodGen4.add(-72, (Object)class_5.method("runAndExec", Type.INT, new Type[]{class_5, Type.STRING, Type.STRING.makeArray()}));
        } else {
            methodGen4.add(-74, (Object)this.me.method("run", Type.INT, new Type[]{Type.STRING, Type.STRING.makeArray()}));
        }
        methodGen4.add(-72, (Object)Type.Class.instance((String)"java.lang.System").method("exit", Type.VOID, new Type[]{Type.INT}));
        methodGen4.add(-79);
        if (this.outDir != null) {
            if (!this.outDir.isDirectory()) {
                throw new IOException("" + this.outDir + " isn't a directory");
            }
            this.cg.dump(this.outDir);
        } else {
            this.cg.dump(this.os);
        }
    }

    private void addConstReturnMethod(String string, int n) {
        MethodGen methodGen = this.cg.addMethod(string, Type.INT, Type.NO_ARGS, 4);
        methodGen.add(18, n);
        methodGen.add(-84);
    }

    private void emitData(int n, DataInputStream dataInputStream, int n2, boolean bl) throws Compiler.Exn, IOException {
        if ((n & 3) != 0 || (n2 & 3) != 0) {
            throw new Compiler.Exn("Data section on weird boundaries");
        }
        int n3 = n + n2;
        while (n < n3) {
            int n4 = Math.min(n2, 28000);
            StringBuffer stringBuffer = new StringBuffer();
            for (int i = 0; i < n4; i += 7) {
                int n5;
                long l = 0;
                for (n5 = 0; n5 < 7; ++n5) {
                    l <<= 8;
                    byte by = i + n5 < n2 ? dataInputStream.readByte() : 1;
                    l |= (long)by & 255;
                }
                for (n5 = 0; n5 < 8; ++n5) {
                    stringBuffer.append((char)(l >>> 7 * (7 - n5) & 127));
                }
            }
            String string = "_data" + ++initDataCount;
            this.cg.addField(string, (Type)Type.INT.makeArray(), 26);
            this.clinit.add(18, (Object)stringBuffer.toString());
            this.clinit.add(18, n4 / 4);
            this.clinit.add(-72, (Object)Type.Class.instance((String)"org.ibex.nestedvm.Runtime").method("decodeData", (Type)Type.INT.makeArray(), new Type[]{Type.STRING, Type.INT}));
            this.clinit.add(-77, (Object)this.me.field(string, (Type)Type.INT.makeArray()));
            this.init.add(42);
            this.init.add(-78, (Object)this.me.field(string, (Type)Type.INT.makeArray()));
            this.init.add(18, n);
            this.init.add(18, bl ? 1 : 0);
            this.init.add(-74, (Object)this.me.method("initPages", Type.VOID, new Type[]{Type.INT.makeArray(), Type.INT, Type.BOOLEAN}));
            n += n4;
            n2 -= n4;
        }
        dataInputStream.close();
    }

    private void emitBSS(int n, int n2) throws Compiler.Exn {
        if ((n & 3) != 0) {
            throw new Compiler.Exn("BSS section on weird boundaries");
        }
        n2 = n2 + 3 & -4;
        int n3 = n2 / 4;
        this.init.add(42);
        this.init.add(18, n);
        this.init.add(18, n3);
        this.init.add(-74, (Object)this.me.method("clearPages", Type.VOID, new Type[]{Type.INT, Type.INT}));
    }

    private boolean jumpable(int n) {
        return this.jumpableAddresses.get(new Integer(n)) != null;
    }

    /*
     * Unable to fully structure code
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     * Lifted jumps to return sites
     */
    private void emitText(int var1_1, DataInputStream var2_2, int var3_3) throws Compiler.Exn, IOException {
        if (this.textDone) {
            throw new Compiler.Exn("Multiple text segments");
        }
        this.textDone = true;
        if ((var1_1 & 3) != 0) throw new Compiler.Exn("Section on weird boundaries");
        if ((var3_3 & 3) != 0) {
            throw new Compiler.Exn("Section on weird boundaries");
        }
        var4_4 = var3_3 / 4;
        var6_5 = -1;
        var7_6 = true;
        var8_7 = false;
        var9_8 = 0;
        do {
            if (var9_8 >= var4_4) {
                this.endMethod(0, var8_7);
                var2_2.close();
                return;
            }
            var5_9 = var7_6 != false ? var2_2.readInt() : var6_5;
            v0 = var6_5 = var9_8 == var4_4 - 1 ? -1 : var2_2.readInt();
            if (var1_1 >= this.endOfMethod) {
                this.endMethod(var1_1, var8_7);
                this.startMethod(var1_1);
            }
            if (this.insnTargets[var9_8 % this.maxInsnPerMethod] == null) ** GOTO lbl26
            this.insnTargets[var9_8 % this.maxInsnPerMethod].setTarget(this.mg.size());
            var8_7 = false;
            ** GOTO lbl-1000
lbl26: // 1 sources:
            if (!var8_7) lbl-1000: // 2 sources:
            {
                try {
                    var10_10 = this.emitInstruction(var1_1, var5_9, var6_5);
                    var8_7 = (var10_10 & 1) != 0;
                    var7_6 = (var10_10 & 2) != 0;
                }
                catch (Compiler.Exn var10_11) {
                    var10_11.printStackTrace(this.warn);
                    this.warn.println("Exception at " + ClassFileCompiler.toHex(var1_1));
                    throw var10_11;
                }
                catch (RuntimeException var10_12) {
                    this.warn.println("Exception at " + ClassFileCompiler.toHex(var1_1));
                    throw var10_12;
                }
                if (var7_6) {
                    var1_1 += 4;
                    ++var9_8;
                }
            }
            ++var9_8;
            var1_1 += 4;
        } while (true);
    }

    private void startMethod(int n) {
        this.startOfMethod = n & this.methodMask;
        this.endOfMethod = this.startOfMethod + this.maxBytesPerMethod;
        this.mg = this.cg.addMethod("run_" + ClassFileCompiler.toHex(this.startOfMethod), Type.VOID, Type.NO_ARGS, 18);
        if (this.onePage) {
            this.mg.add(42);
            this.mg.add(-76, (Object)this.me.field("page", (Type)Type.INT.makeArray()));
            this.mg.add(77);
        } else {
            this.mg.add(42);
            this.mg.add(-76, (Object)this.me.field("readPages", (Type)Type.INT.makeArray(2)));
            this.mg.add(77);
            this.mg.add(42);
            this.mg.add(-76, (Object)this.me.field("writePages", (Type)Type.INT.makeArray(2)));
            this.mg.add(78);
        }
        this.returnTarget = new MethodGen.PhantomTarget();
        this.insnTargets = new MethodGen.PhantomTarget[this.maxBytesPerMethod / 4];
        int[] arrn = new int[this.maxBytesPerMethod / 4];
        Object[] arrobject = new Object[this.maxBytesPerMethod / 4];
        int n2 = 0;
        for (int i = n; i < this.endOfMethod; i += 4) {
            if (!this.jumpable(i)) continue;
            MethodGen.PhantomTarget phantomTarget = new MethodGen.PhantomTarget();
            this.insnTargets[(i - this.startOfMethod) / 4] = phantomTarget;
            arrobject[n2] = phantomTarget;
            arrn[n2] = i;
            ++n2;
        }
        MethodGen.Switch.Lookup lookup = new MethodGen.Switch.Lookup(n2);
        System.arraycopy(arrn, 0, lookup.vals, 0, n2);
        System.arraycopy(arrobject, 0, lookup.targets, 0, n2);
        this.defaultTarget = new MethodGen.PhantomTarget();
        lookup.setDefaultTarget((Object)this.defaultTarget);
        this.fixupRegsStart();
        this.mg.add(42);
        this.mg.add(-76, (Object)this.me.field("pc", Type.INT));
        this.mg.add(-85, (Object)lookup);
    }

    private void endMethod(int n, boolean bl) {
        if (this.startOfMethod == 0) {
            return;
        }
        if (!bl) {
            this.preSetPC();
            this.mg.add(18, n);
            this.setPC();
            this.jumpableAddresses.put(new Integer(n), Boolean.TRUE);
        }
        this.returnTarget.setTarget(this.mg.size());
        this.fixupRegsEnd();
        this.mg.add(-79);
        this.defaultTarget.setTarget(this.mg.size());
        if (this.debugCompiler) {
            this.mg.add(-69, (Object)Type.Class.instance((String)"org.ibex.nestedvm.Runtime$ExecutionException"));
            this.mg.add(89);
            this.mg.add(-69, (Object)Type.STRINGBUFFER);
            this.mg.add(89);
            this.mg.add(18, (Object)"Jumped to invalid address: ");
            this.mg.add(-73, (Object)Type.STRINGBUFFER.method("<init>", Type.VOID, new Type[]{Type.STRING}));
            this.mg.add(42);
            this.mg.add(-76, (Object)this.me.field("pc", Type.INT));
            this.mg.add(-74, (Object)Type.STRINGBUFFER.method("append", (Type)Type.STRINGBUFFER, new Type[]{Type.INT}));
            this.mg.add(-74, (Object)Type.STRINGBUFFER.method("toString", (Type)Type.STRING, Type.NO_ARGS));
            this.mg.add(-73, (Object)Type.Class.instance((String)"org.ibex.nestedvm.Runtime$ExecutionException").method("<init>", Type.VOID, new Type[]{Type.STRING}));
            this.mg.add(-65);
        } else {
            this.mg.add(-69, (Object)Type.Class.instance((String)"org.ibex.nestedvm.Runtime$ExecutionException"));
            this.mg.add(89);
            this.mg.add(18, (Object)"Jumped to invalid address");
            this.mg.add(-73, (Object)Type.Class.instance((String)"org.ibex.nestedvm.Runtime$ExecutionException").method("<init>", Type.VOID, new Type[]{Type.STRING}));
            this.mg.add(-65);
        }
        this.startOfMethod = 0;
        this.endOfMethod = 0;
    }

    private void leaveMethod() {
        this.mg.add(-89, (Object)this.returnTarget);
    }

    private void link(int n) {
        this.preSetReg(31);
        if (this.lessConstants) {
            int n2 = n + 8 + 32768 & -65536;
            int n3 = n + 8 - n2;
            if (n3 < -32768 || n3 > 32767) {
                throw new Error("should never happen " + n3);
            }
            this.mg.add(18, n2);
            this.mg.add(18, n3);
            this.mg.add(96);
        } else {
            this.mg.add(18, n + 8);
        }
        this.setReg();
    }

    private void branch(int n, int n2) {
        if ((n & this.methodMask) == (n2 & this.methodMask)) {
            this.mg.add(-89, (Object)this.insnTargets[(n2 - this.startOfMethod) / 4]);
        } else {
            this.preSetPC();
            this.mg.add(18, n2);
            this.setPC();
            this.leaveMethod();
        }
    }

    private int doIfInstruction(byte by, int n, int n2, int n3) throws Compiler.Exn {
        int n4;
        this.emitInstruction(-1, n3, -1);
        if ((n2 & this.methodMask) == (n & this.methodMask)) {
            this.mg.add(by, (Object)this.insnTargets[(n2 - this.startOfMethod) / 4]);
        } else {
            n4 = this.mg.add(MethodGen.negate((byte)by));
            this.branch(n, n2);
            this.mg.setArg(n4, this.mg.size());
        }
        if (!this.jumpable(n + 4)) {
            return 2;
        }
        if (n + 4 == this.endOfMethod) {
            this.jumpableAddresses.put(new Integer(n + 8), Boolean.TRUE);
            this.branch(n, n + 8);
            return 1;
        }
        n4 = this.mg.add(-89);
        this.insnTargets[(n + 4 - this.startOfMethod) / 4].setTarget(this.mg.size());
        this.emitInstruction(-1, n3, 1);
        this.mg.setArg(n4, this.mg.size());
        return 2;
    }

    private int emitInstruction(int n, int n2, int n3) throws Compiler.Exn {
        int n4;
        MethodGen methodGen = this.mg;
        if (n2 == -1) {
            throw new Compiler.Exn("insn is -1");
        }
        int n5 = 0;
        int n6 = n2 >>> 26 & 255;
        int n7 = n2 >>> 21 & 31;
        int n8 = n2 >>> 16 & 31;
        int n9 = n2 >>> 16 & 31;
        int n10 = n2 >>> 11 & 31;
        int n11 = n2 >>> 11 & 31;
        int n12 = n2 >>> 6 & 31;
        int n13 = n2 >>> 6 & 31;
        int n14 = n2 & 63;
        int n15 = n2 >>> 6 & 1048575;
        int n16 = n2 & 67108863;
        int n17 = n2 & 65535;
        int n18 = n4 = n2 << 16 >> 16;
        block0 : switch (n6) {
            case 0: {
                switch (n14) {
                    case 0: {
                        if (n2 == 0) break block0;
                        this.preSetReg(0 + n10);
                        this.pushRegWZ(0 + n8);
                        methodGen.add(18, n12);
                        methodGen.add(120);
                        this.setReg();
                        break block0;
                    }
                    case 2: {
                        this.preSetReg(0 + n10);
                        this.pushRegWZ(0 + n8);
                        methodGen.add(18, n12);
                        methodGen.add(124);
                        this.setReg();
                        break block0;
                    }
                    case 3: {
                        this.preSetReg(0 + n10);
                        this.pushRegWZ(0 + n8);
                        methodGen.add(18, n12);
                        methodGen.add(122);
                        this.setReg();
                        break block0;
                    }
                    case 4: {
                        this.preSetReg(0 + n10);
                        this.pushRegWZ(0 + n8);
                        this.pushRegWZ(0 + n7);
                        methodGen.add(120);
                        this.setReg();
                        break block0;
                    }
                    case 6: {
                        this.preSetReg(0 + n10);
                        this.pushRegWZ(0 + n8);
                        this.pushRegWZ(0 + n7);
                        methodGen.add(124);
                        this.setReg();
                        break block0;
                    }
                    case 7: {
                        this.preSetReg(0 + n10);
                        this.pushRegWZ(0 + n8);
                        this.pushRegWZ(0 + n7);
                        methodGen.add(122);
                        this.setReg();
                        break block0;
                    }
                    case 8: {
                        if (n == -1) {
                            throw new Compiler.Exn("pc modifying insn in delay slot");
                        }
                        this.emitInstruction(-1, n3, -1);
                        this.preSetPC();
                        this.pushRegWZ(0 + n7);
                        this.setPC();
                        this.leaveMethod();
                        n5 |= 1;
                        break block0;
                    }
                    case 9: {
                        if (n == -1) {
                            throw new Compiler.Exn("pc modifying insn in delay slot");
                        }
                        this.emitInstruction(-1, n3, -1);
                        this.link(n);
                        this.preSetPC();
                        this.pushRegWZ(0 + n7);
                        this.setPC();
                        this.leaveMethod();
                        n5 |= 1;
                        break block0;
                    }
                    case 12: {
                        this.preSetPC();
                        methodGen.add(18, n);
                        this.setPC();
                        this.restoreChangedRegs();
                        this.preSetReg(2);
                        methodGen.add(42);
                        this.pushRegZ(2);
                        this.pushRegZ(4);
                        this.pushRegZ(5);
                        this.pushRegZ(6);
                        this.pushRegZ(7);
                        this.pushRegZ(8);
                        this.pushRegZ(9);
                        methodGen.add(-74, (Object)this.me.method("syscall", Type.INT, new Type[]{Type.INT, Type.INT, Type.INT, Type.INT, Type.INT, Type.INT, Type.INT}));
                        this.setReg();
                        methodGen.add(42);
                        methodGen.add(-76, (Object)this.me.field("state", Type.INT));
                        int n19 = methodGen.add(-103);
                        this.preSetPC();
                        methodGen.add(18, n + 4);
                        this.setPC();
                        this.leaveMethod();
                        methodGen.setArg(n19, methodGen.size());
                        break block0;
                    }
                    case 13: {
                        methodGen.add(-69, (Object)Type.Class.instance((String)"org.ibex.nestedvm.Runtime$ExecutionException"));
                        methodGen.add(89);
                        methodGen.add(18, (Object)("BREAK Code " + ClassFileCompiler.toHex(n15)));
                        methodGen.add(-73, (Object)Type.Class.instance((String)"org.ibex.nestedvm.Runtime$ExecutionException").method("<init>", Type.VOID, new Type[]{Type.STRING}));
                        methodGen.add(-65);
                        n5 |= 1;
                        break block0;
                    }
                    case 16: {
                        this.preSetReg(0 + n10);
                        this.pushReg(64);
                        this.setReg();
                        break block0;
                    }
                    case 17: {
                        this.preSetReg(64);
                        this.pushRegZ(0 + n7);
                        this.setReg();
                        break block0;
                    }
                    case 18: {
                        this.preSetReg(0 + n10);
                        this.pushReg(65);
                        this.setReg();
                        break block0;
                    }
                    case 19: {
                        this.preSetReg(65);
                        this.pushRegZ(0 + n7);
                        this.setReg();
                        break block0;
                    }
                    case 24: {
                        this.pushRegWZ(0 + n7);
                        methodGen.add(-123);
                        this.pushRegWZ(0 + n8);
                        methodGen.add(-123);
                        methodGen.add(105);
                        methodGen.add(92);
                        methodGen.add(-120);
                        if (this.preSetReg(65)) {
                            methodGen.add(95);
                        }
                        this.setReg();
                        methodGen.add(18, 32);
                        methodGen.add(125);
                        methodGen.add(-120);
                        if (this.preSetReg(64)) {
                            methodGen.add(95);
                        }
                        this.setReg();
                        break block0;
                    }
                    case 25: {
                        this.pushRegWZ(0 + n7);
                        methodGen.add(-123);
                        methodGen.add(18, (Object)FFFFFFFF);
                        methodGen.add(127);
                        this.pushRegWZ(0 + n8);
                        methodGen.add(-123);
                        methodGen.add(18, (Object)FFFFFFFF);
                        methodGen.add(127);
                        methodGen.add(105);
                        methodGen.add(92);
                        methodGen.add(-120);
                        if (this.preSetReg(65)) {
                            methodGen.add(95);
                        }
                        this.setReg();
                        methodGen.add(18, 32);
                        methodGen.add(125);
                        methodGen.add(-120);
                        if (this.preSetReg(64)) {
                            methodGen.add(95);
                        }
                        this.setReg();
                        break block0;
                    }
                    case 26: {
                        this.pushRegWZ(0 + n7);
                        this.pushRegWZ(0 + n8);
                        methodGen.add(92);
                        methodGen.add(108);
                        if (this.preSetReg(65)) {
                            methodGen.add(95);
                        }
                        this.setReg();
                        methodGen.add(112);
                        if (this.preSetReg(64)) {
                            methodGen.add(95);
                        }
                        this.setReg();
                        break block0;
                    }
                    case 27: {
                        this.pushRegWZ(0 + n8);
                        methodGen.add(89);
                        this.setTmp();
                        int n20 = methodGen.add(-103);
                        this.pushRegWZ(0 + n7);
                        methodGen.add(-123);
                        methodGen.add(18, (Object)FFFFFFFF);
                        methodGen.add(127);
                        methodGen.add(92);
                        this.pushTmp();
                        methodGen.add(-123);
                        methodGen.add(18, (Object)FFFFFFFF);
                        methodGen.add(127);
                        methodGen.add(94);
                        methodGen.add(109);
                        methodGen.add(-120);
                        if (this.preSetReg(65)) {
                            methodGen.add(95);
                        }
                        this.setReg();
                        methodGen.add(113);
                        methodGen.add(-120);
                        if (this.preSetReg(64)) {
                            methodGen.add(95);
                        }
                        this.setReg();
                        methodGen.setArg(n20, methodGen.size());
                        break block0;
                    }
                    case 32: {
                        throw new Compiler.Exn("ADD (add with oveflow trap) not suported");
                    }
                    case 33: {
                        this.preSetReg(0 + n10);
                        if (n8 != 0 && n7 != 0) {
                            this.pushReg(0 + n7);
                            this.pushReg(0 + n8);
                            methodGen.add(96);
                        } else if (n7 != 0) {
                            this.pushReg(0 + n7);
                        } else {
                            this.pushRegZ(0 + n8);
                        }
                        this.setReg();
                        break block0;
                    }
                    case 34: {
                        throw new Compiler.Exn("SUB (add with oveflow trap) not suported");
                    }
                    case 35: {
                        this.preSetReg(0 + n10);
                        if (n8 != 0 && n7 != 0) {
                            this.pushReg(0 + n7);
                            this.pushReg(0 + n8);
                            methodGen.add(100);
                        } else if (n8 != 0) {
                            this.pushReg(0 + n8);
                            methodGen.add(116);
                        } else {
                            this.pushRegZ(0 + n7);
                        }
                        this.setReg();
                        break block0;
                    }
                    case 36: {
                        this.preSetReg(0 + n10);
                        this.pushRegWZ(0 + n7);
                        this.pushRegWZ(0 + n8);
                        methodGen.add(126);
                        this.setReg();
                        break block0;
                    }
                    case 37: {
                        this.preSetReg(0 + n10);
                        this.pushRegWZ(0 + n7);
                        this.pushRegWZ(0 + n8);
                        methodGen.add(-128);
                        this.setReg();
                        break block0;
                    }
                    case 38: {
                        this.preSetReg(0 + n10);
                        this.pushRegWZ(0 + n7);
                        this.pushRegWZ(0 + n8);
                        methodGen.add(-126);
                        this.setReg();
                        break block0;
                    }
                    case 39: {
                        this.preSetReg(0 + n10);
                        if (n7 != 0 || n8 != 0) {
                            if (n7 != 0 && n8 != 0) {
                                this.pushReg(0 + n7);
                                this.pushReg(0 + n8);
                                methodGen.add(-128);
                            } else if (n7 != 0) {
                                this.pushReg(0 + n7);
                            } else {
                                this.pushReg(0 + n8);
                            }
                            methodGen.add(2);
                            methodGen.add(-126);
                        } else {
                            methodGen.add(18, -1);
                        }
                        this.setReg();
                        break block0;
                    }
                    case 42: {
                        this.preSetReg(0 + n10);
                        if (n7 != n8) {
                            this.pushRegZ(0 + n7);
                            this.pushRegZ(0 + n8);
                            int n21 = methodGen.add(-95);
                            methodGen.add(3);
                            int n22 = methodGen.add(-89);
                            methodGen.setArg(n21, methodGen.add(4));
                            methodGen.setArg(n22, methodGen.size());
                        } else {
                            methodGen.add(18, 0);
                        }
                        this.setReg();
                        break block0;
                    }
                    case 43: {
                        this.preSetReg(0 + n10);
                        if (n7 != n8) {
                            int n23;
                            if (n7 != 0) {
                                this.pushReg(0 + n7);
                                methodGen.add(-123);
                                methodGen.add(18, (Object)FFFFFFFF);
                                methodGen.add(127);
                                this.pushReg(0 + n8);
                                methodGen.add(-123);
                                methodGen.add(18, (Object)FFFFFFFF);
                                methodGen.add(127);
                                methodGen.add(-108);
                                n23 = methodGen.add(-101);
                            } else {
                                this.pushReg(0 + n8);
                                n23 = methodGen.add(-102);
                            }
                            methodGen.add(3);
                            int n24 = methodGen.add(-89);
                            methodGen.setArg(n23, methodGen.add(4));
                            methodGen.setArg(n24, methodGen.size());
                        } else {
                            methodGen.add(18, 0);
                        }
                        this.setReg();
                        break block0;
                    }
                }
                throw new Compiler.Exn("Illegal instruction 0/" + n14);
            }
            case 1: {
                switch (n8) {
                    case 0: {
                        if (n == -1) {
                            throw new Compiler.Exn("pc modifying insn in delay slot");
                        }
                        this.pushRegWZ(0 + n7);
                        return this.doIfInstruction(-101, n, n + n18 * 4 + 4, n3);
                    }
                    case 1: {
                        if (n == -1) {
                            throw new Compiler.Exn("pc modifying insn in delay slot");
                        }
                        this.pushRegWZ(0 + n7);
                        return this.doIfInstruction(-100, n, n + n18 * 4 + 4, n3);
                    }
                    case 16: {
                        if (n == -1) {
                            throw new Compiler.Exn("pc modifying insn in delay slot");
                        }
                        this.pushRegWZ(0 + n7);
                        int n25 = methodGen.add(-100);
                        this.emitInstruction(-1, n3, -1);
                        this.link(n);
                        this.branch(n, n + n18 * 4 + 4);
                        methodGen.setArg(n25, methodGen.size());
                        break block0;
                    }
                    case 17: {
                        if (n == -1) {
                            throw new Compiler.Exn("pc modifying insn in delay slot");
                        }
                        int n26 = -1;
                        if (n7 != 0) {
                            this.pushRegWZ(0 + n7);
                            n26 = methodGen.add(-101);
                        }
                        this.emitInstruction(-1, n3, -1);
                        this.link(n);
                        this.branch(n, n + n18 * 4 + 4);
                        if (n26 != -1) {
                            methodGen.setArg(n26, methodGen.size());
                        }
                        if (n26 != -1) break block0;
                        n5 |= 1;
                        break block0;
                    }
                }
                throw new Compiler.Exn("Illegal Instruction 1/" + n8);
            }
            case 2: {
                if (n == -1) {
                    throw new Compiler.Exn("pc modifying insn in delay slot");
                }
                this.emitInstruction(-1, n3, -1);
                this.branch(n, n & -268435456 | n16 << 2);
                n5 |= 1;
                break;
            }
            case 3: {
                if (n == -1) {
                    throw new Compiler.Exn("pc modifying insn in delay slot");
                }
                int n27 = n & -268435456 | n16 << 2;
                this.emitInstruction(-1, n3, -1);
                this.link(n);
                this.branch(n, n27);
                n5 |= 1;
                break;
            }
            case 4: {
                if (n == -1) {
                    throw new Compiler.Exn("pc modifying insn in delay slot");
                }
                if (n7 == n8) {
                    this.emitInstruction(-1, n3, -1);
                    this.branch(n, n + n18 * 4 + 4);
                    n5 |= 1;
                    break;
                }
                if (n7 == 0 || n8 == 0) {
                    this.pushReg(n8 == 0 ? 0 + n7 : 0 + n8);
                    return this.doIfInstruction(-103, n, n + n18 * 4 + 4, n3);
                }
                this.pushReg(0 + n7);
                this.pushReg(0 + n8);
                return this.doIfInstruction(-97, n, n + n18 * 4 + 4, n3);
            }
            case 5: {
                if (n == -1) {
                    throw new Compiler.Exn("pc modifying insn in delay slot");
                }
                this.pushRegWZ(0 + n7);
                if (n8 == 0) {
                    return this.doIfInstruction(-102, n, n + n18 * 4 + 4, n3);
                }
                this.pushReg(0 + n8);
                return this.doIfInstruction(-96, n, n + n18 * 4 + 4, n3);
            }
            case 6: {
                if (n == -1) {
                    throw new Compiler.Exn("pc modifying insn in delay slot");
                }
                this.pushRegWZ(0 + n7);
                return this.doIfInstruction(-98, n, n + n18 * 4 + 4, n3);
            }
            case 7: {
                if (n == -1) {
                    throw new Compiler.Exn("pc modifying insn in delay slot");
                }
                this.pushRegWZ(0 + n7);
                return this.doIfInstruction(-99, n, n + n18 * 4 + 4, n3);
            }
            case 8: {
                throw new Compiler.Exn("ADDI (add immediate with oveflow trap) not suported");
            }
            case 9: {
                if (n7 != 0 && n4 != 0 && n7 == n8 && this.doLocal(n8) && n4 >= -32768 && n4 <= 32767) {
                    this.regLocalWritten[n8] = true;
                    methodGen.add(-124, (Object)new MethodGen.Pair(this.getLocalForReg(n8), n4));
                    break;
                }
                this.preSetReg(0 + n8);
                this.addiu(n7, n4);
                this.setReg();
                break;
            }
            case 10: {
                this.preSetReg(0 + n8);
                this.pushRegWZ(0 + n7);
                methodGen.add(18, n4);
                int n28 = methodGen.add(-95);
                methodGen.add(3);
                int n29 = methodGen.add(-89);
                methodGen.setArg(n28, methodGen.add(4));
                methodGen.setArg(n29, methodGen.size());
                this.setReg();
                break;
            }
            case 11: {
                this.preSetReg(0 + n8);
                this.pushRegWZ(0 + n7);
                methodGen.add(-123);
                methodGen.add(18, (Object)FFFFFFFF);
                methodGen.add(127);
                methodGen.add(18, (Object)new Long((long)n4 & 0xFFFFFFFFL));
                methodGen.add(-108);
                int n30 = methodGen.add(-101);
                methodGen.add(3);
                int n31 = methodGen.add(-89);
                methodGen.setArg(n30, methodGen.add(4));
                methodGen.setArg(n31, methodGen.size());
                this.setReg();
                break;
            }
            case 12: {
                this.preSetReg(0 + n8);
                this.pushRegWZ(0 + n7);
                methodGen.add(18, n17);
                methodGen.add(126);
                this.setReg();
                break;
            }
            case 13: {
                this.preSetReg(0 + n8);
                if (n7 != 0 && n17 != 0) {
                    this.pushReg(0 + n7);
                    methodGen.add(18, n17);
                    methodGen.add(-128);
                } else if (n7 != 0) {
                    this.pushReg(0 + n7);
                } else {
                    methodGen.add(18, n17);
                }
                this.setReg();
                break;
            }
            case 14: {
                this.preSetReg(0 + n8);
                this.pushRegWZ(0 + n7);
                methodGen.add(18, n17);
                methodGen.add(-126);
                this.setReg();
                break;
            }
            case 15: {
                this.preSetReg(0 + n8);
                methodGen.add(18, n17 << 16);
                this.setReg();
                break;
            }
            case 16: {
                throw new Compiler.Exn("TLB/Exception support not implemented");
            }
            case 17: {
                switch (n7) {
                    case 0: {
                        this.preSetReg(0 + n8);
                        this.pushReg(32 + n10);
                        this.setReg();
                        break block0;
                    }
                    case 2: {
                        if (n11 != 31) {
                            throw new Compiler.Exn("FCR " + n11 + " unavailable");
                        }
                        this.preSetReg(0 + n8);
                        this.pushReg(66);
                        this.setReg();
                        break block0;
                    }
                    case 4: {
                        this.preSetReg(32 + n10);
                        if (n8 != 0) {
                            this.pushReg(0 + n8);
                        } else {
                            methodGen.add(3);
                        }
                        this.setReg();
                        break block0;
                    }
                    case 6: {
                        if (n11 != 31) {
                            throw new Compiler.Exn("FCR " + n11 + " unavailable");
                        }
                        this.preSetReg(66);
                        this.pushReg(0 + n8);
                        this.setReg();
                        break block0;
                    }
                    case 8: {
                        this.pushReg(66);
                        methodGen.add(18, 8388608);
                        methodGen.add(126);
                        return this.doIfInstruction((n2 >>> 16 & 1) == 0 ? -103 : -102, n, n + n18 * 4 + 4, n3);
                    }
                    case 16: 
                    case 17: {
                        boolean bl = n7 == 17;
                        switch (n14) {
                            case 0: {
                                this.preSetDouble(32 + n13, bl);
                                this.pushDouble(32 + n11, bl);
                                this.pushDouble(32 + n9, bl);
                                methodGen.add(bl ? 99 : 98);
                                this.setDouble(bl);
                                break block0;
                            }
                            case 1: {
                                this.preSetDouble(32 + n13, bl);
                                this.pushDouble(32 + n11, bl);
                                this.pushDouble(32 + n9, bl);
                                methodGen.add(bl ? 103 : 102);
                                this.setDouble(bl);
                                break block0;
                            }
                            case 2: {
                                this.preSetDouble(32 + n13, bl);
                                this.pushDouble(32 + n11, bl);
                                this.pushDouble(32 + n9, bl);
                                methodGen.add(bl ? 107 : 106);
                                this.setDouble(bl);
                                break block0;
                            }
                            case 3: {
                                this.preSetDouble(32 + n13, bl);
                                this.pushDouble(32 + n11, bl);
                                this.pushDouble(32 + n9, bl);
                                methodGen.add(bl ? 111 : 110);
                                this.setDouble(bl);
                                break block0;
                            }
                            case 5: {
                                this.preSetDouble(32 + n13, bl);
                                this.pushDouble(32 + n11, bl);
                                methodGen.add(bl ? 92 : 89);
                                methodGen.add(bl ? 14 : 11);
                                methodGen.add(bl ? -104 : -106);
                                int n32 = methodGen.add(-99);
                                methodGen.add(bl ? 14 : 11);
                                if (bl) {
                                    methodGen.add(94);
                                    methodGen.add(88);
                                } else {
                                    methodGen.add(95);
                                }
                                methodGen.add(bl ? 103 : 102);
                                methodGen.setArg(n32, methodGen.size());
                                this.setDouble(bl);
                                break block0;
                            }
                            case 6: {
                                this.preSetReg(32 + n13);
                                this.pushReg(32 + n11);
                                this.setReg();
                                if (!bl) break block0;
                                this.preSetReg(32 + n13 + 1);
                                this.pushReg(32 + n11 + 1);
                                this.setReg();
                                break block0;
                            }
                            case 7: {
                                this.preSetDouble(32 + n13, bl);
                                this.pushDouble(32 + n11, bl);
                                methodGen.add(bl ? 119 : 118);
                                this.setDouble(bl);
                                break block0;
                            }
                            case 32: {
                                this.preSetFloat(32 + n13);
                                this.pushDouble(32 + n11, bl);
                                if (bl) {
                                    methodGen.add(-112);
                                }
                                this.setFloat();
                                break block0;
                            }
                            case 33: {
                                this.preSetDouble(32 + n13);
                                this.pushDouble(32 + n11, bl);
                                if (!bl) {
                                    methodGen.add(-115);
                                }
                                this.setDouble();
                                break block0;
                            }
                            case 36: {
                                MethodGen.Switch.Table table = new MethodGen.Switch.Table(0, 3);
                                this.preSetReg(32 + n13);
                                this.pushDouble(32 + n11, bl);
                                this.pushReg(66);
                                methodGen.add(6);
                                methodGen.add(126);
                                methodGen.add(-86, (Object)table);
                                table.setTarget(2, methodGen.size());
                                if (!bl) {
                                    methodGen.add(-115);
                                }
                                methodGen.add(-72, (Object)Type.Class.instance((String)"java.lang.Math").method("ceil", Type.DOUBLE, new Type[]{Type.DOUBLE}));
                                if (!bl) {
                                    methodGen.add(-112);
                                }
                                int n33 = methodGen.add(-89);
                                table.setTarget(0, methodGen.size());
                                methodGen.add(18, (Object)(bl ? POINT_5_D : POINT_5_F));
                                methodGen.add(bl ? 99 : 98);
                                table.setTarget(3, methodGen.size());
                                if (!bl) {
                                    methodGen.add(-115);
                                }
                                methodGen.add(-72, (Object)Type.Class.instance((String)"java.lang.Math").method("floor", Type.DOUBLE, new Type[]{Type.DOUBLE}));
                                if (!bl) {
                                    methodGen.add(-112);
                                }
                                table.setTarget(1, methodGen.size());
                                table.setDefaultTarget(methodGen.size());
                                methodGen.setArg(n33, methodGen.size());
                                methodGen.add(bl ? -114 : -117);
                                this.setReg();
                                break block0;
                            }
                            case 50: 
                            case 60: 
                            case 62: {
                                int n34;
                                this.preSetReg(66);
                                this.pushReg(66);
                                methodGen.add(18, -8388609);
                                methodGen.add(126);
                                this.pushDouble(32 + n11, bl);
                                this.pushDouble(32 + n9, bl);
                                methodGen.add(bl ? -104 : -106);
                                switch (n14) {
                                    case 50: {
                                        n34 = methodGen.add(-102);
                                        break;
                                    }
                                    case 60: {
                                        n34 = methodGen.add(-100);
                                        break;
                                    }
                                    case 62: {
                                        n34 = methodGen.add(-99);
                                        break;
                                    }
                                    default: {
                                        n34 = -1;
                                    }
                                }
                                methodGen.add(18, 8388608);
                                methodGen.add(-128);
                                methodGen.setArg(n34, methodGen.size());
                                this.setReg();
                                break block0;
                            }
                        }
                        throw new Compiler.Exn("Invalid Instruction 17/" + n7 + "/" + n14);
                    }
                    case 20: {
                        switch (n14) {
                            case 32: {
                                this.preSetFloat(32 + n13);
                                this.pushReg(32 + n11);
                                methodGen.add(-122);
                                this.setFloat();
                                break block0;
                            }
                            case 33: {
                                this.preSetDouble(32 + n13);
                                this.pushReg(32 + n11);
                                methodGen.add(-121);
                                this.setDouble();
                                break block0;
                            }
                        }
                        throw new Compiler.Exn("Invalid Instruction 17/" + n7 + "/" + n14);
                    }
                }
                throw new Compiler.Exn("Invalid Instruction 17/" + n7);
            }
            case 18: 
            case 19: {
                throw new Compiler.Exn("coprocessor 2 and 3 instructions not available");
            }
            case 32: {
                this.preSetReg(0 + n8);
                this.addiu(0 + n7, n4);
                this.setTmp();
                this.preMemRead();
                this.pushTmp();
                this.memRead(true);
                this.pushTmp();
                methodGen.add(2);
                methodGen.add(-126);
                methodGen.add(6);
                methodGen.add(126);
                methodGen.add(6);
                methodGen.add(120);
                methodGen.add(124);
                methodGen.add(-111);
                this.setReg();
                break;
            }
            case 33: {
                this.preSetReg(0 + n8);
                this.addiu(0 + n7, n4);
                this.setTmp();
                this.preMemRead();
                this.pushTmp();
                this.memRead(true);
                this.pushTmp();
                methodGen.add(2);
                methodGen.add(-126);
                methodGen.add(5);
                methodGen.add(126);
                methodGen.add(6);
                methodGen.add(120);
                methodGen.add(124);
                methodGen.add(-109);
                this.setReg();
                break;
            }
            case 34: {
                this.preSetReg(0 + n8);
                this.addiu(0 + n7, n4);
                this.setTmp();
                this.pushRegWZ(0 + n8);
                methodGen.add(18, 16777215);
                this.pushTmp();
                methodGen.add(2);
                methodGen.add(-126);
                methodGen.add(6);
                methodGen.add(126);
                methodGen.add(6);
                methodGen.add(120);
                methodGen.add(124);
                methodGen.add(126);
                this.preMemRead();
                this.pushTmp();
                this.memRead(true);
                this.pushTmp();
                methodGen.add(6);
                methodGen.add(126);
                methodGen.add(6);
                methodGen.add(120);
                methodGen.add(120);
                methodGen.add(-128);
                this.setReg();
                break;
            }
            case 35: {
                this.preSetReg(0 + n8);
                this.memRead(0 + n7, n4);
                this.setReg();
                break;
            }
            case 36: {
                this.preSetReg(0 + n8);
                this.addiu(0 + n7, n4);
                this.setTmp();
                this.preMemRead();
                this.pushTmp();
                this.memRead(true);
                this.pushTmp();
                methodGen.add(2);
                methodGen.add(-126);
                methodGen.add(6);
                methodGen.add(126);
                methodGen.add(6);
                methodGen.add(120);
                methodGen.add(124);
                methodGen.add(18, 255);
                methodGen.add(126);
                this.setReg();
                break;
            }
            case 37: {
                this.preSetReg(0 + n8);
                this.addiu(0 + n7, n4);
                this.setTmp();
                this.preMemRead();
                this.pushTmp();
                this.memRead(true);
                this.pushTmp();
                methodGen.add(2);
                methodGen.add(-126);
                methodGen.add(5);
                methodGen.add(126);
                methodGen.add(6);
                methodGen.add(120);
                methodGen.add(124);
                methodGen.add(-110);
                this.setReg();
                break;
            }
            case 38: {
                this.preSetReg(0 + n8);
                this.addiu(0 + n7, n4);
                this.setTmp();
                this.pushRegWZ(0 + n8);
                methodGen.add(18, -256);
                this.pushTmp();
                methodGen.add(6);
                methodGen.add(126);
                methodGen.add(6);
                methodGen.add(120);
                methodGen.add(120);
                methodGen.add(126);
                this.preMemRead();
                this.pushTmp();
                this.memRead(true);
                this.pushTmp();
                methodGen.add(2);
                methodGen.add(-126);
                methodGen.add(6);
                methodGen.add(126);
                methodGen.add(6);
                methodGen.add(120);
                methodGen.add(124);
                methodGen.add(-128);
                this.setReg();
                break;
            }
            case 40: {
                this.addiu(0 + n7, n4);
                this.setTmp();
                this.preMemRead(true);
                this.pushTmp();
                this.memRead(true);
                methodGen.add(18, -16777216);
                this.pushTmp();
                methodGen.add(6);
                methodGen.add(126);
                methodGen.add(6);
                methodGen.add(120);
                methodGen.add(124);
                methodGen.add(2);
                methodGen.add(-126);
                methodGen.add(126);
                if (n8 != 0) {
                    this.pushReg(0 + n8);
                    methodGen.add(18, 255);
                    methodGen.add(126);
                } else {
                    methodGen.add(18, 0);
                }
                this.pushTmp();
                methodGen.add(2);
                methodGen.add(-126);
                methodGen.add(6);
                methodGen.add(126);
                methodGen.add(6);
                methodGen.add(120);
                methodGen.add(120);
                methodGen.add(-128);
                this.memWrite();
                break;
            }
            case 41: {
                this.addiu(0 + n7, n4);
                this.setTmp();
                this.preMemRead(true);
                this.pushTmp();
                this.memRead(true);
                methodGen.add(18, 65535);
                this.pushTmp();
                methodGen.add(5);
                methodGen.add(126);
                methodGen.add(6);
                methodGen.add(120);
                methodGen.add(120);
                methodGen.add(126);
                if (n8 != 0) {
                    this.pushReg(0 + n8);
                    methodGen.add(18, 65535);
                    methodGen.add(126);
                } else {
                    methodGen.add(18, 0);
                }
                this.pushTmp();
                methodGen.add(2);
                methodGen.add(-126);
                methodGen.add(5);
                methodGen.add(126);
                methodGen.add(6);
                methodGen.add(120);
                methodGen.add(120);
                methodGen.add(-128);
                this.memWrite();
                break;
            }
            case 42: {
                this.addiu(0 + n7, n4);
                this.setTmp();
                this.preMemRead(true);
                this.pushTmp();
                this.memRead(true);
                methodGen.add(18, -256);
                this.pushTmp();
                methodGen.add(2);
                methodGen.add(-126);
                methodGen.add(6);
                methodGen.add(126);
                methodGen.add(6);
                methodGen.add(120);
                methodGen.add(120);
                methodGen.add(126);
                this.pushRegWZ(0 + n8);
                this.pushTmp();
                methodGen.add(6);
                methodGen.add(126);
                methodGen.add(6);
                methodGen.add(120);
                methodGen.add(124);
                methodGen.add(-128);
                this.memWrite();
                break;
            }
            case 43: {
                this.preMemWrite1();
                this.preMemWrite2(0 + n7, n4);
                this.pushRegZ(0 + n8);
                this.memWrite();
                break;
            }
            case 46: {
                this.addiu(0 + n7, n4);
                this.setTmp();
                this.preMemRead(true);
                this.pushTmp();
                this.memRead(true);
                methodGen.add(18, 16777215);
                this.pushTmp();
                methodGen.add(6);
                methodGen.add(126);
                methodGen.add(6);
                methodGen.add(120);
                methodGen.add(124);
                methodGen.add(126);
                this.pushRegWZ(0 + n8);
                this.pushTmp();
                methodGen.add(2);
                methodGen.add(-126);
                methodGen.add(6);
                methodGen.add(126);
                methodGen.add(6);
                methodGen.add(120);
                methodGen.add(120);
                methodGen.add(-128);
                this.memWrite();
                break;
            }
            case 48: {
                this.preSetReg(0 + n8);
                this.memRead(0 + n7, n4);
                this.setReg();
                break;
            }
            case 49: {
                this.preSetReg(32 + n8);
                this.memRead(0 + n7, n4);
                this.setReg();
                break;
            }
            case 56: {
                this.preSetReg(0 + n8);
                this.preMemWrite1();
                this.preMemWrite2(0 + n7, n4);
                this.pushReg(0 + n8);
                this.memWrite();
                methodGen.add(18, 1);
                this.setReg();
                break;
            }
            case 57: {
                this.preMemWrite1();
                this.preMemWrite2(0 + n7, n4);
                this.pushReg(32 + n8);
                this.memWrite();
                break;
            }
            default: {
                throw new Compiler.Exn("Invalid Instruction: " + n6 + " at " + ClassFileCompiler.toHex(n));
            }
        }
        return n5;
    }

    private boolean doLocal(int n) {
        return n == 2 || n == 3 || n == 4 || n == 29;
    }

    private int getLocalForReg(int n) {
        if (this.regLocalMapping[n] != 0) {
            return this.regLocalMapping[n];
        }
        ++this.nextAvailLocal;
        return this.regLocalMapping[n];
    }

    private void fixupRegsStart() {
        int n;
        for (n = 0; n < 67; ++n) {
            this.regLocalMapping[n] = 0;
            this.regLocalWritten[n] = false;
        }
        this.nextAvailLocal = this.onePage ? 4 : 5;
        this.loadsStart = this.mg.size();
        for (n = 0; n < 12; ++n) {
            this.mg.add(0);
        }
    }

    private void fixupRegsEnd() {
        int n = this.loadsStart;
        for (int i = 0; i < 67; ++i) {
            if (this.regLocalMapping[i] == 0) continue;
            this.mg.set(n++, 42);
            this.mg.set(n++, -76, (Object)this.me.field(regField[i], Type.INT));
            this.mg.set(n++, 54, this.regLocalMapping[i]);
            if (!this.regLocalWritten[i]) continue;
            this.mg.add(42);
            this.mg.add(21, this.regLocalMapping[i]);
            this.mg.add(-75, (Object)this.me.field(regField[i], Type.INT));
        }
    }

    private void restoreChangedRegs() {
        for (int i = 0; i < 67; ++i) {
            if (!this.regLocalWritten[i]) continue;
            this.mg.add(42);
            this.mg.add(21, this.regLocalMapping[i]);
            this.mg.add(-75, (Object)this.me.field(regField[i], Type.INT));
        }
    }

    private int pushRegWZ(int n) {
        if (n == 0) {
            this.warn.println("Warning: Pushing r0!");
            new Exception().printStackTrace(this.warn);
        }
        return this.pushRegZ(n);
    }

    private int pushRegZ(int n) {
        if (n == 0) {
            return this.mg.add(3);
        }
        return this.pushReg(n);
    }

    private int pushReg(int n) {
        int n2 = this.mg.size();
        if (this.doLocal(n)) {
            this.mg.add(21, this.getLocalForReg(n));
        } else if (n >= 32 && n <= 63 && this.singleFloat) {
            this.mg.add(42);
            this.mg.add(-76, (Object)this.me.field(regField[n], Type.FLOAT));
            this.mg.add(-72, (Object)Type.FLOAT_OBJECT.method("floatToIntBits", Type.INT, new Type[]{Type.FLOAT}));
        } else {
            this.mg.add(42);
            this.mg.add(-76, (Object)this.me.field(regField[n], Type.INT));
        }
        return n2;
    }

    private boolean preSetReg(int n) {
        this.preSetRegStack[this.preSetRegStackPos] = n;
        ++this.preSetRegStackPos;
        if (this.doLocal(n)) {
            return false;
        }
        this.mg.add(42);
        return true;
    }

    private int setReg() {
        if (this.preSetRegStackPos == 0) {
            throw new RuntimeException("didn't do preSetReg");
        }
        --this.preSetRegStackPos;
        int n = this.preSetRegStack[this.preSetRegStackPos];
        int n2 = this.mg.size();
        if (this.doLocal(n)) {
            this.mg.add(54, this.getLocalForReg(n));
            this.regLocalWritten[n] = true;
        } else if (n >= 32 && n <= 63 && this.singleFloat) {
            this.mg.add(-72, (Object)Type.FLOAT_OBJECT.method("intBitsToFloat", Type.FLOAT, new Type[]{Type.INT}));
            this.mg.add(-75, (Object)this.me.field(regField[n], Type.FLOAT));
        } else {
            this.mg.add(-75, (Object)this.me.field(regField[n], Type.INT));
        }
        return n2;
    }

    private int preSetPC() {
        return this.mg.add(42);
    }

    private int setPC() {
        return this.mg.add(-75, (Object)this.me.field("pc", Type.INT));
    }

    private int pushFloat(int n) throws Compiler.Exn {
        return this.pushDouble(n, false);
    }

    private int pushDouble(int n, boolean bl) throws Compiler.Exn {
        if (n < 32 || n >= 64) {
            throw new IllegalArgumentException("" + n);
        }
        int n2 = this.mg.size();
        if (bl) {
            if (this.singleFloat) {
                throw new Compiler.Exn("Double operations not supported when singleFloat is enabled");
            }
            if (n == 63) {
                throw new Compiler.Exn("Tried to use a double in f31");
            }
            this.pushReg(n + 1);
            this.mg.add(-123);
            this.mg.add(18, 32);
            this.mg.add(121);
            this.pushReg(n);
            this.mg.add(-123);
            this.mg.add(18, (Object)FFFFFFFF);
            this.mg.add(127);
            this.mg.add(-127);
            this.mg.add(-72, (Object)Type.DOUBLE_OBJECT.method("longBitsToDouble", Type.DOUBLE, new Type[]{Type.LONG}));
        } else if (this.singleFloat) {
            this.mg.add(42);
            this.mg.add(-76, (Object)this.me.field(regField[n], Type.FLOAT));
        } else {
            this.pushReg(n);
            this.mg.add(-72, (Object)Type.Class.instance((String)"java.lang.Float").method("intBitsToFloat", Type.FLOAT, new Type[]{Type.INT}));
        }
        return n2;
    }

    private void preSetFloat(int n) {
        this.preSetDouble(n, false);
    }

    private void preSetDouble(int n) {
        this.preSetDouble(n, true);
    }

    private void preSetDouble(int n, boolean bl) {
        this.preSetReg(n);
    }

    private int setFloat() throws Compiler.Exn {
        return this.setDouble(false);
    }

    private int setDouble() throws Compiler.Exn {
        return this.setDouble(true);
    }

    private int setDouble(boolean bl) throws Compiler.Exn {
        int n = this.preSetRegStack[this.preSetRegStackPos - 1];
        if (n < 32 || n >= 64) {
            throw new IllegalArgumentException("" + n);
        }
        int n2 = this.mg.size();
        if (bl) {
            if (this.singleFloat) {
                throw new Compiler.Exn("Double operations not supported when singleFloat is enabled");
            }
            if (n == 63) {
                throw new Compiler.Exn("Tried to use a double in f31");
            }
            this.mg.add(-72, (Object)Type.DOUBLE_OBJECT.method("doubleToLongBits", Type.LONG, new Type[]{Type.DOUBLE}));
            this.mg.add(92);
            this.mg.add(18, 32);
            this.mg.add(125);
            this.mg.add(-120);
            if (this.preSetReg(n + 1)) {
                this.mg.add(95);
            }
            this.setReg();
            this.mg.add(-120);
            this.setReg();
        } else if (this.singleFloat) {
            --this.preSetRegStackPos;
            this.mg.add(-75, (Object)this.me.field(regField[n], Type.FLOAT));
        } else {
            this.mg.add(-72, (Object)Type.FLOAT_OBJECT.method("floatToRawIntBits", Type.INT, new Type[]{Type.FLOAT}));
            this.setReg();
        }
        return n2;
    }

    private void pushTmp() {
        this.mg.add(27);
    }

    private void setTmp() {
        this.mg.add(60);
    }

    private void addiu(int n, int n2) {
        if (n != 0 && n2 != 0) {
            this.pushReg(n);
            this.mg.add(18, n2);
            this.mg.add(96);
        } else if (n != 0) {
            this.pushReg(n);
        } else {
            this.mg.add(18, n2);
        }
    }

    private void preMemWrite1() {
        if (this.memWriteStage != 0) {
            throw new Error("pending preMemWrite1/2");
        }
        this.memWriteStage = 1;
        if (this.onePage) {
            this.mg.add(44);
        } else if (this.fastMem) {
            this.mg.add(25, 3);
        } else {
            this.mg.add(42);
        }
    }

    private void preMemWrite2(int n, int n2) {
        this.addiu(n, n2);
        this.preMemWrite2();
    }

    private void preMemWrite2() {
        this.preMemWrite2(false);
    }

    private void preMemWrite2(boolean bl) {
        if (this.memWriteStage != 1) {
            throw new Error("pending preMemWrite2 or no preMemWrite1");
        }
        this.memWriteStage = 2;
        if (this.nullPointerCheck) {
            this.mg.add(89);
            this.mg.add(42);
            this.mg.add(95);
            this.mg.add(-74, (Object)this.me.method("nullPointerCheck", Type.VOID, new Type[]{Type.INT}));
        }
        if (this.onePage) {
            this.mg.add(5);
            this.mg.add(124);
        } else if (this.fastMem) {
            if (!bl) {
                this.mg.add(90);
            }
            this.mg.add(18, this.pageShift);
            this.mg.add(124);
            this.mg.add(50);
            if (bl) {
                this.pushTmp();
            } else {
                this.mg.add(95);
            }
            this.mg.add(5);
            this.mg.add(124);
            this.mg.add(18, (this.pageSize >> 2) - 1);
            this.mg.add(126);
        }
    }

    private void memWrite() {
        if (this.memWriteStage != 2) {
            throw new Error("didn't do preMemWrite1 or preMemWrite2");
        }
        this.memWriteStage = 0;
        if (this.onePage) {
            this.mg.add(79);
        } else if (this.fastMem) {
            this.mg.add(79);
        } else {
            this.mg.add(-74, (Object)this.me.method("unsafeMemWrite", Type.VOID, new Type[]{Type.INT, Type.INT}));
        }
    }

    private void memRead(int n, int n2) {
        this.preMemRead();
        this.addiu(n, n2);
        this.memRead();
    }

    private void preMemRead() {
        this.preMemRead(false);
    }

    private void preMemRead(boolean bl) {
        if (this.didPreMemRead) {
            throw new Error("pending preMemRead");
        }
        this.didPreMemRead = true;
        this.preMemReadDoPreWrite = bl;
        if (this.onePage) {
            this.mg.add(44);
        } else if (this.fastMem) {
            this.mg.add(25, bl ? 3 : 2);
        } else {
            this.mg.add(42);
        }
    }

    private void memRead() {
        this.memRead(false);
    }

    private void memRead(boolean bl) {
        if (!this.didPreMemRead) {
            throw new Error("didn't do preMemRead");
        }
        this.didPreMemRead = false;
        if (this.preMemReadDoPreWrite) {
            this.memWriteStage = 2;
        }
        if (this.nullPointerCheck) {
            this.mg.add(89);
            this.mg.add(42);
            this.mg.add(95);
            this.mg.add(-74, (Object)this.me.method("nullPointerCheck", Type.VOID, new Type[]{Type.INT}));
        }
        if (this.onePage) {
            this.mg.add(5);
            this.mg.add(124);
            if (this.preMemReadDoPreWrite) {
                this.mg.add(92);
            }
            this.mg.add(46);
        } else if (this.fastMem) {
            if (!bl) {
                this.mg.add(90);
            }
            this.mg.add(18, this.pageShift);
            this.mg.add(124);
            this.mg.add(50);
            if (bl) {
                this.pushTmp();
            } else {
                this.mg.add(95);
            }
            this.mg.add(5);
            this.mg.add(124);
            this.mg.add(18, (this.pageSize >> 2) - 1);
            this.mg.add(126);
            if (this.preMemReadDoPreWrite) {
                this.mg.add(92);
            }
            this.mg.add(46);
        } else {
            if (this.preMemReadDoPreWrite) {
                this.mg.add(92);
            }
            this.mg.add(-74, (Object)this.me.method("unsafeMemRead", Type.INT, new Type[]{Type.INT}));
        }
    }

    static {
        POINT_5_F = new Float(0.5f);
        POINT_5_D = new Double(0.5);
        FFFFFFFF = new Long(0xFFFFFFFFL);
        regField = new String[]{"r0", "r1", "r2", "r3", "r4", "r5", "r6", "r7", "r8", "r9", "r10", "r11", "r12", "r13", "r14", "r15", "r16", "r17", "r18", "r19", "r20", "r21", "r22", "r23", "r24", "r25", "r26", "r27", "r28", "r29", "r30", "r31", "f0", "f1", "f2", "f3", "f4", "f5", "f6", "f7", "f8", "f9", "f10", "f11", "f12", "f13", "f14", "f15", "f16", "f17", "f18", "f19", "f20", "f21", "f22", "f23", "f24", "f25", "f26", "f27", "f28", "f29", "f30", "f31", "hi", "lo", "fcsr"};
    }
}

