import access.BlockchainClient;
import access.NodeConfig;

class App {
    public static void main(String[] args) {
        NodeConfig config = new NodeConfig();
        config.setNetworkUrlFromProperties();
        BlockchainClient client = new BlockchainClient(config);
        client.printData();
    }
}

