package access;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class NodeConfig {
    private final String networkUrl;

    public NodeConfig() {
        Properties properties = new Properties();
        try(InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream("local.properties")) {

            if(inputStream == null) {
                throw new RuntimeException("file not found");
            }

            properties.load(inputStream);
            networkUrl = properties.getProperty("infura.url");
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getNetworkUrl() {
        return networkUrl;
    }
}
