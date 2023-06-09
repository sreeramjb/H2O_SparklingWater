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

trait HasUnsupportedOffsetCol {

  def getOffsetCol(): String = null

  def setOffsetCol(value: String): this.type = {
    val className = this.getClass.getName
    throw new UnsupportedOperationException(s"The parameter 'offsetCol' is not yet supported on $className.")
  }

  private[sparkling] def getUnsupportedOffsetColParam(trainingFrame: H2OFrame): Map[String, Any] = Map.empty
}
