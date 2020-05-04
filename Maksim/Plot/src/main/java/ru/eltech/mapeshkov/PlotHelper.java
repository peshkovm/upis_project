package ru.eltech.mapeshkov;

import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class PlotHelper {
    final private CombinedPlot plot;
    final private String fileName;
    private long numOfNews = 1;

    private final String[] keys = {"real stock", "prediction stock"};

    public PlotHelper(final String fileName) throws IOException, InterruptedException {
        this.fileName = fileName;

        plot = new CombinedPlot("news/stock plot", keys);


        refresh();
    }

    public void refresh() throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new FileReader(this.fileName))) {

            String line;

            while ((line = reader.readLine()) != null) {
                String[] split = line.split(",");

                if (split.length == 2) {
                    double realStock = Double.parseDouble(split[0]);
                    double predictionStock = Double.parseDouble(split[1]);

                    plot.addPoint(new XYDataItem(numOfNews, realStock), keys[0]);
                    plot.addPoint(new XYDataItem(++numOfNews, predictionStock), keys[1]);

                    System.out.println(realStock + " " + predictionStock);
                }
            }
        }
    }

    public void setMaxSeriesLength(int length) {
        plot.setMaxSeriesLength(length);
    }
}