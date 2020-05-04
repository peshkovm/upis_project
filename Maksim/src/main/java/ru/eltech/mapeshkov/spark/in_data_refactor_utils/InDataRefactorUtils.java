package ru.eltech.mapeshkov.spark.in_data_refactor_utils;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import ru.eltech.mapeshkov.spark.Schemes;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.spark.sql.functions.asc;
import static org.apache.spark.sql.functions.desc;

public class InDataRefactorUtils {

    // Suppresses default constructor, ensuring non-instantiability.
    private InDataRefactorUtils() {
    }

    public static Dataset<Row> sortByDate(final SparkSession spark, final Dataset<Row> dataset, final StructType schema) {
/*        List<Row> rows = dataset.collectAsList();
        String[] columns = dataset.columns();

        rows = rows.stream().sorted((row1, row2) -> {
            int dateIndex = Arrays.asList(columns).indexOf("date");

            Timestamp timestamp1 = row1.getTimestamp(dateIndex);
            Timestamp timestamp2 = row2.getTimestamp(dateIndex);

            return timestamp1.compareTo(timestamp2);
        }).collect(Collectors.toList());

        return spark.createDataFrame(rows, schema);*/

        return dataset.orderBy(asc("date"));
    }

    public static Dataset<Row> reformatNotLabeledDataToLabeled(final SparkSession spark, final Dataset<Row> datasetNotLabeled, boolean addNaNLabel) {
        Dataset<Row> datasetNotLabeledCopy = datasetNotLabeled.toDF();
        datasetNotLabeledCopy = datasetNotLabeledCopy.withColumn("date", new Column("date").cast(DataTypes.StringType));
        datasetNotLabeledCopy = datasetNotLabeledCopy.withColumn("label", functions.lit(Double.NaN));
        String[] columns = datasetNotLabeledCopy.columns();
        int today_stockIndex = Arrays.asList(columns).indexOf("today_stock");
        int labelIndex = Arrays.asList(columns).indexOf("label");
        List<Row> rows = datasetNotLabeledCopy.collectAsList();

        for (int rowNum = 1; rowNum < rows.size(); rowNum++) {
            Row rowTomorrow = rows.get(rowNum);
            Row rowToday = rows.get(rowNum - 1);

            String tomorrowStock = rowTomorrow.mkString(",").split(",")[today_stockIndex];

            String[] rowStr = rowToday.mkString(",").split(",");
            rowStr[labelIndex] = tomorrowStock;

            String company = rowStr[Arrays.asList(columns).indexOf("company")];
            String sentiment = rowStr[Arrays.asList(columns).indexOf("sentiment")];
            String date = rowStr[Arrays.asList(columns).indexOf("date")];
            double today_stock = Double.parseDouble(rowStr[Arrays.asList(columns).indexOf("today_stock")]);
            double label = Double.parseDouble(rowStr[Arrays.asList(columns).indexOf("label")]);

            Row rowTodayLabeled = RowFactory.create(company,
                    sentiment,
                    date,
                    today_stock,
                    label);

            rows.set(rowNum - 1, rowTodayLabeled);
        }

        List<Row> rowsLabeled;

        if (addNaNLabel)
            rowsLabeled = rows.subList(0, rows.size() - 1);
        else
            rowsLabeled = rows.subList(0, rows.size());

        StructType schemaLabeled = Schemes.SCHEMA_LABELED.getScheme();

        Dataset<Row> datasetLabeled = spark.createDataFrame(rowsLabeled, schemaLabeled);

        return datasetLabeled;
    }

    public static Dataset<Row> reformatInDataToSlidingWindowLayout(final SparkSession spark, final Dataset<Row> datasetNotWindowed, int windowWidth) {
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
            String date = split[Arrays.asList(columns).indexOf("date")];
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

        StructType schemaWindowed = Schemes.SCHEMA_WINDOWED.getScheme();

        Dataset<Row> datasetWindowed = spark.createDataFrame(rowsWindowed, schemaWindowed);

        return datasetWindowed;
    }
}