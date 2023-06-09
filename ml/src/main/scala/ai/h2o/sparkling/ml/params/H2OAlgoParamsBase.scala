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

package ai.h2o.sparkling.ml.params

import ai.h2o.sparkling.backend.utils.H2OFrameLifecycle
import ai.h2o.sparkling.{H2OContext, H2OFrame}
import ai.h2o.sparkling.utils.SparkSessionUtils
import org.apache.spark.ml.linalg.{DenseMatrix, DenseVector}
import org.apache.spark.sql.{DataFrame, SparkSession}

trait H2OAlgoParamsBase extends ParameterConstructorMethods with H2OFrameLifecycle {
  private[sparkling] def getH2OAlgorithmParams(trainingFrame: H2OFrame): Map[String, Any] = Map.empty

  private[sparkling] def getSWtoH2OParamNameMap(): Map[String, String] = Map.empty

  private def convertWithH2OContext[TInput <: AnyRef, TOutput <: AnyRef](input: TInput)(
      body: (SparkSession, H2OContext) => TOutput): TOutput = {
    if (input == null) {
      null.asInstanceOf[TOutput]
    } else {
      val spark = SparkSessionUtils.active
      val hc = H2OContext.ensure(
        s"H2OContext needs to be created in order to train the ${this.getClass.getSimpleName} model. " +
          "Please create one as H2OContext.getOrCreate().")
      body(spark, hc)
    }
  }

  protected def convert2dArrayToH2OFrame(input: Array[Array[Double]]): String = {
    convertWithH2OContext(input) { (spark, hc) =>
      import spark.implicits._
      val df = spark.sparkContext.parallelize(input).toDF()
      val frame = hc.asH2OFrame(df)
      registerH2OFrameForDeletion(frame)
      frame.frameId
    }
  }

  protected def convertVectorArrayToH2OFrameKeyArray(vectors: Array[DenseVector]): Array[String] = {
    convertWithH2OContext(vectors) { (spark, hc) =>
      import spark.implicits._
      vectors.map { vector =>
        val df = spark.sparkContext.parallelize(vector.values).toDF()
        val frame = hc.asH2OFrame(df)
        registerH2OFrameForDeletion(frame)
        frame.frameId
      }
    }
  }

  protected def convertMatrixToH2OFrameKeyArray(matrix: Array[DenseMatrix]): Array[String] = {
    convertWithH2OContext(matrix) { (spark, hc) =>
      import spark.implicits._
      matrix.map { matrix =>
        val rows = matrix.rowIter.map(_.toArray).toArray
        val df = spark.sparkContext.parallelize(rows).toDF()
        val frame = hc.asH2OFrame(df)
        registerH2OFrameForDeletion(frame)
        frame.frameId
      }
    }
  }

  protected def convertDataFrameToH2OFrameKey(dataFrame: DataFrame): String = {
    if (dataFrame == null) {
      null
    } else {
      val hc = H2OContext.ensure(
        s"H2OContext needs to be created in order to train the ${this.getClass.getSimpleName} model. " +
          "Please create one as H2OContext.getOrCreate().")
      val frame = hc.asH2OFrame(dataFrame)
      registerH2OFrameForDeletion(frame)
      frame.frameId
    }
  }

  implicit class ParametersExtraMethods(val parameters: Map[String, Any]) {
    def +++(other: Map[String, Any]): Map[String, Any] = {
      val keys = parameters.keySet ++ other.keySet
      keys.map { key =>
        val first = parameters.getOrElse(key, null)
        val second = other.getOrElse(key, null)
        val value = (first, second) match {
          case (f, null) => f
          case (null, s) => s
          case (f: Array[_], s: Array[_]) => (f ++ s).distinct
          case _ => throw new IllegalStateException("Merge operation on non-array types is not supported.")
        }
        key -> value
      }.toMap
    }
  }
}
