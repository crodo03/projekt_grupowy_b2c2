package exceptions;

import java.math.BigInteger;

public class GetBlockException extends RuntimeException {
    public GetBlockException(BigInteger blockNumber, Throwable cause) {
        super("failed to fetch the block: " + blockNumber, cause);
    }

    public GetBlockException(Throwable cause) {
        super("failed to fetch latest block", cause);
    }
}
