/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.helper;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.helper.DataUtil;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.parser.TokenQueue;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class HttpConnection
implements Connection {
    public static final String CONTENT_ENCODING = "Content-Encoding";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String MULTIPART_FORM_DATA = "multipart/form-data";
    private static final String FORM_URL_ENCODED = "application/x-www-form-urlencoded";
    private Connection.Request req = new Request();
    private Connection.Response res = new Response();

    public static Connection connect(String url) {
        HttpConnection con = new HttpConnection();
        con.url(url);
        return con;
    }

    public static Connection connect(URL url) {
        HttpConnection con = new HttpConnection();
        con.url(url);
        return con;
    }

    private static String encodeUrl(String url) {
        if (url == null) {
            return null;
        }
        return url.replaceAll(" ", "%20");
    }

    private static String encodeMimeName(String val) {
        if (val == null) {
            return null;
        }
        return val.replaceAll("\"", "%22");
    }

    private HttpConnection() {
    }

    @Override
    public Connection url(URL url) {
        this.req.url(url);
        return this;
    }

    @Override
    public Connection url(String url) {
        Validate.notEmpty(url, "Must supply a valid URL");
        try {
            this.req.url(new URL(HttpConnection.encodeUrl(url)));
        }
        catch (MalformedURLException e) {
            throw new IllegalArgumentException("Malformed URL: " + url, e);
        }
        return this;
    }

    @Override
    public Connection userAgent(String userAgent) {
        Validate.notNull(userAgent, "User agent must not be null");
        this.req.header("User-Agent", userAgent);
        return this;
    }

    @Override
    public Connection timeout(int millis) {
        this.req.timeout(millis);
        return this;
    }

    @Override
    public Connection maxBodySize(int bytes) {
        this.req.maxBodySize(bytes);
        return this;
    }

    @Override
    public Connection followRedirects(boolean followRedirects) {
        this.req.followRedirects(followRedirects);
        return this;
    }

    @Override
    public Connection referrer(String referrer) {
        Validate.notNull(referrer, "Referrer must not be null");
        this.req.header("Referer", referrer);
        return this;
    }

    @Override
    public Connection method(Connection.Method method) {
        this.req.method(method);
        return this;
    }

    @Override
    public Connection ignoreHttpErrors(boolean ignoreHttpErrors) {
        this.req.ignoreHttpErrors(ignoreHttpErrors);
        return this;
    }

    @Override
    public Connection ignoreContentType(boolean ignoreContentType) {
        this.req.ignoreContentType(ignoreContentType);
        return this;
    }

    @Override
    public Connection validateTLSCertificates(boolean value) {
        this.req.validateTLSCertificates(value);
        return this;
    }

    @Override
    public Connection data(String key, String value) {
        this.req.data(KeyVal.create(key, value));
        return this;
    }

    @Override
    public Connection data(String key, String filename, InputStream inputStream) {
        this.req.data(KeyVal.create(key, filename, inputStream));
        return this;
    }

    @Override
    public Connection data(Map<String, String> data) {
        Validate.notNull(data, "Data map must not be null");
        for (Map.Entry<String, String> entry : data.entrySet()) {
            this.req.data(KeyVal.create(entry.getKey(), entry.getValue()));
        }
        return this;
    }

    @Override
    public /* varargs */ Connection data(String ... keyvals) {
        Validate.notNull(keyvals, "Data key value pairs must not be null");
        Validate.isTrue(keyvals.length % 2 == 0, "Must supply an even number of key value pairs");
        for (int i = 0; i < keyvals.length; i += 2) {
            String key = keyvals[i];
            String value = keyvals[i + 1];
            Validate.notEmpty(key, "Data key must not be empty");
            Validate.notNull(value, "Data value must not be null");
            this.req.data(KeyVal.create(key, value));
        }
        return this;
    }

    @Override
    public Connection data(Collection<Connection.KeyVal> data) {
        Validate.notNull(data, "Data collection must not be null");
        for (Connection.KeyVal entry : data) {
            this.req.data(entry);
        }
        return this;
    }

    @Override
    public Connection header(String name, String value) {
        this.req.header(name, value);
        return this;
    }

    @Override
    public Connection cookie(String name, String value) {
        this.req.cookie(name, value);
        return this;
    }

    @Override
    public Connection cookies(Map<String, String> cookies) {
        Validate.notNull(cookies, "Cookie map must not be null");
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            this.req.cookie(entry.getKey(), entry.getValue());
        }
        return this;
    }

    @Override
    public Connection parser(Parser parser) {
        this.req.parser(parser);
        return this;
    }

    @Override
    public Document get() throws IOException {
        this.req.method(Connection.Method.GET);
        this.execute();
        return this.res.parse();
    }

    @Override
    public Document post() throws IOException {
        this.req.method(Connection.Method.POST);
        this.execute();
        return this.res.parse();
    }

    @Override
    public Connection.Response execute() throws IOException {
        this.res = Response.execute(this.req);
        return this.res;
    }

    @Override
    public Connection.Request request() {
        return this.req;
    }

    @Override
    public Connection request(Connection.Request request) {
        this.req = request;
        return this;
    }

    @Override
    public Connection.Response response() {
        return this.res;
    }

    @Override
    public Connection response(Connection.Response response) {
        this.res = response;
        return this;
    }

    @Override
    public Connection postDataCharset(String charset) {
        this.req.postDataCharset(charset);
        return this;
    }

    public static class KeyVal
    implements Connection.KeyVal {
        private String key;
        private String value;
        private InputStream stream;

        public static KeyVal create(String key, String value) {
            return new KeyVal().key(key).value(value);
        }

        public static KeyVal create(String key, String filename, InputStream stream) {
            return new KeyVal().key(key).value(filename).inputStream(stream);
        }

        private KeyVal() {
        }

        public KeyVal key(String key) {
            Validate.notEmpty(key, "Data key must not be empty");
            this.key = key;
            return this;
        }

        public String key() {
            return this.key;
        }

        public KeyVal value(String value) {
            Validate.notNull(value, "Data value must not be null");
            this.value = value;
            return this;
        }

        public String value() {
            return this.value;
        }

        public KeyVal inputStream(InputStream inputStream) {
            Validate.notNull(this.value, "Data input stream must not be null");
            this.stream = inputStream;
            return this;
        }

        public InputStream inputStream() {
            return this.stream;
        }

        public boolean hasInputStream() {
            return this.stream != null;
        }

        public String toString() {
            return this.key + "=" + this.value;
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static class Response
    extends Base<Connection.Response>
    implements Connection.Response {
        private static final int MAX_REDIRECTS = 20;
        private static SSLSocketFactory sslSocketFactory;
        private static final String LOCATION = "Location";
        private int statusCode;
        private String statusMessage;
        private ByteBuffer byteData;
        private String charset;
        private String contentType;
        private boolean executed = false;
        private int numRedirects = 0;
        private Connection.Request req;
        private static final Pattern xmlContentTypeRxp;

        Response() {
            super();
        }

        private Response(Response previousResponse) throws IOException {
            super();
            if (previousResponse != null) {
                this.numRedirects = previousResponse.numRedirects + 1;
                if (this.numRedirects >= 20) {
                    throw new IOException(String.format("Too many redirects occurred trying to load URL %s", previousResponse.url()));
                }
            }
        }

        static Response execute(Connection.Request req) throws IOException {
            return Response.execute(req, null);
        }

        static Response execute(Connection.Request req, Response previousResponse) throws IOException {
            Response res;
            Validate.notNull(req, "Request must not be null");
            String protocol = req.url().getProtocol();
            if (!protocol.equals("http") && !protocol.equals("https")) {
                throw new MalformedURLException("Only http & https protocols supported");
            }
            String mimeBoundary = null;
            if (!req.method().hasBody() && req.data().size() > 0) {
                Response.serialiseRequestUrl(req);
            } else if (req.method().hasBody()) {
                mimeBoundary = Response.setOutputContentType(req);
            }
            HttpURLConnection conn = Response.createConnection(req);
            try {
                conn.connect();
                if (conn.getDoOutput()) {
                    Response.writePost(req, conn.getOutputStream(), mimeBoundary);
                }
                int status = conn.getResponseCode();
                res = new Response(previousResponse);
                res.setupFromConnection(conn, previousResponse);
                res.req = req;
                if (res.hasHeader("Location") && req.followRedirects()) {
                    req.method(Connection.Method.GET);
                    req.data().clear();
                    String location = res.header("Location");
                    if (location != null && location.startsWith("http:/") && location.charAt(6) != '/') {
                        location = location.substring(6);
                    }
                    req.url(new URL(req.url(), HttpConnection.encodeUrl(location)));
                    for (Map.Entry cookie : res.cookies.entrySet()) {
                        req.cookie((String)cookie.getKey(), (String)cookie.getValue());
                    }
                    Response i$ = Response.execute(req, res);
                    return i$;
                }
                if (!(status >= 200 && status < 400 || req.ignoreHttpErrors())) {
                    throw new HttpStatusException("HTTP error fetching URL", status, req.url().toString());
                }
                String contentType = res.contentType();
                if (!(contentType == null || req.ignoreContentType() || contentType.startsWith("text/") || contentType.startsWith("application/xml") || xmlContentTypeRxp.matcher(contentType).matches())) {
                    throw new UnsupportedMimeTypeException("Unhandled content type. Must be text/*, application/xml, or application/xhtml+xml", contentType, req.url().toString());
                }
                res.charset = DataUtil.getCharsetFromContentType(res.contentType);
                if (conn.getContentLength() != 0) {
                    InputStream bodyStream = null;
                    InputStream dataStream = null;
                    try {
                        dataStream = conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream();
                        bodyStream = res.hasHeaderWithValue("Content-Encoding", "gzip") ? new BufferedInputStream(new GZIPInputStream(dataStream)) : new BufferedInputStream(dataStream);
                        res.byteData = DataUtil.readToByteBuffer(bodyStream, req.maxBodySize());
                    }
                    finally {
                        if (bodyStream != null) {
                            bodyStream.close();
                        }
                        if (dataStream != null) {
                            dataStream.close();
                        }
                    }
                }
                res.byteData = DataUtil.emptyByteBuffer();
            }
            finally {
                conn.disconnect();
            }
            res.executed = true;
            return res;
        }

        @Override
        public int statusCode() {
            return this.statusCode;
        }

        @Override
        public String statusMessage() {
            return this.statusMessage;
        }

        @Override
        public String charset() {
            return this.charset;
        }

        @Override
        public String contentType() {
            return this.contentType;
        }

        @Override
        public Document parse() throws IOException {
            Validate.isTrue(this.executed, "Request must be executed (with .execute(), .get(), or .post() before parsing response");
            Document doc = DataUtil.parseByteData(this.byteData, this.charset, this.url.toExternalForm(), this.req.parser());
            this.byteData.rewind();
            this.charset = doc.outputSettings().charset().name();
            return doc;
        }

        @Override
        public String body() {
            Validate.isTrue(this.executed, "Request must be executed (with .execute(), .get(), or .post() before getting response body");
            String body = this.charset == null ? Charset.forName("UTF-8").decode(this.byteData).toString() : Charset.forName(this.charset).decode(this.byteData).toString();
            this.byteData.rewind();
            return body;
        }

        @Override
        public byte[] bodyAsBytes() {
            Validate.isTrue(this.executed, "Request must be executed (with .execute(), .get(), or .post() before getting response body");
            return this.byteData.array();
        }

        private static HttpURLConnection createConnection(Connection.Request req) throws IOException {
            HttpURLConnection conn = (HttpURLConnection)req.url().openConnection();
            conn.setRequestMethod(req.method().name());
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(req.timeout());
            conn.setReadTimeout(req.timeout());
            if (conn instanceof HttpsURLConnection && !req.validateTLSCertificates()) {
                Response.initUnSecureTSL();
                ((HttpsURLConnection)conn).setSSLSocketFactory(sslSocketFactory);
                ((HttpsURLConnection)conn).setHostnameVerifier(Response.getInsecureVerifier());
            }
            if (req.method().hasBody()) {
                conn.setDoOutput(true);
            }
            if (req.cookies().size() > 0) {
                conn.addRequestProperty("Cookie", Response.getRequestCookieString(req));
            }
            for (Map.Entry<String, String> header : req.headers().entrySet()) {
                conn.addRequestProperty(header.getKey(), header.getValue());
            }
            return conn;
        }

        private static HostnameVerifier getInsecureVerifier() {
            return new HostnameVerifier(){

                public boolean verify(String urlHostName, SSLSession session) {
                    return true;
                }
            };
        }

        private static synchronized void initUnSecureTSL() throws IOException {
            if (sslSocketFactory == null) {
                TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager(){

                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                }};
                try {
                    SSLContext sslContext = SSLContext.getInstance("SSL");
                    sslContext.init(null, trustAllCerts, new SecureRandom());
                    sslSocketFactory = sslContext.getSocketFactory();
                }
                catch (NoSuchAlgorithmException e) {
                    throw new IOException("Can't create unsecure trust manager");
                }
                catch (KeyManagementException e) {
                    throw new IOException("Can't create unsecure trust manager");
                }
            }
        }

        private void setupFromConnection(HttpURLConnection conn, Connection.Response previousResponse) throws IOException {
            this.method = Connection.Method.valueOf(conn.getRequestMethod());
            this.url = conn.getURL();
            this.statusCode = conn.getResponseCode();
            this.statusMessage = conn.getResponseMessage();
            this.contentType = conn.getContentType();
            Map<String, List<String>> resHeaders = conn.getHeaderFields();
            this.processResponseHeaders(resHeaders);
            if (previousResponse != null) {
                for (Map.Entry<String, String> prevCookie : previousResponse.cookies().entrySet()) {
                    if (this.hasCookie(prevCookie.getKey())) continue;
                    this.cookie(prevCookie.getKey(), prevCookie.getValue());
                }
            }
        }

        void processResponseHeaders(Map<String, List<String>> resHeaders) {
            for (Map.Entry<String, List<String>> entry : resHeaders.entrySet()) {
                String name = entry.getKey();
                if (name == null) continue;
                List<String> values = entry.getValue();
                if (name.equalsIgnoreCase("Set-Cookie")) {
                    for (String value : values) {
                        if (value == null) continue;
                        TokenQueue cd = new TokenQueue(value);
                        String cookieName = cd.chompTo("=").trim();
                        String cookieVal = cd.consumeTo(";").trim();
                        if (cookieName.length() <= 0) continue;
                        this.cookie(cookieName, cookieVal);
                    }
                    continue;
                }
                if (values.isEmpty()) continue;
                this.header(name, values.get(0));
            }
        }

        private static String setOutputContentType(Connection.Request req) {
            boolean needsMulti = false;
            for (Connection.KeyVal keyVal : req.data()) {
                if (!keyVal.hasInputStream()) continue;
                needsMulti = true;
                break;
            }
            String bound = null;
            if (needsMulti) {
                bound = DataUtil.mimeBoundary();
                req.header("Content-Type", "multipart/form-data; boundary=" + bound);
            } else {
                req.header("Content-Type", "application/x-www-form-urlencoded; charset=" + req.postDataCharset());
            }
            return bound;
        }

        private static void writePost(Connection.Request req, OutputStream outputStream, String bound) throws IOException {
            Collection<Connection.KeyVal> data = req.data();
            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
            if (bound != null) {
                for (Connection.KeyVal keyVal : data) {
                    w.write("--");
                    w.write(bound);
                    w.write("\r\n");
                    w.write("Content-Disposition: form-data; name=\"");
                    w.write(HttpConnection.encodeMimeName(keyVal.key()));
                    w.write("\"");
                    if (keyVal.hasInputStream()) {
                        w.write("; filename=\"");
                        w.write(HttpConnection.encodeMimeName(keyVal.value()));
                        w.write("\"\r\nContent-Type: application/octet-stream\r\n\r\n");
                        w.flush();
                        DataUtil.crossStreams(keyVal.inputStream(), outputStream);
                        outputStream.flush();
                    } else {
                        w.write("\r\n\r\n");
                        w.write(keyVal.value());
                    }
                    w.write("\r\n");
                }
                w.write("--");
                w.write(bound);
                w.write("--");
            } else {
                boolean first = true;
                for (Connection.KeyVal keyVal : data) {
                    if (!first) {
                        w.append('&');
                    } else {
                        first = false;
                    }
                    w.write(URLEncoder.encode(keyVal.key(), req.postDataCharset()));
                    w.write(61);
                    w.write(URLEncoder.encode(keyVal.value(), req.postDataCharset()));
                }
            }
            w.close();
        }

        private static String getRequestCookieString(Connection.Request req) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> cookie : req.cookies().entrySet()) {
                if (!first) {
                    sb.append("; ");
                } else {
                    first = false;
                }
                sb.append(cookie.getKey()).append('=').append(cookie.getValue());
            }
            return sb.toString();
        }

        private static void serialiseRequestUrl(Connection.Request req) throws IOException {
            URL in = req.url();
            StringBuilder url = new StringBuilder();
            boolean first = true;
            url.append(in.getProtocol()).append("://").append(in.getAuthority()).append(in.getPath()).append("?");
            if (in.getQuery() != null) {
                url.append(in.getQuery());
                first = false;
            }
            for (Connection.KeyVal keyVal : req.data()) {
                if (!first) {
                    url.append('&');
                } else {
                    first = false;
                }
                url.append(URLEncoder.encode(keyVal.key(), "UTF-8")).append('=').append(URLEncoder.encode(keyVal.value(), "UTF-8"));
            }
            req.url(new URL(url.toString()));
            req.data().clear();
        }

        static {
            xmlContentTypeRxp = Pattern.compile("application/\\w+\\+xml.*");
        }

    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static class Request
    extends Base<Connection.Request>
    implements Connection.Request {
        private int timeoutMilliseconds = 3000;
        private int maxBodySizeBytes = 1048576;
        private boolean followRedirects = true;
        private Collection<Connection.KeyVal> data = new ArrayList<Connection.KeyVal>();
        private boolean ignoreHttpErrors = false;
        private boolean ignoreContentType = false;
        private Parser parser;
        private boolean validateTSLCertificates = true;
        private String postDataCharset = "UTF-8";

        private Request() {
            super();
            this.method = Connection.Method.GET;
            this.headers.put("Accept-Encoding", "gzip");
            this.parser = Parser.htmlParser();
        }

        @Override
        public int timeout() {
            return this.timeoutMilliseconds;
        }

        @Override
        public Request timeout(int millis) {
            Validate.isTrue(millis >= 0, "Timeout milliseconds must be 0 (infinite) or greater");
            this.timeoutMilliseconds = millis;
            return this;
        }

        @Override
        public int maxBodySize() {
            return this.maxBodySizeBytes;
        }

        @Override
        public Connection.Request maxBodySize(int bytes) {
            Validate.isTrue(bytes >= 0, "maxSize must be 0 (unlimited) or larger");
            this.maxBodySizeBytes = bytes;
            return this;
        }

        @Override
        public boolean followRedirects() {
            return this.followRedirects;
        }

        @Override
        public Connection.Request followRedirects(boolean followRedirects) {
            this.followRedirects = followRedirects;
            return this;
        }

        @Override
        public boolean ignoreHttpErrors() {
            return this.ignoreHttpErrors;
        }

        @Override
        public boolean validateTLSCertificates() {
            return this.validateTSLCertificates;
        }

        @Override
        public void validateTLSCertificates(boolean value) {
            this.validateTSLCertificates = value;
        }

        @Override
        public Connection.Request ignoreHttpErrors(boolean ignoreHttpErrors) {
            this.ignoreHttpErrors = ignoreHttpErrors;
            return this;
        }

        @Override
        public boolean ignoreContentType() {
            return this.ignoreContentType;
        }

        @Override
        public Connection.Request ignoreContentType(boolean ignoreContentType) {
            this.ignoreContentType = ignoreContentType;
            return this;
        }

        @Override
        public Request data(Connection.KeyVal keyval) {
            Validate.notNull(keyval, "Key val must not be null");
            this.data.add(keyval);
            return this;
        }

        @Override
        public Collection<Connection.KeyVal> data() {
            return this.data;
        }

        @Override
        public Request parser(Parser parser) {
            this.parser = parser;
            return this;
        }

        @Override
        public Parser parser() {
            return this.parser;
        }

        @Override
        public Connection.Request postDataCharset(String charset) {
            Validate.notNull(charset, "Charset must not be null");
            if (!Charset.isSupported(charset)) {
                throw new IllegalCharsetNameException(charset);
            }
            this.postDataCharset = charset;
            return this;
        }

        @Override
        public String postDataCharset() {
            return this.postDataCharset;
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static abstract class Base<T extends Connection.Base>
    implements Connection.Base<T> {
        URL url;
        Connection.Method method;
        Map<String, String> headers = new LinkedHashMap<String, String>();
        Map<String, String> cookies = new LinkedHashMap<String, String>();

        private Base() {
        }

        @Override
        public URL url() {
            return this.url;
        }

        @Override
        public T url(URL url) {
            Validate.notNull(url, "URL must not be null");
            this.url = url;
            return (T)this;
        }

        @Override
        public Connection.Method method() {
            return this.method;
        }

        @Override
        public T method(Connection.Method method) {
            Validate.notNull((Object)method, "Method must not be null");
            this.method = method;
            return (T)this;
        }

        @Override
        public String header(String name) {
            Validate.notNull(name, "Header name must not be null");
            return this.getHeaderCaseInsensitive(name);
        }

        @Override
        public T header(String name, String value) {
            Validate.notEmpty(name, "Header name must not be empty");
            Validate.notNull(value, "Header value must not be null");
            this.removeHeader(name);
            this.headers.put(name, value);
            return (T)this;
        }

        @Override
        public boolean hasHeader(String name) {
            Validate.notEmpty(name, "Header name must not be empty");
            return this.getHeaderCaseInsensitive(name) != null;
        }

        @Override
        public boolean hasHeaderWithValue(String name, String value) {
            return this.hasHeader(name) && this.header(name).equalsIgnoreCase(value);
        }

        @Override
        public T removeHeader(String name) {
            Validate.notEmpty(name, "Header name must not be empty");
            Map.Entry<String, String> entry = this.scanHeaders(name);
            if (entry != null) {
                this.headers.remove(entry.getKey());
            }
            return (T)this;
        }

        @Override
        public Map<String, String> headers() {
            return this.headers;
        }

        private String getHeaderCaseInsensitive(String name) {
            Map.Entry<String, String> entry;
            Validate.notNull(name, "Header name must not be null");
            String value = this.headers.get(name);
            if (value == null) {
                value = this.headers.get(name.toLowerCase());
            }
            if (value == null && (entry = this.scanHeaders(name)) != null) {
                value = entry.getValue();
            }
            return value;
        }

        private Map.Entry<String, String> scanHeaders(String name) {
            String lc = name.toLowerCase();
            for (Map.Entry<String, String> entry : this.headers.entrySet()) {
                if (!entry.getKey().toLowerCase().equals(lc)) continue;
                return entry;
            }
            return null;
        }

        @Override
        public String cookie(String name) {
            Validate.notEmpty(name, "Cookie name must not be empty");
            return this.cookies.get(name);
        }

        @Override
        public T cookie(String name, String value) {
            Validate.notEmpty(name, "Cookie name must not be empty");
            Validate.notNull(value, "Cookie value must not be null");
            this.cookies.put(name, value);
            return (T)this;
        }

        @Override
        public boolean hasCookie(String name) {
            Validate.notEmpty(name, "Cookie name must not be empty");
            return this.cookies.containsKey(name);
        }

        @Override
        public T removeCookie(String name) {
            Validate.notEmpty(name, "Cookie name must not be empty");
            this.cookies.remove(name);
            return (T)this;
        }

        @Override
        public Map<String, String> cookies() {
            return this.cookies;
        }
    }

}

