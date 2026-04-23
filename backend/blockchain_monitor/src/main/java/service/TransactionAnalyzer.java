package service;

import org.web3j.protocol.core.methods.response.EthBlock;

import java.math.BigInteger;
import java.util.List;

public class TransactionAnalyzer {
    private final EthBlock.Block block;

    public TransactionAnalyzer(EthBlock.Block block) {
        this.block = block;
    }

    public void getTransactionInfo() {
        block.getTransactions().forEach(transaction -> {
            var transactionObject = (EthBlock.TransactionObject) transaction.get();
            String hash = transactionObject.getHash();
            String to = transactionObject.getTo();
            String from = transactionObject.getFrom();
            BigInteger value = transactionObject.getValue();
            BigInteger gas = transactionObject.getGas();

            System.out.printf("""
                    hash: %s
                    to: %s
                    from: %s
                    value: %s
                    gas: %s
                    \n""", hash, to, from, value, gas);
        });
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
