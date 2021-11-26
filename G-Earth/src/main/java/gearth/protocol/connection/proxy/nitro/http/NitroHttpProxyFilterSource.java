package gearth.protocol.connection.proxy.nitro.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.AttributeKey;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;

public class NitroHttpProxyFilterSource extends HttpFiltersSourceAdapter {

    private static final AttributeKey<String> CONNECTED_URL = AttributeKey.valueOf("connected_url");

    private final NitroHttpProxyServerCallback callback;

    public NitroHttpProxyFilterSource(NitroHttpProxyServerCallback callback) {
        this.callback = callback;
    }

    @Override
    public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
        // https://github.com/ganskef/LittleProxy-mitm#resolving-uri-in-case-of-https
        String uri = originalRequest.getUri();
        if (originalRequest.getMethod() == HttpMethod.CONNECT) {
            if (ctx != null) {
                String prefix = "https://" + uri.replaceFirst(":443$", "");
                ctx.channel().attr(CONNECTED_URL).set(prefix);
            }
            return new HttpFiltersAdapter(originalRequest, ctx);
        }
        
        String connectedUrl = ctx.channel().attr(CONNECTED_URL).get();
        if (connectedUrl == null) {
            return new NitroHttpProxyFilter(originalRequest, ctx, callback, uri);
        }

        return new NitroHttpProxyFilter(originalRequest, ctx, callback, connectedUrl + uri);
    }

    @Override
    public int getMaximumResponseBufferSizeInBytes() {
        // Increasing this causes LittleProxy to output "FullHttpResponse" objects.
        return 1024 * 1024 * 1024;
    }
}
