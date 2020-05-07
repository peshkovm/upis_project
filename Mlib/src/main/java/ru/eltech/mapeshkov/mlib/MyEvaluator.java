package ru.eltech.mapeshkov.mlib;

import java.util.Arrays;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.ml.evaluation.Evaluator;
import org.apache.spark.ml.param.ParamMap;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

/** Class that evaluates error between label and prediction in specified dataset */
public class MyEvaluator extends Evaluator {
  /**
   * Evaluates error. If dataset doesn't contain prediction for label, than NaN will be returned
   *
   * @param dataset
   * @return
   */
  @Override
  public double evaluate(Dataset<?> dataset) {
    Dataset<Row> datasetCopy = dataset.toDF();

    String[] columns = datasetCopy.columns();
    int labelIndex = Arrays.asList(columns).indexOf("label");
    int predictionIndex = Arrays.asList(columns).indexOf("prediction");

    double error = Double.NaN;
    JavaRDD<Row> filteredDatasetCopy =
        datasetCopy
            .toJavaRDD()
            .filter(
                row -> {
                  String label = row.mkString(";").split(";")[labelIndex];
                  return !label.equals("null");
                });

    if (!filteredDatasetCopy.isEmpty()) {
      error =
          filteredDatasetCopy
              .map(
                  row -> {
                    String label = row.mkString(";").split(";")[labelIndex];
                    double labelDouble = 0;
                    labelDouble = Double.parseDouble(label);
                    String prediction = row.mkString(";").split(";")[predictionIndex];
                    double predictionDouble;
                    predictionDouble = Double.parseDouble(prediction);

                    return Math.abs(predictionDouble - labelDouble) / labelDouble * 100;
                  })
              .reduce((num1, num2) -> num1 + num2);

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
