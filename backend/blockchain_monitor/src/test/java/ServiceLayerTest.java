import access.BlockchainClient;
import access.NodeConfig;
import exceptions.GetBlockException;
import network.dto.BlockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import service.BlockAnalyzer;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ServiceLayerTest {
    private Web3j web3j = null;
    private EthBlockNumber ethBlockNumber = null;
    private Request<?, EthBlockNumber> request = null;
    private EthBlock ethBlock = null;
    private EthBlock.Block block = null;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void declareMockedClasses() {
        web3j = mock(Web3j.class);
        ethBlockNumber = mock(EthBlockNumber.class);
        request = (Request<?, EthBlockNumber>) mock(Request.class);
        ethBlock = mock(EthBlock.class);
        block = mock(EthBlock.Block.class);
    }

    @Test
    public void shouldThrowGetBlockExceptionOnConnectionFailure() throws IOException {
        // forces web3j.ethBlockNumber() to return mocked request
        doReturn(request).when(web3j).ethBlockNumber();
        // forces request.send() to throw an exception
        when(request.send()).thenThrow(new IOException("connection failed"));

        NodeConfig config = new NodeConfig();
        try(BlockchainClient client = new BlockchainClient(config)) {
            client.setWeb3j(web3j); // injecting mocked client
            assertThrows(GetBlockException.class,
                    () -> new BlockAnalyzer(client.getWeb3j()).getLatestBlocks(2)
            );
        }
    }

    @Test
    public void shouldReturnLatestBlockNumber() throws IOException {
        doReturn(request).when(web3j).ethBlockNumber();
        when(request.send()).thenReturn(ethBlockNumber);
        when(ethBlockNumber.getBlockNumber()).thenReturn(BigInteger.valueOf(12345));

        BlockAnalyzer blockAnalyzer = new BlockAnalyzer(web3j);

        BigInteger result = blockAnalyzer.getLatestBlockNumber();

        assertEquals(BigInteger.valueOf(12345), result);
    }

    @Test
    public void shouldThrowGetBlockExceptionOnIOException() throws IOException {
        doReturn(request).when(web3j).ethBlockNumber();
        when(request.send()).thenThrow(new IOException("connection failed"));

        assertThrows(GetBlockException.class, () -> new BlockAnalyzer(web3j).getLatestBlockNumber());
    }

    @Test
    public void shouldReturnBlockResponseObject() throws IOException {
        doReturn(request).when(web3j).ethGetBlockByNumber(
                any(DefaultBlockParameter.class),
                eq(true)
        );
        doReturn(ethBlock).when(request).send();
        when(ethBlock.getBlock()).thenReturn(block);

        List<EthBlock.TransactionResult<?>> transactionResults = Collections.emptyList();
        doReturn(transactionResults).when(block).getTransactions();
        when(block.getHash()).thenReturn("0xHash");
        when(block.getNumber()).thenReturn(BigInteger.TWO);

        BlockAnalyzer blockAnalyzer = new BlockAnalyzer(web3j);
        BlockResponse result = blockAnalyzer.getBlockResponseObject(BigInteger.ONE);

        assertEquals(BigInteger.TWO, result.getBlockNumber());
        assertEquals(0, result.getNumberOfTransactions());
        assertEquals("0xHash", result.getBlockHash());
    }

    @Test
    public void shouldReturnNullOnIOException() throws IOException {
        doReturn(request).when(web3j).ethGetBlockByNumber(
                any(DefaultBlockParameter.class),
                eq(true)
        );
        when(request.send()).thenThrow(new IOException());

        BlockAnalyzer blockAnalyzer = new BlockAnalyzer(web3j);
        EthBlock.Block result = blockAnalyzer.getBlock(BigInteger.ONE);

        assertNull(result);
        assertEquals(1, blockAnalyzer.getFailedBlockNumbers().size());
        assertEquals(BigInteger.ONE, blockAnalyzer.getFailedBlockNumbers().getFirst());
    }
}
