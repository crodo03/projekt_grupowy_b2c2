package service;

import lombok.extern.slf4j.Slf4j;
import network.dto.BlockTransactionInfo;
import network.dto.TransactionInfoResult;
import org.web3j.protocol.core.methods.response.EthBlock;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class TransactionAnalyzer {
    private final EthBlock.Block block;
    private final List<BlockTransactionInfo> transactionInfoList = new ArrayList<>();

    public TransactionAnalyzer(EthBlock.Block block) {
        this.block = block;
    }

    public TransactionInfoResult getTransactionInfo() {
        List<BigInteger> gasPrices = new ArrayList<>();
        block.getTransactions().forEach(transaction -> {
            var transactionObject = (EthBlock.TransactionObject) transaction.get();
            String hash = transactionObject.getHash();
            String to = transactionObject.getTo();
            String from = transactionObject.getFrom();
            BigInteger value = transactionObject.getValue();
            BigInteger gas = transactionObject.getGas();
            BigInteger gasPrice = transactionObject.getGasPrice();
            gasPrices.add(gasPrice);

            transactionInfoList.add(new BlockTransactionInfo(
                    hash,
                    to,
                    from,
                    value,
                    gas,
                    gasPrice
            ));
        });

        List<BigInteger> validGasPrices = gasPrices.stream()
                .filter(Objects::nonNull)
                .toList();

        if(validGasPrices.isEmpty()) {
            return new TransactionInfoResult(transactionInfoList, BigInteger.ZERO);
        }

        BigInteger gasPricesMean = sumGasPrices(validGasPrices)
                .divide(BigInteger.valueOf(validGasPrices.size()));

        return new TransactionInfoResult(transactionInfoList, gasPricesMean);
    }

    private BigInteger sumGasPrices(List<BigInteger> gasPrices) {
        return gasPrices
                .stream()
                .reduce(
                        BigInteger.ZERO,
                        BigInteger::add
                );
    }
}
