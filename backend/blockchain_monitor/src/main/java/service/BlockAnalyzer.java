package service;

import exceptions.GetBlockException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.dto.BlockResponse;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthBlockNumber;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Getter
@Slf4j
public class BlockAnalyzer {
    private final Web3j web3j;
    private List<BlockResponse> blocks;
    private final List<BigInteger> failedBlockNumbers = new ArrayList<>();

    public BlockAnalyzer(Web3j web3j) {
        this.web3j = web3j;
    }

    public BigInteger getLatestBlockNumber() {
        BigInteger latest;
        try {
            EthBlockNumber blockNumberResponse = web3j.ethBlockNumber().send();
            latest = blockNumberResponse.getBlockNumber();
        } catch(IOException e) {
            throw new GetBlockException(e);
        }
        return latest;
    }

    public List<CompletableFuture<BlockResponse>> getLatestBlocks(int numberOfBlocks, Consumer<BlockResponse> onBlockReady) {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        Set<BigInteger> sentBlocks = ConcurrentHashMap.newKeySet();
        List<CompletableFuture<BlockResponse>> futures = new ArrayList<>();
        BigInteger blockNumber = getLatestBlockNumber();
        blocks = new ArrayList<>(numberOfBlocks);

        // TODO: FREEZES ON LOW NUMBERS LIKE 5. CHECK
        for(int i = 0; i < numberOfBlocks; i++) {
            BigInteger finalBlockNumber = blockNumber;

            CompletableFuture<BlockResponse> future = CompletableFuture
                    // TODO: WHEN GET BLOCK METHOD FAILS, FETCHES DIFFERENT NUMBER OF BLOCKS
                    .supplyAsync(() -> getBlockResponseObject(finalBlockNumber), executor)
                    .thenApply(block -> {
                        if(block == null) {
                            log.error("block is null, skipping");
                            return null;
                        }
                        if(sentBlocks.add(block.getBlockNumber())) {
                            onBlockReady.accept(block);
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
        return futures;
    }

    public EthBlock.Block getBlock(BigInteger blockNumber) {
        EthBlock.Block block;
        try {
            block = web3j.ethGetBlockByNumber(
                    DefaultBlockParameter.valueOf(blockNumber),
                    true
            ).send().getBlock();
        } catch(IOException e) {
            System.out.println("skipping block " + blockNumber + " | " + e.getMessage());
            failedBlockNumbers.add(blockNumber);
            return null;
        }
        return block;
    }

    public BlockResponse getBlockResponseObject(BigInteger blockNumber)  {
        EthBlock.Block block = getBlock(blockNumber);

        return BlockResponse.builder()
                .numberOfTransactions(block.getTransactions().size())
                .blockHash(block.getHash())
                .blockNumber(block.getNumber())
                .build();
    }
}
