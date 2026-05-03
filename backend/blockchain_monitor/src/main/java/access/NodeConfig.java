package access;

import exceptions.NodeConfigException;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Getter
public class NodeConfig {
    private String networkUrl;
    private final String propertiesPath;
    private final String infuraUrlProperty;

    public NodeConfig() {
        this("local.properties", "infura.url");
    }

    public NodeConfig(String path, String propertyName) {
        this.propertiesPath = path;
        this.infuraUrlProperty = propertyName;
        loadProperties();
    }

    private void loadProperties() {
        Properties properties = new Properties();
        try(InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream(propertiesPath)) {

            if(inputStream == null) {
                throw new NodeConfigException("file not found");
            }

            properties.load(inputStream);
            networkUrl = properties.getProperty(infuraUrlProperty);

            if(networkUrl == null) {
                throw new NodeConfigException("property not found " + infuraUrlProperty);
            }
        } catch(IOException e) {
            throw new NodeConfigException("failed to load properties file: " + propertiesPath, e);
        }
    }
}
