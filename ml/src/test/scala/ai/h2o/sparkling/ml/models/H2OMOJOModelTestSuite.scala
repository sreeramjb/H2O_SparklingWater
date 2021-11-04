/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.h2o.sparkling.ml.models

import ai.h2o.sparkling.ml.algos.{H2ODeepLearning, H2OGBM, H2OGLM}
import ai.h2o.sparkling.{SharedH2OTestContext, TestUtils}
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.sql.functions._
import _root_.hex.genmodel.easy.{EasyPredictModelWrapper, RowData}
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}

@RunWith(classOf[JUnitRunner])
class H2OMOJOModelTestSuite extends FunSuite with SharedH2OTestContext with Matchers {

  override def createSparkSession(): SparkSession = sparkSession("local[*]")

  import spark.implicits._

  test("H2OMOJOModel saved with scala 2.11 behaves the same way as H2OMOJOModel saved with scala 2.12") {
    val model11 = H2OMOJOModel.load("ml/src/test/resources/sw_mojo_scala_2.11_df_java_serde")
    val model12 = H2OMOJOModel.load("ml/src/test/resources/sw_mojo_scala_2.12_df_java_serde")
    compareMOJOModels(model11, model12)
  }

  test("H2OMOJOModel saved with current serialization behaves the same way as old models") {
    val model11 = H2OMOJOModel.load("ml/src/test/resources/sw_mojo_scala_2.11_df_java_serde")
    val path = "ml/build/mojo_model_serialization_compatibility"
    model11.write.overwrite.save(path)
    val currentModel = H2OMOJOModel.load(path)
    compareMOJOModels(model11, currentModel)
  }

  private def compareMOJOModels(first: H2OMOJOModel, second: H2OMOJOModel) = {
    TestUtils.assertDataFramesAreIdentical(first.transform(prostateDataFrame), second.transform(prostateDataFrame))
    TestUtils.assertDataFramesAreIdentical(first.getFeatureImportances(), second.getFeatureImportances())
    TestUtils.assertDataFramesAreIdentical(
      first.getCrossValidationMetricsSummary(),
      second.getCrossValidationMetricsSummary())
    first.getTrainingMetrics().-("ScoringTime") shouldEqual second.getTrainingMetrics().-("ScoringTime")
  }

  test("[MOJO] Export and Import - binomial model") {
    val (inputDf, model) = binomialModelFixture()
    testModelReload("binomial_model_import_export", inputDf, model)
  }

  test("[MOJO] Export and Import - multinomial model") {
    val (inputDf, model) = multinomialModelFixture()
    testModelReload("multinomial_model_import_export", inputDf, model)
  }

  test("[MOJO] Export and Import - regression model") {
    val (inputDf, model) = regressionModelFixture()
    testModelReload("regression_model_import_export", inputDf, model)
  }

  test("[MOJO] Export and import - deep learning model") {
    val (inputDf, model) = deepLearningModelFixture()
    testModelReload("deeplearning_model_import_export", inputDf, model)
  }

  test("[MOJO] Load from mojo file - binomial model") {
    val (inputDf, mojoModel) = savedBinomialModel()
    val (_, model) = binomialModelFixture()
    assertEqual(mojoModel, model, inputDf)
  }

  test("[MOJO] Load from mojo file - multinomial model") {
    val (inputDf, mojoModel) = savedMultinomialModel()
    val (_, model) = multinomialModelFixture()
    assertEqual(mojoModel, model, inputDf)
  }

  test("[MOJO] Load from mojo file - regression model") {
    val (inputDf, mojoModel) = savedRegressionModel()
    val (_, model) = regressionModelFixture()
    assertEqual(mojoModel, model, inputDf)
  }

  test("[MOJO] Load from mojo file - deep learning model") {
    val (inputDf, mojoModel) = savedDeepLearningModel()
    val (_, model) = deepLearningModelFixture()
    assertEqual(mojoModel, model, inputDf)
  }

  test("BooleanColumn as String for mojo predictions") {
    val mojo = H2OMOJOModel.createFromMojo(
      this.getClass.getClassLoader.getResourceAsStream("airlines_boolean.mojo"),
      "airlines_boolean.mojo")
    val data = Seq(
      Row(1987, 10, 3, "PS", 1451, "SAN", "SFO", 447, "true", "true"),
      Row(1987, 10, 4, "PS", 1451, "SAN", "SFO", 447, "false", "true"),
      Row(1987, 10, 6, "PS", 1451, "SAN", "SFO", 447, "true", "true"))

    val schema = StructType(
      List(
        StructField("Year", IntegerType, nullable = true),
        StructField("Month", IntegerType, nullable = true),
        StructField("DayOfWeek", IntegerType, nullable = true),
        StructField("UniqueCarrier", StringType, nullable = true),
        StructField("FlightNum", IntegerType, nullable = true),
        StructField("Origin", StringType, nullable = true),
        StructField("Dest", StringType, nullable = true),
        StructField("Distance", IntegerType, nullable = true),
        StructField("IsDepDelayed", StringType, nullable = true),
        StructField("IsArrDelayed", StringType, nullable = true)))

    val df = spark.createDataFrame(spark.sparkContext.parallelize(data), schema)

    mojo.transform(df).show(3, truncate = false)
  }

