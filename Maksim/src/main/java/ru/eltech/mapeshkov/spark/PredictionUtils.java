package ru.eltech.mapeshkov.spark;

import org.apache.spark.ml.Model;
import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.classification.LogisticRegression;
import org.apache.spark.ml.evaluation.Evaluator;
import org.apache.spark.ml.feature.*;
import org.apache.spark.ml.param.ParamMap;
import org.apache.spark.ml.regression.*;
import org.apache.spark.ml.tuning.CrossValidator;
import org.apache.spark.ml.tuning.CrossValidatorModel;
import org.apache.spark.ml.tuning.ParamGridBuilder;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

public class PredictionUtils {

    // Suppresses default constructor, ensuring non-instantiability.
    private PredictionUtils() {
    }

    public static Model<?> trainModel(Dataset<Row> trainingDataset, MyFileWriter logWriter) {
        //System.setProperty("hadoop.home.dir", "C:\\winutils\\");

/*        SparkSession spark = SparkSession
                .builder()
                .appName("Spark ML my application")
                .config("spark.some.config.option", "some-value")
                .master("local[*]")
                .getOrCreate();*/

        //SparkContext conf = spark.sparkContext();

        // Create a Java version of the Spark Context
        //JavaSparkContext sc = new JavaSparkContext(conf);

        Dataset<Row> inDataCopy = trainingDataset.toDF();

        logWriter.println("Initial:");
        logWriter.printSchema(inDataCopy);
        logWriter.show(inDataCopy);

        StringIndexer companyIndexer = new StringIndexer()
                .setInputCol("company")
                .setOutputCol("companyIndex");

        StringIndexer sentimentIndexer = new StringIndexer()
                .setInputCol("sentiment")
                .setOutputCol("sentimentIndex");

        inDataCopy = companyIndexer.fit(inDataCopy).transform(inDataCopy);
        inDataCopy = sentimentIndexer.fit(inDataCopy).transform(inDataCopy);

        logWriter.println("After companyIndexer and sentimentIndexer:");
        logWriter.printSchema(inDataCopy);
        logWriter.show(inDataCopy);


        VectorAssembler assembler = new VectorAssembler()
                .setInputCols(new String[]{
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

        LinearRegression lr = new LinearRegression()
                .setFeaturesCol("features")
                .setLabelCol("label")
                .setPredictionCol("prediction")
                .setMaxIter(1000);

        GeneralizedLinearRegression glr = new GeneralizedLinearRegression()
                .setFeaturesCol("features")
                .setLabelCol("label")
                .setPredictionCol("prediction")
                .setMaxIter(1000);
        //.setRegParam(0.001);


        LogisticRegression logr = new LogisticRegression()
                .setFeaturesCol("features")
                .setLabelCol("label")
                .setPredictionCol("prediction")
                .setMaxIter(10)
                .setRegParam(0.01);

        DecisionTreeRegressor dt = new DecisionTreeRegressor()
                .setLabelCol("label")
                .setFeaturesCol("features")
                .setPredictionCol("prediction");

        GBTRegressor gbt = new GBTRegressor()
                .setLabelCol("label")
                .setFeaturesCol("features")
                .setPredictionCol("prediction")
                .setMaxIter(50);

        RandomForestRegressor rf = new RandomForestRegressor()
                .setLabelCol("label")
                .setFeaturesCol("features")
                .setPredictionCol("prediction");

        IsotonicRegression ir = new IsotonicRegression()
                .setLabelCol("label")
                .setFeaturesCol("features")
                .setPredictionCol("prediction");


        Pipeline pipeline = new Pipeline()
                .setStages(new PipelineStage[]{companyIndexer, sentimentIndexer, assembler, lr});

        logWriter.println("Training data:");
        logWriter.printSchema(trainingDataset);
        logWriter.show(trainingDataset);

        logWriter.println("Training data count = " + trainingDataset.count());
        logWriter.println();

        Evaluator evaluator = new MyEvaluator();

        ParamMap[] paramGrid = new ParamGridBuilder()
                .addGrid(lr.maxIter(), new int[]{10, 1000, 1_000})
                .addGrid(lr.regParam(), new double[]{0, 0.3, 0.001})
                .addGrid(lr.elasticNetParam(), new double[]{0, 0.5, 1})
                .build();

        CrossValidator crossValidator = new CrossValidator()
                .setEstimator(pipeline)
                .setEstimatorParamMaps(paramGrid)
                .setEvaluator(evaluator)
                .setNumFolds(3);

        CrossValidatorModel crossValidatorModel = crossValidator.fit(trainingDataset);

        return crossValidatorModel.bestModel();
    }

    public static void predict(Dataset<Row> inDataset, MyFileWriter logWriter) {
        Dataset<Row> trainingDataset;
        Dataset<Row> testDataset;

        do {
            Dataset<Row>[] datasets = inDataset.randomSplit(new double[]{0.8, 0.2});
            trainingDataset = datasets[0];
            testDataset = datasets[1];
        } while (testDataset.count() == 0);

        predict(trainingDataset, testDataset, logWriter);
    }

    public static void predict(Dataset<Row> trainingDataset, Dataset<Row> testDataset, MyFileWriter logWriter) {
        Model<?> trainedModel = trainModel(trainingDataset, logWriter);

        logWriter.println("Test data:");
        logWriter.printSchema(trainingDataset);
        logWriter.show(trainingDataset);

        logWriter.println("Test data count = " + testDataset.count());
        logWriter.println();

        Dataset<Row> predictions = trainedModel.transform(testDataset);

        logWriter.println("Result:");
        logWriter.show(predictions);

        MyEvaluator evaluator = new MyEvaluator();

        double accuracy = evaluator.evaluate(predictions);
        logWriter.println("accuracy= " + accuracy);

        ////////////////////PLOT/////////////////////
        CombinedPlot plot = new CombinedPlot("Plot", predictions);
        plot.pack();
        plot.setVisible(true);
        ////////////////////PLOT/////////////////////
    }
}