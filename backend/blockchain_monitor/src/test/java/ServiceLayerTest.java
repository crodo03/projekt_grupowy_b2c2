import access.BlockchainClient;
import access.NodeConfig;
import exceptions.GetBlockException;
import org.junit.jupiter.api.Test;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import service.BlockAnalyzer;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ServiceLayerTest {

    @Test
    public void shouldThrowGetBlockExceptionOnConnectionFailure() throws IOException {
        Web3j web3j = mock(Web3j.class);

        @SuppressWarnings("unchecked")
        Request<?, EthBlockNumber> request = (Request<?, EthBlockNumber>) mock(Request.class);

        // eth block number returns request
        doReturn(request).when(web3j).ethBlockNumber();
        // calling .send() on request throws an exception
        when(request.send()).thenThrow(new IOException("connection failed"));

        NodeConfig config = new NodeConfig();
        try(BlockchainClient client = new BlockchainClient(config)) {
            client.setWeb3j(web3j); // injecting mocked client
            assertThrows(GetBlockException.class,
                    () -> new BlockAnalyzer(client.getWeb3j()).getLatestBlocksInfo(2)
            );
        }
    }
}
