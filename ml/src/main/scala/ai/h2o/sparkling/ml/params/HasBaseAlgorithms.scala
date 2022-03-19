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

import ai.h2o.sparkling.H2OFrame
import ai.h2o.sparkling.ml.algos.H2OAlgorithm
import hex.Model

trait HasBaseAlgorithms extends H2OAlgoParamsBase {

  private val base_algorithms = new NullableAlgoArrayParam(this, "baseAlgorithms", "An array of base algorithms")

  setDefault(base_algorithms -> null)

  def getBaseAlgorithms(): Array[H2OAlgorithm[_ <: Model.Parameters]] = $(base_algorithms)

  def setBaseAlgorithms(value: Array[H2OAlgorithm[_ <: Model.Parameters]]): this.type = set(base_algorithms, value)

  private[sparkling] def getBaseAlgorithmsParam(trainingFrame: H2OFrame): Map[String, Any] = {
    // the base_algorithms parameter isn't used by H2O backend
    Map.empty
  }
}
