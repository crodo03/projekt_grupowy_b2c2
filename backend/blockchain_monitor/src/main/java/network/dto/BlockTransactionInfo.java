package network.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigInteger;

@AllArgsConstructor
@Data
public class BlockTransactionInfo {
    private String hash;
    private String to;
    private String from;
    private BigInteger value;
    private BigInteger gas;
}
