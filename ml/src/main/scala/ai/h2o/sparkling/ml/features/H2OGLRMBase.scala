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

package ai.h2o.sparkling.ml.features

import ai.h2o.sparkling.ml.models.H2OGLRMMOJOModel
import ai.h2o.sparkling.ml.params.{H2OGLRMExtraParams, HasInputCols}
import hex.Model
import org.apache.spark.sql.Dataset

import scala.reflect.ClassTag

abstract class H2OGLRMBase[P <: Model.Parameters: ClassTag]
  extends H2ODimReductionEstimator[P]
  with H2OGLRMExtraParams
  with HasInputCols {

  override def fit(dataset: Dataset[_]): H2OGLRMMOJOModel = {
    val model = super.fit(dataset).asInstanceOf[H2OGLRMMOJOModel]
    copyExtraParams(model)
    model
  }
}
