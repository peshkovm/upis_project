package ru.eltech.mapeshkov.plot;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.WindowConstants;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/** A chart class that can contain multiple plots */
public class CombinedChart extends ApplicationFrame {
  private final java.util.List<XYSeries> seriesList;
  private final JFreeChart chart;
  private final int width, height;

  /**
   * Construct a new chart with given parameters
   *
   * @param title chart title (shown on top of chart frame window)
   * @param xLabel X axis label
   * @param yLabel Y axis label
   * @param series data of chart. Each series represents one plot
   */
  public CombinedChart(
      final String title, final String xLabel, final String yLabel, final XYSeries... series) {
    this(title, xLabel, yLabel, 500, 300, series);
  }

  /**
   * Construct a new chart with given parameters
   *
   * @param title chart title (shown on top of chart frame window)
   * @param xLabel X axis label
   * @param yLabel Y axis label
   * @param width width of chart frame window
   * @param height height of chart frame window
   * @param series
   */
  public CombinedChart(
      final String title,
      final String xLabel,
      final String yLabel,
      final int width,
      final int height,
      final XYSeries... series) {
    super(title);

    seriesList = new ArrayList<>(Arrays.asList(series));

    chart = createCombinedChart(title, xLabel, yLabel, seriesList);
    this.width = width;
    this.height = height;

    final ChartPanel panel = new ChartPanel(chart, true, true, true, false, true);
    panel.setPreferredSize(new Dimension(width, height));
    setContentPane(panel);
    pack();
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setVisible(true);
  }

  /**
   * Construct a new chart with given parameters. Chart creates with empty data
   *
   * @param title chart title (shown on top of chart frame window)
   * @param xLabel X axis label
   * @param yLabel Y axis label
   * @param keys array of names of plots of chart
   */
  public CombinedChart(
      final String title, final String xLabel, final String yLabel, final Comparable<?>... keys) {
    this(title, xLabel, yLabel, Arrays.stream(keys).map(XYSeries::new).toArray(XYSeries[]::new));
  }

  /**
   * Construct a new chart with given parameters. Chart creates with empty data
   *
   * @param title chart title (shown on top of chart frame window)
   * @param xLabel X axis label
   * @param yLabel Y axis label
   * @param width width of chart frame window
   * @param height height of chart frame window
   * @param keys array of names of plots of chart
   */
  public CombinedChart(
      final String title,
      final String xLabel,
      final String yLabel,
      final int width,
      final int height,
      final Comparable<?>... keys) {
    this(
        title,
        xLabel,
        yLabel,
        width,
        height,
        Arrays.stream(keys).map(XYSeries::new).toArray(XYSeries[]::new));
  }

  private JFreeChart createCombinedChart(
      final String title,
      final String xLabel,
      final String yLabel,
      final java.util.List<XYSeries> seriesList) {
    final XYSeriesCollection collection = new XYSeriesCollection();

    seriesList.forEach(collection::addSeries);

    // final XYDataset data1 = collection;
    final StandardXYItemRenderer renderer = new StandardXYItemRenderer();
    renderer.setBaseShapesVisible(true);
    renderer.setDefaultItemLabelGenerator(new StandardXYItemLabelGenerator());
    final org.jfree.chart.axis.NumberAxis rangeAxis = new org.jfree.chart.axis.NumberAxis(yLabel);
    final org.jfree.chart.axis.NumberAxis domainAxis = new org.jfree.chart.axis.NumberAxis(xLabel);

    final XYPlot subplot = new XYPlot(collection, domainAxis, rangeAxis, renderer);
    final CombinedDomainXYPlot plot = new CombinedDomainXYPlot();

    plot.add(subplot);
    plot.setOrientation(PlotOrientation.VERTICAL);

    return new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
  }

  /**
   * Add given point to plot with given name
   *
   * @param point point to add
   * @param key name of plot
   */
  public void addPoint(final XYDataItem point, final Comparable<?> key) {
    seriesList.forEach(
        series -> {
          if (series.getKey().equals(key)) {
            series.add(point);
          }
        });
  }

  /**
   * Add new plot to chart
   *
   * @param series plot to add
   */
  public void addSeries(final XYSeries series) {
    seriesList.add(series);
  }

  /**
   * Returns list of plots names
   *
   * @return
   */
  public List<Comparable> getKeys() {
    return seriesList.stream().map(series -> series.getKey()).collect(Collectors.toList());
  }

  /**
   * Sets max count of points in chart
   *
   * @param length
   */
  public void setMaxSeriesLength(int length) {
    seriesList.forEach(series -> series.setMaximumItemCount(length));
  }

  /**
   * Save chart as .jpg to file
   *
   * @param file the file
   */
  public void saveChartAsJPEG(Path file) {
    saveChartAsJPEG(file, this.width, this.height);
  }

  /**
   * Save chart as .jpg to file
   *
   * @param file the file
   * @param width the image width
   * @param height the image height
   */
  public void saveChartAsJPEG(final Path file, final int width, final int height) {
    try {
      File parentFile = file.toFile().getParentFile();
      if (!parentFile.exists() && !parentFile.mkdirs())
        throw new IllegalStateException("Couldn't create dir: " + parentFile);
      ChartUtils.saveChartAsJPEG(file.toFile(), chart, width, height);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
