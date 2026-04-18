package access;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthChainId;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;

public class BlockchainClient {
    private final Web3j web3j;

    public BlockchainClient(NodeConfig config) {
        web3j = Web3j.build(new HttpService(config.getNetworkUrl()));
    }

    public void printData() {
        try {
            EthBlockNumber blockNumber = web3j.ethBlockNumber().send();
            System.out.println("Numer bloku: " + blockNumber.getBlockNumber());

            EthChainId chainId = web3j.ethChainId().send();
            System.out.println("Chain ID: " + chainId.getChainId());

            Web3ClientVersion clientVersion = web3j.web3ClientVersion().send();
            System.out.println("Klient: " + clientVersion.getWeb3ClientVersion());
        } catch(IOException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException();
        }
    }

    public Web3j getWeb3j() {
        return web3j;
    }
}

