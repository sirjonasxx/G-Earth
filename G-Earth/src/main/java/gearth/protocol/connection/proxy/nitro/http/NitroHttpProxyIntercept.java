package gearth.protocol.connection.proxy.nitro.http;

import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptInitializer;
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptPipeline;
import com.github.monkeywie.proxyee.intercept.common.FullResponseIntercept;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NitroHttpProxyIntercept extends HttpProxyInterceptInitializer {

    private static final Logger log = LoggerFactory.getLogger(NitroHttpProxyIntercept.class);

    public NitroHttpProxyIntercept(NitroHttpProxyServerCallback serverCallback) {

    }

    @Override
    public void init(HttpProxyInterceptPipeline pipeline) {
        pipeline.addLast(new FullResponseIntercept() {
            @Override
            public boolean match(HttpRequest httpRequest, HttpResponse httpResponse, HttpProxyInterceptPipeline httpProxyInterceptPipeline) {
                log.debug("Intercepting response for {}", httpRequest.uri());
                return false;
            }

            @Override
            public void handleResponse(HttpRequest httpRequest, FullHttpResponse httpResponse, HttpProxyInterceptPipeline pipeline) {
                super.handleResponse(httpRequest, httpResponse, pipeline);
            }
        });
    }
}
