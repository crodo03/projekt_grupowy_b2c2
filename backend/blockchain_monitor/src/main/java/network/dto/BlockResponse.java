package network.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigInteger;
import java.util.Random;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class BlockResponse {
    private int numberOfTransactions;
    private String blockHash;
    BigInteger blockNumber;

    // for testing
    public static BlockResponse getTestBlock(BigInteger blockNumber) {
        return new BlockResponse(new Random().nextInt(), UUID.randomUUID().toString(), blockNumber);
    }
}
