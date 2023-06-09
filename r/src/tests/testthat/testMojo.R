#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

context("Test MOJO predictions")

config <- spark_config()
config <- c(config, list(
  "spark.hadoop.yarn.timeline-service.enabled" = "false",
  "spark.ext.h2o.external.cluster.size" = "1",
  "spark.ext.h2o.backend.cluster.mode" = Sys.getenv("spark.ext.h2o.backend.cluster.mode"),
  "sparklyr.connect.enablehivesupport" = FALSE,
  "sparklyr.gateway.connect.timeout" = 240,
  "sparklyr.gateway.start.timeout" = 240,
  "sparklyr.backend.timeout" = 240,
  "sparklyr.log.console" = TRUE,
  "spark.ext.h2o.external.start.mode" = "auto",
  "spark.ext.h2o.external.disable.version.check" = "true",
  "sparklyr.gateway.port" = 55555,
  "sparklyr.connect.timeout" = 60 * 5,
  "spark.master" = "local[*]"
))

for (i in 1:4) {
  tryCatch(
    {
    sc <- spark_connect(master = "local[*]", config = config)
  }, error = function(e) { }
  )
}

locate <- function(fileName) {
  normalizePath(file.path("../../../../../examples/", fileName))
}

test_that("test MOJO predictions", {
  path <- paste0("file://", locate("smalldata/prostate/prostate.csv"))
  dataset <- spark_read_csv(sc, path = path, infer_schema = TRUE, header = TRUE)
  # Try loading the Mojo and prediction on it without starting H2O Context
  mojo <- H2OMOJOModel.createFromMojo(paste0("file://", normalizePath("../../../../../ml/src/test/resources/binom_model_prostate.mojo")))
  sdf <- mojo$transform(dataset)
  data <- dplyr::collect(mojo$transform(sdf))
  expect_equal(colnames(data), c("ID", "CAPSULE", "AGE", "RACE", "DPROS", "DCAPS", "PSA", "VOL", "GLEASON", "detailed_prediction", "prediction"))
})

test_that("test getDomainValues", {
  model <- H2OMOJOModel.createFromMojo(paste0("file://", normalizePath("../../../../../ml/src/test/resources/binom_model_prostate.mojo")))
  domainValues <- model$getDomainValues()
  expect_true(is.null(domainValues[["DPROS"]]))
  expect_true(is.null(domainValues[["DCAPS"]]))
  expect_true(is.null(domainValues[["VOL"]]))
  expect_true(is.null(domainValues[["AGE"]]))
  expect_true(is.null(domainValues[["PSA"]]))
  expect_equal(domainValues[["capsule"]][[1]], "0")
  expect_equal(domainValues[["capsule"]][[2]], "1")
  expect_true(is.null(domainValues[["RACE"]]))
  expect_true(is.null(domainValues[["ID"]]))
})

test_that("test getScoringHistory", {
  model <- H2OMOJOModel.createFromMojo(paste0("file://", normalizePath("../../../../../ml/src/test/resources/binom_model_prostate.mojo")))
  scoringHistory <- model$getScoringHistory()

  numberOfRecordsFrame <- dplyr::tally(scoringHistory)
  count <- as.double(dplyr::collect(numberOfRecordsFrame)[[1]])

  expect_true(count > 0)
})

test_that("test getFeatureImportances", {
  model <- H2OMOJOModel.createFromMojo(paste0("file://", normalizePath("../../../../../ml/src/test/resources/binom_model_prostate.mojo")))
  featureImportances <- model$getFeatureImportances()
  expectedCount <- length(model$getFeaturesCols())

  numberOfRecordsFrame <- dplyr::tally(featureImportances)
  count <- as.double(dplyr::collect(numberOfRecordsFrame)[[1]])

  expect_equal(count, expectedCount)
})

test_that("test getCrossValidationMetricsSummary", {
  model <- H2OMOJOModel.createFromMojo(paste0("file://", normalizePath("../../../../../ml/src/test/resources/gbm_cv.mojo")))
  summary <- model$getCrossValidationMetricsSummary()

  numberOfRecordsFrame <- dplyr::tally(summary)
  count <- as.double(dplyr::collect(numberOfRecordsFrame)[[1]])

  expect_true(count > 0)
})

test_that("test training params", {
  model <- H2OMOJOModel.createFromMojo(paste0("file://", normalizePath("../../../../../ml/src/test/resources/binom_model_prostate.mojo")))
  params <- model$getTrainingParams()
  expect_equal(params[["distribution"]], "bernoulli")
  expect_equal(params[["ntrees"]], "2")
  expect_equal(length(params), 44)
})

test_that("test model category", {
  model <- H2OMOJOModel.createFromMojo(paste0("file://", normalizePath("../../../../../ml/src/test/resources/binom_model_prostate.mojo")))
  category <- model$getModelCategory()
  expect_equal(category, "Binomial")
})

test_that("test training metrics", {
  model <- H2OMOJOModel.createFromMojo(paste0("file://", normalizePath("../../../../../ml/src/test/resources/binom_model_prostate.mojo")))
  metrics <- model$getTrainingMetrics()
  expect_equal(as.character(metrics[["AUC"]]), "0.896878869021911")
  expect_equal(length(metrics), 10)
})

