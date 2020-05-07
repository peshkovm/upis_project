package ru.eltech.mapeshkov.plot;

import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import ru.eltech.dapeshkov.plot.Watcher;

public class Main {
  public static void main(String[] args) throws Exception {
    final PlotHelper plotHelper = new PlotHelper("working_files/prediction/predict.txt");
    final Watcher watcher = new Watcher(Paths.get("working_files/prediction/"));
    // plotHelper.setMaxSeriesLength(5);

    for (; ; ) {
      watcher.take(StandardWatchEventKinds.ENTRY_CREATE, "predict.txt");
      plotHelper.refresh();
      System.out.print("plot refreshed: ");
    }
  }
}
