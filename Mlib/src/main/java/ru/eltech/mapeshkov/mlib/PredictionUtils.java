package ru.eltech.mapeshkov.mlib;

import java.util.ArrayList;
import org.apache.spark.ml.Model;
import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.classification.LogisticRegression;
import org.apache.spark.ml.evaluation.Evaluator;
import org.apache.spark.ml.evaluation.RegressionEvaluator;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.StringIndexerModel;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.param.ParamMap;
import org.apache.spark.ml.regression.DecisionTreeRegressor;
import org.apache.spark.ml.regression.GBTRegressor;
import org.apache.spark.ml.regression.GeneralizedLinearRegression;
import org.apache.spark.ml.regression.IsotonicRegression;
import org.apache.spark.ml.regression.LinearRegression;
import org.apache.spark.ml.regression.LinearRegressionModel;
import org.apache.spark.ml.regression.RandomForestRegressor;
import org.apache.spark.ml.tuning.CrossValidator;
import org.apache.spark.ml.tuning.CrossValidatorModel;
import org.apache.spark.ml.tuning.ParamGridBuilder;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

/** Class that contains util methods for prediction */
public class PredictionUtils {

  // Suppresses default constructor, ensuring non-instantiability.
  private PredictionUtils() {}

  /**
   * Trains linear regression model for sliding window with sentiment prediction
   *
   * @param trainingDatasetWindowed
   * @param windowWidth
   * @param logWriter
   * @return
   * @throws Exception
   */
  public static Model<?> trainSlidingWindowWithSentimentModel(
      Dataset<Row> trainingDatasetWindowed, int windowWidth, MyFileWriter logWriter)
      throws Exception {
    // System.setProperty("hadoop.home.dir", "C:\\winutils\\");

    /*        SparkSession mlib = SparkSession
            .builder()
            .appName("Spark ML my application")
            .config("mlib.some.config.option", "some-value")
            .master("local[*]")
            .getOrCreate();

    SparkContext conf = mlib.sparkContext();

    // Create a Java version of the Spark Context
    JavaSparkContext sc = new JavaSparkContext(conf);*/
    Dataset<Row> inDataCopy = trainingDatasetWindowed.toDF();

    logWriter.println("Initial:");
    logWriter.printSchema(inDataCopy);
    logWriter.show(inDataCopy);

    ArrayList<StringIndexer> sentimentIndexers = new ArrayList<>();
    ArrayList<String> stockList = new ArrayList<>();

    for (int i = windowWidth - 1; i >= 0; i--) {
      if (i == 0) {
        sentimentIndexers.add(
            new StringIndexer()
                .setInputCol("sentiment_today")
                .setOutputCol("sentiment_today_index")
                .setStringOrderType("alphabetAsc"));
        stockList.add("stock_today");
      } else {
        sentimentIndexers.add(
            new StringIndexer()
                .setInputCol("sentiment_" + i)
                .setOutputCol("sentiment_" + i + "_index")
                .setStringOrderType("alphabetAsc"));
        stockList.add("stock_" + i);
      }
    }

    ArrayList<String> featuresInputColsList = new ArrayList<>();

    for (int i = 0; i < windowWidth; i++) {
      featuresInputColsList.add(sentimentIndexers.get(i).getOutputCol());
      featuresInputColsList.add(stockList.get(i));
    }

    String[] featuresInputCols = new String[featuresInputColsList.size()];
    featuresInputCols = featuresInputColsList.toArray(featuresInputCols);

    ArrayList<StringIndexerModel> sentimentIndexersModels = new ArrayList<>();
    for (StringIndexer sentimentIndexer : sentimentIndexers) {
      sentimentIndexersModels.add(sentimentIndexer.fit(trainingDatasetWindowed));
    }

    for (StringIndexer sentimentIndexer : sentimentIndexers) {
      inDataCopy = sentimentIndexer.fit(inDataCopy).transform(inDataCopy);
    }

    logWriter.println("After sentimentIndexer:");
    logWriter.printSchema(inDataCopy);
    logWriter.show(inDataCopy);

    VectorAssembler assembler =
        new VectorAssembler().setInputCols(featuresInputCols).setOutputCol("features");

    inDataCopy = assembler.transform(inDataCopy);

    logWriter.println("After assembler:");
    logWriter.printSchema(inDataCopy);
    logWriter.show(inDataCopy);

    LinearRegression lr =
        new LinearRegression()
            .setFeaturesCol("features")
            .setLabelCol("label")
            .setPredictionCol("prediction");

    GeneralizedLinearRegression glr =
        new GeneralizedLinearRegression()
            .setFeaturesCol("features")
            .setLabelCol("label")
            .setPredictionCol("prediction")
            .setMaxIter(1000);
    // .setRegParam(0.001);

    LogisticRegression logr =
        new LogisticRegression()
            .setFeaturesCol("features")
            .setLabelCol("label")
            .setPredictionCol("prediction")
            .setMaxIter(10)
            .setRegParam(0.01);

    DecisionTreeRegressor dt =
        new DecisionTreeRegressor()
            .setLabelCol("label")
            .setFeaturesCol("features")
            .setPredictionCol("prediction");

    GBTRegressor gbt =
        new GBTRegressor()
            .setLabelCol("label")
            .setFeaturesCol("features")
            .setPredictionCol("prediction")
            .setMaxIter(50);

    RandomForestRegressor rf =
        new RandomForestRegressor()
            .setLabelCol("label")
            .setFeaturesCol("features")
            .setPredictionCol("prediction");

    IsotonicRegression ir =
        new IsotonicRegression()
            .setLabelCol("label")
            .setFeaturesCol("features")
            .setPredictionCol("prediction");

    ArrayList<PipelineStage> pipelineStagesList = new ArrayList<>(sentimentIndexersModels);

    pipelineStagesList.add(assembler);
    pipelineStagesList.add(lr); // regression

    PipelineStage[] pipelineStages = new PipelineStage[pipelineStagesList.size()];
    pipelineStages = pipelineStagesList.toArray(pipelineStages);

    Pipeline pipeline = new Pipeline().setStages(pipelineStages);

    logWriter.println("trainingData:");
    logWriter.printSchema(trainingDatasetWindowed);
    logWriter.show(trainingDatasetWindowed);

    logWriter.println("trainingData count= " + trainingDatasetWindowed.count());
    logWriter.println();

    Evaluator evaluator = new MyEvaluator();

    // lr
    ParamMap[] paramGrid =
        new ParamGridBuilder()
            .addGrid(lr.maxIter(), new int[] {10, 100, 10_000})
            .addGrid(lr.elasticNetParam(), new double[] {0, 0.1, 0.5, 0.8, 1})
            .addGrid(lr.regParam(), new double[] {0, 0.001, 0.5, 1, 10, 50, 100})
            .build();

    CrossValidator crossValidator =
        new CrossValidator()
            .setEstimator(pipeline)
            .setEstimatorParamMaps(paramGrid)
            .setEvaluator(new RegressionEvaluator())
            .setNumFolds(10);

    // PipelineModel pipelineModel = pipeline.fit(trainingDatasetWindowed);
    CrossValidatorModel crossValidatorModel = crossValidator.fit(trainingDatasetWindowed);
    Model<?> bestModel = crossValidatorModel.bestModel();

    LinearRegressionModel lrModelFromBestModel =
        (LinearRegressionModel) (((PipelineModel) bestModel).stages()[pipelineStages.length - 1]);
    logWriter.println(
        "Coefficients: "
            + lrModelFromBestModel.coefficients()
            + " Intercept: "
            + lrModelFromBestModel.intercept());

    return bestModel;
  }

