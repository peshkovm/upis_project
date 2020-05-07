package ru.eltech.mapeshkov.plot;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import org.jfree.data.xy.XYDataItem;

public class PlotHelper {
  private final CombinedChart chart;
  private final String fileName;
  private long numOfNews = 1;

  private final Comparable<?>[] keys = {"real stock", "prediction stock"};

  /**
   * Creates helper for instance of {@link CombinedChart}
   *
   * @param fileName
   * @throws IOException
   * @throws InterruptedException
   */
  public PlotHelper(final String fileName) throws IOException, InterruptedException {
    this.fileName = fileName;

    chart = new CombinedChart("news/stock chart", "label", "date", keys);

    refresh();
  }

  /**
   * Refresh the chart Reread file and add new data to chart if presents
   *
   * @throws IOException
   */
  public void refresh() throws IOException {
    try (BufferedReader reader = new BufferedReader(new FileReader(this.fileName))) {

      String line;

      while ((line = reader.readLine()) != null) {
        String[] split = line.split(",");

        if (split.length == 2) {
          double realStock = Double.parseDouble(split[0]);
          double predictionStock = Double.parseDouble(split[1]);

          chart.addPoint(new XYDataItem(numOfNews, realStock), keys[0]);
          chart.addPoint(new XYDataItem(++numOfNews, predictionStock), keys[1]);

          System.out.println(realStock + " " + predictionStock);
        }
      }
    }
  }

  /**
   * Sets max count of points in chart
   *
   * @param length
   */
  public void setMaxSeriesLength(int length) {
    chart.setMaxSeriesLength(length);
  }
}
