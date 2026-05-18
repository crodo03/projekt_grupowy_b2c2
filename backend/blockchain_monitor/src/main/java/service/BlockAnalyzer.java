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
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Getter
@Slf4j
public class BlockAnalyzer {
    private final Web3j web3j;
    private final List<BigInteger> failedBlockNumbers = new ArrayList<>();
    private final Deque<BlockResponse> currentBlocks = new ConcurrentLinkedDeque<>();
    private BigInteger latestKnownBlock = BigInteger.ZERO;
    private final int maxQueueSize = 100;
    private int totalNumberOfBlocks = 0;
    private int totalNumberOfTransactions = 0;

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

        for(int i = 0; i < numberOfBlocks; i++) {
            BigInteger finalBlockNumber = blockNumber;

            CompletableFuture<BlockResponse> future = CompletableFuture
                    .supplyAsync(() -> getBlockResponseObject(finalBlockNumber), executor)
                    .thenApply(block -> {
                        if(block == null) {
                            log.error("block is null, skipping");
                            failedBlockNumbers.add(finalBlockNumber);
                            return null;
                        }
                        if(sentBlocks.add(block.getBlockNumber())) {
                            onBlockReady.accept(block);
                        }
                        addBlockToQueue(block);
                        totalNumberOfBlocks += 1;
                        return block;
                    })
                    .exceptionally(e -> {
                        log.error("future failed: {}", e.getMessage());
                        return null;
                    });

            futures.add(future);
            blockNumber = blockNumber.subtract(BigInteger.ONE);
        }
        latestKnownBlock = blockNumber;
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
        if (block == null) {
            log.warn("block {} is null", blockNumber);
            return null;
        }

        return BlockResponse.builder()
                .numberOfTransactions(block.getTransactions().size())
                .blockHash(block.getHash())
                .blockNumber(block.getNumber())
                .fetchedAt(BlockResponse.getCurrentTime())
                .build();
    }

    public List<BlockResponse> pollForNewBlocks() {
        BigInteger latest = getLatestBlockNumber();
        if(latest.compareTo(latestKnownBlock) <= 0) {
            return Collections.emptyList();
        }

        latestKnownBlock = latest;
        BlockResponse newBlock = getBlockResponseObject(latest);
        if(newBlock == null) {
            log.warn("could not fetch block number {}", latest);
            failedBlockNumbers.add(latest);
            return Collections.emptyList();
        }

        if(!currentBlocks.contains(newBlock)) {
            addBlockToQueue(newBlock);
            totalNumberOfBlocks += 1;
            log.info("added {} to queue", newBlock);
            return getQueueAsList();
        }
        return Collections.emptyList();
    }

    private void addBlockToQueue(BlockResponse block) {
        currentBlocks.addFirst(block);
        totalNumberOfTransactions += block.getNumberOfTransactions();
        if(currentBlocks.size() > maxQueueSize) {
            currentBlocks.removeLast();
        }
    }

    public List<BlockResponse> getQueueAsList() {
        return new ArrayList<>(currentBlocks);
    }
}
