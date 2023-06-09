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

package ai.h2o.sparkling

case class H2OColumn(
    name: String,
    dataType: H2OColumnType.Value,
    min: Double,
    max: Double,
    mean: Double,
    sigma: Double,
    numberOfZeros: Long,
    numberOfMissingElements: Long,
    domain: Array[String],
    domainCardinality: Long,
    private val percentilesGetter: String => Array[Double]) {
  def nullable: Boolean = numberOfMissingElements > 0

  def isString(): Boolean = dataType == H2OColumnType.string

  def isNumeric(): Boolean = dataType == H2OColumnType.real || dataType == H2OColumnType.int

  def isTime(): Boolean = dataType == H2OColumnType.time

  def isCategorical(): Boolean = dataType == H2OColumnType.`enum`

  def isUUID(): Boolean = dataType == H2OColumnType.uuid

  def percentiles(): Array[Double] = percentilesGetter(name)
}
