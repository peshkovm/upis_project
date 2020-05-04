package ru.eltech;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

public class Watcher {

  private final WatchService watchService;
  private final String file;

  public Watcher(Path path) throws IOException {
    watchService = FileSystems.getDefault().newWatchService();
    WatchKey key = path.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
    file = path.getName(path.getNameCount() - 1).toString();
  }

  public void process(Action action) {
    while (true) {
      final WatchKey wk;
      try {
        wk = watchService.take();
      } catch (InterruptedException e) {
        e.printStackTrace();
        return;
      }
      for (WatchEvent<?> event : wk.pollEvents()) {
        final Path changed = (Path) event.context();
        if (changed.endsWith(file)) {
          action.run();
        }
      }
      // reset the key
      boolean valid = wk.reset();
      if (!valid) {
        System.err.println("Key has been unregistered");
      }
    }
  }

  public boolean check() {
    final WatchKey wk;

    wk = watchService.poll();

    if (wk != null) {
      for (WatchEvent<?> event : wk.pollEvents()) {
        final Path changed = (Path) event.context();
        if (changed.endsWith(file)) {
          return true;
        }
      }
      // reset the key
      boolean valid = wk.reset();
      if (!valid) {
        System.err.println("Key has been unregistered");
      }
    }
    return false;
  }
}
