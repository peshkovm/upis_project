package ru.eltech.mapeshkov.spark.in_data_refactor_utils;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class InDataRefactorUtils {

    // Suppresses default constructor, ensuring non-instantiability.
    private InDataRefactorUtils() {
    }

    public static Dataset<Row> reformatNotLabeledDataToLabeled(SparkSession spark, Dataset<Row> datasetNotLabeled) {
        datasetNotLabeled = datasetNotLabeled.withColumn("label", functions.lit(-1.0));
        String[] columns = datasetNotLabeled.columns();
        int today_stockIndex = Arrays.asList(columns).indexOf("today_stock");
        int labelIndex = Arrays.asList(columns).indexOf("label");
        List<Row> rows = datasetNotLabeled.collectAsList();

        for (int rowNum = 1; rowNum < rows.size(); rowNum++) {
            Row rowTomorrow = rows.get(rowNum);
            Row rowToday = rows.get(rowNum - 1);

            String tomorrowStock = rowTomorrow.mkString(",").split(",")[today_stockIndex];

            String[] rowStr = rowToday.mkString(",").split(",");
            rowStr[labelIndex] = tomorrowStock;

            String company = rowStr[0];
            String sentiment = rowStr[1];
            int year = Integer.parseInt(rowStr[2]);
            int month = Integer.parseInt(rowStr[3]);
            int day = Integer.parseInt(rowStr[4]);
            double today_stock = Double.parseDouble(rowStr[5]);
            double label = Double.parseDouble(rowStr[6]);

            Row rowTodayLabeled = RowFactory.create(company,
                    sentiment,
                    year,
                    month,
                    day,
                    today_stock,
                    label);

            rows.set(rowNum - 1, rowTodayLabeled);
        }

        List<Row> rowsLabeled = rows.subList(0, rows.size() - 1);

        StructType schemaLabeled = new StructType(new StructField[]{
                new StructField("company", DataTypes.StringType, false, Metadata.empty()),
                new StructField("sentiment", DataTypes.StringType, false, Metadata.empty()),
                new StructField("year", DataTypes.IntegerType, false, Metadata.empty()),
                new StructField("month", DataTypes.IntegerType, false, Metadata.empty()),
                new StructField("day", DataTypes.IntegerType, false, Metadata.empty()),
                new StructField("today_stock", DataTypes.DoubleType, false, Metadata.empty()),
                new StructField("label", DataTypes.DoubleType, true, Metadata.empty()),
        });

        Dataset<Row> datasetLabeled = spark.createDataFrame(rowsLabeled, schemaLabeled);

        return datasetLabeled;
    }

    public static Dataset<Row> reformatInDataToSlidingWindowLayout(SparkSession spark, Dataset<Row> datasetNotWindowed, int windowWidth) {
        class WindowDataPair {
            private String sentiment = "";
            private double stock;

            public WindowDataPair() {
            }

            public WindowDataPair(String sentiment, double stock) {
                this.sentiment = sentiment;
                this.stock = stock;
            }

            @Override
            public String toString() {
                return sentiment + "," + stock + ",";
            }

            public String getSentiment() {
                return sentiment;
            }

            public double getStock() {
                return stock;
            }
        }

        class Label {
            private double label;

            public Label() {

            }

            public Label(double label) {
                this.label = label;
            }

            @Override
            public String toString() {
                return String.valueOf(label);
            }

            public double getLabel() {
                return label;
            }
        }

        List<Row> rowsNotWindowed = datasetNotWindowed.collectAsList();
        List<WindowDataPair> window = new ArrayList<>(windowWidth);
        List<Row> rowsWindowed = new ArrayList<>();
        Label label;
        String[] columns = datasetNotWindowed.columns();

        //fill window with blank data
        for (int i = 0; i < windowWidth; i++) {
            window.add(new WindowDataPair());
        }

        //write data
        for (int rowNum = 0; rowNum < rowsNotWindowed.size(); rowNum++) {
            Row row = rowsNotWindowed.get(rowNum);
            String[] split = row.mkString(",").split(",");

            //Arrays.stream(split).forEach(System.out::println);

            String company = split[Arrays.asList(columns).indexOf("company")];
            String sentiment = split[Arrays.asList(columns).indexOf("sentiment")];
            String year = split[Arrays.asList(columns).indexOf("year")];
            String month = split[Arrays.asList(columns).indexOf("month")];
            String day = split[Arrays.asList(columns).indexOf("day")];
            double today_stock = Double.parseDouble(split[Arrays.asList(columns).indexOf("today_stock")]);
            double tomorrow_stock = Double.parseDouble(split[Arrays.asList(columns).indexOf("label")]);

            window.set(windowWidth - 1, new WindowDataPair(sentiment, today_stock));
            label = new Label(tomorrow_stock);

            List<Object> windowList = new ArrayList<>();

            if (rowNum >= windowWidth - 1) {
                //window.forEach(writer::print);
                // writer.println(label);

                window.forEach(windowPair -> {
                    windowList.add(windowPair.getSentiment());
                    windowList.add(windowPair.getStock());
                });
                windowList.add(label.getLabel());

                Row newRow = RowFactory.create(windowList.toArray());
                rowsWindowed.add(newRow);
            }

            //writer.println(Arrays.toString(window.toArray()) + label);
            Collections.rotate(window, -1);
        }

        ////////////fill schema///////////////////
        ArrayList<StructField> structFieldList = new ArrayList<>();
        StructField[] structFields = new StructField[2 * windowWidth + 1];

        for (int i = windowWidth - 1; i >= 0; i--) {
            if (i != 0) {
                structFieldList.add(new StructField("sentiment_" + i, DataTypes.StringType, false, Metadata.empty()));
                structFieldList.add(new StructField("stock_" + i, DataTypes.DoubleType, false, Metadata.empty()));
            } else {
                structFieldList.add(new StructField("sentiment_today", DataTypes.StringType, false, Metadata.empty()));
                structFieldList.add(new StructField("stock_today", DataTypes.DoubleType, false, Metadata.empty()));
                structFieldList.add(new StructField("label", DataTypes.DoubleType, true, Metadata.empty()));
            }
        }

        structFields = structFieldList.toArray(structFields);
        //////////////////////////////////////////

        StructType schemaWindowed = new StructType(structFields);

        Dataset<Row> datasetWindowed = spark.createDataFrame(rowsWindowed, schemaWindowed);

        return datasetWindowed;
    }
}