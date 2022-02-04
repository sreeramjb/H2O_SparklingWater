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


class BackingJar(object):

    @staticmethod
    def getName():
        return "sparkling_water_scoring_assembly.jar"

    @staticmethod
    def getRelativePath():
        return "sparkling_water/" + BackingJar.getName()


class BackingJar(object):

    def __init__(self, name, module):
        self._name = name
        self._module = module

    def getName(self):
        return self._name

    def getModule(self):
        return self._module

    def getRelativePath(self):
        return self._module + "/" + self._name

    @staticmethod
    def getMainBackingJar():
        return BackingJar.getBackingJars()[0]

    @staticmethod
    def getBackingJars():
        return [new BackingJar(name = "sparkling_water_scoring_assembly.jar", module = "sparkling_water"),]
