package ru.eltech.dapeshkov.streaming;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.spark.storage.StorageLevel;
import ru.eltech.utils.PathEventsListener;
import ru.eltech.utils.PathEventsListener.DoJobUntilStrategies;
import ru.eltech.utils.PropertiesClass;

/** Receiver to push data to Apache Streaming We need it because we of sliding-window */
public class Receiver extends org.apache.spark.streaming.receiver.Receiver<String> {
  private final String companyDirPath;
  private final int window;

  /**
   * @param companyDirPath the path to companyDirPath to read files from
   * @param window size of window (how many messages should be accumulated before pushing to Apache
   *     Spark)
   */
  public Receiver(String companyDirPath, int window) {
    super(StorageLevel.MEMORY_AND_DISK_2());
    this.companyDirPath = companyDirPath;
    this.window = window;
  }

  @Override
  public void onStart() {
    new Thread(this::receive).start();
  }

  @Override
  public void onStop() {}

  private void receive() {
    Map<String, String> map =
        new LinkedHashMap<String, String>(window + 1, 1) {
          @Override
          protected boolean removeEldestEntry(Entry<String, String> eldest) {
            return size() > window;
          }
        };

    try {
      PathEventsListener.doJobOnEvent(
          Paths.get(companyDirPath),
          StandardWatchEventKinds.ENTRY_CREATE,
          (feedPath -> {
            String userInput;

            System.out.println("Streaming got " + feedPath);

            if (!map.containsKey(feedPath.toString())) {
              // System.out.println(path.toString());
              try (BufferedReader reader = new BufferedReader(new FileReader(feedPath.toFile()))) {
                userInput = reader.readLine();
                if (userInput != null) {
                  map.put(feedPath.toString(), userInput);

                  if (map.size() == window) {
                    store(map.values().iterator());
                    String streamingLogDirectory = PropertiesClass.getStreamingLogDirectory();
                    PrintWriter writer =
                        new PrintWriter(
                            new FileWriter(
                                streamingLogDirectory
                                    + Paths.get(companyDirPath).getFileName()
                                    + "//"
                                    + "receivedFeeds.log",
                                true),
                            true);
                    map.keySet().forEach(writer::println);
                    writer.println();
                    writer.close();
                  }
                }
              } catch (IOException e) {
                stop("IOException");
              }
            }
          }),
          DoJobUntilStrategies.FEEDS,
          false);

    } catch (Exception e) {
      stop("IOException");
      throw new RuntimeException(e);
    }
  }
}
