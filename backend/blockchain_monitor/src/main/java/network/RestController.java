package network;

import io.javalin.http.Context;
import io.javalin.http.sse.SseClient;
import lombok.extern.slf4j.Slf4j;
import network.dto.BlockTransactionInfo;
import org.web3j.protocol.core.methods.response.EthBlock;
import service.BlockAnalyzer;
import service.TransactionAnalyzer;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class RestController {
    private final BlockAnalyzer blockAnalyzer;

    public RestController(BlockAnalyzer blockAnalyzer) {
        this.blockAnalyzer = blockAnalyzer;
    }

    public void getLatestBlocks(SseClient sseClient) {
        sseClient.keepAlive();

        Instant start = Instant.now();
        var futures = blockAnalyzer.getLatestBlocks(
                10,
                block -> sseClient.sendEvent("block", block)
        );

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    double timeElapsed = (double) Duration.between(start, Instant.now()).toMillis() / 1000;
                    sseClient.sendEvent("done", "done in " + timeElapsed + "seconds");
                    sseClient.close();
                })
                .exceptionally(e -> {
                    System.out.println(e.getMessage());
                    return null;
                })
                .join();
    }

    public void getBlockInfo(Context context) {
        BigInteger blockNumber;
        try {
            blockNumber = new BigInteger(context.pathParam("block-number"));
        } catch(NumberFormatException e) {
            context.json("error").status(404);
            return;
        }
        EthBlock.Block block = blockAnalyzer.getBlock(blockNumber);
        if(block == null) {
            context.json("error").status(404);
            return;
        }
        TransactionAnalyzer transactionAnalyzer = new TransactionAnalyzer(block);
        List<BlockTransactionInfo> transactionInfoList = transactionAnalyzer.getTransactionInfo();
        context.json(transactionInfoList).status(200);
    }
}
