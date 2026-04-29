import access.BlockchainClient;
import access.NodeConfig;
import io.javalin.Javalin;
import io.javalin.http.sse.SseClient;
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
//            ExecutorService executor = Executors.newFixedThreadPool(10);

            // cors
            Javalin.create(javalinConfig -> {
                javalinConfig.bundledPlugins.enableCors(cors -> {
                    cors.addRule(it -> {
                        it.reflectClientOrigin = true;
                    });
                });

                javalinConfig.routes.sse("/sse", sseClient -> {
                    sseClient.keepAlive();

                    
                    Set<BigInteger> sentBlocks = ConcurrentHashMap.newKeySet();
                    List<CompletableFuture<BlockResponse>> futures = new ArrayList<>();
                    BigInteger blockNumber = blockAnalyzer.getLatestBlockNumber();

                    Instant start = Instant.now();
                    // TODO: FREEZES ON LOW NUMBERS LIKE 5. CHECK
                    for(int i = 0; i < 10; i++) {
                        BigInteger finalBlockNumber = blockNumber;

                        CompletableFuture<BlockResponse> future = CompletableFuture
                                .supplyAsync(() -> blockAnalyzer.getBlockResponseObject(finalBlockNumber))
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
                                System.out.println(e.getMessage());
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

