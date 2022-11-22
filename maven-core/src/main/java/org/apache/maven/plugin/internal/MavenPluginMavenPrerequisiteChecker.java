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
package org.apache.maven.plugin.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.plugin.MavenPluginPrerequisitesChecker;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class MavenPluginMavenPrerequisiteChecker implements MavenPluginPrerequisitesChecker {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final RuntimeInformation runtimeInformation;

    @Inject
    public MavenPluginMavenPrerequisiteChecker(RuntimeInformation runtimeInformation) {
        super();
        this.runtimeInformation = runtimeInformation;
    }

    @Override
    public void accept(PluginDescriptor pluginDescriptor) {
        String requiredMavenVersion = pluginDescriptor.getRequiredMavenVersion();
        if (StringUtils.isNotBlank(requiredMavenVersion)) {
            boolean isRequirementMet = false;
            try {
                isRequirementMet = runtimeInformation.isMavenVersion(requiredMavenVersion);
            } catch (IllegalArgumentException e) {
                logger.warn(
                        "Could not verify plugin's Maven prerequisite as an invalid version is given in "
                                + requiredMavenVersion,
                        e);
                return;
            }
            if (!isRequirementMet) {
                throw new IllegalStateException("Required Maven version " + requiredMavenVersion
                        + " is not met by current version " + runtimeInformation.getMavenVersion());
            }
        }
    }
}
