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

package ai.h2o.sparkling.doc.generation

object ModelDetailsTocTreeTemplate {
  def apply(algorithmModels: Seq[Class[_]], featureTransformerModels: Seq[Class[_]]): String = {
    val algorithmItems = algorithmModels.map(algorithm => s"   model_details_${algorithm.getSimpleName}").mkString("\n")
    val featureItems =
      featureTransformerModels.map(feature => s"   model_details_${feature.getSimpleName}").mkString("\n")
    s""".. _model_details:
       |
       |Model Details
       |=============
       |
       |**Algorithm Models**
       |
       |.. toctree::
       |   :maxdepth: 2
       |
       |$algorithmItems
       |
       |**Feature Transformer Models**
       |
       |.. toctree::
       |   :maxdepth: 2
       |
       |$featureItems
    """.stripMargin
  }
}
