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

package ai.h2o.sparkling.ml.models

import org.apache.hadoop.fs.Path
import ai.h2o.sparkling.ml.utils.{H2OReaderBase, Utils}
import ai.h2o.sparkling.utils.SparkSessionUtils
import ai.h2o.sparkling.utils.ScalaUtils._

private[models] class H2OMOJOReader[T <: HasMojo] extends H2OReaderBase[T] {

  override def load(path: String): T = {
    val model = super.load(path)
    val inputPath = new Path(path, H2OMOJOProps.serializedFileName)
    withResource(SparkSessionUtils.readHDFSFile(inputPath)) { inputStream =>
      model.setMojo(inputStream)
    }
    if (model.isInstanceOf[H2OMOJOModel]) {
      val mojoModel = model.asInstanceOf[H2OMOJOModel]
      mojoModel.h2oMojoModel = Utils.getMojoModel(mojoModel.getMojo())
      val numberOfCVModels = mojoModel.getOrDefault(mojoModel.numberOfCrossValidationModels)
      if (numberOfCVModels > 0) {
        val cvModels = (0 until numberOfCVModels).map { i =>
          val cvModelPath = new Path(new Path(path, H2OMOJOProps.crossValidationDirectoryName), i.toString).toString
          load(cvModelPath).asInstanceOf[H2OMOJOModel]
        }.toArray
        mojoModel.setCrossValidationModels(cvModels)
      }
    }
    model
  }
}