  /**
   * Trains linear regression model for sliding window without sentiment prediction
   *
   * @param trainingDatasetWindowed
   * @param windowWidth
   * @param logWriter
   * @return
   * @throws Exception
   */
  public static Model<?> trainSlidingWindowWithoutSentimentModel(
      Dataset<Row> trainingDatasetWindowed, int windowWidth, MyFileWriter logWriter)
      throws Exception {
    // System.setProperty("hadoop.home.dir", "C:\\winutils\\");

    /*        SparkSession mlib = SparkSession
            .builder()
            .appName("Spark ML my application")
            .config("mlib.some.config.option", "some-value")
            .master("local[*]")
            .getOrCreate();

    SparkContext conf = mlib.sparkContext();

    // Create a Java version of the Spark Context
    JavaSparkContext sc = new JavaSparkContext(conf);*/
    Dataset<Row> inDataCopy = trainingDatasetWindowed.toDF();

    logWriter.println("Initial:");
    logWriter.printSchema(inDataCopy);
    logWriter.show(inDataCopy);

    ArrayList<String> stockList = new ArrayList<>();

    for (int i = windowWidth - 1; i >= 0; i--) {
      if (i == 0) {
        stockList.add("stock_today");
      } else {
        stockList.add("stock_" + i);
      }
    }

    ArrayList<String> featuresInputColsList = new ArrayList<>();

    for (int i = 0; i < windowWidth; i++) {
      featuresInputColsList.add(stockList.get(i));
    }

    String[] featuresInputCols = new String[featuresInputColsList.size()];
    featuresInputCols = featuresInputColsList.toArray(featuresInputCols);

    VectorAssembler assembler =
        new VectorAssembler().setInputCols(featuresInputCols).setOutputCol("features");

    inDataCopy = assembler.transform(inDataCopy);

    logWriter.println("After assembler:");
    logWriter.printSchema(inDataCopy);
    logWriter.show(inDataCopy);

    LinearRegression lr =
        new LinearRegression()
            .setFeaturesCol("features")
            .setLabelCol("label")
            .setPredictionCol("prediction")
            .setMaxIter(1000);

    GeneralizedLinearRegression glr =
        new GeneralizedLinearRegression()
            .setFeaturesCol("features")
            .setLabelCol("label")
            .setPredictionCol("prediction")
            .setMaxIter(1000);
    // .setRegParam(0.001);

    LogisticRegression logr =
        new LogisticRegression()
            .setFeaturesCol("features")
            .setLabelCol("label")
            .setPredictionCol("prediction")
            .setMaxIter(10)
            .setRegParam(0.01);

    DecisionTreeRegressor dt =
        new DecisionTreeRegressor()
            .setLabelCol("label")
            .setFeaturesCol("features")
            .setPredictionCol("prediction");

    GBTRegressor gbt =
        new GBTRegressor()
            .setLabelCol("label")
            .setFeaturesCol("features")
            .setPredictionCol("prediction")
            .setMaxIter(50);

    RandomForestRegressor rf =
        new RandomForestRegressor()
            .setLabelCol("label")
            .setFeaturesCol("features")
            .setPredictionCol("prediction");

    IsotonicRegression ir =
        new IsotonicRegression()
            .setLabelCol("label")
            .setFeaturesCol("features")
            .setPredictionCol("prediction");

    ArrayList<PipelineStage> pipelineStagesList = new ArrayList<>();

    pipelineStagesList.add(assembler);
    pipelineStagesList.add(lr); // regression

    PipelineStage[] pipelineStages = new PipelineStage[pipelineStagesList.size()];
    pipelineStages = pipelineStagesList.toArray(pipelineStages);

    Pipeline pipeline = new Pipeline().setStages(pipelineStages);

    logWriter.println("trainingData:");
    logWriter.printSchema(trainingDatasetWindowed);
    logWriter.show(trainingDatasetWindowed);

    logWriter.println("trainingData count= " + trainingDatasetWindowed.count());
    logWriter.println();

    Evaluator evaluator = new MyEvaluator();

    // lr
    ParamMap[] paramGrid =
        new ParamGridBuilder()
            .addGrid(lr.maxIter(), new int[] {10, 100, 10_000})
            .addGrid(lr.elasticNetParam(), new double[] {0, 0.1, 0.5, 0.8, 1})
            .addGrid(lr.regParam(), new double[] {0, 0.001, 0.5, 1, 10, 50, 100})
            .build();

    CrossValidator crossValidator =
        new CrossValidator()
            .setEstimator(pipeline)
            .setEstimatorParamMaps(paramGrid)
            .setEvaluator(new RegressionEvaluator())
            .setNumFolds(10);

    // PipelineModel pipelineModel = pipeline.fit(trainingDatasetWindowed);
    CrossValidatorModel crossValidatorModel = crossValidator.fit(trainingDatasetWindowed);
    Model<?> bestModel = crossValidatorModel.bestModel();

    LinearRegressionModel lrModelFromBestModel =
        (LinearRegressionModel) (((PipelineModel) bestModel).stages()[pipelineStages.length - 1]);
    logWriter.println(
        "Coefficients: "
            + lrModelFromBestModel.coefficients()
            + " Intercept: "
            + lrModelFromBestModel.intercept());

    return bestModel;
  }

