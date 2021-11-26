package gearth.protocol.connection.proxy.nitro.http;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.littleshoot.proxy.HttpFiltersAdapter;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NitroHttpProxyFilter extends HttpFiltersAdapter {

    private static final String NitroConfigSearch = "\"socket.url\"";
    private static final String NitroClientSearch = "configurationUrls:";
    private static final Pattern NitroConfigPattern = Pattern.compile("\"socket\\.url\":.?\"(wss?://.*?)\"", Pattern.MULTILINE);

    private static final String HeaderAcceptEncoding = "Accept-Encoding";
    private static final String HeaderAge = "Age";
    private static final String HeaderCacheControl = "Cache-Control";
    private static final String HeaderContentSecurityPolicy = "Content-Security-Policy";
    private static final String HeaderETag = "ETag";
    private static final String HeaderIfNoneMatch = "If-None-Match";
    private static final String HeaderIfModifiedSince = "If-Modified-Since";
    private static final String HeaderLastModified = "Last-Modified";

    private final NitroHttpProxyServerCallback callback;
    private final String url;

    public NitroHttpProxyFilter(HttpRequest originalRequest, ChannelHandlerContext ctx, NitroHttpProxyServerCallback callback, String url) {
        super(originalRequest, ctx);
        this.callback = callback;
        this.url = url;
    }

    @Override
    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
        if (httpObject instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) httpObject;
            HttpHeaders headers = request.headers();

            // Only support gzip or deflate.
            // The LittleProxy library does not support brotli.
            if (headers.contains(HeaderAcceptEncoding)) {
                String encoding = headers.get(HeaderAcceptEncoding);

                if (encoding.contains("br")) {
                    if (encoding.contains("gzip") && encoding.contains("deflate")) {
                        headers.set(HeaderAcceptEncoding, "gzip, deflate");
                    } else if (encoding.contains("gzip")) {
                        headers.set(HeaderAcceptEncoding, "gzip, deflate");
                    } else {
                        headers.remove(HeaderAcceptEncoding);
                    }
                }
            }

            // Disable caching.
            stripCacheHeaders(headers);
        }

        return super.clientToProxyRequest(httpObject);
    }

    @Override
    public HttpObject serverToProxyResponse(HttpObject httpObject) {
        if (httpObject instanceof FullHttpResponse) {
            final FullHttpResponse response = (FullHttpResponse) httpObject;

            // Find nitro configuration file.
            boolean responseModified = false;
            String responseBody = responseRead(response);

            if (responseBody.contains(NitroConfigSearch)) {
                final Matcher matcher = NitroConfigPattern.matcher(responseBody);

                if (matcher.find()) {
                    final String originalWebsocket = matcher.group(1);
                    final String replacementWebsocket = callback.replaceWebsocketServer(url, originalWebsocket);

                    if (replacementWebsocket != null) {
                        responseBody = responseBody.replace(originalWebsocket, replacementWebsocket);
                        responseModified = true;
                    }
                }
            }

            // Apply changes.
            if (responseModified) {
                responseWrite(response, responseBody);
            }

            // CSP.
            if (responseBody.contains(NitroClientSearch)) {
                stripContentSecurityPolicy(response);
            }
        }

        return httpObject;
    }

    /**
     * Modify Content-Security-Policy header, which could prevent Nitro from connecting with G-Earth.
     */
    private void stripContentSecurityPolicy(FullHttpResponse response) {
        final HttpHeaders headers = response.headers();

        if (!headers.contains(HeaderContentSecurityPolicy)){
            return;
        }

        String csp = headers.get(HeaderContentSecurityPolicy);

        if (csp.contains("connect-src")) {
            csp = csp.replace("connect-src", "connect-src *");
        } else if (csp.contains("default-src")) {
            csp = csp.replace("default-src", "default-src *");
        }

        headers.set(HeaderContentSecurityPolicy, csp);
    }

    private static String responseRead(FullHttpResponse response) {
        final ByteBuf contentBuf = response.content();
        return contentBuf.toString(CharsetUtil.UTF_8);
    }

    private static void responseWrite(FullHttpResponse response, String content) {
        final byte[] body = content.getBytes(StandardCharsets.UTF_8);

        // Update content.
        response.content().clear().writeBytes(body);

        // Update content-length.
        HttpHeaders.setContentLength(response, body.length);

        // Ensure modified response is not cached.
        stripCacheHeaders(response.headers());
    }

    private static void stripCacheHeaders(HttpHeaders headers) {
        headers.remove(HeaderAcceptEncoding);
        headers.remove(HeaderAge);
        headers.remove(HeaderCacheControl);
        headers.remove(HeaderETag);
        headers.remove(HeaderIfNoneMatch);
        headers.remove(HeaderIfModifiedSince);
        headers.remove(HeaderLastModified);
    }

}
