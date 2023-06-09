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

package ai.h2o.sparkling.backend.converters

import java.sql.{Date, Timestamp}
import java.time.ZoneId
import java.util.TimeZone

import org.apache.spark.sql.catalyst.util.DateTimeUtils

trait TimeZoneConversions {
  protected def sparkTimeZone: TimeZone

  def fromSparkTimeZoneToUTC(timestamp: Long): Long = DateTimeUtils.fromUTCTime(timestamp, sparkTimeZone.getID)

  def fromSparkTimeZoneToUTC(timestamp: Timestamp): Long = fromSparkTimeZoneToUTC(timestamp.getTime * 1000) / 1000

  def fromSparkTimeZoneToUTC(date: Date): Long = {
    DateTimeUtils.fromUTCTime(date.getTime * 1000, ZoneId.systemDefault().getId) / 1000
  }

  def fromUTCToSparkTimeZone(timestamp: Long): Long = DateTimeUtils.toUTCTime(timestamp, sparkTimeZone.getID)
}

class TimeZoneConverter(val sparkTimeZone: TimeZone) extends TimeZoneConversions
