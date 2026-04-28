import access.BlockchainClient;
import access.NodeConfig;
import io.javalin.Javalin;
import io.javalin.http.sse.SseClient;
import network.dto.BlockResponse;
import org.web3j.protocol.core.methods.response.EthBlock;
import service.BlockAnalyzer;
import service.TransactionAnalyzer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class App {
    public static void main(String[] args) {
        NodeConfig config = new NodeConfig();

        try(BlockchainClient client = new BlockchainClient(config)) {
            BlockAnalyzer blockAnalyzer = new BlockAnalyzer(client.getWeb3j());
//            blockAnalyzer.getLatestBlocks(10);
//            blockAnalyzer.getFailedBlocksNumbers();

//            List<BlockResponse> response = blockAnalyzer.getBlocks();
//            TransactionAnalyzer transactionAnalyzer = new TransactionAnalyzer(block);
//            transactionAnalyzer.getTransactionInfo();
            // Thread pool sized to your RPC rate limits
            ExecutorService executor = Executors.newFixedThreadPool(10);

            Javalin.create(javalinConfig -> {
                javalinConfig.routes.get("/", context -> {
                    SseClient sseClient = new SseClient(context);
                    sseClient.keepAlive();

                    Set<BigInteger> sentBlocks = ConcurrentHashMap.newKeySet();
                    List<CompletableFuture<BlockResponse>> futures = new ArrayList<>();

                    BigInteger blockNumber = blockAnalyzer.getLatestBlockNumber();
                    for(int i = 0; i < 100; i++) {
                        BigInteger finalBlockNumber = blockNumber;

                        CompletableFuture<BlockResponse> future = CompletableFuture
                                .supplyAsync(() -> blockAnalyzer.getAnotherBlock(finalBlockNumber), executor)
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
                            .thenRun(() -> sseClient.sendEvent("done", "{}"));
                });
                javalinConfig.routes.post("/post", context -> context.status(200));
            }).start(8080);
        }
    }
}

