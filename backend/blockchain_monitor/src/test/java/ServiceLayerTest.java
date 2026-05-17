import access.BlockchainClient;
import access.NodeConfig;
import exceptions.GetBlockException;
import network.dto.BlockResponse;
import network.dto.BlockTransactionInfo;
import network.dto.TransactionInfoResult;
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
    public void getLatestBlocks_nullBlock_doesNotCallCallback() throws IOException {
        when(mockEthBlockNumber.getBlockNumber()).thenReturn(BigInteger.ONE);
        when(mockWeb3j.ethBlockNumber().send()).thenReturn(mockEthBlockNumber);
        when(mockEthBlock.getBlock()).thenReturn(null);
        when(mockWeb3j.ethGetBlockByNumber(any(), anyBoolean()).send()).thenReturn(mockEthBlock);

        List<BlockResponse> received = new ArrayList<>();
        List<CompletableFuture<BlockResponse>> futures = blockAnalyzer.getLatestBlocks(1, received::add);
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        assertTrue(received.isEmpty());
    }

    @Test
    public void getLatestBlocks_exceptionThrown_doesNotCallCallback() throws IOException {
        when(mockEthBlockNumber.getBlockNumber()).thenReturn(BigInteger.ONE);
        when(mockWeb3j.ethBlockNumber().send()).thenReturn(mockEthBlockNumber);
        when(mockWeb3j.ethGetBlockByNumber(any(), anyBoolean()).send())
                .thenThrow(new RuntimeException("network error"));

        List<BlockResponse> received = new ArrayList<>();
        List<CompletableFuture<BlockResponse>> futures = blockAnalyzer.getLatestBlocks(1, received::add);
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        assertTrue(received.isEmpty());
    }

    @Test
    public void getTransactionInfo_mapsTransactionFieldsCorrectly() {
        var transactionObject = mock(EthBlock.TransactionObject.class);
        setTransactionObjectReturnValues(transactionObject);

        var transactionResult = mock(EthBlock.TransactionResult.class);
        when(transactionResult.get()).thenReturn(transactionObject);
        when(mockBlockBlock.getTransactions()).thenReturn(List.of(transactionResult));

        TransactionAnalyzer analyzer = new TransactionAnalyzer(mockBlockBlock);
        TransactionInfoResult result = analyzer.getTransactionInfo();

        var transactionInfoList = result.getBlockTransactionInfoList();
        assertEquals(1, transactionInfoList.size());

        BlockTransactionInfo info = transactionInfoList.getFirst();
        assertAll(
                () -> assertEquals("0xHash", info.getHash()),
                () -> assertEquals("0xTo", info.getTo()),
                () -> assertEquals("0xFrom", info.getFrom()),
                () -> assertEquals(BigInteger.ONE, info.getValue()),
                () -> assertEquals(BigInteger.TEN, info.getGas()),
                () -> assertEquals(BigInteger.TWO, info.getGasPrice())
        );

        // mean of 2 and 2 = 2
        assertEquals(BigInteger.valueOf(2), result.getGasPricesMean());
    }

    @Test
    public void getTransactionInfo_calculatesMeanGasPrice() {
        var transactionObject1 = mock(EthBlock.TransactionObject.class);
        when(transactionObject1.getGasPrice()).thenReturn(BigInteger.TWO);
        var transactionObject2 = mock(EthBlock.TransactionObject.class);
        when(transactionObject2.getGasPrice()).thenReturn(BigInteger.valueOf(4));

        setTransactionObjectReturnValues(transactionObject1);
        setTransactionObjectReturnValues(transactionObject2);

        var result1 = mock(EthBlock.TransactionResult.class);
        var result2 = mock(EthBlock.TransactionResult.class);
        when(result1.get()).thenReturn(transactionObject1);
        when(result2.get()).thenReturn(transactionObject2);
        when(mockBlockBlock.getTransactions()).thenReturn(List.of(result1, result2));

        TransactionAnalyzer analyzer = new TransactionAnalyzer(mockBlockBlock);
        TransactionInfoResult result = analyzer.getTransactionInfo();

        // mean of 2 and 2 = 2
        assertEquals(BigInteger.valueOf(2), result.getGasPricesMean());
    }

    @Test
    public void getTransactionInfo_emptyBlock_returnsEmptyList() {
        when(mockBlockBlock.getTransactions()).thenReturn(Collections.emptyList());
        TransactionInfoResult result = transactionAnalyzer.getTransactionInfo();
        var transactionInfoList = result.getBlockTransactionInfoList();
        assertTrue(transactionInfoList.isEmpty());
    }

    @Test
    public void getTransactionInfo_nullGasPrice_doesNotThrow() {
        var transactionObject = mock(EthBlock.TransactionObject.class);

        // change gasPrice to null, set everything else to avoid NPE
        setTransactionObjectReturnValues(transactionObject);
        when(transactionObject.getGasPrice()).thenReturn(null);

        var transactionResult = mock(EthBlock.TransactionResult.class);
        when(transactionResult.get()).thenReturn(transactionObject);
        when(mockBlockBlock.getTransactions()).thenReturn(List.of(transactionResult));

        TransactionInfoResult result = transactionAnalyzer.getTransactionInfo();

        assertEquals(BigInteger.ZERO, result.getGasPricesMean());
    }

    private void setTransactionObjectReturnValues(EthBlock.TransactionObject transactionObject) {
        when(transactionObject.getHash()).thenReturn("0xHash");
        when(transactionObject.getTo()).thenReturn("0xTo");
        when(transactionObject.getFrom()).thenReturn("0xFrom");
        when(transactionObject.getValue()).thenReturn(BigInteger.ONE);
        when(transactionObject.getGas()).thenReturn(BigInteger.TEN);
        when(transactionObject.getGasPrice()).thenReturn(BigInteger.TWO);
    }

    @Test
    public void pollForNewBlocks_latestBlockIsTheSame_returnsEmptyList() throws IOException {
        // same as the default value
        when(mockEthBlockNumber.getBlockNumber()).thenReturn(BigInteger.ZERO);
        when(mockWeb3j.ethBlockNumber().send()).thenReturn(mockEthBlockNumber);

        List<BlockResponse> result = blockAnalyzer.pollForNewBlocks();

        assertTrue(result.isEmpty());
    }

    @Test
    public void pollForNewBlocks_blockResponseIsNull_returnsEmptyList() throws IOException {
        when(mockEthBlockNumber.getBlockNumber()).thenReturn(BigInteger.ONE);
        when(mockWeb3j.ethBlockNumber().send()).thenReturn(mockEthBlockNumber);

        when(mockEthBlock.getBlock()).thenReturn(null);
        when(mockWeb3j.ethGetBlockByNumber(any(), anyBoolean()).send()).thenReturn(mockEthBlock);

        List<BlockResponse> result = blockAnalyzer.pollForNewBlocks();
        assertTrue(result.isEmpty());
    }

    @Test
    public void pollForNewBlocks_happyPath_returnsList() throws IOException {
        when(mockEthBlockNumber.getBlockNumber()).thenReturn(BigInteger.ONE);
        when(mockWeb3j.ethBlockNumber().send()).thenReturn(mockEthBlockNumber);

        when(mockEthBlock.getBlock()).thenReturn(mockBlockBlock);
        when(mockWeb3j.ethGetBlockByNumber(any(), anyBoolean()).send()).thenReturn(mockEthBlock);
        when(mockBlockBlock.getTransactions()).thenReturn(Collections.emptyList());

        List<BlockResponse> result = blockAnalyzer.pollForNewBlocks();
        assertEquals(1, result.size());
    }

    @Test
    public void pollForNewBlocks_noNewBlockSinceLastCall_returnsEmptyList() throws IOException {
        when(mockEthBlockNumber.getBlockNumber()).thenReturn(BigInteger.ONE);
        when(mockWeb3j.ethBlockNumber().send()).thenReturn(mockEthBlockNumber);

        when(mockEthBlock.getBlock()).thenReturn(mockBlockBlock);
        when(mockWeb3j.ethGetBlockByNumber(any(), anyBoolean()).send()).thenReturn(mockEthBlock);
        when(mockBlockBlock.getTransactions()).thenReturn(Collections.emptyList());

        blockAnalyzer.pollForNewBlocks();
        List<BlockResponse> result = blockAnalyzer.pollForNewBlocks();
        assertEquals(0, result.size());
    }
}
