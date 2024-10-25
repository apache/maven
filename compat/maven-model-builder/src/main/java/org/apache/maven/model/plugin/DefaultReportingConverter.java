/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.model.plugin;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;

/**
 * Handles conversion of the <code>&lt;reporting&gt;</code> section into the configuration of Maven Site Plugin 3.x,
 * i.e. <code>reportPlugins</code> and <code>outputDirectory</code> parameters.
 *
 * @deprecated since maven 4.0, this class is now a no-op class and is only here for compatibility
 */
@Named
@Singleton
@Deprecated
public class DefaultReportingConverter implements ReportingConverter {

    @Override
    public void convertReporting(Model model, ModelBuildingRequest request, ModelProblemCollector problems) {}
}
