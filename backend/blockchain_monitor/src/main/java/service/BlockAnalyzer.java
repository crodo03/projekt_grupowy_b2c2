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
        BigInteger latest = getLatestBlockNumber();

        blocks = new ArrayList<>(numberOfBlocks);

        for(int i = 0; i < numberOfBlocks; i++) {
            BigInteger current = latest.subtract(BigInteger.valueOf(i));
            // TODO: WHEN GET BLOCK METHOD FAILS, FETCHES DIFFERENT NUMBER OF BLOCKS
            blocks.add(getBlockResponseObject(current));
        }
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
