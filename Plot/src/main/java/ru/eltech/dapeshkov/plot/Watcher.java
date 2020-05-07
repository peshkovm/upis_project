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

public class Watcher {

  private final WatchService watchService;

  public Watcher(Path path) throws IOException {
    watchService = FileSystems.getDefault().newWatchService();
    WatchKey key =
        path.register(
            watchService,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_CREATE);
  }

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