test_that("test training metrics object", {
  model <- H2OMOJOModel.createFromMojo(paste0("file://", normalizePath("../../../../../ml/src/test/resources/binom_model_prostate.mojo")))
  metrics <- model$getTrainingMetricsObject()
  aucValue <- metrics$getAUC()
  scoringTime <- metrics$getScoringTime()

  thresholdsAndScores <- metrics$getThresholdsAndMetricScores()
  thresholdsAndScoresFrame <- dplyr::tally(thresholdsAndScores)
  thresholdsAndScoresCount <- as.double(dplyr::collect(thresholdsAndScoresFrame)[[1]])

  gainsLiftTable <- metrics$getGainsLiftTable()
  gainsLiftTableFrame <- dplyr::tally(gainsLiftTable)
  gainsLiftTableCount <- as.double(dplyr::collect(gainsLiftTableFrame)[[1]])

  expect_equal(as.character(aucValue), "0.896878869021911")
  expect_true(scoringTime > 0)
  expect_true(thresholdsAndScoresCount > 0)
  expect_true(gainsLiftTableCount > 0)
})

test_that("test null cross validation metrics object", {
  model <- H2OMOJOModel.createFromMojo(paste0("file://", normalizePath("../../../../../ml/src/test/resources/binom_model_prostate.mojo")))
  cvObject <- model$getCrossValidationMetricsObject()
  expect_true(is.null(cvObject))
})

test_that("test current metrics", {
  model <- H2OMOJOModel.createFromMojo(paste0("file://", normalizePath("../../../../../ml/src/test/resources/binom_model_prostate.mojo")))
  metrics <- model$getCurrentMetrics()
  expect_equal(metrics, model$getTrainingMetrics())
})

test_that("test MOJO predictions on unseen categoricals", {
  path <- paste0("file://", normalizePath("../../../../../ml/src/test/resources/deep_learning_airlines_categoricals.zip"))
  settings <- H2OMOJOSettings(convertUnknownCategoricalLevelsToNa = TRUE)
  mojo <- H2OMOJOModel.createFromMojo(path, settings)

  df <- as.data.frame(t(c(5.1, 3.5, 1.4, 0.2, "Missing_categorical")))
  colnames(df) <- c("sepal_len", "sepal_wid", "petal_len", "petal_wid", "class")
  sdf <- copy_to(sc, df, overwrite = TRUE)

  data <- dplyr::collect(mojo$transform(sdf))

  expect_equal(as.character(dplyr::select(data, class)), "Missing_categorical")
  expect_equal(as.double(dplyr::select(data, petal_len)), 1.4)
  expect_equal(as.double(dplyr::select(data, petal_wid)), 0.2)
  expect_equal(as.double(dplyr::select(data, sepal_len)), 5.1)
  expect_equal(as.double(dplyr::select(data, sepal_wid)), 3.5)
  expect_equal(as.double(dplyr::select(data, prediction)), 5.240174068202646)
})

test_that("test start time", {
  model <- H2OMOJOModel.createFromMojo(paste0("file://", normalizePath("../../../../../ml/src/test/resources/multi_model_iris.mojo")))
  startTime <- model$getStartTime()
  expect_equal(startTime, 1631392711317)
})

test_that("test end time", {
  model <- H2OMOJOModel.createFromMojo(paste0("file://", normalizePath("../../../../../ml/src/test/resources/multi_model_iris.mojo")))
  endTime <- model$getEndTime()
  expect_equal(endTime, 1631392711360)
})

test_that("test run time", {
  model <- H2OMOJOModel.createFromMojo(paste0("file://", normalizePath("../../../../../ml/src/test/resources/multi_model_iris.mojo")))
  runTime <- model$getRunTime()
  expect_equal(runTime, 43)
})

test_that("test default threshold", {
  model <- H2OMOJOModel.createFromMojo(paste0("file://", normalizePath("../../../../../ml/src/test/resources/binom_model_prostate.mojo")))
  threshold <- model$getDefaultThreshold()
  expect_equal(threshold, 0.40858428648438255)
})

test_that("test cross validation models scoring history", {
  model <- H2OMOJOModel.createFromMojo(paste0("file://", normalizePath("../../../../../ml/src/test/resources/gbm_cv.mojo")))
  history <- model$getCrossValidationModelsScoringHistory()
  expect_equal(length(history), 3)
})

test_that("test unavailable cross validation models scoring history", {
  model <- H2OMOJOModel.createFromMojo(paste0("file://", normalizePath("../../../../../ml/src/test/resources/deep_learning_prostate.mojo")))
  history <- model$getCrossValidationModelsScoringHistory()
  expect_equal(length(history), 0)
})

test_that("test getModelSummary", {
  model <- H2OMOJOModel.createFromMojo(paste0("file://", normalizePath("../../../../../ml/src/test/resources/deep_learning_prostate.mojo")))
  modelSummary <- model$getModelSummary()

  numberOfRecordsFrame <- dplyr::tally(modelSummary)
  count <- as.double(dplyr::collect(numberOfRecordsFrame)[[1]])

  expect_equal(count, 4)
})

spark_disconnect(sc)