  test("BooleanColumn for mojo predictions") {
    val mojo = H2OMOJOModel.createFromMojo(
      this.getClass.getClassLoader.getResourceAsStream("airlines_boolean.mojo"),
      "airlines_boolean.mojo")
    val data = Seq(
      Row(1987, 10, 3, "PS", 1451, "SAN", "SFO", 447, true, true),
      Row(1987, 10, 4, "PS", 1451, "SAN", "SFO", 447, false, true),
      Row(1987, 10, 6, "PS", 1451, "SAN", "SFO", 447, true, true))

    val schema = StructType(
      List(
        StructField("Year", IntegerType, nullable = true),
        StructField("Month", IntegerType, nullable = true),
        StructField("DayOfWeek", IntegerType, nullable = true),
        StructField("UniqueCarrier", StringType, nullable = true),
        StructField("FlightNum", IntegerType, nullable = true),
        StructField("Origin", StringType, nullable = true),
        StructField("Dest", StringType, nullable = true),
        StructField("Distance", IntegerType, nullable = true),
        StructField("IsDepDelayed", BooleanType, nullable = true),
        StructField("IsArrDelayed", BooleanType, nullable = true)))

    val df = spark.createDataFrame(spark.sparkContext.parallelize(data), schema)

    mojo.transform(df).show(3, truncate = false)
  }

  def compareGbmOnTwoDatasets(reference: DataFrame, tested: DataFrame) = {
    val columnsForComparison =
      Seq($"prediction", $"detailed_prediction.probabilities.0", $"detailed_prediction.probabilities.1")

    val expectedModel = configureGBMForProstateDF().fit(reference)
    val expectedPredictionDF = expectedModel.transform(reference).select(columnsForComparison: _*)

    val model = configureGBMForProstateDF().fit(tested)
    val predictionDF = model.transform(tested).select(columnsForComparison: _*)

    TestUtils.assertDataFramesAreIdentical(expectedPredictionDF, predictionDF)
  }

