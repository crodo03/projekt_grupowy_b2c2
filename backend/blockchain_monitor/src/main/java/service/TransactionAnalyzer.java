package service;

import network.dto.BlockTransactionInfo;
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

    public List<BlockTransactionInfo> getTransactionInfo() {
        block.getTransactions().forEach(transaction -> {
            var transactionObject = (EthBlock.TransactionObject) transaction.get();
            String hash = transactionObject.getHash();
            String to = transactionObject.getTo();
            String from = transactionObject.getFrom();
            BigInteger value = transactionObject.getValue();
            BigInteger gas = transactionObject.getGas();

            transactionInfoList.add(new BlockTransactionInfo(
                    hash,
                    to,
                    from,
                    value,
                    gas
            ));
        });
        return transactionInfoList;
    }

    private BigInteger sumWithdrawals(List<EthBlock.Withdrawal> withdrawals) {
        if(withdrawals == null) {
            return BigInteger.ZERO;
        }
        return withdrawals
                .stream()
                .map(EthBlock.Withdrawal::getAmount)
                .reduce(
                        BigInteger.ZERO,
                        BigInteger::add
                );
    }
}
