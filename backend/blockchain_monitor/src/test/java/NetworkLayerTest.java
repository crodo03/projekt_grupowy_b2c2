import io.javalin.Javalin;
import io.javalin.http.sse.SseClient;
import io.javalin.testtools.JavalinTest;
import network.RestController;
import network.dto.BlockResponse;
import org.junit.jupiter.api.Test;
import org.web3j.protocol.core.methods.response.EthBlock;
import service.BlockAnalyzer;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.Mockito.*;

public class NetworkLayerTest {
    private final BlockAnalyzer blockAnalyzer = mock(BlockAnalyzer.class);
    private final RestController restController = new RestController(blockAnalyzer);
    private final SseClient sseClient = mock(SseClient.class);

    private Javalin app() {
        return Javalin.create(
                config -> config.routes.get(
                        "/block/{block-number}", restController::getBlockInfo
                )
        );
    }

    @Test
    public void getBlockInfo_validBlock_returns200() {
        EthBlock.Block mockedBlock = mock(EthBlock.Block.class);

        when(blockAnalyzer.getBlock(BigInteger.ONE)).thenReturn(mockedBlock);

        JavalinTest.test(app(), (server, client) -> {
            var response = client.get("/block/1");

            assertEquals(200, response.code());
        });
    }

    @Test
    public void getBlockInfo_validBlock_returnsJsonList() {
        EthBlock.Block mockedBlock = mock(EthBlock.Block.class);

        when(blockAnalyzer.getBlock(BigInteger.ONE)).thenReturn(mockedBlock);

        JavalinTest.test(app(), (server, client) -> {
            var response = client.get("/block/1");
            List<String> headers = response.headers().get("Content-type");

            assertNotNull(headers);
            assertTrue(headers.contains("application/json"));
            assertTrue(response.body().string().startsWith("["));
        });
    }

    @Test
    public void getBlockInfo_invalidBlock_returns404() {
        JavalinTest.test(app(), (server, client) -> {
            var response = client.get("/block/not-a-number");

            assertEquals(404, response.code());
        });
    }

    @Test
    public void getBlockInfo_blockNotFound_returns404() {
        when(blockAnalyzer.getBlock(BigInteger.ONE)).thenReturn(null);

        JavalinTest.test(app(), (server, client) -> {
            var response = client.get("/block/1");

            assertEquals(404, response.code());
        });
    }

    @Test
    public void getLatestBlocks_sendsBlockEvents() {
        BlockResponse block1 = BlockResponse.getTestBlock(BigInteger.ONE);
        BlockResponse block2 = BlockResponse.getTestBlock(BigInteger.TWO);

        when(blockAnalyzer.getLatestBlockNumber()).thenReturn(BigInteger.TWO);
        when(blockAnalyzer.getBlockResponseObject(BigInteger.TWO)).thenReturn(block2);
        when(blockAnalyzer.getBlockResponseObject(BigInteger.ONE)).thenReturn(block1);
        // every other number returns null
        when(blockAnalyzer.getBlockResponseObject(
                not(
                        or(eq(BigInteger.ONE), eq(BigInteger.TWO)))
                )
        ).thenReturn(null);

        restController.getLatestBlocks(sseClient);

        verify(sseClient).sendEvent("block", block1);
        verify(sseClient).sendEvent("block", block2);
    }

    @Test
    public void getLatestBlocks_sendsDoneEvent() {
        when(blockAnalyzer.getLatestBlockNumber()).thenReturn(BigInteger.ONE);
        when(blockAnalyzer.getBlockResponseObject(any())).thenReturn(null);

        restController.getLatestBlocks(sseClient);

        verify(sseClient).sendEvent(eq("done"), anyString());
    }

    @Test
    public void getLatestBlocks_closesSseClientWhenDone() {
        when(blockAnalyzer.getLatestBlockNumber()).thenReturn(BigInteger.ONE);
        when(blockAnalyzer.getBlockResponseObject(any())).thenReturn(null);

        restController.getLatestBlocks(sseClient);

        verify(sseClient).close();
    }

    @Test
    public void getLatestBlocks_doesNotSendDuplicateBlocks() {
        BlockResponse block = BlockResponse.getTestBlock(BigInteger.ONE);

        when(blockAnalyzer.getLatestBlockNumber()).thenReturn(BigInteger.ONE);
        // same block everytime
        when(blockAnalyzer.getBlockResponseObject(any())).thenReturn(block);

        restController.getLatestBlocks(sseClient);

        verify(sseClient).sendEvent("block", block);
    }

    @Test
    public void getLatestBlocks_nullBlockDoesNotSendBlockEvent() {
        when(blockAnalyzer.getLatestBlockNumber()).thenReturn(BigInteger.ONE);
        when(blockAnalyzer.getBlockResponseObject(any())).thenReturn(null);

        restController.getLatestBlocks(sseClient);

        // if null check is missing, null pointer exception is swallowed
        // but sentBlocks.add() never runs
        // either way we should never see a block event with null data
        verify(sseClient, never()).sendEvent(eq("block"), isNull());
        verify(sseClient, never()).sendEvent(eq("block"), any(BlockResponse.class));
        verify(sseClient).close();
    }
}

