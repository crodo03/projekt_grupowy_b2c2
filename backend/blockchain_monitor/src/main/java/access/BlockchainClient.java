package access;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

public class BlockchainClient implements AutoCloseable {
    private Web3j web3j;

    public BlockchainClient(NodeConfig config) {
        web3j = Web3j.build(new HttpService(config.getNetworkUrl()));
    }

    public Web3j getWeb3j() {
        return web3j;
    }

    public void setWeb3j(Web3j web3j) {
        this.web3j = web3j;
    }

    @Override
    public void close() {
        System.out.println("shutting down");
        web3j.shutdown();
    }
}

