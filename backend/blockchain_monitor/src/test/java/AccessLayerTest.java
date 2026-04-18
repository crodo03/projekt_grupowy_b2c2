import access.BlockchainClient;
import access.NodeConfig;
import org.junit.jupiter.api.Test;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthChainId;

import java.io.IOException;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

public class AccessLayerTest {

    @Test
    public void shouldConnectToSepolia() throws IOException {
        NodeConfig config = new NodeConfig();
        config.setNetworkUrlFromProperties();
        BlockchainClient client = new BlockchainClient(config);
        Web3j web3jClient = client.getWeb3j();

        EthBlockNumber blockNumber = web3jClient.ethBlockNumber().send();
        EthChainId chainId = web3jClient.ethChainId().send();

        assertNotNull(blockNumber.getBlockNumber());
        assertTrue(blockNumber.getBlockNumber().compareTo(BigInteger.ZERO) > 0);
        assertEquals(new BigInteger("11155111"), chainId.getChainId());
    }
}
