package ru.eltech.mapeshkov.spark;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.ml.evaluation.Evaluator;
import org.apache.spark.ml.param.ParamMap;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.util.Arrays;

public class MyEvaluator extends Evaluator {
    @Override
    public double evaluate(Dataset<?> dataset) {
        Dataset<Row> datasetCopy = dataset.toDF();

        String[] columns = datasetCopy.columns();
        int labelIndex = Arrays.asList(columns).indexOf("label");
        int predictionIndex = Arrays.asList(columns).indexOf("prediction");

        //double[] accuracyError = {0.0};
        //final long datasetSize = rowDataset.count();

/*        rowDataset.foreach(row -> {
            String label = row.mkString(";").split(";")[4];
            String prediction = row.mkString(";").split(";")[6];

            MyFileWriter.println((Math.abs(
                    Double.parseDouble(prediction) - Double.parseDouble(label)) / datasetSize));

            accuracyError[0] += (Math.abs(
                    Double.parseDouble(prediction) - Double.parseDouble(label)) / datasetSize);

            MyFileWriter.println("accuracyError[0]= " + accuracyError[0]);
        });*/

        double error = Double.NaN;
        JavaRDD<Row> filteredDatasetCopy = datasetCopy.toJavaRDD().filter(row -> {
                    String label = row.mkString(";").split(";")[labelIndex];
                    return !label.equals("null");
                }
        );

        if (!filteredDatasetCopy.isEmpty()) {
            error = filteredDatasetCopy.map(row -> {
                String label = row.mkString(";").split(";")[labelIndex];
                double labelDouble = 0;
                labelDouble = Double.parseDouble(label);
                String prediction = row.mkString(";").split(";")[predictionIndex];
                double predictionDouble;
                predictionDouble = Double.parseDouble(prediction);

                return Math.abs(predictionDouble - labelDouble);
            }).reduce((num1, num2) -> num1 + num2);

            error /= datasetCopy.count();
        }

        return error;
    }

    @Override
    public Evaluator copy(ParamMap paramMap) {
        return null;
    }

    @Override
    public String uid() {
        return null;
    }
}