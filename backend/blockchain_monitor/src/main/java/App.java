import access.BlockchainClient;
import access.NodeConfig;
import io.javalin.Javalin;
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
class App {
    public static void main(String[] args) {
        NodeConfig config = new NodeConfig();

        try(BlockchainClient client = new BlockchainClient(config)) {
            BlockAnalyzer blockAnalyzer = new BlockAnalyzer(client.getWeb3j());
            blockAnalyzer.getLatestBlocks(2);
            blockAnalyzer.getBlocks().forEach(System.out::println);
//            blockAnalyzer.getLatestBlocks(10);
//            blockAnalyzer.getFailedBlocksNumbers();

//            List<BlockResponse> response = blockAnalyzer.getBlocks();
//            TransactionAnalyzer transactionAnalyzer = new TransactionAnalyzer(block);
//            transactionAnalyzer.getTransactionInfo();
            ExecutorService executor = Executors.newFixedThreadPool(10);

            // TODO: PUT THIS IN SEPARATE PACKAGES, CREATE CONTROLLER
            Javalin.create(javalinConfig -> {
                javalinConfig.routes.get("/", context -> {
                    // TODO: CLOSE SSE CLIENT
                    SseClient sseClient = new SseClient(context);
                    sseClient.keepAlive();

                    Set<BigInteger> sentBlocks = ConcurrentHashMap.newKeySet();
                    List<CompletableFuture<BlockResponse>> futures = new ArrayList<>();
                    BigInteger blockNumber = blockAnalyzer.getLatestBlockNumber();

                    Instant start = Instant.now();
                    // TODO: FREEZES ON LOW NUMBERS LIKE 5. CHECK
                    // TODO: REPLACE FOR LOOP FOR GET LATEST BLOCKS METHOD
                    for(int i = 0; i < 10; i++) {
                        BigInteger finalBlockNumber = blockNumber;

                        CompletableFuture<BlockResponse> future = CompletableFuture
                                .supplyAsync(() -> blockAnalyzer.getBlockResponseObject(finalBlockNumber), executor)
                                .thenApply(block -> {
                                    if(sentBlocks.add(block.getBlockNumber())) {
                                        sseClient.sendEvent("block", block);
                                    }
                                    return block;
                                });

                        futures.add(future);
                        blockNumber = blockNumber.subtract(BigInteger.valueOf(1));
                    }
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .thenRun(() -> {
                                double timeElapsed = (double) Duration.between(start, Instant.now()).toMillis() / 1000;
                                sseClient.sendEvent("done", "done in " + timeElapsed + "seconds");
                            })
                            .exceptionally(e -> {
                                log.error(e.getMessage());
                                sseClient.close();
                                return null;
                            });
                });

                javalinConfig.routes.get("/block/{block-number}", context -> {
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
                });

                javalinConfig.routes.post("/post", context -> context.status(200));
            }).start(8080);
        }
    }
}

