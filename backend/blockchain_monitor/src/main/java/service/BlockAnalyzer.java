package service;

import exceptions.GetBlockException;
import lombok.Getter;
import network.dto.BlockResponse;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthBlockNumber;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Getter
public class BlockAnalyzer {
    private final Web3j web3j;
    private List<BlockResponse> blocks;
    private final List<BigInteger> failedBlockNumbers = new ArrayList<>();

    public BlockAnalyzer(Web3j web3j) {
        this.web3j = web3j;
    }

    public BlockResponse getAnotherBlock(BigInteger blockNumber) {
        BlockResponse blockResponse = null;
        try {
            blockResponse = getBlockResponseObject(blockNumber);
        } catch(IOException e) {
            System.out.println("skipping block " + blockNumber + " | " + e.getMessage());
            failedBlockNumbers.add(blockNumber);
        }
        return blockResponse;
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

    public void getLatestBlocks(int numberOfBlocks) {
        BigInteger latest;
        try {
            EthBlockNumber blockNumberResponse = web3j.ethBlockNumber().send();
            latest = blockNumberResponse.getBlockNumber();
        } catch(IOException e) {
            throw new GetBlockException(e);
        }

        blocks = new ArrayList<>(numberOfBlocks);

        for(int i = 0; i < numberOfBlocks; i++) {
            BigInteger current = latest.subtract(BigInteger.valueOf(i));
            try {
                blocks.add(getBlockResponseObject(current));
            } catch(IOException e) {
                System.out.println("skipping block " + current + " | " + e.getMessage());
                failedBlockNumbers.add(current);
                // throw new GetBlockException(current, e);
            }
        }
    }

    public BlockResponse getBlockResponseObject(BigInteger blockNumber) throws IOException {
        EthBlock.Block block = web3j.ethGetBlockByNumber(
                DefaultBlockParameter.valueOf(blockNumber),
                true // include transactions
        ).send().getBlock();

        return BlockResponse.builder()
                .numberOfTransactions(block.getTransactions().size())
                .blockHash(block.getHash())
                .blockNumber(block.getNumber())
                .build();
    }

    public void getFailedBlocksNumbers() {
        if(failedBlockNumbers.isEmpty()) {
            System.out.println("no failed blocks");
            return;
        }
        failedBlockNumbers.forEach(System.out::println);
    }
}
