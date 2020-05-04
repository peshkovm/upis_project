package ru.eltech.mapeshkov.plot;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.data.general.AbstractDataset;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class CombinedPlot extends ApplicationFrame {
  private final java.util.List<XYSeries> seriesList;

  /**
   * Constructs a new application frame.
   *
   * @param title the frame title.
   */
  public CombinedPlot(final String title, final XYSeries... series) {
    this(title, 500, 300, series);
  }

  public CombinedPlot(
      final String title, final int width, final int height, final XYSeries... series) {
    super(title);

    seriesList = new ArrayList<>(Arrays.asList(series));

    final JFreeChart chart = createCombinedChart(seriesList);
    final ChartPanel panel = new ChartPanel(chart, true, true, true, false, true);
    panel.setPreferredSize(new Dimension(width, height));
    setContentPane(panel);
    pack();
    setVisible(true);
  }

  public CombinedPlot(final String title, final Comparable... keys) {
    this(title, Arrays.stream(keys).map(XYSeries::new).toArray(XYSeries[]::new));
  }

  private JFreeChart createCombinedChart(final java.util.List<XYSeries> seriesList) {
    final XYSeriesCollection collection = new XYSeriesCollection();

    seriesList.forEach(collection::addSeries);

    // final XYDataset data1 = collection;
    final StandardXYItemRenderer renderer = new StandardXYItemRenderer();
    renderer.setBaseShapesVisible(true);
    renderer.setDefaultItemLabelGenerator(new StandardXYItemLabelGenerator());
    final org.jfree.chart.axis.NumberAxis rangeAxis = new org.jfree.chart.axis.NumberAxis("label");
    final org.jfree.chart.axis.NumberAxis domainAxis = new org.jfree.chart.axis.NumberAxis("date");

    final XYPlot subplot = new XYPlot(collection, domainAxis, rangeAxis, renderer);
    final CombinedDomainXYPlot plot = new CombinedDomainXYPlot();

    plot.add(subplot);
    plot.setOrientation(PlotOrientation.VERTICAL);

    return new JFreeChart("Plot", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
  }

  public void addPoint(final XYDataItem point, final Comparable key) {
    seriesList.forEach(
        series -> {
          if (series.getKey().equals(key)) {
            series.add(point);
          }
        });
  }

  public void addSeries(final XYSeries series) {
    seriesList.add(series);
  }

  public List<Comparable> getKeys() {
    return seriesList.stream().map(series -> series.getKey()).collect(Collectors.toList());
  }

  public void setMaxSeriesLength(int length) {
    seriesList.forEach(series -> series.setMaximumItemCount(length));
  }

  private static class LabeledXYDataset extends AbstractDataset {}
}
