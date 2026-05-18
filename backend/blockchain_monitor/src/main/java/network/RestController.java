package network;

import io.javalin.http.Context;
import io.javalin.http.sse.SseClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import network.dto.BlockResponse;
import org.web3j.protocol.core.methods.response.EthBlock;
import report.ReportFileCreator;
import service.BlockAnalyzer;
import service.TransactionAnalyzer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class RestController {
    private final BlockAnalyzer blockAnalyzer;
    private SseClient sseClient;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public void sendLatestBlocks(SseClient sseClient) {
        this.sseClient = sseClient;
        sseClient.keepAlive();

        Instant start = Instant.now();
        var futures = blockAnalyzer.getLatestBlocks(
                100,
                block -> sseClient.sendEvent("block", block)
        );

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    double timeElapsed = (double) Duration.between(start, Instant.now()).toMillis() / 1000;
                    sseClient.sendEvent("done", "done in " + timeElapsed + "seconds");
                    scheduler.scheduleAtFixedRate(this::startPolling, 2, 2, TimeUnit.SECONDS);
                })
                .exceptionally(e -> {
                    System.out.println(e.getMessage());
                    return null;
                })
                .join();
    }

    public void sendBlockInfo(Context context) {
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
        var transactionInfoResult = transactionAnalyzer.getTransactionInfo();
        context.json(transactionInfoResult).status(200);
    }

    public void serveCsvFile(Context context) {
        ReportFileCreator reportFileCreator = new ReportFileCreator(blockAnalyzer);
        String filename = reportFileCreator.createCSVFile();

        File file = new File(filename);
        try {
            context.contentType("text/csv");
            context.header("Content-Disposition\", \"attachment; filename=report.csv");
            context.result(new FileInputStream("backend/blockchain_monitor/" + file));
        } catch(FileNotFoundException e) {
            context.status(404).result("report file not found");
        }
    }

    private void startPolling() {
        // not connected yet or never got any blocks
        if(sseClient == null) {
            log.info("sse client null");
            return;
        }

        List<BlockResponse> newBlocks = blockAnalyzer.pollForNewBlocks();
        if(!newBlocks.isEmpty()) {
            sseClient.sendEvent("updated_blocks", newBlocks);
        }
    }

    public void closeSSE() {
        if (sseClient != null) {
            sseClient.close();
            sseClient = null;
        }
    }
}
