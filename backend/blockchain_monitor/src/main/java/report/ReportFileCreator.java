package report;

import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import service.BlockAnalyzer;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@RequiredArgsConstructor
public class ReportFileCreator {
    private final BlockAnalyzer blockAnalyzer;

    public String createCSVFile() {
        LocalDateTime generatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        int totalBlocks = blockAnalyzer.getTotalNumberOfBlocks().get();
        int failedBlocks = blockAnalyzer.getFailedBlockNumbers().size();
        int successfulBlocks = totalBlocks - failedBlocks;
        double successRate = failedBlocks == 0 ? 100 : ((double) successfulBlocks / totalBlocks) * 100;
        int totalTransactions = blockAnalyzer.getTotalNumberOfTransactions().get();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        String generatedAtString = generatedAt.format(formatter);
        String filename = generatedAtString + "-report.csv";

        try (CSVWriter writer = new CSVWriter(new FileWriter("backend/blockchain_monitor/" + filename))) {
            writer.writeNext(new String[]{"# SESSION SUMMARY"}, false);
            writer.writeNext(new String[]{
                    "generated_at", "total_blocks", "failed_blocks", "successful_blocks",
                    "success_rate", "total_transactions"
            });

            // row
            writer.writeNext(new String[]{
                    generatedAtString,
                    String.valueOf(totalBlocks),
                    String.valueOf(failedBlocks),
                    String.valueOf(successfulBlocks),
                    String.format("%.2f%%", successRate),
                    String.valueOf(totalTransactions)
            });
            writer.writeNext(new String[]{});

            writer.writeNext(new String[]{"# PER BLOCK DETAILS"}, false);
            writer.writeNext(new String[]{
                    "block_number", "block_hash", "transaction_count"
            });
            blockAnalyzer.getQueueAsList().forEach(block ->
                    writer.writeNext(new String[]{
                            String.valueOf(block.getBlockNumber()),
                            block.getBlockHash(),
                            String.valueOf(block.getNumberOfTransactions()),
                    })
            );

        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        return filename;
    }

    public void createJSONFile() {}
}
