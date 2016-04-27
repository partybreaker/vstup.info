/*
 * Decompiled with CFR 0_114.
 */
package org.sqlite;

import java.net.URL;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class ResourceFinder {
    public static URL find(Class<?> referenceClass, String resourceFileName) {
        return ResourceFinder.find(referenceClass.getClassLoader(), referenceClass.getPackage(), resourceFileName);
    }

    public static URL find(ClassLoader classLoader, Package basePackage, String resourceFileName) {
        return ResourceFinder.find(classLoader, basePackage.getName(), resourceFileName);
    }

    public static URL find(ClassLoader classLoader, String packageName, String resourceFileName) {
        String packagePath = ResourceFinder.packagePath(packageName);
        String resourcePath = packagePath + resourceFileName;
        if (!resourcePath.startsWith("/")) {
            resourcePath = "/" + resourcePath;
        }
        return classLoader.getResource(resourcePath);
    }

    private static String packagePath(Class<?> referenceClass) {
        return ResourceFinder.packagePath(referenceClass.getPackage());
    }

    private static String packagePath(Package basePackage) {
        return ResourceFinder.packagePath(basePackage.getName());
    }

    private static String packagePath(String packageName) {
        String packageAsPath = packageName.replaceAll("\\.", "/");
        return packageAsPath.endsWith("/") ? packageAsPath : packageAsPath + "/";
    }
}

