package gearth.services.packet_info.providers;

import gearth.misc.Cacher;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;

public abstract class RemotePacketInfoProvider extends PacketInfoProvider {
    public RemotePacketInfoProvider(String hotelVersion) {
        super(hotelVersion);
    }

    protected abstract String getRemoteUrl();
    protected abstract String getCacheName();

    @Override
    protected File getFile() {
        File f = new File(Cacher.getCacheDir(), getCacheName());
        if (!f.exists()) {
            Connection connection = Jsoup.connect(getRemoteUrl()).ignoreContentType(true);
            try {
                connection.timeout(3000);
                Connection.Response response = connection.execute();
                if (response.statusCode() == 200) {
                    String messagesBodyJson = response.body();
                    Cacher.updateCache(messagesBodyJson, getCacheName());
                }
                else {
                    return null;
                }
            } catch (IOException e) {
                return null;
            }
        }

        return new File(Cacher.getCacheDir(), getCacheName());
    }
}
