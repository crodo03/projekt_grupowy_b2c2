import access.BlockchainClient;
import access.NodeConfig;
import network.RestClient;
import service.BlockAnalyzer;

class App {
    public static void main(String[] args) {

        NodeConfig config = new NodeConfig();

        try(BlockchainClient client = new BlockchainClient(config)) {
            BlockAnalyzer blockAnalyzer = new BlockAnalyzer(client.getWeb3j());
            RestClient restClient = new RestClient(blockAnalyzer);
            restClient.startServer();
        }
    }
}

