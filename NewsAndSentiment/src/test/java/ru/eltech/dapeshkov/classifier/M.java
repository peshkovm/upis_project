package ru.eltech.dapeshkov.classifier;

import static java.nio.file.Files.newBufferedWriter;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import ru.eltech.dapeshkov.news.Connection;
import ru.eltech.dapeshkov.news.Item;
import ru.eltech.dapeshkov.news.JSONProcessor;

public class M {
  static void write(final String str, final OutputStream out) {
    try (final PrintWriter writer =
        new PrintWriter(new BufferedWriter(new OutputStreamWriter(out)), true)) {
      writer.println(str);
    }
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    BernoulliNaiveBayes<String, String> bernoulliNaiveBayes = new BernoulliNaiveBayes<>();
    JSONProcessor.Train[] arr = null;
    try (InputStream in = BernoulliNaiveBayes.class.getResourceAsStream("/train.json")) {
      arr = JSONProcessor.parse(in, JSONProcessor.Train[].class);
    } catch (IOException e) {
      e.printStackTrace();
    }

    for (JSONProcessor.Train a : arr) {
      String[] str = BernoulliNaiveBayes.parse(a.getText(), 1);
      if (str != null) {
        bernoulliNaiveBayes.train(a.getSentiment(), Arrays.asList(str));
      }
    }

    try (InputStream in = BernoulliNaiveBayes.class.getResourceAsStream("/test1.json")) {
      arr = JSONProcessor.parse(in, JSONProcessor.Train[].class);
    } catch (IOException e) {
      e.printStackTrace();
    }

    int i2 = 0;

    for (JSONProcessor.Train a : arr) {
      if (a.getText() == null) continue;
      String[] str = BernoulliNaiveBayes.parse(a.getText(), 1);
      String sentiment = null;
      if (str != null) {
        sentiment = bernoulliNaiveBayes.sentiment(Arrays.asList(str));
      }
      if (sentiment == null) {
        continue;
      }
      String sentiment1 = a.getSentiment();
      if (sentiment.equals(sentiment1)) {
        i2++;
      }
    }
    System.out.println((i2 / (double) arr.length) * 100);

    String a = "Сбербанк";
    JSONProcessor.News news = new JSONProcessor.News();
    List<JSONProcessor.Item> list = new ArrayList<>();
    int j = 0;
    while (j < 5000) {
      final Connection connection =
          new Connection(
              "https://www.rbc.ru/v10/search/ajax/?project=rbcnews&limit=1000"
                  + "&offset="
                  + j
                  + "&query=",
              "Сбербанк");
      j += 1000;
      JSONProcessor.News parse = JSONProcessor.parse(connection.get(), JSONProcessor.News.class);
      list.addAll(Arrays.stream(parse.getItems()).collect(Collectors.toList()));
      connection.close();
    }

    news.setItems(list.toArray(new JSONProcessor.Item[0]));

    BufferedWriter bufferedWriter =
        newBufferedWriter(Paths.get("news.csv"), StandardOpenOption.CREATE);
    for (JSONProcessor.Item i1 : news.getItems()) {
      String[] parse = BernoulliNaiveBayes.parse(i1.getTitle(), 1);
      StringBuilder str = new StringBuilder();
      for (String i : parse) {
        str.append(i);
      }
      bufferedWriter.write(str.toString());
      bufferedWriter.newLine();
    }
    bufferedWriter.close();
    ProcessBuilder processBuilder =
        new ProcessBuilder("./mystem", "-cld", "news.csv", "news_lem.csv");
    Process start = processBuilder.start();
    start.waitFor();
    BufferedWriter bufferedWriter1 =
        newBufferedWriter(Paths.get("news_lem_parsed.csv"), StandardOpenOption.CREATE);
    try (Stream<String> lines = Files.lines(Paths.get("news_lem.csv"))) {
      lines
          .map(i -> i.replaceAll("(\\{|})", ""))
          .map(i -> i.replaceAll("\\p{L}*\\?+", ""))
          .map(i -> i.trim())
          .map(i -> i.replaceAll(" +", " "))
          .filter(i -> i.length() != 0)
          .forEach(
              i -> {
                try {
                  bufferedWriter1.write(i);
                  bufferedWriter1.newLine();
                } catch (IOException e) {
                  e.printStackTrace();
                }
              });
    }
    bufferedWriter1.close();
    int[] i1 = {0};
    try (Stream<String> news_lem = Files.lines(Paths.get("news_lem_parsed.csv"))) {
      news_lem.forEach(
          s -> {
            news.getItems()[i1[0]++].setAnons(s);
          });
    }

    Map<LocalDate, Double> collect =
        Files.lines(
                Paths.get(
                    "NewsAndSentiment/src/test/resources/allStockData/allStockData"
                        + "_"
                        + "sberbank"
                        + ".txt"))
            .collect(
                Collectors.toMap(
                    (String s) -> LocalDate.parse(s.split(",")[1]),
                    s -> Double.valueOf(s.split(",")[2])));
    Comparator<Map.Entry<LocalDate, Double>> entryComparator =
        (Map.Entry<LocalDate, Double> b, Map.Entry<LocalDate, Double> v) ->
            b.getKey().compareTo(v.getKey());
    entryComparator = entryComparator.reversed();
    int l = 0;
    for (JSONProcessor.Item i : news.getItems()) {
      final Item item =
          new Item(
              a,
              bernoulliNaiveBayes.sentiment(
                  Arrays.asList(BernoulliNaiveBayes.parse(i.getAnons(), 1))),
              Timestamp.valueOf(i.getPublish_date()),
              collect.entrySet().stream()
                  .sorted(entryComparator)
                  .filter(x -> !x.getKey().isAfter(i.getPublish_date().toLocalDate()))
                  .findFirst()
                  .get()
                  .getValue());
      write(
          item.toString(),
          new FileOutputStream(
              "NewsAndSentiment/src/test/resources/files/" + "sberbank" + "/" + l++ + ".txt"));
    }
  }
}
