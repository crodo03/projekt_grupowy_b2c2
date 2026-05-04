import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import network.RestController;
import org.junit.jupiter.api.Test;
import org.web3j.protocol.core.methods.response.EthBlock;
import service.BlockAnalyzer;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NetworkLayerTest {
    private final BlockAnalyzer blockAnalyzer = mock(BlockAnalyzer.class);
    private final RestController restController = new RestController(blockAnalyzer);

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
}

