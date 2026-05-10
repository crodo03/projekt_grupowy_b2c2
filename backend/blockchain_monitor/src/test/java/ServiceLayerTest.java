import access.BlockchainClient;
import access.NodeConfig;
import exceptions.GetBlockException;
import network.dto.BlockResponse;
import network.dto.BlockTransactionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import service.BlockAnalyzer;
import service.TransactionAnalyzer;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ServiceLayerTest {
    private Web3j mockWeb3j = null;
    private EthBlockNumber mockEthBlockNumber = null;
    private Request<?, EthBlockNumber> mockRequest = null;
    private EthBlock mockEthBlock = null;
    private EthBlock.Block mockBlockBlock = null;
    private BlockAnalyzer blockAnalyzer = null;
    private TransactionAnalyzer transactionAnalyzer = null;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void declareMockedClasses() {
        mockWeb3j = mock(Web3j.class, RETURNS_DEEP_STUBS);
        mockEthBlockNumber = mock(EthBlockNumber.class);
        mockRequest = (Request<?, EthBlockNumber>) mock(Request.class);
        mockEthBlock = mock(EthBlock.class);
        mockBlockBlock = mock(EthBlock.Block.class);
        blockAnalyzer = new BlockAnalyzer(mockWeb3j);
        transactionAnalyzer = new TransactionAnalyzer(mockBlockBlock);
    }

    @Test
    public void shouldThrowGetBlockExceptionOnConnectionFailure() throws IOException {
        // forces web3j.ethBlockNumber() to return mocked request
        doReturn(mockRequest).when(mockWeb3j).ethBlockNumber();
        // forces request.send() to throw an exception
        when(mockRequest.send()).thenThrow(new IOException("connection failed"));
        @SuppressWarnings("unchecked")
        Consumer<BlockResponse> consumer = mock(Consumer.class);
        NodeConfig config = new NodeConfig();
        try(BlockchainClient client = new BlockchainClient(config)) {
            client.setWeb3j(mockWeb3j); // injecting mocked client
            assertThrows(GetBlockException.class,
                    () -> new BlockAnalyzer(client.getWeb3j()).getLatestBlocks(2, consumer)
            );
        }
    }

    @Test
    public void shouldReturnLatestBlockNumber() throws IOException {
        doReturn(mockRequest).when(mockWeb3j).ethBlockNumber();
        when(mockRequest.send()).thenReturn(mockEthBlockNumber);
        when(mockEthBlockNumber.getBlockNumber()).thenReturn(BigInteger.valueOf(12345));

        BlockAnalyzer blockAnalyzer = new BlockAnalyzer(mockWeb3j);

        BigInteger result = blockAnalyzer.getLatestBlockNumber();

        assertEquals(BigInteger.valueOf(12345), result);
    }

    @Test
    public void shouldThrowGetBlockExceptionOnIOException() throws IOException {
        doReturn(mockRequest).when(mockWeb3j).ethBlockNumber();
        when(mockRequest.send()).thenThrow(new IOException("connection failed"));

        assertThrows(GetBlockException.class, () -> new BlockAnalyzer(mockWeb3j).getLatestBlockNumber());
    }

    @Test
    public void shouldReturnBlockResponseObject() throws IOException {
        doReturn(mockRequest).when(mockWeb3j).ethGetBlockByNumber(
                any(DefaultBlockParameter.class),
                eq(true)
        );
        doReturn(mockEthBlock).when(mockRequest).send();
        when(mockEthBlock.getBlock()).thenReturn(mockBlockBlock);

        List<EthBlock.TransactionResult<?>> transactionResults = Collections.emptyList();
        doReturn(transactionResults).when(mockBlockBlock).getTransactions();
        when(mockBlockBlock.getHash()).thenReturn("0xHash");
        when(mockBlockBlock.getNumber()).thenReturn(BigInteger.TWO);

        BlockAnalyzer blockAnalyzer = new BlockAnalyzer(mockWeb3j);
        BlockResponse result = blockAnalyzer.getBlockResponseObject(BigInteger.ONE);

        assertEquals(BigInteger.TWO, result.getBlockNumber());
        assertEquals(0, result.getNumberOfTransactions());
        assertEquals("0xHash", result.getBlockHash());
    }

    @Test
    public void shouldReturnNullOnIOException() throws IOException {
        doReturn(mockRequest).when(mockWeb3j).ethGetBlockByNumber(
                any(DefaultBlockParameter.class),
                eq(true)
        );
        when(mockRequest.send()).thenThrow(new IOException());

        BlockAnalyzer blockAnalyzer = new BlockAnalyzer(mockWeb3j);
        EthBlock.Block result = blockAnalyzer.getBlock(BigInteger.ONE);

        assertNull(result);
        assertEquals(1, blockAnalyzer.getFailedBlockNumbers().size());
        assertEquals(BigInteger.ONE, blockAnalyzer.getFailedBlockNumbers().getFirst());
    }

    @Test
    public void getLatestBlocks_doesNotSendDuplicateBlocks() throws IOException {
        when(mockBlockBlock.getNumber()).thenReturn(BigInteger.ONE);
        when(mockEthBlock.getBlock()).thenReturn(mockBlockBlock);
        when(mockWeb3j.ethGetBlockByNumber(any(), anyBoolean()).send()).thenReturn(mockEthBlock);

        List<BlockResponse> received = new ArrayList<>();
        var futures = blockAnalyzer.getLatestBlocks(3, received::add);

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        assertEquals(1, received.size());
    }

    @Test
    public void getTransactionInfo_mapsTransactionFieldsCorrectly() {
        var transactionObject = mock(EthBlock.TransactionObject.class);
        when(transactionObject.getHash()).thenReturn("0xHash");
        when(transactionObject.getTo()).thenReturn("0xTo");
        when(transactionObject.getFrom()).thenReturn("0xFrom");
        when(transactionObject.getValue()).thenReturn(BigInteger.ONE);
        when(transactionObject.getGas()).thenReturn(BigInteger.TEN);

        var transactionResult = mock(EthBlock.TransactionResult.class);
        when(transactionResult.get()).thenReturn(transactionObject);

        when(mockBlockBlock.getTransactions()).thenReturn(List.of(transactionResult));
        List<BlockTransactionInfo> result = transactionAnalyzer.getTransactionInfo();
        assertEquals(1, result.size());

        BlockTransactionInfo info = result.getFirst();
        assertAll(
            () -> assertEquals("0xHash", info.getHash()),
            () -> assertEquals("0xTo", info.getTo()),
            () -> assertEquals("0xFrom", info.getFrom()),
            () -> assertEquals(BigInteger.ONE, info.getValue()),
            () -> assertEquals(BigInteger.TEN, info.getGas())
        );
    }

    @Test
    public void getTransactionInfo_emptyBlock_returnsEmptyList() {
        when(mockBlockBlock.getTransactions()).thenReturn(Collections.emptyList());
        List<BlockTransactionInfo> result = transactionAnalyzer.getTransactionInfo();
        assertTrue(result.isEmpty());
    }
}
