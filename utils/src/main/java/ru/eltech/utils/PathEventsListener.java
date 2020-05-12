package ru.eltech.utils;

import io.reactivex.Observable;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.stream.Collectors;

/** Listen the watchable path for registered events and do the job on each one */
public class PathEventsListener {

  public static enum DoJobUntilStrategies implements Consumer<LongConsumer> {
    COMPANIES {
      @Override
      public void accept(LongConsumer longConsumer) {
        try {
          // emit newly created files
          for (long companyNumber = 0;
              companyNumber < PropertiesClass.getTotalAmountOfCompanies();
              companyNumber++) {
            longConsumer.accept(companyNumber);
          }
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    },
    FEEDS {
      @Override
      public void accept(LongConsumer longConsumer) {
        try {
          // emit newly created files
          for (long feedNumber = getCurrentNumberOfFeedsInLiveFeedDirectory();
              feedNumber < PropertiesClass.getTotalAmountOfFeeds(); ) {
            feedNumber = getCurrentNumberOfFeedsInLiveFeedDirectory();
            longConsumer.accept(feedNumber);
          }
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    };

    @Override
    public void accept(LongConsumer longConsumer) {}
  }

  private PathEventsListener() {}

  public static void doJobOnEvent(
      final Path watchable,
      final Kind<Path> watchEvent,
      final Consumer<Path> jobToDo,
      final DoJobUntilStrategies doJobUntilStrategy,
      final boolean doJobForExistingPaths) {
    try {
      waitingPathToBeCreated(watchable);

      WatchService watchService = FileSystems.getDefault().newWatchService();
      watchable.register(
          watchService,
          StandardWatchEventKinds.ENTRY_CREATE,
          StandardWatchEventKinds.ENTRY_MODIFY,
          StandardWatchEventKinds.ENTRY_DELETE);

      Observable<Path> source =
          Observable.create(
              emitter -> {
                if (doJobForExistingPaths) {
                  // emit existing files
                  List<Path> existingPaths =
                      Files.list(watchable)
                          .sorted(Comparator.comparingLong(path -> path.toFile().lastModified()))
                          .collect(Collectors.toList());

                  existingPaths.forEach(emitter::onNext);
                }

                // emit newly created files
                doJobUntilStrategy.accept(
                    (feedOrCompanyNumber) -> {
                      try {
                        WatchKey key = watchService.take();

                        List<Path> contextPaths =
                            key.pollEvents().stream()
                                .distinct()
                                .filter(event -> event.kind() == watchEvent)
                                .map(event -> watchable.resolve((Path) event.context()))
                                .collect(Collectors.toList());

                        TimeUnit.MILLISECONDS.sleep(100);

                        contextPaths.forEach(emitter::onNext);

                        // reset the key
                        boolean valid = key.reset();
                        if (!valid) {
                          System.err.println("Key has been unregistered");
                        }
                      } catch (Exception e) {
                        throw new RuntimeException(e);
                      }
                    });

                emitter.onComplete();
              });

      source.subscribe(
          jobToDo::accept,
          Throwable::printStackTrace,
          () ->
              System.out.println(
                  "PathEventsListener for " + watchable.toString() + " is completed"));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void waitingPathToBeCreated(final Path path) {
    try {
      int amountOfFeedsInMinute = PropertiesClass.getAmountOfFeedsInMinute();
      while (!path.toFile().exists()) {
        TimeUnit.NANOSECONDS.sleep((long) (60D / amountOfFeedsInMinute * 1_000_000_000));
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static long getCurrentNumberOfFeedsInLiveFeedDirectory() throws IOException {
    return Files.list(Paths.get(PropertiesClass.getLiveFeedDirectory()))
        .flatMap(
            companyPath -> {
              try {
                return Files.list(companyPath).map(feedPath -> feedPath.toFile().isFile());
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            })
        .count();
  }
}
