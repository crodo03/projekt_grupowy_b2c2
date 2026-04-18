package access;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class NodeConfig {
    private String networkUrl;

    public void setNetworkUrlFromProperties() {
        try {
            Properties properties = new Properties();
            properties.load(new FileInputStream("local.properties"));
            this.networkUrl = properties.getProperty("infura.url");
        } catch(IOException e) {
            throw new RuntimeException();
        }
    }

    public String getNetworkUrl() {
        return networkUrl;
    }
}
