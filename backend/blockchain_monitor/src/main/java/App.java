import access.BlockchainClient;
import access.NodeConfig;
import org.web3j.protocol.core.methods.response.EthBlock;
import service.BlockAnalyzer;
import service.TransactionAnalyzer;

class App {
    public static void main(String[] args) {
        NodeConfig config = new NodeConfig();

        try(BlockchainClient client = new BlockchainClient(config)) {
            BlockAnalyzer blockAnalyzer = new BlockAnalyzer(client.getWeb3j());
            blockAnalyzer.getLatestBlocksInfo(1);
            blockAnalyzer.getFailedBlocksNumbers();

            EthBlock.Block block = blockAnalyzer.getBlocks().getFirst();
            TransactionAnalyzer transactionAnalyzer = new TransactionAnalyzer(block);
            transactionAnalyzer.getTransactionInfo();
        }
    }
}

