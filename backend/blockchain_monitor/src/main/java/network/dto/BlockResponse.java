package network.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigInteger;

@Data
@Builder
public class BlockResponse {
    private int numberOfTransactions;
    private String blockHash;
    BigInteger blockNumber;
}
