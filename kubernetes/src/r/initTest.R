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
library(sparklyr)
library(rsparkling)
library(testthat)
options(sparklyr.console.log = TRUE)
master <- Sys.getenv("KUBERNETES_MASTER")
registry <- Sys.getenv("REGISTRY")
version <- Sys.getenv("SW_VERSION")
sparkHome <- Sys.getenv("SPARK_HOME")
extraOptions <- Sys.getenv("EXTRA_OPTIONS")
sparkVersion <- Sys.getenv("SPARK_VERSION")
extraOptionsParsed <- list()
if (extraOptions != "") {
  options <- unlist(strsplit(extraOptions," "))
  for (pair in options) {
    parsedPair <- unlist(strsplit(pair, "="))
    extraOptionsParsed[parsedPair[1]] <- parsedPair[2]
  }
}
extraOptionsParsed["spark.kubernetes.file.upload.path"] <-"file:///tmp"

optionName <- "spark.ext.h2o.backend.cluster.mode"
if (optionName %in% names(extraOptionsParsed) && extraOptionsParsed[optionName] == "external") {
  numExecutors <- 1
} else {
  numExecutors <- 2
}

config <- spark_config_kubernetes(master = master,
                                 image = paste0(registry, "sparkling-water:r-", version),
                                 account = "default",
                                 driver = "sparkling-water-app",
                                 version = sparkVersion,
                                 executors = numExecutors,
                                 conf = extraOptionsParsed,
                                 ports = c(8880, 8881, 4040, 54321))
config["spark.home"] <- sparkHome
config["sparklyr.connect.enablehivesupport"] <- FALSE
sc <- spark_connect(config = config, spark_home = sparkHome)
hc <- H2OContext.getOrCreate()

if (length(invoke(hc$jhc, "getH2ONodes")) != 2) {
  print("ASSERTION ERROR")
}

# Test conversions
df <- as.data.frame(t(c(1, 2, 3, 4, "A")))
sdf <- copy_to(sc, df, overwrite = TRUE)
hc <- H2OContext.getOrCreate()
hf <- hc$asH2OFrame(sdf)
sdf2 <- hc$asSparkFrame(hf)

if (sdf_nrow(sdf2) != nrow(hf)) {
  print("ASSERTION ERROR")
}

if (sdf_ncol(sdf2) != ncol(hf)) {
  print("ASSERTION ERROR")
}

if (!all(colnames(sdf2)==colnames(hf))) {
  print("ASSERTION ERROR")
}

spark_disconnect(sc)
