package network.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigInteger;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BlockResponse {
    private int numberOfTransactions;
    private String blockHash;
    @EqualsAndHashCode.Include
    BigInteger blockNumber;
    private String fetchedAt;

    public static BlockResponse getTestBlock(BigInteger blockNumber) {
        return new BlockResponse(
                new Random().nextInt(),
                UUID.randomUUID().toString(),
                blockNumber,
                getCurrentTime()
        );
    }

    public static String getCurrentTime() {
        return LocalTime.now().truncatedTo(ChronoUnit.SECONDS).toString();
    }
}
