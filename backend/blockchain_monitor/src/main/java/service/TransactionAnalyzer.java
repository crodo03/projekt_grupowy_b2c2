package service;

import network.dto.BlockTransactionInfo;
import network.dto.TransactionInfoResult;
import org.web3j.protocol.core.methods.response.EthBlock;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

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
        BigInteger gasPricesMean = sumGasPrices(gasPrices).divide(BigInteger.valueOf(gasPrices.size()));
        return new TransactionInfoResult(transactionInfoList, gasPricesMean);
    }

    private BigInteger sumGasPrices(List<BigInteger> gasPrices) {
        if(gasPrices == null) {
            return BigInteger.ZERO;
        }
        return gasPrices
                .stream()
                .reduce(
                        BigInteger.ZERO,
                        BigInteger::add
                );
    }
}
