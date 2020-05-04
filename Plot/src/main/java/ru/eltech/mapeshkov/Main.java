package ru.eltech.mapeshkov;

import java.util.concurrent.TimeUnit;

public class Main {
  public static void main(String[] args) throws Exception {
    final PlotHelper plotHelper = new PlotHelper("working_files/prediction/predict.txt");
    plotHelper.setMaxSeriesLength(5);

    for (; ; ) {
      TimeUnit.SECONDS.sleep(3);
      plotHelper.refresh();
      System.out.print("plot refreshed: ");
    }
  }
}
