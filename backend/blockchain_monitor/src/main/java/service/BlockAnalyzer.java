package service;

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

    public BlockAnalyzer(Web3j web3j) {
        this.web3j = web3j;
    }

    public void getLatestBlocksInfo(int numberOfBlocks) {
        try {
            EthBlockNumber blockNumberResponse = web3j.ethBlockNumber().send();
            BigInteger latest = blockNumberResponse.getBlockNumber();

            blocks = new ArrayList<>(numberOfBlocks);

            for(int i = 0; i < numberOfBlocks; i++) {
                BigInteger current = latest.subtract(BigInteger.valueOf(i));

                EthBlock blockResponse = web3j.ethGetBlockByNumber(
                        DefaultBlockParameter.valueOf(current),
                        true // include transactions
                ).send();

                blocks.add(blockResponse.getBlock());
            }

            blocks.forEach(block -> {
                int numberOfTransactions = block.getTransactions().size();
                String blockHash = block.getHash();
                BigInteger blockNumber = block.getNumber();
                System.out.println("number of transactions: " + numberOfTransactions);
                System.out.println("block hash: " + blockHash);
                System.out.println("block number: " + blockNumber);
            });
        } catch(IOException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public List<EthBlock.Block> getBlocks() {
        return blocks;
    }
}