  test("DataFrame contains structs") {
    val structuredDataFrame = prostateDataFrame.select(
      'ID,
      'CAPSULE,
      'AGE,
      struct('RACE, 'DPROS, struct('DCAPS, 'PSA) as "b") as "a",
      'VOL,
      'GLEASON)

    compareGbmOnTwoDatasets(prostateDataFrame, structuredDataFrame)
  }

  def prostateDataFrameWithDoubles =
    prostateDataFrame.select(
      'CAPSULE cast "string" as "CAPSULE",
      'AGE cast "double" as "AGE",
      'RACE cast "double" as "RACE",
      'DPROS cast "double" as "DPROS",
      'DCAPS cast "double" as "DCAPS",
      'PSA,
      'VOL,
      'GLEASON cast "double" as "GLEASON")

  test("DataFrame contains array") {
    val arrayDataFrame = prostateDataFrameWithDoubles.select(
      'CAPSULE,
      array('AGE, 'RACE, 'DPROS, 'DCAPS, 'PSA, 'VOL, 'GLEASON) as "features")

    compareGbmOnTwoDatasets(prostateDataFrameWithDoubles, arrayDataFrame)
  }

  test("DataFrame contains vector") {
    val assembler = new VectorAssembler()
      .setInputCols(Array("AGE", "RACE", "DPROS", "DCAPS", "PSA", "VOL", "GLEASON"))
      .setOutputCol("features")

    val vectorDataFrame = assembler.transform(prostateDataFrameWithDoubles).select("CAPSULE", "features")

    compareGbmOnTwoDatasets(prostateDataFrameWithDoubles, vectorDataFrame)
  }

  test("Testing dataset is missing one of feature columns") {
    val Array(trainingDF, rawTestingDF) = prostateDataFrame.randomSplit(Array(0.9, 0.1))
    val testingDF = rawTestingDF
      .drop("CAPSULE", "AGE") // Remove label and one of feature columns
      .cache()
    val gbm = configureGBMForProstateDF()

    val model = gbm.fit(trainingDF)
    val predictionDF = model.transform(testingDF)

    assertGBMPredictions(testingDF, predictionDF)
  }

  test("Testing dataset has an extra feature column") {
    val Array(trainingDF, rawTestingDF) = prostateDataFrame.randomSplit(Array(0.9, 0.1))
    val testingDF = rawTestingDF
      .drop("CAPSULE") // Remove label column
      .withColumn("EXTRA", rand()) // Add an extra column
      .cache()
    val gbm = configureGBMForProstateDF()

    val model = gbm.fit(trainingDF)
    val predictionDF = model.transform(testingDF)

    assertGBMPredictions(testingDF, predictionDF)
  }

  private def configureGBMForProstateDF(): H2OGBM = {
    new H2OGBM()
      .setSeed(42)
      .setDistribution("bernoulli")
      .setLabelCol("CAPSULE")
  }

  private def assertGBMPredictions(originalDF: DataFrame, predictionDF: DataFrame): Unit = {
    val records = predictionDF.select("detailed_prediction.probabilities").collect()
    val expectedNumberOfRecords = originalDF.count()
    records should have size expectedNumberOfRecords
    records.foreach { row =>
      val probabilities = row.getStruct(0)
      assert(probabilities.length == 2)
      probabilities.getDouble(0) should (be >= 0.0 and be <= 1.0)
      probabilities.getDouble(1) should (be >= 0.0 and be <= 1.0)
    }
  }

  private def testModelReload(name: String, df: DataFrame, model: H2OMOJOModel): Unit = {
    val predBeforeSave = model.transform(df)
    val modelFolder = tempFolder(name)
    model.write.overwrite.save(modelFolder)
    val reloadedModel = H2OMOJOModel.load(modelFolder)
    val predAfterReload = reloadedModel.transform(df)

    TestUtils.assertDataFramesAreIdentical(predBeforeSave, predAfterReload)
  }

  private def assertEqual(m1: H2OMOJOModel, m2: H2OMOJOModel, df: DataFrame): Unit = {
    val predMojo = m1.transform(df)
    val predModel = m2.transform(df)

    TestUtils.assertDataFramesAreIdentical(predMojo, predModel)
  }

  private def tempFolder(prefix: String) = {
    val path = java.nio.file.Files.createTempDirectory(prefix)
    path.toFile.deleteOnExit()
    path.toString
  }

  private lazy val irisDataFrame = {
    spark.read.option("header", "true").option("inferSchema", "true").csv("examples/smalldata/iris/iris_wheader.csv")
  }

  private lazy val prostateDataFrame = {
    spark.read.option("header", "true").option("inferSchema", "true").csv("examples/smalldata/prostate/prostate.csv")
  }

  private def binomialModelFixture() = {
    val inputDf = prostateDataFrame
    val gbm = new H2OGBM()
      .setNtrees(2)
      .setSeed(42)
      .setDistribution("bernoulli")
      .setLabelCol("capsule")

    (inputDf, gbm.fit(inputDf))
  }

  private def multinomialModelFixture() = {
    val inputDf = irisDataFrame
    val gbm = new H2OGBM()
      .setNtrees(2)
      .setSeed(42)
      .setDistribution("multinomial")
      .setLabelCol("class")

    (inputDf, gbm.fit(inputDf))
  }

  private def regressionModelFixture() = {
    val inputDf = prostateDataFrame
    val gbm = new H2OGBM()
      .setNtrees(2)
      .setSeed(42)
      .setLabelCol("capsule")

    (inputDf, gbm.fit(inputDf))
  }

  private def deepLearningModelFixture() = {
    val inputDf = prostateDataFrame
    val dl = new H2ODeepLearning()
      .setSeed(42)
      .setReproducible(true)
      .setLabelCol("CAPSULE")

    (inputDf, dl.fit(inputDf))
  }

  private def savedBinomialModel() = {
    val mojo = H2OMOJOModel.createFromMojo(
      this.getClass.getClassLoader.getResourceAsStream("binom_model_prostate.mojo"),
      "binom_model_prostate.mojo")
    (prostateDataFrame, mojo)
  }

  private def savedRegressionModel() = {
    val mojo = H2OMOJOModel.createFromMojo(
      this.getClass.getClassLoader.getResourceAsStream("regre_model_prostate.mojo"),
      "regre_model_prostate.mojo")
    (prostateDataFrame, mojo)
  }

  private def savedMultinomialModel() = {
    val mojo = H2OMOJOModel.createFromMojo(
      this.getClass.getClassLoader.getResourceAsStream("multi_model_iris.mojo"),
      "multi_model_iris.mojo")
    (irisDataFrame, mojo)
  }

  private def savedDeepLearningModel() = {
    val mojo = H2OMOJOModel.createFromMojo(
      this.getClass.getClassLoader.getResourceAsStream("deep_learning_prostate.mojo"),
      "deep_learning_prostate.mojo")
    (prostateDataFrame, mojo)
  }

  test("getCurrentMetrics when trained with just training frame") {
    val estimator = new H2OGLM()
      .setSeed(1)
      .setFeaturesCols("CAPSULE", "RACE", "DPROS", "DCAPS", "PSA", "VOL", "GLEASON")
      .setLabelCol("AGE")
    val model = estimator.fit(prostateDataFrame)

    assert(model.getTrainingMetrics().nonEmpty)
    assert(model.getValidationMetrics().isEmpty)
    assert(model.getCrossValidationMetrics().isEmpty)
    assert(model.getCurrentMetrics().nonEmpty)
    assert(model.getTrainingMetrics() == model.getCurrentMetrics())
  }

  test("getCurrentMetrics when trained with validation frame") {
    val estimator = new H2OGLM()
      .setSeed(1)
      .setSplitRatio(0.8)
      .setFeaturesCols("CAPSULE", "RACE", "DPROS", "DCAPS", "PSA", "VOL", "GLEASON")
      .setLabelCol("AGE")
    val model = estimator.fit(prostateDataFrame)

    assert(model.getTrainingMetrics().nonEmpty)
    assert(model.getValidationMetrics().nonEmpty)
    assert(model.getCrossValidationMetrics().isEmpty)
    assert(model.getCurrentMetrics().nonEmpty)
    assert(model.getValidationMetrics() == model.getCurrentMetrics())
  }

  test("getCurrentMetrics when trained with cross-validation") {
    val estimator = new H2OGLM()
      .setSeed(1)
      .setNfolds(3)
      .setFeaturesCols("CAPSULE", "RACE", "DPROS", "DCAPS", "PSA", "VOL", "GLEASON")
      .setLabelCol("AGE")
    val model = estimator.fit(prostateDataFrame)

    assert(model.getTrainingMetrics().nonEmpty)
    assert(model.getValidationMetrics().isEmpty)
    assert(model.getCrossValidationMetrics().nonEmpty)
    assert(model.getCurrentMetrics().nonEmpty)
    assert(model.getCrossValidationMetrics() == model.getCurrentMetrics())
  }

  test("getCurrentMetrics when trained with validation frame and cross-validation") {
    val estimator = new H2OGLM()
      .setSeed(1)
      .setNfolds(3)
      .setSplitRatio(0.8)
      .setFeaturesCols("CAPSULE", "RACE", "DPROS", "DCAPS", "PSA", "VOL", "GLEASON")
      .setLabelCol("AGE")
    val model = estimator.fit(prostateDataFrame)

    assert(model.getTrainingMetrics().nonEmpty)
    assert(model.getValidationMetrics().nonEmpty)
    assert(model.getCrossValidationMetrics().nonEmpty)
    assert(model.getCurrentMetrics().nonEmpty)
    assert(model.getCrossValidationMetrics() == model.getCurrentMetrics())
  }

  test("Exposed hex mojo model gives the same prediction as SW model") {
    val gbm = configureGBMForProstateDF()

    val model = gbm.fit(prostateDataFrame)
    val h2o3model = model.unwrapMojoModel()
    val config = new EasyPredictModelWrapper.Config()
    config.setModel(h2o3model)
    val wrapper = new EasyPredictModelWrapper(config)

    val rowWithPrediction = model.transform(prostateDataFrame).first()
    val rowData = new RowData()
    rowData.put("ID", rowWithPrediction.get(0).toString())
    rowData.put("AGE", rowWithPrediction.get(2).toString())
    rowData.put("RACE", rowWithPrediction.get(3).toString())
    rowData.put("DPROS", rowWithPrediction.get(4).toString())
    rowData.put("DCAPS", rowWithPrediction.get(5).toString())
    rowData.put("PSA", rowWithPrediction.get(6).toString())
    rowData.put("VOL", rowWithPrediction.get(7).toString())
    rowData.put("GLEASON", rowWithPrediction.get(8).toString())

    val prediction = wrapper.predictBinomial(rowData)
    prediction.classProbabilities(0) shouldEqual rowWithPrediction.getStruct(9).getStruct(1).get(0)
    prediction.classProbabilities(1) shouldEqual rowWithPrediction.getStruct(9).getStruct(1).get(1)
  }

  test("getCrossValidationMetricsSummary returns a non-empty data frame when cross validation enabled") {
    val estimator = new H2OGLM()
      .setSeed(1)
      .setNfolds(3)
      .setSplitRatio(0.8)
      .setFeaturesCols("CAPSULE", "RACE", "DPROS", "DCAPS", "PSA", "VOL", "GLEASON")
      .setLabelCol("AGE")
    val model = estimator.fit(prostateDataFrame)

    val crossValidationModelSummary = model.getCrossValidationMetricsSummary()

    val expectedColumns = Seq("metric", "mean", "sd", "cv_1_valid", "cv_2_valid", "cv_3_valid")
    crossValidationModelSummary.columns.toList shouldEqual expectedColumns
    crossValidationModelSummary.count() shouldBe >(0L)

    val row = crossValidationModelSummary.first()
    for (columnId <- 1 to 5) {
      row.getFloat(columnId) shouldBe >(0.0f)
    }
  }

  {
    def numberOfFolds = 3
    lazy val gbm = configureGBMForProstateDF()
      .setNfolds(numberOfFolds)
      .setKeepCrossValidationModels(true)
    def trainedModel = gbm.fit(prostateDataFrame)

    lazy val model = {
      val path = "ml/build/model_with_cv_models"
      trainedModel.write.overwrite().save(path)
      H2OGBMMOJOModel.load(path)
    }

    test("Cross validation models are able to score") {
      val cvModels = model.getCrossValidationModels()
      cvModels.length shouldEqual numberOfFolds

      val result = model.transform(prostateDataFrame)
      val cvResult = cvModels(0).transform(prostateDataFrame)

      result.schema shouldEqual cvResult.schema
      result.count() shouldEqual cvResult.count()
      cvResult.show(truncate = false)
    }

    test("Cross validation models can provide training and validation metrics") {
      val cvModels = model.getCrossValidationModels()
      cvModels.length shouldEqual numberOfFolds
      val firstCVModel = cvModels(0)

      firstCVModel.getTrainingMetrics() should not be (null)
      firstCVModel.getTrainingMetrics() should not be (Map.empty)
      firstCVModel.getValidationMetrics() should not be (null)
      firstCVModel.getValidationMetrics() should not be (Map.empty)
      firstCVModel.getCrossValidationMetrics() should be(Map.empty)

      firstCVModel.getTrainingMetrics().keySet shouldEqual model.getTrainingMetrics().keySet
      for ((k, v) <- firstCVModel.getTrainingMetrics()) {
        assert(v != Double.NaN && v != 0, s"The training metric $k has value '$v'")
      }

      firstCVModel.getValidationMetrics().keySet shouldEqual model.getCrossValidationMetrics().keySet
      for ((k, v) <- firstCVModel.getValidationMetrics()) {
        assert(v != Double.NaN && v != 0, s"The validation metric $k has value '$v'")
      }
    }

    test("Cross validation models can provide scoring history") {
      val cvModels = model.getCrossValidationModels()
      cvModels.length shouldEqual numberOfFolds
      val cvScoringHistory = cvModels(0).getScoringHistory()

      cvScoringHistory.columns shouldEqual Array(
        "Timestamp",
        "Duration",
        "Number of Trees",
        "Training RMSE",
        "Training LogLoss",
        "Training AUC",
        "Training pr_auc",
        "Training Lift",
        "Training Classification Error",
        "Validation RMSE",
        "Validation LogLoss",
        "Validation AUC",
        "Validation pr_auc",
        "Validation Lift",
        "Validation Classification Error")
      cvScoringHistory.count() shouldBe >(0L)
    }

    test("Cross validation models can provide feature importances") {
      val cvModels = model.getCrossValidationModels()
      cvModels.length shouldEqual numberOfFolds
      val featureImportances = cvModels(0).getFeatureImportances()

      featureImportances.columns shouldEqual Array("Variable", "Relative Importance", "Scaled Importance", "Percentage")
      featureImportances.count() shouldBe >(0L)
      featureImportances.count() shouldEqual gbm.getFeaturesCols().length
    }

    test("Cross validation models are null when simple validation is used") {
      val gbm = configureGBMForProstateDF().setSplitRatio(0.8)
      val model = gbm.fit(prostateDataFrame)

      val cvModels = model.getCrossValidationModels()

      cvModels should be(null)
    }

    test("Cross validation models are null if generating of them is disabled") {
      val gbm = configureGBMForProstateDF().setKeepCrossValidationModels(true)
      val model = gbm.fit(prostateDataFrame)

      val cvModels = model.getCrossValidationModels()

      cvModels should be(null)
    }
  }
}
