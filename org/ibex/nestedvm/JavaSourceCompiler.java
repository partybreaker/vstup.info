/*
 * Decompiled with CFR 0_114.
 */
package org.ibex.nestedvm;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Hashtable;
import org.ibex.nestedvm.Compiler;
import org.ibex.nestedvm.util.ELF;
import org.ibex.nestedvm.util.Seekable;

public class JavaSourceCompiler
extends Compiler {
    private StringBuffer runs = new StringBuffer();
    private StringBuffer inits = new StringBuffer();
    private StringBuffer classLevel = new StringBuffer();
    private PrintWriter out;
    private int indent;
    private static String[] indents = new String[16];
    private int startOfMethod = 0;
    private int endOfMethod = 0;
    private HashMap relativeAddrs = new HashMap();
    private boolean textDone;
    private int initDataCount = 0;
    private boolean unreachable = false;

    private void p() {
        this.out.println();
    }

    private void p(String string) {
        this.out.println(indents[this.indent] + string);
    }

    private void pblock(StringBuffer stringBuffer) {
        this.out.print(stringBuffer.toString());
    }

    public JavaSourceCompiler(Seekable seekable, String string, Writer writer) throws IOException {
        super(seekable, string);
        this.out = new PrintWriter(writer);
    }

    protected void _go() throws Compiler.Exn, IOException {
        String string;
        Object object;
        String string2;
        int n;
        if (this.singleFloat) {
            throw new Compiler.Exn("JavaSourceCompiler doesn't support singleFloat");
        }
        if (this.fullClassName.indexOf(46) != -1) {
            string2 = this.fullClassName.substring(0, this.fullClassName.lastIndexOf(46));
            string = this.fullClassName.substring(this.fullClassName.lastIndexOf(46) + 1);
        } else {
            string = this.fullClassName;
            string2 = null;
        }
        this.p("/* This file was generated from " + this.source + " by Mips2Java on " + JavaSourceCompiler.dateTime() + " */");
        if (string2 != null) {
            this.p("package " + string2 + ";");
        }
        if (this.runtimeStats) {
            this.p("import java.util.*;");
        }
        this.p();
        this.p("public final class " + string + " extends " + this.runtimeClass + " {");
        ++this.indent;
        this.p("/* program counter */");
        this.p("private int pc = 0;");
        if (this.debugCompiler) {
            this.p("private int lastPC = 0;");
        }
        this.p();
        this.p("/* General Purpose registers */");
        this.p("private final static int r0 = 0;");
        this.p("private int      r1,  r2,  r3,  r4,  r5,  r6,  r7,");
        this.p("            r8,  r9,  r10, r11, r12, r13, r14, r15,");
        this.p("            r16, r17, r18, r19, r20, r21, r22, r23,");
        this.p("            r24, r25, r26, r27, r28, r29, r30, r31,");
        this.p("            hi = 0, lo = 0;");
        this.p("/* FP registers */");
        this.p("private int f0,  f1,  f2,  f3,  f4,  f5,  f6,  f7,");
        this.p("            f8,  f9,  f10, f11, f12, f13, f14, f15,");
        this.p("            f16, f17, f18, f19, f20, f21, f22, f23,");
        this.p("            f24, f25, f26, f27, f28, f29, f30, f31;");
        this.p("/* FP Control Register */");
        this.p("private int fcsr = 0;");
        this.p();
        if (this.onePage) {
            this.p("private final int[] page = readPages[0];");
        }
        int n2 = 0;
        for (n = 0; n < this.elf.sheaders.length; ++n) {
            ELF.SHeader sHeader = this.elf.sheaders[n];
            object = sHeader.name;
            if (sHeader.addr == 0) continue;
            n2 = Math.max(n2, sHeader.addr + sHeader.size);
            if (object.equals(".text")) {
                this.emitText(sHeader.addr, new DataInputStream(sHeader.getInputStream()), sHeader.size);
                continue;
            }
            if (object.equals(".data") || object.equals(".sdata") || object.equals(".rodata") || object.equals(".ctors") || object.equals(".dtors")) {
                this.emitData(sHeader.addr, new DataInputStream(sHeader.getInputStream()), sHeader.size, object.equals(".rodata"));
                continue;
            }
            if (object.equals(".bss") || object.equals(".sbss")) {
                this.emitBSS(sHeader.addr, sHeader.size);
                continue;
            }
            throw new Compiler.Exn("Unknown segment: " + (String)object);
        }
        this.p();
        this.pblock(this.classLevel);
        this.p();
        this.p("private final void trampoline() throws ExecutionException {");
        ++this.indent;
        this.p("while(state == RUNNING) {");
        ++this.indent;
        this.p("switch(pc>>>" + this.methodShift + ") {");
        ++this.indent;
        this.pblock(this.runs);
        this.p("default: throw new ExecutionException(\"invalid address 0x\" + Long.toString(this.pc&0xffffffffL,16) + \": r2: \" + r2);");
        --this.indent;
        this.p("}");
        --this.indent;
        this.p("}");
        --this.indent;
        this.p("}");
        this.p();
        this.p("public " + string + "() {");
        ++this.indent;
        this.p("super(" + this.pageSize + "," + this.totalPages + ");");
        this.pblock(this.inits);
        --this.indent;
        this.p("}");
        this.p();
        this.p("protected int entryPoint() { return " + JavaSourceCompiler.toHex(this.elf.header.entry) + "; }");
        this.p("protected int heapStart() { return " + JavaSourceCompiler.toHex(n2) + "; }");
        this.p("protected int gp() { return " + JavaSourceCompiler.toHex(this.gp.addr) + "; }");
        if (this.userInfo != null) {
            this.p("protected int userInfoBase() { return " + JavaSourceCompiler.toHex(this.userInfo.addr) + "; }");
            this.p("protected int userInfoSize() { return " + JavaSourceCompiler.toHex(this.userInfo.size) + "; }");
        }
        this.p("public static void main(String[] args) throws Exception {");
        ++this.indent;
        this.p("" + string + " me = new " + string + "();");
        this.p("int status = me.run(\"" + this.fullClassName + "\",args);");
        if (this.runtimeStats) {
            this.p("me.printStats();");
        }
        this.p("System.exit(status);");
        --this.indent;
        this.p("}");
        this.p();
        this.p("protected void _execute() throws ExecutionException { trampoline(); }");
        this.p();
        this.p("protected void setCPUState(CPUState state) {");
        ++this.indent;
        for (n = 1; n < 32; ++n) {
            this.p("r" + n + "=state.r[" + n + "];");
        }
        for (n = 0; n < 32; ++n) {
            this.p("f" + n + "=state.f[" + n + "];");
        }
        this.p("hi=state.hi; lo=state.lo; fcsr=state.fcsr;");
        this.p("pc=state.pc;");
        --this.indent;
        this.p("}");
        this.p("protected void getCPUState(CPUState state) {");
        ++this.indent;
        for (n = 1; n < 32; ++n) {
            this.p("state.r[" + n + "]=r" + n + ";");
        }
        for (n = 0; n < 32; ++n) {
            this.p("state.f[" + n + "]=f" + n + ";");
        }
        this.p("state.hi=hi; state.lo=lo; state.fcsr=fcsr;");
        this.p("state.pc=pc;");
        --this.indent;
        this.p("}");
        this.p();
        if (this.supportCall) {
            this.p("private static final " + this.hashClass + " symbols = new " + this.hashClass + "();");
            this.p("static {");
            ++this.indent;
            ELF.Symbol[] arrsymbol = this.elf.getSymtab().symbols;
            for (int i = 0; i < arrsymbol.length; ++i) {
                object = arrsymbol[i];
                if (object.type != 2 || object.binding != 1 || !object.name.equals("_call_helper") && object.name.startsWith("_")) continue;
                this.p("symbols.put(\"" + object.name + "\",new Integer(" + JavaSourceCompiler.toHex(object.addr) + "));");
            }
            --this.indent;
            this.p("}");
            this.p("public int lookupSymbol(String symbol) { Integer i = (Integer) symbols.get(symbol); return i==null ? -1 : i.intValue(); }");
            this.p();
        }
        if (this.runtimeStats) {
            this.p("private HashMap counters = new HashMap();");
            this.p("private void inc(String k) { Long i = (Long)counters.get(k); counters.put(k,new Long(i==null ? 1 : i.longValue() + 1)); }");
            this.p("private void printStats() {");
            this.p(" Iterator i = new TreeSet(counters.keySet()).iterator();");
            this.p(" while(i.hasNext()) { Object o = i.next(); System.err.println(\"\" + o + \": \" + counters.get(o)); }");
            this.p("}");
            this.p();
        }
        --this.indent;
        this.p("}");
    }

    private void startMethod(int n) {
        this.startOfMethod = n &= ~ (this.maxBytesPerMethod - 1);
        this.endOfMethod = n + this.maxBytesPerMethod;
        String string = "run_" + Long.toString((long)n & 0xFFFFFFFFL, 16);
        this.runs.append(indents[4] + "case " + JavaSourceCompiler.toHex(n >>> this.methodShift) + ": " + string + "(); break; \n");
        this.p("private final void " + string + "() throws ExecutionException { /" + "* " + JavaSourceCompiler.toHex(n) + " - " + JavaSourceCompiler.toHex(this.endOfMethod) + " *" + "/");
        ++this.indent;
        this.p("int addr, tmp;");
        this.p("for(;;) {");
        ++this.indent;
        this.p("switch(pc) {");
        ++this.indent;
    }

    private void endMethod() {
        this.endMethod(this.endOfMethod);
    }

    private void endMethod(int n) {
        if (this.startOfMethod == 0) {
            return;
        }
        this.p("case " + JavaSourceCompiler.toHex(n) + ":");
        ++this.indent;
        this.p("pc=" + this.constant(n) + ";");
        this.leaveMethod();
        --this.indent;
        if (this.debugCompiler) {
            this.p("default: throw new ExecutionException(\"invalid address 0x\" + Long.toString(pc&0xffffffffL,16)  + \" (got here from 0x\" + Long.toString(lastPC&0xffffffffL,16)+\")\");");
        } else {
            this.p("default: throw new ExecutionException(\"invalid address 0x\" + Long.toString(pc&0xffffffffL,16));");
        }
        --this.indent;
        this.p("}");
        this.p("/* NOT REACHED */");
        --this.indent;
        this.p("}");
        --this.indent;
        this.p("}");
        this.startOfMethod = 0;
        this.endOfMethod = 0;
    }

    private String constant(int n) {
        if (n >= 4096 && this.lessConstants) {
            int n2 = n & -1024;
            String string = "N_" + JavaSourceCompiler.toHex8(n2);
            if (this.relativeAddrs.get(new Integer(n2)) == null) {
                this.relativeAddrs.put(new Integer(n2), Boolean.TRUE);
                this.classLevel.append(indents[1] + "private static int " + string + " = " + JavaSourceCompiler.toHex(n2) + ";\n");
            }
            return "(" + string + " + " + JavaSourceCompiler.toHex(n - n2) + ")";
        }
        return JavaSourceCompiler.toHex(n);
    }

    private void branch(int n, int n2) {
        if (this.debugCompiler) {
            this.p("lastPC = " + JavaSourceCompiler.toHex(n) + ";");
        }
        this.p("pc=" + this.constant(n2) + ";");
        if (n2 == 0) {
            this.p("throw new ExecutionException(\"Branch to addr 0x0\");");
        } else if ((n & this.methodMask) == (n2 & this.methodMask)) {
            this.p("continue;");
        } else if (this.assumeTailCalls) {
            this.p("run_" + Long.toString((long)(n2 & this.methodMask) & 0xFFFFFFFFL, 16) + "(); return;");
        } else {
            this.leaveMethod();
        }
    }

    private void leaveMethod() {
        this.p("return;");
    }

    /*
     * Unable to fully structure code
     * Enabled aggressive block sorting
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
        var5_5 = var2_2.readInt();
        if (var5_5 == -1) {
            throw new Error("Actually read -1 at " + JavaSourceCompiler.toHex(var1_1));
        }
        var7_6 = 0;
        do {
            if (var7_6 >= var4_4) {
                this.endMethod(var1_1);
                this.p();
                var2_2.close();
                return;
            }
            var6_7 = var5_5;
            v0 = var5_5 = var7_6 == var4_4 - 1 ? -1 : var2_2.readInt();
            if (var1_1 >= this.endOfMethod) {
                this.endMethod();
                this.startMethod(var1_1);
            }
            if (this.jumpableAddresses != null && var1_1 != this.startOfMethod && this.jumpableAddresses.get(new Integer(var1_1)) == null) ** GOTO lbl27
            this.p("case " + JavaSourceCompiler.toHex(var1_1) + ":");
            this.unreachable = false;
            ** GOTO lbl30
lbl27: // 1 sources:
            if (!this.unreachable) {
                if (this.debugCompiler) {
                    this.p("/* pc = " + JavaSourceCompiler.toHex(var1_1) + "*" + "/");
                }
lbl30: // 4 sources:
                ++this.indent;
                this.emitInstruction(var1_1, var6_7, var5_5);
                --this.indent;
            }
            ++var7_6;
            var1_1 += 4;
        } while (true);
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
                char c;
                int n5;
                long l = 0;
                for (n5 = 0; n5 < 7; ++n5) {
                    l <<= 8;
                    c = i + n5 < n2 ? dataInputStream.readByte() : '\u0001';
                    l |= (long)c & 255;
                }
                for (n5 = 0; n5 < 8; ++n5) {
                    c = (char)(l >>> 7 * (7 - n5) & 127);
                    if (c == '\n') {
                        stringBuffer.append("\\n");
                        continue;
                    }
                    if (c == '\r') {
                        stringBuffer.append("\\r");
                        continue;
                    }
                    if (c == '\\') {
                        stringBuffer.append("\\\\");
                        continue;
                    }
                    if (c == '\"') {
                        stringBuffer.append("\\\"");
                        continue;
                    }
                    if (c >= ' ' && c <= '~') {
                        stringBuffer.append(c);
                        continue;
                    }
                    stringBuffer.append("\\" + JavaSourceCompiler.toOctal3(c));
                }
            }
            String string = "_data" + ++this.initDataCount;
            this.p("private static final int[] " + string + " = decodeData(\"" + stringBuffer.toString() + "\"," + JavaSourceCompiler.toHex(n4 / 4) + ");");
            this.inits.append(indents[2] + "initPages(" + string + "," + JavaSourceCompiler.toHex(n) + "," + (bl ? "true" : "false") + ");\n");
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
        this.inits.append(indents[2] + "clearPages(" + JavaSourceCompiler.toHex(n) + "," + JavaSourceCompiler.toHex(n3) + ");\n");
    }

    private void emitInstruction(int n, int n2, int n3) throws IOException, Compiler.Exn {
        int n4;
        if (n2 == -1) {
            throw new Error("insn is -1");
        }
        int n5 = n2 >>> 26 & 255;
        int n6 = n2 >>> 21 & 31;
        int n7 = n2 >>> 16 & 31;
        int n8 = n2 >>> 16 & 31;
        int n9 = n2 >>> 11 & 31;
        int n10 = n2 >>> 11 & 31;
        int n11 = n2 >>> 6 & 31;
        int n12 = n2 >>> 6 & 31;
        int n13 = n2 & 63;
        int n14 = n2 & 67108863;
        int n15 = n2 & 65535;
        int n16 = n4 = n2 << 16 >> 16;
        if (n == -1) {
            this.p("/* Next insn is delay slot */ ");
        }
        if (this.runtimeStats && n5 != 0) {
            this.p("inc(\"opcode: " + n5 + "\");");
        }
        block0 : switch (n5) {
            case 0: {
                if (this.runtimeStats && n2 != 0) {
                    this.p("inc(\"opcode: 0/" + n13 + "\");");
                }
                switch (n13) {
                    case 0: {
                        if (n2 == 0) break block0;
                        this.p("r" + n9 + " = r" + n7 + " << " + n11 + ";");
                        break block0;
                    }
                    case 2: {
                        this.p("r" + n9 + " = r" + n7 + " >>> " + n11 + ";");
                        break block0;
                    }
                    case 3: {
                        this.p("r" + n9 + " = r" + n7 + " >> " + n11 + ";");
                        break block0;
                    }
                    case 4: {
                        this.p("r" + n9 + " = r" + n7 + " << (r" + n6 + "&0x1f);");
                        break block0;
                    }
                    case 6: {
                        this.p("r" + n9 + " = r" + n7 + " >>> (r" + n6 + "&0x1f);");
                        break block0;
                    }
                    case 7: {
                        this.p("r" + n9 + " = r" + n7 + " >> (r" + n6 + "&0x1f);");
                        break block0;
                    }
                    case 8: {
                        if (n == -1) {
                            throw new Error("pc modifying insn in delay slot");
                        }
                        this.emitInstruction(-1, n3, -1);
                        if (this.debugCompiler) {
                            this.p("lastPC = " + JavaSourceCompiler.toHex(n) + ";");
                        }
                        this.p("pc=r" + n6 + ";");
                        this.leaveMethod();
                        this.unreachable = true;
                        break block0;
                    }
                    case 9: {
                        if (n == -1) {
                            throw new Error("pc modifying insn in delay slot");
                        }
                        this.emitInstruction(-1, n3, -1);
                        if (this.debugCompiler) {
                            this.p("lastPC = " + JavaSourceCompiler.toHex(n) + ";");
                        }
                        this.p("pc=r" + n6 + ";");
                        this.p("r31=" + this.constant(n + 8) + ";");
                        this.leaveMethod();
                        this.unreachable = true;
                        break block0;
                    }
                    case 12: {
                        this.p("pc = " + JavaSourceCompiler.toHex(n) + ";");
                        this.p("r2 = syscall(r2,r4,r5,r6,r7,r8,r9);");
                        this.p("if (state != RUNNING) {");
                        ++this.indent;
                        this.p("pc = " + JavaSourceCompiler.toHex(n + 4) + ";");
                        this.leaveMethod();
                        --this.indent;
                        this.p("}");
                        break block0;
                    }
                    case 13: {
                        this.p("throw new ExecutionException(\"Break\");");
                        this.unreachable = true;
                        break block0;
                    }
                    case 16: {
                        this.p("r" + n9 + " = hi;");
                        break block0;
                    }
                    case 17: {
                        this.p("hi = r" + n6 + ";");
                        break block0;
                    }
                    case 18: {
                        this.p("r" + n9 + " = lo;");
                        break block0;
                    }
                    case 19: {
                        this.p("lo = r" + n6 + ";");
                        break block0;
                    }
                    case 24: {
                        this.p("{ long hilo = (long)(r" + n6 + ") * ((long)r" + n7 + "); " + "hi = (int) (hilo >>> 32); " + "lo = (int) hilo; }");
                        break block0;
                    }
                    case 25: {
                        this.p("{ long hilo = (r" + n6 + " & 0xffffffffL) * (r" + n7 + " & 0xffffffffL); " + "hi = (int) (hilo >>> 32); " + "lo = (int) hilo; } ");
                        break block0;
                    }
                    case 26: {
                        this.p("hi = r" + n6 + "%r" + n7 + "; lo = r" + n6 + "/r" + n7 + ";");
                        break block0;
                    }
                    case 27: {
                        this.p("if(r" + n7 + "!=0) {");
                        this.p("hi = (int)((r" + n6 + " & 0xffffffffL) % (r" + n7 + " & 0xffffffffL)); " + "lo = (int)((r" + n6 + " & 0xffffffffL) / (r" + n7 + " & 0xffffffffL));");
                        this.p("}");
                        break block0;
                    }
                    case 32: {
                        throw new Compiler.Exn("ADD (add with oveflow trap) not suported");
                    }
                    case 33: {
                        this.p("r" + n9 + " = r" + n6 + " + r" + n7 + ";");
                        break block0;
                    }
                    case 34: {
                        throw new Compiler.Exn("SUB (add with oveflow trap) not suported");
                    }
                    case 35: {
                        this.p("r" + n9 + " = r" + n6 + " - r" + n7 + ";");
                        break block0;
                    }
                    case 36: {
                        this.p("r" + n9 + " = r" + n6 + " & r" + n7 + ";");
                        break block0;
                    }
                    case 37: {
                        this.p("r" + n9 + " = r" + n6 + " | r" + n7 + ";");
                        break block0;
                    }
                    case 38: {
                        this.p("r" + n9 + " = r" + n6 + " ^ r" + n7 + ";");
                        break block0;
                    }
                    case 39: {
                        this.p("r" + n9 + " = ~(r" + n6 + " | r" + n7 + ");");
                        break block0;
                    }
                    case 42: {
                        this.p("r" + n9 + " = r" + n6 + " < r" + n7 + " ? 1 : 0;");
                        break block0;
                    }
                    case 43: {
                        this.p("r" + n9 + " = ((r" + n6 + " & 0xffffffffL) < (r" + n7 + " & 0xffffffffL)) ? 1 : 0;");
                        break block0;
                    }
                }
                throw new RuntimeException("Illegal instruction 0/" + n13);
            }
            case 1: {
                switch (n7) {
                    case 0: {
                        if (n == -1) {
                            throw new Error("pc modifying insn in delay slot");
                        }
                        this.p("if(r" + n6 + " < 0) {");
                        ++this.indent;
                        this.emitInstruction(-1, n3, -1);
                        this.branch(n, n + n16 * 4 + 4);
                        --this.indent;
                        this.p("}");
                        break block0;
                    }
                    case 1: {
                        if (n == -1) {
                            throw new Error("pc modifying insn in delay slot");
                        }
                        this.p("if(r" + n6 + " >= 0) {");
                        ++this.indent;
                        this.emitInstruction(-1, n3, -1);
                        this.branch(n, n + n16 * 4 + 4);
                        --this.indent;
                        this.p("}");
                        break block0;
                    }
                    case 16: {
                        if (n == -1) {
                            throw new Error("pc modifying insn in delay slot");
                        }
                        this.p("if(r" + n6 + " < 0) {");
                        ++this.indent;
                        this.emitInstruction(-1, n3, -1);
                        this.p("r31=" + this.constant(n + 8) + ";");
                        this.branch(n, n + n16 * 4 + 4);
                        --this.indent;
                        this.p("}");
                        break block0;
                    }
                    case 17: {
                        if (n == -1) {
                            throw new Error("pc modifying insn in delay slot");
                        }
                        this.p("if(r" + n6 + " >= 0) {");
                        ++this.indent;
                        this.emitInstruction(-1, n3, -1);
                        this.p("r31=" + this.constant(n + 8) + ";");
                        this.branch(n, n + n16 * 4 + 4);
                        --this.indent;
                        this.p("}");
                        break block0;
                    }
                }
                throw new RuntimeException("Illegal Instruction 1/" + n7);
            }
            case 2: {
                if (n == -1) {
                    throw new Error("pc modifying insn in delay slot");
                }
                this.emitInstruction(-1, n3, -1);
                this.branch(n, n & -268435456 | n14 << 2);
                this.unreachable = true;
                break;
            }
            case 3: {
                if (n == -1) {
                    throw new Error("pc modifying insn in delay slot");
                }
                int n17 = n & -268435456 | n14 << 2;
                this.emitInstruction(-1, n3, -1);
                this.p("r31=" + this.constant(n + 8) + ";");
                this.branch(n, n17);
                this.unreachable = true;
                break;
            }
            case 4: {
                if (n == -1) {
                    throw new Error("pc modifying insn in delay slot");
                }
                this.p("if(r" + n6 + " == r" + n7 + ") {");
                ++this.indent;
                this.emitInstruction(-1, n3, -1);
                this.branch(n, n + n16 * 4 + 4);
                --this.indent;
                this.p("}");
                break;
            }
            case 5: {
                if (n == -1) {
                    throw new Error("pc modifying insn in delay slot");
                }
                this.p("if(r" + n6 + " != r" + n7 + ") {");
                ++this.indent;
                this.emitInstruction(-1, n3, -1);
                this.branch(n, n + n16 * 4 + 4);
                --this.indent;
                this.p("}");
                break;
            }
            case 6: {
                if (n == -1) {
                    throw new Error("pc modifying insn in delay slot");
                }
                this.p("if(r" + n6 + " <= 0) {");
                ++this.indent;
                this.emitInstruction(-1, n3, -1);
                this.branch(n, n + n16 * 4 + 4);
                --this.indent;
                this.p("}");
                break;
            }
            case 7: {
                if (n == -1) {
                    throw new Error("pc modifying insn in delay slot");
                }
                this.p("if(r" + n6 + " > 0) {");
                ++this.indent;
                this.emitInstruction(-1, n3, -1);
                this.branch(n, n + n16 * 4 + 4);
                --this.indent;
                this.p("}");
                break;
            }
            case 8: {
                this.p("r" + n7 + " = r" + n6 + " + " + n4 + ";");
                break;
            }
            case 9: {
                this.p("r" + n7 + " = r" + n6 + " + " + n4 + ";");
                break;
            }
            case 10: {
                this.p("r" + n7 + " = r" + n6 + " < " + n4 + " ? 1 : 0;");
                break;
            }
            case 11: {
                this.p("r" + n7 + " = (r" + n6 + "&0xffffffffL) < (" + n4 + "&0xffffffffL) ? 1 : 0;");
                break;
            }
            case 12: {
                this.p("r" + n7 + " = r" + n6 + " & " + n15 + ";");
                break;
            }
            case 13: {
                this.p("r" + n7 + " = r" + n6 + " | " + n15 + ";");
                break;
            }
            case 14: {
                this.p("r" + n7 + " = r" + n6 + " ^ " + n15 + ";");
                break;
            }
            case 15: {
                this.p("r" + n7 + " = " + n15 + " << 16;");
                break;
            }
            case 16: {
                throw new Compiler.Exn("TLB/Exception support not implemented");
            }
            case 17: {
                switch (n6) {
                    case 0: {
                        this.p("r" + n7 + " = f" + n9 + ";");
                        break block0;
                    }
                    case 2: {
                        if (n10 != 31) {
                            throw new Compiler.Exn("FCR " + n10 + " unavailable");
                        }
                        this.p("r" + n7 + " = fcsr;");
                        break block0;
                    }
                    case 4: {
                        this.p("f" + n9 + " = r" + n7 + ";");
                        break block0;
                    }
                    case 6: {
                        if (n10 != 31) {
                            throw new Compiler.Exn("FCR " + n10 + " unavailable");
                        }
                        this.p("fcsr = r" + n7 + ";");
                        break block0;
                    }
                    case 8: {
                        int n18 = n2 >>> 16 & 1;
                        this.p("if(((fcsr&0x800000)!=0) == (" + n18 + "!=0)) {");
                        ++this.indent;
                        this.emitInstruction(-1, n3, -1);
                        this.branch(n, n + n16 * 4 + 4);
                        --this.indent;
                        this.p("}");
                        break block0;
                    }
                    case 16: {
                        switch (n13) {
                            case 0: {
                                this.p(JavaSourceCompiler.setFloat(n12, JavaSourceCompiler.getFloat(n10) + "+" + JavaSourceCompiler.getFloat(n8)));
                                break block0;
                            }
                            case 1: {
                                this.p(JavaSourceCompiler.setFloat(n12, JavaSourceCompiler.getFloat(n10) + "-" + JavaSourceCompiler.getFloat(n8)));
                                break block0;
                            }
                            case 2: {
                                this.p(JavaSourceCompiler.setFloat(n12, JavaSourceCompiler.getFloat(n10) + "*" + JavaSourceCompiler.getFloat(n8)));
                                break block0;
                            }
                            case 3: {
                                this.p(JavaSourceCompiler.setFloat(n12, JavaSourceCompiler.getFloat(n10) + "/" + JavaSourceCompiler.getFloat(n8)));
                                break block0;
                            }
                            case 5: {
                                this.p(JavaSourceCompiler.setFloat(n12, "Math.abs(" + JavaSourceCompiler.getFloat(n10) + ")"));
                                break block0;
                            }
                            case 6: {
                                this.p("f" + n12 + " = f" + n10 + "; // MOV.S");
                                break block0;
                            }
                            case 7: {
                                this.p(JavaSourceCompiler.setFloat(n12, "-" + JavaSourceCompiler.getFloat(n10)));
                                break block0;
                            }
                            case 33: {
                                this.p(JavaSourceCompiler.setDouble(n12, "(float)" + JavaSourceCompiler.getFloat(n10)));
                                break block0;
                            }
                            case 36: {
                                this.p("switch(fcsr & 3) {");
                                ++this.indent;
                                this.p("case 0: f" + n12 + " = (int)Math.floor(" + JavaSourceCompiler.getFloat(n10) + "+0.5); break; // Round to nearest");
                                this.p("case 1: f" + n12 + " = (int)" + JavaSourceCompiler.getFloat(n10) + "; break; // Round towards zero");
                                this.p("case 2: f" + n12 + " = (int)Math.ceil(" + JavaSourceCompiler.getFloat(n10) + "); break; // Round towards plus infinity");
                                this.p("case 3: f" + n12 + " = (int)Math.floor(" + JavaSourceCompiler.getFloat(n10) + "); break; // Round towards minus infinity");
                                --this.indent;
                                this.p("}");
                                break block0;
                            }
                            case 50: {
                                this.p("fcsr = (fcsr&~0x800000) | ((" + JavaSourceCompiler.getFloat(n10) + "==" + JavaSourceCompiler.getFloat(n8) + ") ? 0x800000 : 0x000000);");
                                break block0;
                            }
                            case 60: {
                                this.p("fcsr = (fcsr&~0x800000) | ((" + JavaSourceCompiler.getFloat(n10) + "<" + JavaSourceCompiler.getFloat(n8) + ") ? 0x800000 : 0x000000);");
                                break block0;
                            }
                            case 62: {
                                this.p("fcsr = (fcsr&~0x800000) | ((" + JavaSourceCompiler.getFloat(n10) + "<=" + JavaSourceCompiler.getFloat(n8) + ") ? 0x800000 : 0x000000);");
                                break block0;
                            }
                        }
                        throw new Compiler.Exn("Invalid Instruction 17/" + n6 + "/" + n13);
                    }
                    case 17: {
                        switch (n13) {
                            case 0: {
                                this.p(JavaSourceCompiler.setDouble(n12, JavaSourceCompiler.getDouble(n10) + "+" + JavaSourceCompiler.getDouble(n8)));
                                break block0;
                            }
                            case 1: {
                                this.p(JavaSourceCompiler.setDouble(n12, JavaSourceCompiler.getDouble(n10) + "-" + JavaSourceCompiler.getDouble(n8)));
                                break block0;
                            }
                            case 2: {
                                this.p(JavaSourceCompiler.setDouble(n12, JavaSourceCompiler.getDouble(n10) + "*" + JavaSourceCompiler.getDouble(n8)));
                                break block0;
                            }
                            case 3: {
                                this.p(JavaSourceCompiler.setDouble(n12, JavaSourceCompiler.getDouble(n10) + "/" + JavaSourceCompiler.getDouble(n8)));
                                break block0;
                            }
                            case 5: {
                                this.p(JavaSourceCompiler.setDouble(n12, "Math.abs(" + JavaSourceCompiler.getDouble(n10) + ")"));
                                break block0;
                            }
                            case 6: {
                                this.p("f" + n12 + " = f" + n10 + ";");
                                this.p("f" + (n12 + 1) + " = f" + (n10 + 1) + ";");
                                break block0;
                            }
                            case 7: {
                                this.p(JavaSourceCompiler.setDouble(n12, "-" + JavaSourceCompiler.getDouble(n10)));
                                break block0;
                            }
                            case 32: {
                                this.p(JavaSourceCompiler.setFloat(n12, "(float)" + JavaSourceCompiler.getDouble(n10)));
                                break block0;
                            }
                            case 36: {
                                this.p("switch(fcsr & 3) {");
                                ++this.indent;
                                this.p("case 0: f" + n12 + " = (int)Math.floor(" + JavaSourceCompiler.getDouble(n10) + "+0.5); break; // Round to nearest");
                                this.p("case 1: f" + n12 + " = (int)" + JavaSourceCompiler.getDouble(n10) + "; break; // Round towards zero");
                                this.p("case 2: f" + n12 + " = (int)Math.ceil(" + JavaSourceCompiler.getDouble(n10) + "); break; // Round towards plus infinity");
                                this.p("case 3: f" + n12 + " = (int)Math.floor(" + JavaSourceCompiler.getDouble(n10) + "); break; // Round towards minus infinity");
                                --this.indent;
                                this.p("}");
                                break block0;
                            }
                            case 50: {
                                this.p("fcsr = (fcsr&~0x800000) | ((" + JavaSourceCompiler.getDouble(n10) + "==" + JavaSourceCompiler.getDouble(n8) + ") ? 0x800000 : 0x000000);");
                                break block0;
                            }
                            case 60: {
                                this.p("fcsr = (fcsr&~0x800000) | ((" + JavaSourceCompiler.getDouble(n10) + "<" + JavaSourceCompiler.getDouble(n8) + ") ? 0x800000 : 0x000000);");
                                break block0;
                            }
                            case 62: {
                                this.p("fcsr = (fcsr&~0x800000) | ((" + JavaSourceCompiler.getDouble(n10) + "<=" + JavaSourceCompiler.getDouble(n8) + ") ? 0x800000 : 0x000000);");
                                break block0;
                            }
                        }
                        throw new Compiler.Exn("Invalid Instruction 17/" + n6 + "/" + n13);
                    }
                    case 20: {
                        switch (n13) {
                            case 32: {
                                this.p(" // CVS.S.W");
                                this.p(JavaSourceCompiler.setFloat(n12, "((float)f" + n10 + ")"));
                                break block0;
                            }
                            case 33: {
                                this.p(JavaSourceCompiler.setDouble(n12, "((double)f" + n10 + ")"));
                                break block0;
                            }
                        }
                        throw new Compiler.Exn("Invalid Instruction 17/" + n6 + "/" + n13);
                    }
                }
                throw new Compiler.Exn("Invalid Instruction 17/" + n6);
            }
            case 18: 
            case 19: {
                throw new Compiler.Exn("coprocessor 2 and 3 instructions not available");
            }
            case 32: {
                if (this.runtimeStats) {
                    this.p("inc(\"LB\");");
                }
                this.p("addr=r" + n6 + "+" + n4 + ";");
                this.memRead("addr", "tmp");
                this.p("tmp = (tmp>>>(((~addr)&3)<<3)) & 0xff;");
                this.p("if((tmp&0x80)!=0) tmp |= 0xffffff00; /* sign extend */");
                this.p("r" + n7 + " = tmp;");
                break;
            }
            case 33: {
                if (this.runtimeStats) {
                    this.p("inc(\"LH\");");
                }
                this.p("addr=r" + n6 + "+" + n4 + ";");
                this.memRead("addr", "tmp");
                this.p("tmp = (tmp>>>(((~addr)&2)<<3)) & 0xffff;");
                this.p("if((tmp&0x8000)!=0) tmp |= 0xffff0000; /* sign extend */");
                this.p("r" + n7 + " = tmp;");
                break;
            }
            case 34: {
                this.p("addr=r" + n6 + "+" + n4 + ";");
                this.memRead("addr", "tmp");
                this.p("r" + n7 + " = (r" + n7 + "&(0x00ffffff>>>(((~addr)&3)<<3)))|(tmp<<((addr&3)<<3));");
                break;
            }
            case 35: {
                if (this.runtimeStats) {
                    this.p("inc(\"LW\");");
                }
                this.memRead("r" + n6 + "+" + n4, "r" + n7);
                break;
            }
            case 36: {
                this.p("addr=r" + n6 + "+" + n4 + ";");
                this.memRead("addr", "tmp");
                this.p("tmp = (tmp>>>(((~addr)&3)<<3)) & 0xff;");
                this.p("r" + n7 + " = tmp;");
                break;
            }
            case 37: {
                this.p("addr=r" + n6 + "+" + n4 + ";");
                this.memRead("addr", "tmp");
                this.p("tmp = (tmp>>>(((~addr)&2)<<3)) & 0xffff;");
                this.p("r" + n7 + " = tmp;");
                break;
            }
            case 38: {
                this.p("addr=r" + n6 + "+" + n4 + ";");
                this.memRead("addr", "tmp");
                this.p("r" + n7 + " = (r" + n7 + "&(0xffffff00<<((addr&3)<<3)))|(tmp>>>(((~addr)&3)<<3));");
                break;
            }
            case 40: {
                if (this.runtimeStats) {
                    this.p("inc(\"SB\");");
                }
                this.p("addr=r" + n6 + "+" + n4 + ";");
                this.memRead("addr", "tmp");
                this.p("tmp = (tmp&~(0xff000000>>>((addr&3)<<3)))|((r" + n7 + "&0xff)<<(((~addr)&3)<<3));");
                this.memWrite("addr", "tmp");
                break;
            }
            case 41: {
                if (this.runtimeStats) {
                    this.p("inc(\"SH\");");
                }
                this.p("addr=r" + n6 + "+" + n4 + ";");
                this.memRead("addr", "tmp");
                this.p("tmp = (tmp&(0xffff<<((addr&2)<<3)))|((r" + n7 + "&0xffff)<<(((~addr)&2)<<3));");
                this.memWrite("addr", "tmp");
                break;
            }
            case 42: {
                this.p(" // SWL");
                this.p("addr=r" + n6 + "+" + n4 + ";");
                this.memRead("addr", "tmp");
                this.p("tmp = (tmp&(0xffffff00<<(((~addr)&3)<<3)))|(r" + n7 + ">>>((addr&3)<<3));");
                this.memWrite("addr", "tmp");
                break;
            }
            case 43: {
                if (this.runtimeStats) {
                    this.p("inc(\"SW\");");
                }
                this.memWrite("r" + n6 + "+" + n4, "r" + n7);
                break;
            }
            case 46: {
                this.p(" // SWR");
                this.p("addr=r" + n6 + "+" + n4 + ";");
                this.memRead("addr", "tmp");
                this.p("tmp = (tmp&(0x00ffffff>>>((addr&3)<<3)))|(r" + n7 + "<<(((~addr)&3)<<3));");
                this.memWrite("addr", "tmp");
                break;
            }
            case 48: {
                this.memRead("r" + n6 + "+" + n4, "r" + n7);
                break;
            }
            case 49: {
                this.memRead("r" + n6 + "+" + n4, "f" + n7);
                break;
            }
            case 56: {
                this.memWrite("r" + n6 + "+" + n4, "r" + n7);
                this.p("r" + n7 + "=1;");
                break;
            }
            case 57: {
                this.memWrite("r" + n6 + "+" + n4, "f" + n7);
                break;
            }
            default: {
                throw new Compiler.Exn("Invalid Instruction: " + n5 + " at " + JavaSourceCompiler.toHex(n));
            }
        }
    }

    private void memWrite(String string, String string2) {
        if (this.nullPointerCheck) {
            this.p("nullPointerCheck(" + string + ");");
        }
        if (this.onePage) {
            this.p("page[(" + string + ")>>>2] = " + string2 + ";");
        } else if (this.fastMem) {
            this.p("writePages[(" + string + ")>>>" + this.pageShift + "][((" + string + ")>>>2)&" + JavaSourceCompiler.toHex((this.pageSize >> 2) - 1) + "] = " + string2 + ";");
        } else {
            this.p("unsafeMemWrite(" + string + "," + string2 + ");");
        }
    }

    private void memRead(String string, String string2) {
        if (this.nullPointerCheck) {
            this.p("nullPointerCheck(" + string + ");");
        }
        if (this.onePage) {
            this.p(string2 + "= page[(" + string + ")>>>2];");
        } else if (this.fastMem) {
            this.p(string2 + " = readPages[(" + string + ")>>>" + this.pageShift + "][((" + string + ")>>>2)&" + JavaSourceCompiler.toHex((this.pageSize >> 2) - 1) + "];");
        } else {
            this.p(string2 + " = unsafeMemRead(" + string + ");");
        }
    }

    private static String getFloat(int n) {
        return "(Float.intBitsToFloat(f" + n + "))";
    }

    private static String getDouble(int n) {
        return "(Double.longBitsToDouble(((f" + (n + 1) + "&0xffffffffL) << 32) | (f" + n + "&0xffffffffL)))";
    }

    private static String setFloat(int n, String string) {
        return "f" + n + "=Float.floatToRawIntBits(" + string + ");";
    }

    private static String setDouble(int n, String string) {
        return "{ long l = Double.doubleToLongBits(" + string + "); " + "f" + (n + 1) + " = (int)(l >>> 32); f" + n + " = (int)l; }";
    }

    static {
        String string = "";
        for (int i = 0; i < indents.length; ++i) {
            JavaSourceCompiler.indents[i] = string;
            string = string + "    ";
        }
    }
}