  /**
   * Trains linear regression model for not-sliding window prediction
   *
   * @param trainingDataset
   * @param logWriter
   * @return
   */
  public static Model<?> trainModel(Dataset<Row> trainingDataset, MyFileWriter logWriter) {
    // System.setProperty("hadoop.home.dir", "C:\\winutils\\");

    /*        SparkSession mlib = SparkSession
            .builder()
            .appName("Spark ML my application")
            .config("mlib.some.config.option", "some-value")
            .master("local[*]")
            .getOrCreate();

    SparkContext conf = mlib.sparkContext();

    //Create a Java version of the Spark Context
    JavaSparkContext sc = new JavaSparkContext(conf);*/

    Dataset<Row> inDataCopy = trainingDataset.toDF();

    logWriter.println("Initial:");
    logWriter.printSchema(inDataCopy);
    logWriter.show(inDataCopy);

    StringIndexer companyIndexer =
        new StringIndexer().setInputCol("company").setOutputCol("companyIndex");

    StringIndexer sentimentIndexer =
        new StringIndexer().setInputCol("sentiment").setOutputCol("sentimentIndex");

    inDataCopy = companyIndexer.fit(inDataCopy).transform(inDataCopy);
    inDataCopy = sentimentIndexer.fit(inDataCopy).transform(inDataCopy);

    logWriter.println("After companyIndexer and sentimentIndexer:");
    logWriter.printSchema(inDataCopy);
    logWriter.show(inDataCopy);

    VectorAssembler assembler =
        new VectorAssembler()
            .setInputCols(
                new String[] {
                  companyIndexer.getOutputCol(),
                  sentimentIndexer.getOutputCol(),
                  "year",
                  "month",
                  "day",
                  "today_stock"
                })
            .setOutputCol("features");

    inDataCopy = assembler.transform(inDataCopy);

    logWriter.println("After assembler:");
    logWriter.printSchema(inDataCopy);
    logWriter.show(inDataCopy);

    LinearRegression lr =
        new LinearRegression()
            .setFeaturesCol("features")
            .setLabelCol("label")
            .setPredictionCol("prediction")
            .setMaxIter(1000);

    GeneralizedLinearRegression glr =
        new GeneralizedLinearRegression()
            .setFeaturesCol("features")
            .setLabelCol("label")
            .setPredictionCol("prediction")
            .setMaxIter(1000);
    // .setRegParam(0.001);

    LogisticRegression logr =
        new LogisticRegression()
            .setFeaturesCol("features")
            .setLabelCol("label")
            .setPredictionCol("prediction")
            .setMaxIter(10)
            .setRegParam(0.01);

    DecisionTreeRegressor dt =
        new DecisionTreeRegressor()
            .setLabelCol("label")
            .setFeaturesCol("features")
            .setPredictionCol("prediction");

    GBTRegressor gbt =
        new GBTRegressor()
            .setLabelCol("label")
            .setFeaturesCol("features")
            .setPredictionCol("prediction")
            .setMaxIter(50);

    RandomForestRegressor rf =
        new RandomForestRegressor()
            .setLabelCol("label")
            .setFeaturesCol("features")
            .setPredictionCol("prediction");

    IsotonicRegression ir =
        new IsotonicRegression()
            .setLabelCol("label")
            .setFeaturesCol("features")
            .setPredictionCol("prediction");

    Pipeline pipeline =
        new Pipeline()
            .setStages(new PipelineStage[] {companyIndexer, sentimentIndexer, assembler, lr});

    logWriter.println("Training data:");
    logWriter.printSchema(trainingDataset);
    logWriter.show(trainingDataset);

    logWriter.println("Training data count = " + trainingDataset.count());
    logWriter.println();

    Evaluator evaluator = new MyEvaluator();

    ParamMap[] paramGrid =
        new ParamGridBuilder()
            .addGrid(lr.maxIter(), new int[] {10, 1000, 1_000})
            .addGrid(lr.regParam(), new double[] {0, 0.3, 0.001})
            .addGrid(lr.elasticNetParam(), new double[] {0, 0.5, 1})
            .build();

    CrossValidator crossValidator =
        new CrossValidator()
            .setEstimator(pipeline)
            .setEstimatorParamMaps(paramGrid)
            .setEvaluator(evaluator)
            .setNumFolds(3);

    CrossValidatorModel crossValidatorModel = crossValidator.fit(trainingDataset);

    return crossValidatorModel.bestModel();
  }

  /**
   * Predicts labels of given dataset
   *
   * @param trainedModel
   * @param testDataset
   * @param logWriter
   * @return
   */
  public static Dataset<Row> predict(
      Model<?> trainedModel, Dataset<Row> testDataset, MyFileWriter logWriter) {
    logWriter.println("Test data:");
    logWriter.printSchema(testDataset);
    logWriter.show(testDataset);

    logWriter.println("Test data count = " + testDataset.count());
    logWriter.println();

    Dataset<Row> predictions = trainedModel.transform(testDataset);

    logWriter.println("Result:");
    logWriter.show(predictions);

    MyEvaluator evaluator = new MyEvaluator();

    double accuracy = evaluator.evaluate(predictions);
    logWriter.println("accuracy= " + accuracy);

    return predictions;
  }
}
