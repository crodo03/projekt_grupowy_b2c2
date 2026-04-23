package service;

import exceptions.GetBlockException;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthBlockNumber;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class BlockAnalyzer {
    private final Web3j web3j;
    private List<EthBlock.Block> blocks;
    private final List<BigInteger> failedBlockNumbers = new ArrayList<>();

    public BlockAnalyzer(Web3j web3j) {
        this.web3j = web3j;
    }

    public void getLatestBlocksInfo(int numberOfBlocks) {
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
            EthBlock blockResponse;
            try {
                blockResponse = web3j.ethGetBlockByNumber(
                        DefaultBlockParameter.valueOf(current),
                        true // include transactions
                ).send();
                blocks.add(blockResponse.getBlock());
            } catch(IOException e) {
                System.out.println("skipping block " + current + " | " + e.getMessage());
                failedBlockNumbers.add(current);
                // throw new GetBlockException(current, e);
            }
        }

        blocks.forEach(block -> {
            int numberOfTransactions = block.getTransactions().size();
            String blockHash = block.getHash();
            BigInteger blockNumber = block.getNumber();
            System.out.println("number of transactions: " + numberOfTransactions);
            System.out.println("block hash: " + blockHash);
            System.out.println("block number: " + blockNumber);
        });
    }

    public void getFailedBlocksNumbers() {
        if(failedBlockNumbers.isEmpty()) {
            System.out.println("no failed blocks");
            return;
        }
        failedBlockNumbers.forEach(System.out::println);
    }

    public List<EthBlock.Block> getBlocks() {
        return blocks;
    }
}
