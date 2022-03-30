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

from pyspark.ml.param import *
from ai.h2o.sparkling.ml.metrics.H2OMultinomialMetricsBase import H2OMultinomialMetricsBase
from ai.h2o.sparkling.Initializer import Initializer
from pyspark.ml.util import _jvm


class H2OMultinomialMetrics(H2OMultinomialMetricsBase):

    @staticmethod
    def calculate(dataFrame,
                  domain,
                  predictionCol = "detailed_prediction",
                  labelCol = "label",
                  weightCol = None,
                  aucType = "AUTO"):
        # We need to make sure that Sparkling Water classes are available on the Spark driver and executor paths
        Initializer.load_sparkling_jar()
        javaMetrics = _jvm().ai.h2o.sparkling.ml.metrics.H2OMultinomialMetrics.calculate(dataFrame,
                                                                                         domain,
                                                                                         predictionCol,
                                                                                         labelCol,
                                                                                         weightCol,
                                                                                         aucType)
        return H2OMultinomialMetrics(javaMetrics)
