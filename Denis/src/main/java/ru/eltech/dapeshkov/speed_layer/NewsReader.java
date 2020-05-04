package ru.eltech.dapeshkov.speed_layer;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import ru.eltech.dapeshkov.classifier.Processing;
import ru.eltech.mapeshkov.not_spark.ApiUtils;

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
  private final String[] url;
  private final String out;

  /**
   * Initialize the instance of {@code NewsReader}.
   *
   * @param url the array of {@link String}
   * @param out the output file {@link String}
   */
  public NewsReader(final String out, final String... url) {
    this.url = url;
    this.out = out;
    Processing.train(2);
    System.out.println("Ready");
  }

  synchronized void write(final String str, final OutputStream out) {
    try (final PrintWriter writer =
        new PrintWriter(new BufferedWriter(new OutputStreamWriter(out)), true)) {
      writer.println(str);
    }
  }

  /**
   * This method requests all given sites and outputs the contents to the given files. Most of the
   * time this method should be invoked only once. Method works as a service running all the time
   * with 3 second interval
   */
  public void start() {
    for (final String a : url) {
      final Connection connection =
          new Connection("http://192.168.0.111:8080/Test_war_exploded/?tag=" + a);
      ex.scheduleAtFixedRate(
          new Runnable() {
            private LocalDateTime lastpubdate = null;
            private Integer i = 0;

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
                          a,
                          Processing.sentiment(news.getItems()[0].toString()),
                          Timestamp.valueOf(lastpubdate),
                          ApiUtils.AlphaVantageParser.getLatestStock(a).getChange());
                  write(item.toString(), new FileOutputStream(out + a + "/" + i++ + ".txt"));
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
