package network.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigInteger;

@AllArgsConstructor
@Getter
public class BlockTransactionInfo {
    private String hash;
    private String to;
    private String from;
    private BigInteger value;
    private BigInteger gas;
    private BigInteger gasPrice;
}
