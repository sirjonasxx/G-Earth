package gearth.services.packet_info.providers;

import gearth.misc.Cacher;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public abstract class RemotePacketInfoProvider extends PacketInfoProvider {

    private static final Logger LOG = LoggerFactory.getLogger(RemotePacketInfoProvider.class);

    public RemotePacketInfoProvider(String hotelVersion) {
        super(hotelVersion);
    }

    protected abstract String getRemoteUrl();
    protected abstract String getCacheName();

    @Override
    protected File getFile() {
        final File cacheFile = new File(Cacher.getCacheDir(), getCacheName());

        // Check if cache file exists and last modified less than 5 minutes.
        if (cacheFile.exists() && cacheFile.lastModified() > System.currentTimeMillis() - 5 * 60 * 1000) {
            return cacheFile;
        }

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            final String remoteUrl = getRemoteUrl();

            LOG.info("Fetching packet info from {}", remoteUrl);

            final HttpGet request = new HttpGet(getRemoteUrl());
            final String response = client.execute(request, res -> res.getCode() == 200
                    ? EntityUtils.toString(res.getEntity())
                    : null);

            if (response != null) {
                Cacher.updateCache(response, getCacheName());
            } else {
                return null;
            }
        } catch (Exception e) {
            LOG.error("Failed to fetch remote packet info for version {}", this.hotelVersion, e);
        }

        return cacheFile;
    }
}
