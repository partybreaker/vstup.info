/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.helper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

public final class DataUtil {
    private static final Pattern charsetPattern = Pattern.compile("(?i)\\bcharset=\\s*(?:\"|')?([^\\s,;\"']*)");
    static final String defaultCharset = "UTF-8";
    private static final int bufferSize = 131072;
    private static final int UNICODE_BOM = 65279;
    private static final char[] mimeBoundaryChars = "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    static final int boundaryLength = 32;

    private DataUtil() {
    }

    public static Document load(File in, String charsetName, String baseUri) throws IOException {
        ByteBuffer byteData = DataUtil.readFileToByteBuffer(in);
        return DataUtil.parseByteData(byteData, charsetName, baseUri, Parser.htmlParser());
    }

    public static Document load(InputStream in, String charsetName, String baseUri) throws IOException {
        ByteBuffer byteData = DataUtil.readToByteBuffer(in);
        return DataUtil.parseByteData(byteData, charsetName, baseUri, Parser.htmlParser());
    }

    public static Document load(InputStream in, String charsetName, String baseUri, Parser parser) throws IOException {
        ByteBuffer byteData = DataUtil.readToByteBuffer(in);
        return DataUtil.parseByteData(byteData, charsetName, baseUri, parser);
    }

    static void crossStreams(InputStream in, OutputStream out) throws IOException {
        int len;
        byte[] buffer = new byte[131072];
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
    }

    static Document parseByteData(ByteBuffer byteData, String charsetName, String baseUri, Parser parser) {
        String docData;
        Document doc = null;
        if (charsetName == null) {
            docData = Charset.forName("UTF-8").decode(byteData).toString();
            doc = parser.parseInput(docData, baseUri);
            Element meta = doc.select("meta[http-equiv=content-type], meta[charset]").first();
            if (meta != null) {
                String foundCharset = null;
                if (meta.hasAttr("http-equiv")) {
                    foundCharset = DataUtil.getCharsetFromContentType(meta.attr("content"));
                }
                if (foundCharset == null && meta.hasAttr("charset")) {
                    try {
                        if (Charset.isSupported(meta.attr("charset"))) {
                            foundCharset = meta.attr("charset");
                        }
                    }
                    catch (IllegalCharsetNameException e) {
                        foundCharset = null;
                    }
                }
                if (foundCharset != null && foundCharset.length() != 0 && !foundCharset.equals("UTF-8")) {
                    charsetName = foundCharset = foundCharset.trim().replaceAll("[\"']", "");
                    byteData.rewind();
                    docData = Charset.forName(foundCharset).decode(byteData).toString();
                    doc = null;
                }
            }
        } else {
            Validate.notEmpty(charsetName, "Must set charset arg to character set of file to parse. Set to null to attempt to detect from HTML");
            docData = Charset.forName(charsetName).decode(byteData).toString();
        }
        if (docData.length() > 0 && docData.charAt(0) == '\ufeff') {
            byteData.rewind();
            docData = Charset.forName("UTF-8").decode(byteData).toString();
            docData = docData.substring(1);
            charsetName = "UTF-8";
            doc = null;
        }
        if (doc == null) {
            doc = parser.parseInput(docData, baseUri);
            doc.outputSettings().charset(charsetName);
        }
        return doc;
    }

    static ByteBuffer readToByteBuffer(InputStream inStream, int maxSize) throws IOException {
        int read;
        Validate.isTrue(maxSize >= 0, "maxSize must be 0 (unlimited) or larger");
        boolean capped = maxSize > 0;
        byte[] buffer = new byte[131072];
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(131072);
        int remaining = maxSize;
        while ((read = inStream.read(buffer)) != -1) {
            if (capped) {
                if (read > remaining) {
                    outStream.write(buffer, 0, remaining);
                    break;
                }
                remaining -= read;
            }
            outStream.write(buffer, 0, read);
        }
        ByteBuffer byteData = ByteBuffer.wrap(outStream.toByteArray());
        return byteData;
    }

    static ByteBuffer readToByteBuffer(InputStream inStream) throws IOException {
        return DataUtil.readToByteBuffer(inStream, 0);
    }

    static ByteBuffer readFileToByteBuffer(File file) throws IOException {
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "r");
            byte[] bytes = new byte[(int)randomAccessFile.length()];
            randomAccessFile.readFully(bytes);
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
            return byteBuffer;
        }
        finally {
            if (randomAccessFile != null) {
                randomAccessFile.close();
            }
        }
    }

    static ByteBuffer emptyByteBuffer() {
        return ByteBuffer.allocate(0);
    }

    static String getCharsetFromContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        Matcher m = charsetPattern.matcher(contentType);
        if (m.find()) {
            String charset = m.group(1).trim();
            if ((charset = charset.replace("charset=", "")).length() == 0) {
                return null;
            }
            try {
                if (Charset.isSupported(charset)) {
                    return charset;
                }
                if (Charset.isSupported(charset = charset.toUpperCase(Locale.ENGLISH))) {
                    return charset;
                }
            }
            catch (IllegalCharsetNameException e) {
                return null;
            }
        }
        return null;
    }

    static String mimeBoundary() {
        StringBuilder mime = new StringBuilder(32);
        Random rand = new Random();
        for (int i = 0; i < 32; ++i) {
            mime.append(mimeBoundaryChars[rand.nextInt(mimeBoundaryChars.length)]);
        }
        return mime.toString();
    }
}

