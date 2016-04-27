/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

public interface Connection {
    public Connection url(URL var1);

    public Connection url(String var1);

    public Connection userAgent(String var1);

    public Connection timeout(int var1);

    public Connection maxBodySize(int var1);

    public Connection referrer(String var1);

    public Connection followRedirects(boolean var1);

    public Connection method(Method var1);

    public Connection ignoreHttpErrors(boolean var1);

    public Connection ignoreContentType(boolean var1);

    public Connection validateTLSCertificates(boolean var1);

    public Connection data(String var1, String var2);

    public Connection data(String var1, String var2, InputStream var3);

    public Connection data(Collection<KeyVal> var1);

    public Connection data(Map<String, String> var1);

    public /* varargs */ Connection data(String ... var1);

    public Connection header(String var1, String var2);

    public Connection cookie(String var1, String var2);

    public Connection cookies(Map<String, String> var1);

    public Connection parser(Parser var1);

    public Connection postDataCharset(String var1);

    public Document get() throws IOException;

    public Document post() throws IOException;

    public Response execute() throws IOException;

    public Request request();

    public Connection request(Request var1);

    public Response response();

    public Connection response(Response var1);

    public static interface KeyVal {
        public KeyVal key(String var1);

        public String key();

        public KeyVal value(String var1);

        public String value();

        public KeyVal inputStream(InputStream var1);

        public InputStream inputStream();

        public boolean hasInputStream();
    }

    public static interface Response
    extends Base<Response> {
        public int statusCode();

        public String statusMessage();

        public String charset();

        public String contentType();

        public Document parse() throws IOException;

        public String body();

        public byte[] bodyAsBytes();
    }

    public static interface Request
    extends Base<Request> {
        public int timeout();

        public Request timeout(int var1);

        public int maxBodySize();

        public Request maxBodySize(int var1);

        public boolean followRedirects();

        public Request followRedirects(boolean var1);

        public boolean ignoreHttpErrors();

        public Request ignoreHttpErrors(boolean var1);

        public boolean ignoreContentType();

        public Request ignoreContentType(boolean var1);

        public boolean validateTLSCertificates();

        public void validateTLSCertificates(boolean var1);

        public Request data(KeyVal var1);

        public Collection<KeyVal> data();

        public Request parser(Parser var1);

        public Parser parser();

        public Request postDataCharset(String var1);

        public String postDataCharset();
    }

    public static interface Base<T extends Base> {
        public URL url();

        public T url(URL var1);

        public Method method();

        public T method(Method var1);

        public String header(String var1);

        public T header(String var1, String var2);

        public boolean hasHeader(String var1);

        public boolean hasHeaderWithValue(String var1, String var2);

        public T removeHeader(String var1);

        public Map<String, String> headers();

        public String cookie(String var1);

        public T cookie(String var1, String var2);

        public boolean hasCookie(String var1);

        public T removeCookie(String var1);

        public Map<String, String> cookies();
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static enum Method {
        GET(false),
        POST(true),
        PUT(true),
        DELETE(false),
        PATCH(true);
        
        private final boolean hasBody;

        private Method(boolean hasBody) {
            this.hasBody = hasBody;
        }

        public final boolean hasBody() {
            return this.hasBody;
        }
    }

}

