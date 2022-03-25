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

import hex.ModelMetricsMultinomial.IndependentMetricBuilderMultinomial
import hex.MultinomialAucType
import org.apache.spark.{ExposeUtils, ml, mllib}
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{DataFrame, Row}
import org.apache.spark.sql.types.{ArrayType, DataType, DoubleType, FloatType, StringType, StructType}

@MetricsDescription(
  description =
    "The class makes available all metrics that shared across all algorithms supporting multinomial classification.")
class H2OMultinomialMetrics(override val uid: String) extends H2OMultinomialMetricsBase(uid) {

  def this() = this(Identifiable.randomUID("H2OBinomialMetrics"))
}

object H2OMultinomialMetrics extends MetricCalculation {
  def calculate(
      dataFrame: DataFrame,
      domain: Array[String],
      predictionProbabilitiesCol: String = "detailed_prediction",
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
    val castedLabelDF = dataFrame.withColumn(labelCol, col(labelCol) cast StringType)

    val gson = getMetricGson(
      getMetricBuilder,
      castedLabelDF,
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

  override protected def getPredictionValues(dataType: DataType, domain: Array[String], row: Row): Array[Double] = {
    dataType match {
      case StructType(fields)
          if fields(0).dataType == StringType && fields(1).dataType.isInstanceOf[StructType] &&
            fields(1).dataType.asInstanceOf[StructType].fields.forall(_.dataType == DoubleType) =>
        val predictionStructure = row.getStruct(0)
        val prediction = predictionStructure.getString(0)
        val index = domain.indexOf(prediction).toDouble
        val probabilities = predictionStructure.getStruct(1)

        Array(index) ++ probabilities.toSeq.map(_.asInstanceOf[Double])
      case ArrayType(DoubleType, _) => row.getSeq[Double](0).toArray
      case ArrayType(FloatType, _) => row.getSeq[Float](0).map(_.toDouble).toArray
      case v if ExposeUtils.isMLVectorUDT(v) => row.getAs[ml.linalg.Vector](0).toDense.values
      case _: mllib.linalg.VectorUDT => row.getAs[mllib.linalg.Vector](0).toDense.values
    }
  }

  override protected def getActualValue(dataType: DataType, domain: Array[String], row: Row): Double = {
    val label = row.getString(1)
    domain.indexOf(label).toDouble
  }
}
