package ru.eltech.mapeshkov.plot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.util.concurrent.TimeUnit;
import org.jfree.data.xy.XYDataItem;
import ru.eltech.utils.PathEventsListener;
import ru.eltech.utils.PathEventsListener.DoJobUntilStrategies;
import ru.eltech.utils.PropertiesClass;

public class PlotHelper {
  private static final Comparable<?>[] KEYS = {"real stock", "prediction stock"};
  private static final String STREAMING_VIEW_DIRECTORY =
      PropertiesClass.getStreamingViewDirectory();

  public static void start() {
    PathEventsListener.doJobOnEvent(
        Paths.get(STREAMING_VIEW_DIRECTORY),
        StandardWatchEventKinds.ENTRY_CREATE,
        (contextPath) -> new Thread(() -> threadJobForEachCompany(contextPath)).start(),
        DoJobUntilStrategies.COMPANIES,
        true);
  }

  private static void threadJobForEachCompany(Path companyDirPath) {
    final CombinedChart chart =
        new CombinedChart(
            companyDirPath.getFileName().toString(), "streaming view number", "price", KEYS);

    System.out.println("Plot for " + companyDirPath.toString());

    PathEventsListener.doJobOnEvent(
        companyDirPath,
        StandardWatchEventKinds.ENTRY_CREATE,
        (streamingViewPath) -> {
          try {

            System.out.println("Plot received " + streamingViewPath.toString());

            waitingFileToBeFilled(companyDirPath);

            Files.readAllLines(streamingViewPath)
                .forEach(
                    line -> {
                      String[] split = line.split(",");
                      double stockToday = Double.parseDouble(split[0]);

                      int streamingViewNumber =
                          Integer.parseInt(
                              streamingViewPath
                                  .getFileName()
                                  .toString()
                                  .split("\\.")[0]
                                  .split("w")[1]);

                      waitingFileToBeFilled(streamingViewPath);

                      chart.addPoint(new XYDataItem(streamingViewNumber, stockToday), KEYS[0]);

                      if (!split[1].equals("null")) {
                        double predictionStock = Double.parseDouble(split[1]);

                        chart.addPoint(
                            new XYDataItem(streamingViewNumber, predictionStock), KEYS[1]);
                      }
                    });

          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        },
        DoJobUntilStrategies.FEEDS,
        true);

    chart.saveChartAsJPEG(
        Paths.get(
            PropertiesClass.getPlotDirectory() + companyDirPath.getFileName() + "//" + "chart.jpg"),
        1200,
        400);
  }

  private static void waitingFileToBeFilled(final Path watchable) {
    try {
      TimeUnit.MILLISECONDS.sleep(100);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
