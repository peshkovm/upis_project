package ru.eltech.dapeshkov.news;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import ru.eltech.dapeshkov.classifier.BernoulliNaiveBayes;
import ru.eltech.mapeshkov.stock.ApiUtils;

/**
 * This class reads content from given URLs and outputs the parsed content in the files. After
 * invocation of method {@link #start() start()} it outputs parsed content of all sites to the
 * files, then it will output only newly added items when Connection are updated. This program
 * requests sites contests every 3 seconds.
 *
 * @author Peshkov Denis.
 */
public class NewsReader {
  private final ScheduledExecutorService ex =
      Executors.newScheduledThreadPool(4); // ExecutorService that runs the tasks
  private Map<String, String> map;
  private final String out;

  /**
   * Initialize the instance of {@code NewsReader}.
   *
   * @param url the array of {@link String}
   * @param out the output file {@link String}
   */
  public NewsReader(String out, News... news) {
    this.map = Arrays.stream(news).collect((Collectors.toMap(s -> s.name, s -> s.train)));
    this.out = out;
    // a.train(2);
    System.out.println("Ready");
  }

  static class News {
    String name;
    String train;

    public News(String name, String train) {
      this.name = name;
      this.train = train;
    }
  }

  /**
   * writes {@link String} to file
   *
   * @param str {@link String} to write to file
   * @param out file name
   */
  synchronized void write(final String str, final OutputStream out) {
    try (final PrintWriter writer =
        new PrintWriter(new BufferedWriter(new OutputStreamWriter(out)), true)) {
      writer.println(str);
    }
  }

  /**
   * This method requests all given sites and outputs the contents to the given files. Most of the
   * time this method should be invoked only once. Method works as a.txt service running all the
   * time with 3 second interval
   */
  public void start() {
    for (final Map.Entry<String, String> a : map.entrySet()) {
      final Connection connection =
          new Connection(
              "https://www.rbc.ru/v10/search/ajax/?project=rbcnews&limit=1&query=", a.getKey());
      ex.scheduleAtFixedRate(
          new Runnable() {
            private LocalDateTime lastpubdate = null;
            private Integer i = 0;
            private BernoulliNaiveBayes<String, String> bayes = new BernoulliNaiveBayes<>();

            {
              String[] arr = new File(out + a + "/").list();
              if (arr != null) {
                i = arr.length;
              }

              JSONProcessor.Train[] arr1 = null;

              try (InputStream in = BernoulliNaiveBayes.class.getResourceAsStream(a.getValue())) {
                arr1 = JSONProcessor.parse(in, JSONProcessor.Train[].class);
              } catch (IOException e) {
                e.printStackTrace();
              }

              for (JSONProcessor.Train a : arr1) {
                String[] str = BernoulliNaiveBayes.parse(a.getText(), 1);
                if (str != null) {
                  bayes.train(a.getSentiment(), Arrays.asList(str));
                }
              }
            }

            @Override
            public void run() {
              try (final Connection con = connection) {
                final JSONProcessor.News news =
                    JSONProcessor.parse(con.get(), JSONProcessor.News.class);
                if (news != null
                    && (lastpubdate == null
                        || news.getItems()[0].getPublish_date().isAfter(lastpubdate))) {
                  lastpubdate = news.getItems()[0].getPublish_date();
                  final Item item =
                      new Item(
                          a.getKey(),
                          bayes.sentiment(
                              Arrays.asList(
                                  Objects.requireNonNull(
                                      BernoulliNaiveBayes.parse(
                                          news.getItems()[0].toString(), 1)))),
                          Timestamp.valueOf(LocalDateTime.now()),
                          ApiUtils.AlphaVantageParser.getLatestStock(a.getKey()).getPrice());
                  write(item.toString(), new FileOutputStream(out + a + "/" + i++ + ".txt"));
                } else {
                  final Item item =
                      new Item(
                          a.getKey(),
                          "neutral",
                          Timestamp.valueOf(LocalDateTime.now()),
                          ApiUtils.AlphaVantageParser.getLatestStock(a.getKey()).getPrice());
                  write(item.toString(), new FileOutputStream(out + a + "/" + i++ + ".txt"));
                  System.out.println("no news");
                }
              } catch (Throwable e) {
                throw new RuntimeException(e);
              }
            }
          },
          0,
          3,
          TimeUnit.SECONDS);
    }
  }
}
