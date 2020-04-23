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

package org.apache.spark.h2o.ui

import org.apache.spark.SparkConf
import org.apache.spark.scheduler._
import org.apache.spark.status.{AppHistoryServerPlugin, ElementTrackingStore}
import org.apache.spark.ui.SparkUI

/**
  * History server plugin to enable Sparkling Water UI on history server
  */
class AppStatusPlugin extends AppHistoryServerPlugin {
  override def createListeners(conf: SparkConf, store: ElementTrackingStore): Seq[SparkListener] = {
    Seq(new AppStatusListener(conf, store, live = false))
  }

  override def setupUI(ui: SparkUI): Unit = {
    val sparklingWaterAppStatusStore = new AppStatusStore(ui.store.store)
    if (sparklingWaterAppStatusStore.isSparklingWaterStarted()) {
      new SparklingWaterUITab(sparklingWaterAppStatusStore, ui)
    }
  }
}