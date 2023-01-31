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
package org.apache.maven.project.interpolation;

import java.io.IOException;
import java.util.Properties;

import org.apache.maven.project.path.PathTranslator;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;

/**
 * Use a regular expression search to find and resolve expressions within the POM.
 *
 * @author jdcasey Created on Feb 3, 2005
 * TODO Consolidate this logic with the PluginParameterExpressionEvaluator, minus deprecations/bans.
 */
@Deprecated
public class RegexBasedModelInterpolator extends AbstractStringBasedModelInterpolator {

    public RegexBasedModelInterpolator() throws IOException {}

    public RegexBasedModelInterpolator(PathTranslator pathTranslator) {
        super(pathTranslator);
    }

    public RegexBasedModelInterpolator(Properties envars) {}

    protected Interpolator createInterpolator() {
        return new RegexBasedInterpolator(true);
    }
}
