package ru.eltech.mapeshkov.spark;

        import org.apache.spark.sql.Dataset;
        import org.apache.spark.sql.Row;
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
        import org.jfree.data.xy.XYDataset;
        import org.jfree.data.xy.XYSeries;
        import org.jfree.data.xy.XYSeriesCollection;

        import java.awt.*;
        import java.util.Arrays;
        import java.util.List;

public class CombinedPlot extends ApplicationFrame {
    private final XYSeries labelSeries = new XYSeries("label");
    private final XYSeries oldPrediction = new XYSeries("old prediction");
    private final XYSeries newPrediction = new XYSeries("new prediction");

    /**
     * Constructs a new application frame.
     *
     * @param title the frame title.
     */
    public CombinedPlot(String title, Dataset<Row> rowDataset) {
        this(title, rowDataset, 500, 300);
    }

    public CombinedPlot(String title, Dataset<Row> rowDataset, int width, int height) {
        super(title);
        final JFreeChart chart = createCombinedChart(rowDataset);
        final ChartPanel panel = new ChartPanel(chart,
                true,
                true,
                true,
                false,
                true);
        panel.setPreferredSize(new Dimension(width, height));
        setContentPane(panel);
    }

    private JFreeChart createCombinedChart(Dataset<Row> rowDataset) {
        final XYDataset data1 = createPlotDataset(rowDataset);
        final StandardXYItemRenderer renderer = new StandardXYItemRenderer();
        renderer.setBaseShapesVisible(true);
        renderer.setDefaultItemLabelGenerator(new StandardXYItemLabelGenerator());
        final org.jfree.chart.axis.NumberAxis rangeAxis = new org.jfree.chart.axis.NumberAxis("label");
        final org.jfree.chart.axis.NumberAxis domainAxis = new org.jfree.chart.axis.NumberAxis("date");
        final XYPlot subplot = new XYPlot(data1, domainAxis, rangeAxis, renderer);

        final CombinedDomainXYPlot plot = new CombinedDomainXYPlot();

        plot.add(subplot);
        plot.setOrientation(PlotOrientation.VERTICAL);

        return new JFreeChart("Plot",
                JFreeChart.DEFAULT_TITLE_FONT,
                plot,
                true);
    }

    private XYDataset createPlotDataset(Dataset<Row> rowDataset) {
        List<Row> rows = rowDataset.collectAsList();
        final int[] i = {1};

        String[] columns = rowDataset.columns();
        int labelIndex = Arrays.asList(columns).indexOf("label");
        int predictionIndex = Arrays.asList(columns).indexOf("prediction");

        rows.forEach(row -> {
            String label = row.mkString(";").split(";")[labelIndex];
            String prediction = row.mkString(";").split(";")[predictionIndex];
            double predictionDouble = Double.parseDouble(prediction);

            if (!label.equals("null")) {
                double labelDouble = Double.parseDouble(label);
                //double error = Math.abs(predictionDouble - labelDouble);

                labelSeries.add(i[0], labelDouble);
                oldPrediction.add(i[0]++, predictionDouble);
            } else
                newPrediction.add(i[0]++, predictionDouble);
        });

        final XYSeriesCollection collection = new XYSeriesCollection();
        collection.addSeries(labelSeries);
        collection.addSeries(oldPrediction);
        collection.addSeries(newPrediction);

        return collection;
    }

    private static class LabeledXYDataset extends AbstractDataset {

    }
}