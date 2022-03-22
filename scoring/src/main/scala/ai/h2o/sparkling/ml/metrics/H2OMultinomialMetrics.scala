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

import ai.h2o.sparkling.ml.metrics.H2OBinomialMetrics.getMetricGson
import hex.ModelMetricsMultinomial.IndependentMetricBuilderMultinomial
import hex.MultinomialAucType
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.DataFrame

@MetricsDescription(description = "The class makes available all metrics that shared across all algorithms supporting multinomial classification.")
class H2OMultinomialMetrics(override val uid: String) extends H2OMultinomialMetricsBase(uid) {

  def this() = this(Identifiable.randomUID("H2OBinomialMetrics"))
}

object H2OMultinomialMetrics {
  def calculate(
      dataFrame: DataFrame,
      domain: Array[String],
      predictionProbabilitiesCol: String = "detailed_prediction.probabilities",
      labelCol: String = "label",
      weightColOption: Option[String] = None,
      offsetColOption: Option[String] = None,
      priorDistributionOption: Option[Array[Double]] = None,
      aucType: String = "AUTO"): H2OMultinomialMetrics = {

    val aucTypeEnum = MultinomialAucType.valueOf(aucType)
    val nclasses = domain.length
    val priorDistribution = priorDistributionOption match {
      case Some(x) => x
      case None => null
    }
    val getMetricBuilder =
      () => new IndependentMetricBuilderMultinomial(nclasses, domain, aucTypeEnum, priorDistribution)

    val gson = getMetricGson(
      getMetricBuilder,
      dataFrame,
      predictionProbabilitiesCol,
      labelCol,
      offsetColOption,
      weightColOption,
      domain)
    val result = new H2OMultinomialMetrics()
    result.setMetrics(gson, "H2OMultinomialMetrics.calculate")
    result
  }

  def calculate(
      dataFrame: DataFrame,
      domain: Array[String],
      predictionProbabilitiesCol: String,
      labelCol: String,
      weightCol: String,
      offsetCol: String,
      priorDistribution: Array[Double],
      aucType: String): H2OMultinomialMetrics = {
    calculate(
      dataFrame,
      domain,
      predictionProbabilitiesCol,
      labelCol,
      Option(weightCol),
      Option(offsetCol),
      Option(priorDistribution),
      aucType)
  }
}