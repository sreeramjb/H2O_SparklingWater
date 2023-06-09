#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

from ai.h2o.sparkling.ml.algos.regression import H2OAutoMLRegressor
from ai.h2o.sparkling.ml.algos.regression import H2OGLMRegressor
from ai.h2o.sparkling.ml.algos.regression import H2OGAMRegressor
from ai.h2o.sparkling.ml.algos.regression import H2OGBMRegressor
from ai.h2o.sparkling.ml.algos.regression import H2OXGBoostRegressor
from ai.h2o.sparkling.ml.algos.regression import H2ODeepLearningRegressor
from ai.h2o.sparkling.ml.algos.regression import H2ODRFRegressor
from ai.h2o.sparkling.ml.algos.regression import H2ORuleFitRegressor

__all__ = [
    "H2OAutoMLRegressor",
    "H2OGLMRegressor",
    "H2OGAMRegressor",
    "H2OGBMRegressor",
    "H2OXGBoostRegressor",
    "H2ODeepLearningRegressor",
    "H2ODRFRegressor",
    "H2ORuleFitRegressor"]
