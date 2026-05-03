package network;

import io.javalin.http.Context;
import io.javalin.http.sse.SseClient;
import lombok.extern.slf4j.Slf4j;
import network.dto.BlockResponse;
import network.dto.BlockTransactionInfo;
import org.web3j.protocol.core.methods.response.EthBlock;
import service.BlockAnalyzer;
import service.TransactionAnalyzer;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class RestController {
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final BlockAnalyzer blockAnalyzer;

    public RestController(BlockAnalyzer blockAnalyzer) {
        this.blockAnalyzer = blockAnalyzer;
    }

    public void getLatestBlocks(SseClient sseClient) {
        sseClient.keepAlive();

        Set<BigInteger> sentBlocks = ConcurrentHashMap.newKeySet();
        List<CompletableFuture<BlockResponse>> futures = new ArrayList<>();
        BigInteger blockNumber = blockAnalyzer.getLatestBlockNumber();

        Instant start = Instant.now();
        // TODO: FREEZES ON LOW NUMBERS LIKE 5. CHECK
        for(int i = 0; i < 10; i++) {
            BigInteger finalBlockNumber = blockNumber;

            CompletableFuture<BlockResponse> future = CompletableFuture
                    .supplyAsync(() -> blockAnalyzer.getBlockResponseObject(finalBlockNumber), executor)
                    .thenApply(block -> {
                        if(block == null) {
                            log.error("block is null, skipping");
                            return null;
                        }
                        if(sentBlocks.add(block.getBlockNumber())) {
                            sseClient.sendEvent("block", block);
                        }
                        return block;
                    })
                    .exceptionally(e -> {
                        log.error("future failed: {}", e.getMessage());
                        return null;
                    });

            futures.add(future);
            blockNumber = blockNumber.subtract(BigInteger.valueOf(1));
        }
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
        BigInteger blockNumber = null;
        try {
            blockNumber = new BigInteger(context.pathParam("block-number"));
        } catch(NumberFormatException e) {
            context.json("error").status(404);
        }
        EthBlock.Block block = blockAnalyzer.getBlock(blockNumber);
        TransactionAnalyzer transactionAnalyzer = new TransactionAnalyzer(block);
        List<BlockTransactionInfo> transactionInfoList = transactionAnalyzer.getTransactionInfo();
        context.json(transactionInfoList).status(200);
    }
}
