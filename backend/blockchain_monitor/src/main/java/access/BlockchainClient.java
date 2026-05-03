package access;

import lombok.Getter;
import lombok.Setter;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Getter
@Setter
public class BlockchainClient implements AutoCloseable {
    private Web3j web3j;

    public BlockchainClient(NodeConfig config) {
        web3j = Web3j.build(new HttpService(config.getNetworkUrl()));
    }

    @Override
    public void close() {
        System.out.println("shutting down web3j");
        web3j.shutdown();
    }
}

