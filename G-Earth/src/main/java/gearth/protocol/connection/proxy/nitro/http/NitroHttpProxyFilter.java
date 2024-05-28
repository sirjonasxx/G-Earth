package gearth.protocol.connection.proxy.nitro.http;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.littleshoot.proxy.HttpFiltersAdapter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NitroHttpProxyFilter extends HttpFiltersAdapter {

    private static final String NitroConfigSearch = "socket.url";
    private static final String NitroClientSearch = "configurationUrls:";
    private static final Pattern NitroConfigPattern = Pattern.compile("[\"']socket\\.url[\"']:(\\s+)?[\"'](wss?:.*?)[\"']", Pattern.MULTILINE);

    // https://developers.cloudflare.com/fundamentals/get-started/reference/cloudflare-cookies/
    private static final HashSet<String> CloudflareCookies = new HashSet<>(Arrays.asList(
            "__cflb",
            "__cf_bm",
            "__cfseq",
            "cf_ob_info",
            "cf_use_ob",
            "__cfwaitingroom",
            "__cfruid",
            "_cfuvid",
            "cf_clearance",
            "cf_chl_rc_i",
            "cf_chl_rc_ni",
            "cf_chl_rc_m"
    ));

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
    private String cookies;

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

            // Find relevant cookies for the WebSocket connection.
            this.cookies = parseCookies(request);
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
                    final String originalWebsocket = matcher.group(2).replace("\\/", "/");
                    final String replacementWebsocket = callback.replaceWebsocketServer(this.url, originalWebsocket);

                    if (replacementWebsocket != null) {
                        responseBody = responseBody.replace(matcher.group(2), replacementWebsocket);
                        responseModified = true;
                    }
                }

                callback.setOriginCookies(this.cookies);
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
     * Check if cookies from the request need to be recorded for the websocket connection to the origin server.
     */
    private String parseCookies(final HttpRequest request) {
        final List<String> result = new ArrayList<>();
        final List<String> cookieHeaders = request.headers().getAll("Cookie");

        for (final String cookieHeader : cookieHeaders) {
            final String[] cookies = cookieHeader.split(";");

            for (final String cookie : cookies) {
                final String[] parts = cookie.trim().split("=");

                if (CloudflareCookies.contains(parts[0])) {
                    result.add(cookie.trim());
                }
            }
        }

        if (result.size() == 0) {
            return null;
        }

        return String.join("; ", result);
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
