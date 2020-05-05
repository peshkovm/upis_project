package ru.eltech;

import org.apache.commons.io.FileUtils;
import ru.eltech.dapeshkov.streaming.StreamingMain;
import ru.eltech.mapeshkov.batch.BatchMain;
import ru.eltech.utils.JavaProcess;
import ru.eltech.utils.MyFileWriter;
import ru.eltech.utils.PropertiesClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws Exception {

        final String liveFeedDirectory = PropertiesClass.getLiveFeedDirectory();
        final String liveFeedLogDirectory = PropertiesClass.getLiveFeedLogDirectory();
        final String realStockPricesDirectory = PropertiesClass.getRealStockPricesDirectory();
        final int amountOfFeedsInMinute = PropertiesClass.getAmountOfFeedsInMinute(); // must be <= 60
        final int totalAmountOfFeeds = PropertiesClass.getTotalAmountOfFeeds();
        int totalAmountOfCompanies = PropertiesClass.getTotalAmountOfCompanies();
        String[] availableCompanies = PropertiesClass.getAvailableCompanies();
        String companyName;
        String[] availableSentiments = new String[]{"negative", "neutral", "positive"};
        String sentiment;
        LocalDateTime date = LocalDateTime.now();

        if (availableCompanies.length != totalAmountOfCompanies)
            throw new RuntimeException("availableCompanies.length!=totalAmountOfCompanies");

        deleteWorkingAndLogsDirectories();

        //    new Thread(
        //            () -> {
        //              int exitValue = JavaProcess.exec(StreamingMain.class, null);
        //              if (exitValue == 0) {
        //                System.out.println("Streaming Layer terminated normally");
        //              }
        //            })
        //        .start();
        //    new Thread(
        //            () -> {
        //              int exitValue = JavaProcess.exec(BatchMain.class, null);
        //              if (exitValue == 0) {
        //                System.out.println("Batch Layer terminated normally");
        //              }
        //            })
        //        .start();
        //    new Thread(
        //            () -> {
        //              int exitValue = JavaProcess.exec(PlotMain.class, null);
        //              if (exitValue == 0) {
        //                System.out.println("Plot terminated normally");
        //              }
        //            })
        //        .start();

        JavaProcess.exec(StreamingMain.class, null);
        JavaProcess.exec(BatchMain.class, null);
//    JavaProcess.exec(PlotMain.class, null);

        List<Double> realStockPrices =
                Files.list(Paths.get(realStockPricesDirectory))
                        .sorted(
                                Comparator.comparingInt(
                                        path ->
                                                Integer.parseInt(
                                                        path.getFileName().toString().split("\\.")[0].split("e")[2])))
                        .flatMap(
                                path -> {
                                    try {
                                        return Files.lines(path).map(Double::parseDouble);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                        .collect(Collectors.toList());

        TimeUnit.SECONDS.sleep(5);

        for (int feedNumber = 0;
             feedNumber < realStockPrices.size() && feedNumber < totalAmountOfFeeds;
             feedNumber++) {
            companyName = availableCompanies[ThreadLocalRandom.current().nextInt(totalAmountOfCompanies)];
            try (MyFileWriter feedWriter =
                         new MyFileWriter(
                                 Paths.get(
                                         liveFeedDirectory + companyName + "\\" + "feed " + feedNumber + ".txt"));
                 MyFileWriter logWriter =
                         new MyFileWriter(
                                 Paths.get(liveFeedLogDirectory + companyName + "\\" + "sentFeeds.log"))) {

                Timestamp timestamp = Timestamp.valueOf(date);
                Double realStockPrice = realStockPrices.get(feedNumber);
                sentiment = availableSentiments[ThreadLocalRandom.current().nextInt(3)];

                feedWriter.println(companyName + "," + sentiment + "," + timestamp + "," + realStockPrice);
                logWriter.println("feed " + feedNumber + " at time : " + timestamp);
                System.out.println(companyName + "\\" + "feed " + feedNumber + " at time : " + timestamp);

                date = date.plusNanos((long) (60D / amountOfFeedsInMinute * 1_000_000_000));
                TimeUnit.NANOSECONDS.sleep((long) (60D / amountOfFeedsInMinute * 1_000_000_000));
            }
        }

        System.out.println("main stopped");
    }

    private static void deleteWorkingAndLogsDirectories() throws IOException {
        final String batchLogDirectory = PropertiesClass.getBatchLogDirectory();
        final String batchMlModelDirectory = PropertiesClass.getbatchMlModelDirectory();
        final String liveFeedDirectory = PropertiesClass.getLiveFeedDirectory();
        final String liveFeedLogDirectory = PropertiesClass.getLiveFeedLogDirectory();
        final String streamingViewDirectory = PropertiesClass.getStreamingViewDirectory();
        final String streamingLogDirectory = PropertiesClass.getStreamingLogDirectory();
        final String plotDirectory = PropertiesClass.getPlotDirectory();

        if (new File(liveFeedDirectory).exists()) {
            FileUtils.deleteDirectory(new File(liveFeedDirectory));
        }

        if (new File(liveFeedLogDirectory).exists()) {
            FileUtils.deleteDirectory(new File(liveFeedLogDirectory));
        }

        if (new File(batchLogDirectory).exists()) {
            FileUtils.deleteDirectory(new File(batchLogDirectory));
        }

        if (new File(batchMlModelDirectory).exists()) {
            FileUtils.deleteDirectory(new File(batchMlModelDirectory));
        }

        if (new File(streamingViewDirectory).exists()) {
            FileUtils.deleteDirectory(new File(streamingViewDirectory));
        }

        if (new File(streamingLogDirectory).exists()) {
            FileUtils.deleteDirectory(new File(streamingLogDirectory));
        }

        if (new File(plotDirectory).exists()) {
            FileUtils.deleteDirectory(new File(plotDirectory));
        }
    }
}
