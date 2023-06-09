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

source(file.path("R", "H2OMOJOModelBase.R"))

H2OMOJOPipelineModel.createFromMojo <- function(pathToMojo, settings = H2OMOJOSettings.default()) {
  sc <- spark_connection_find()[[1]]
  jmojo <- invoke_static(sc, "ai.h2o.sparkling.ml.models.H2OMOJOPipelineModel", "createFromMojo", pathToMojo, settings$toJavaObject())
  H2OMOJOPipelineModel(jmojo)
}

#' @export H2OMOJOPipelineModel
H2OMOJOPipelineModel <- setRefClass("H2OMOJOPipelineModel", contains = ("H2OAlgorithmMOJOModelBase"), methods = list(
  getWithInternalContributions = function() {
    invoke(.self$jmojo, "getWithInternalContributions")
  },
  getWithPredictionInterval = function() {
    invoke(.self$jmojo, "getWithPredictionInterval")
  },
  getScoringBulkSize = function() {
    invoke(.self$jmojo, "getScoringBulkSize")
  }
))
