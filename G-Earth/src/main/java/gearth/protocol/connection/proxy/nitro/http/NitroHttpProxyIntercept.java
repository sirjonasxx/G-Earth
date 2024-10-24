package gearth.protocol.connection.proxy.nitro.http;

import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptInitializer;
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptPipeline;
import com.github.monkeywie.proxyee.intercept.common.FullRequestIntercept;
import com.github.monkeywie.proxyee.intercept.common.FullResponseIntercept;
import com.github.monkeywie.proxyee.util.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NitroHttpProxyIntercept extends HttpProxyInterceptInitializer {

    private static final Logger log = LoggerFactory.getLogger(NitroHttpProxyIntercept.class);

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

    public NitroHttpProxyIntercept(NitroHttpProxyServerCallback callback) {
        this.callback = callback;
    }

    @Override
    public void init(HttpProxyInterceptPipeline pipeline) {
        pipeline.addLast(new FullResponseIntercept() {
            @Override
            public boolean match(HttpRequest httpRequest, HttpResponse httpResponse, HttpProxyInterceptPipeline httpProxyInterceptPipeline) {
                log.debug("Intercepting response for {} {}", httpRequest.headers().get(HttpHeaderNames.HOST), httpRequest.uri());
                return true;
            }

            @Override
            public void handleResponse(HttpRequest httpRequest, FullHttpResponse httpResponse, HttpProxyInterceptPipeline pipeline) {
                // Strip cache headers.
                stripCacheHeaders(httpResponse.headers());

                // Check for response body.
                final ByteBuf content = httpResponse.content();

                if (content == null || content.readableBytes() == 0) {
                    return;
                }

                // Find nitro configuration.
                if (ByteUtil.findText(content, NitroConfigSearch) != -1) {
                    final String responseBody = responseRead(httpResponse);
                    final Matcher matcher = NitroConfigPattern.matcher(responseBody);

                    // Replace websocket with proxy.
                    if (matcher.find()) {
                        final String originalWebsocket = matcher.group(2).replace("\\/", "/");
                        final String replacementWebsocket = callback.replaceWebsocketServer(originalWebsocket);

                        if (replacementWebsocket != null) {
                            final String updatedBody = responseBody.replace(matcher.group(2), replacementWebsocket);

                            responseWrite(httpResponse, updatedBody);
                        }
                    }

                    // Retrieve cookies for request to the origin.
                    final String requestCookies = parseCookies(httpRequest);

                    if (requestCookies != null && !requestCookies.isEmpty()) {
                        callback.setOriginCookies(requestCookies);
                    }
                }

                // Strip CSP headers
                if (ByteUtil.findText(content, NitroClientSearch) != -1) {
                    stripContentSecurityPolicy(httpResponse);
                }
            }
        });

        pipeline.addLast(new FullRequestIntercept() {
            @Override
            public boolean match(HttpRequest httpRequest, HttpProxyInterceptPipeline pipeline) {
                log.debug("Intercepting request for {} {}", httpRequest.headers().get(HttpHeaderNames.HOST), httpRequest.uri());
                return true;
            }

            @Override
            public void handleRequest(FullHttpRequest httpRequest, HttpProxyInterceptPipeline pipeline) {
                // Disable caching.
                stripCacheHeaders(httpRequest.headers());
            }
        });
    }

    /**
     * Check if cookies from the request need to be recorded for the websocket connection to the origin server.
     */
    private static String parseCookies(final HttpRequest request) {
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

        if (result.isEmpty()) {
            return null;
        }

        return String.join("; ", result);
    }

    /**
     * Modify Content-Security-Policy header, which could prevent Nitro from connecting with G-Earth.
     */
    private static void stripContentSecurityPolicy(FullHttpResponse response) {
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

    /**
     * Strip cache headers from the response.
     */
    private static void stripCacheHeaders(HttpHeaders headers) {
        headers.remove(HeaderAcceptEncoding);
        headers.remove(HeaderAge);
        headers.remove(HeaderCacheControl);
        headers.remove(HeaderETag);
        headers.remove(HeaderIfNoneMatch);
        headers.remove(HeaderIfModifiedSince);
        headers.remove(HeaderLastModified);
    }

    private static String responseRead(FullHttpResponse response) {
        final ByteBuf contentBuf = response.content();
        return contentBuf.toString(StandardCharsets.UTF_8);
    }

    private static void responseWrite(FullHttpResponse response, String content) {
        final byte[] body = content.getBytes(StandardCharsets.UTF_8);

        // Update content.
        response.content().clear().writeBytes(body);

        // Update content-length.
        HttpUtil.setContentLength(response, body.length);

        // Ensure modified response is not cached.
        stripCacheHeaders(response.headers());
    }
}
