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
package org.apache.maven.its.mng7891;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.logging.Logger;

@Named
@Singleton
public class MyLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    private PlexusConfiguration configuration;
    private Logger logger;

    @Inject
    public MyLifecycleParticipant(
            @Named("org.apache.maven.its.mng7891:extension") PlexusConfiguration configuration, Logger logger) {
        this.configuration = configuration;
        this.logger = logger;
    }

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        PlexusConfiguration messages = configuration.getChild("messages");
        PlexusConfiguration sessionStart = messages.getChild("projectsRead");
        logger.info(sessionStart.getValue("No session projects read message configured"));
    }
}
