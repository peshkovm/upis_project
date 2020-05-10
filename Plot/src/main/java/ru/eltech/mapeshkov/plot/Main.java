package ru.eltech.mapeshkov.plot;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

public class Main {
  public static void main(String[] args) throws Exception {
    final String dirName =
        "C:\\JavaLessons\\bachelor-diploma\\Streaming\\src\\test\\resources\\streaming_files\\prediction";
    final String fileName = "predict.txt";
    final Path path = Paths.get(dirName);
    final PlotHelper plotHelper = new PlotHelper(dirName + "\\" + fileName);
    try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
      final WatchKey watchKey = path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
      while (true) {
        final WatchKey wk = watchService.take();
        for (WatchEvent<?> event : wk.pollEvents()) {
          // we only register "ENTRY_MODIFY" so the context is always a Path.
          final Path changed = (Path) event.context();
          System.out.println(changed);
          if (changed.endsWith(fileName)) {
            // System.out.println("My file has changed");
            plotHelper.refresh();
            System.out.println("plot refreshed: ");
          }
        }
        // reset the key
        boolean valid = wk.reset();
        if (!valid) {
          System.out.println("Key has been unregisterede");
        }
      }
    }
  }

  static void fooDenis() throws Exception {
    String fileName =
        "C:\\JavaLessons\\bachelor-diploma\\Streaming\\src\\test\\resources\\streaming_files\\prediction\\predict.txt";
    final PlotHelper plotHelper = new PlotHelper(fileName);
    // final Watcher watcher = new
    // Watcher(Paths.get("C:\\JavaLessons\\bachelor-diploma\\Streaming\\src\\test\\resources\\streaming_files\\prediction\\"));

    Path path = Paths.get(fileName);
    final WatchService watcher = FileSystems.getDefault().newWatchService();

    // plotHelper.setMaxSeriesLength(5);

    for (; ; ) {
      path.register(watcher, ENTRY_MODIFY);
      // watcher.take(StandardWatchEventKinds.ENTRY_MODIFY, "predict.txt");
      plotHelper.refresh();
      System.out.println("plot refreshed: ");
      // TimeUnit.SECONDS.sleep(1);
    }
  }
}
