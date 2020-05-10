package ru.eltech.dapeshkov.streaming;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.spark.storage.StorageLevel;

/** Receiver to push data to Apache Streaming We need it because we of sliding-window */
public class Receiver extends org.apache.spark.streaming.receiver.Receiver<String> {
  private final String directory;
  private final int window;

  /**
   * @param directory the path to directory to read files from
   * @param window size of window (how many messages should be accumulated before pushing to Apache
   *     Spark)
   */
  public Receiver(String directory, int window) {
    super(StorageLevel.MEMORY_AND_DISK_2());
    this.directory = directory;
    this.window = window;
  }

  @Override
  public void onStart() {
    new Thread(this::receive).start();
  }

  @Override
  public void onStop() {}

  private void receive() {
    // List<String> list = Arrays.asList(new String[window]);
    Map<String, String> map =
        new LinkedHashMap<String, String>(window + 1, 1) {
          @Override
          protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > window;
          }
        };
    Watcher watcher = null;
    try {
      watcher = new Watcher(Paths.get(directory));
    } catch (IOException e) {
      stop("IOException");
    }
    String userInput;
    int i = 0;
    List<Path> changedFiles;
    while (!isStopped()) {
      changedFiles = watcher.getChangedFiles(StandardWatchEventKinds.ENTRY_MODIFY);
      for (Path path : changedFiles) {
        if (!map.containsKey(path.toString())) {
          // System.out.println(path.toString());
          try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            userInput = reader.readLine();
            if (userInput != null) {
              map.put(path.toString(), userInput);

              // list.set(i, userInput);
              // i = Integer.parseInt(FilenameUtils.removeExtension(path.getFileName().toString()))
              // % window;
              // i = (i++) % window;
              if (map.size() == window) {
                store(map.values().iterator());
                PrintWriter writer = new PrintWriter(new FileWriter("receive.txt", true), true);
                map.keySet().forEach(writer::println);
                writer.println();
                writer.close();
              }
            }
          } catch (IOException e) {
            stop("IOException");
          }
        }
      }
    }
  }
}
