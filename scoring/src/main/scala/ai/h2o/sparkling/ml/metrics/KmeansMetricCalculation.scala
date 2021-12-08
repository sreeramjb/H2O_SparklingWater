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

package ai.h2o.sparkling.ml.metrics

import ai.h2o.sparkling.ml.models.H2OKMeansMOJOModel
import hex.ModelMetrics.IndependentMetricBuilder
import hex.ModelMetricsClustering.IndependentMetricBuilderClustering
import hex.genmodel.GenModel
import hex.genmodel.algos.kmeans.KMeansMojoModel
import hex.genmodel.easy.{EasyPredictModelWrapper, RowData}

trait KmeansMetricCalculation {
  self: H2OKMeansMOJOModel =>

  override private[sparkling] def makeMetricBuilder(wrapper: EasyPredictModelWrapper): IndependentMetricBuilder[_] = {
    val model = wrapper.m.asInstanceOf[KMeansMojoModel]
    new IndependentMetricBuilderClustering(model.nfeatures(), getK(), model._centers, model._modes)
  }

  override private[sparkling] def extractActualValues(
      rowData: RowData,
      wrapper: EasyPredictModelWrapper): Array[Double] = {
    val model = wrapper.m.asInstanceOf[KMeansMojoModel]
    val rawData = new Array[Double](wrapper.m.nfeatures())
    wrapper.fillRawData(rowData, rawData)
    if (model._standardize) {
      GenModel.Kmeans_preprocessData(rawData, model._means, model._mults, model._modes)
    }
    rawData
  }
}