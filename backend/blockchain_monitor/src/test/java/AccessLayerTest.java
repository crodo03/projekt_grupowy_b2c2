import access.BlockchainClient;
import access.NodeConfig;
import exceptions.NodeConfigException;
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
        try(BlockchainClient client = new BlockchainClient(config)) {
            Web3j web3j = client.getWeb3j();

            EthBlockNumber blockNumber = web3j.ethBlockNumber().send();
            EthChainId chainId = web3j.ethChainId().send();

            assertNotNull(blockNumber.getBlockNumber());
            assertTrue(blockNumber.getBlockNumber().compareTo(BigInteger.ZERO) > 0);
            assertEquals(new BigInteger("11155111"), chainId.getChainId());
        }
    }

    @Test
    public void shouldLoadNetworkUrlFromProperties() {
        NodeConfig config = new NodeConfig("test.properties", "infura.url");
        String networkUrl = config.getNetworkUrl();
        assertEquals("https://some-test-url", networkUrl);
    }

    @Test
    public void shouldThrowWhenPropertiesFileMissing() {
        assertThrows(NodeConfigException.class,
                () -> new NodeConfig("bad-path.properties", "infura.url")
        );
    }

    @Test
    public void shouldThrowWhenInfuraUrlPropertyMissing() {
        assertThrows(NodeConfigException.class,
                () -> new NodeConfig("test.properties", "bad-property-name.url")
        );
    }
}
