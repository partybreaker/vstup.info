/*
 * Decompiled with CFR 0_114.
 */
package org.ibex.nestedvm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import org.ibex.nestedvm.ClassFileCompiler;
import org.ibex.nestedvm.Compiler;
import org.ibex.nestedvm.UnixRuntime;
import org.ibex.nestedvm.util.Seekable;

public class RuntimeCompiler {
    public static Class compile(Seekable seekable) throws IOException, Compiler.Exn {
        return RuntimeCompiler.compile(seekable, null);
    }

    public static Class compile(Seekable seekable, String string) throws IOException, Compiler.Exn {
        return RuntimeCompiler.compile(seekable, string, null);
    }

    public static Class compile(Seekable seekable, String string, String string2) throws IOException, Compiler.Exn {
        byte[] arrby;
        String string3 = "nestedvm.runtimecompiled";
        try {
            arrby = RuntimeCompiler.runCompiler(seekable, string3, string, string2, null);
        }
        catch (Compiler.Exn var5_5) {
            if (var5_5.getMessage() != null || var5_5.getMessage().indexOf("constant pool full") != -1) {
                arrby = RuntimeCompiler.runCompiler(seekable, string3, string, string2, "lessconstants");
            }
            throw var5_5;
        }
        return new SingleClassLoader().fromBytes(string3, arrby);
    }

    private static byte[] runCompiler(Seekable seekable, String string, String string2, String string3, String string4) throws IOException, Compiler.Exn {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ClassFileCompiler classFileCompiler = new ClassFileCompiler(seekable, string, (OutputStream)byteArrayOutputStream);
            classFileCompiler.parseOptions("nosupportcall,maxinsnpermethod=256");
            classFileCompiler.setSource(string3);
            if (string2 != null) {
                classFileCompiler.parseOptions(string2);
            }
            if (string4 != null) {
                classFileCompiler.parseOptions(string4);
            }
            classFileCompiler.go();
        }
        finally {
            seekable.seek(0);
        }
        byteArrayOutputStream.close();
        return byteArrayOutputStream.toByteArray();
    }

    public static void main(String[] arrstring) throws Exception {
        if (arrstring.length == 0) {
            System.err.println("Usage: RuntimeCompiler mipsbinary");
            System.exit(1);
        }
        UnixRuntime unixRuntime = (UnixRuntime)RuntimeCompiler.compile(new Seekable.File(arrstring[0]), "unixruntime").newInstance();
        System.err.println("Instansiated: " + unixRuntime);
        System.exit(UnixRuntime.runAndExec(unixRuntime, arrstring));
    }

    private RuntimeCompiler() {
    }

    private static class SingleClassLoader
    extends ClassLoader {
        private SingleClassLoader() {
        }

        public Class loadClass(String string, boolean bl) throws ClassNotFoundException {
            return super.loadClass(string, bl);
        }

        public Class fromBytes(String string, byte[] arrby) {
            return this.fromBytes(string, arrby, 0, arrby.length);
        }

        public Class fromBytes(String string, byte[] arrby, int n, int n2) {
            Class class_ = super.defineClass(string, arrby, n, n2);
            this.resolveClass(class_);
            return class_;
        }
    }

}

