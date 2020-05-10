package ru.eltech.dapeshkov.plot;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

/** Watcher directory for changes */
public class Watcher {

  private final WatchService watchService;

  /**
   * creates new {@link Watcher} instance
   *
   * @param path path to directory to watch for changes
   * @throws IOException
   */
  public Watcher(Path path) throws IOException {
    watchService = FileSystems.getDefault().newWatchService();
    WatchKey key =
        path.register(
            watchService,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_CREATE);
  }

  /**
   * waits for file to change
   *
   * @param watchEvent what kind of event to watch
   * @param file path to file
   */
  public void take(WatchEvent.Kind<Path> watchEvent, String file) {
    WatchKey wk = null;

    for (; ; ) {
      try {
        wk = watchService.take();
      } catch (InterruptedException e) {
        e.printStackTrace();
        return;
      }

      for (WatchEvent<?> event : wk.pollEvents()) {
        if (event.kind() == watchEvent) {
          final Path changed = (Path) event.context();
          try {
            if (Files.isSameFile(changed, Paths.get(file))) {
              return;
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
      // reset the key
      boolean valid = wk.reset();
      if (!valid) {
        System.err.println("Key has been unregistered");
      }
    }
  }
}
