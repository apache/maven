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
package org.apache.maven;

import java.util.List;
import java.util.Set;

import org.apache.maven.api.services.Lookup;
import org.apache.maven.execution.BuildResumptionAnalyzer;
import org.apache.maven.execution.BuildResumptionDataRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.graph.GraphBuilder;
import org.apache.maven.internal.impl.DefaultSessionFactory;
import org.apache.maven.lifecycle.internal.ExecutionEventCatapult;
import org.apache.maven.model.Model;
import org.apache.maven.model.superpom.SuperPomProvider;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.apache.maven.resolver.RepositorySystemSessionFactory;
import org.apache.maven.session.scope.internal.SessionScope;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultMavenProfileValidationTest {

    @Test
    void includesProfilesInjectedFromAnUnavailableParentProject() {
        MavenProject project = new MavenProject();
        project.getModel().setModelVersion("4.0.0");
        project.setInjectedProfileIds("org.example:parent:1.0", List.of("parent-profile"));

        MavenSession session = mock(MavenSession.class);
        when(session.getProjects()).thenReturn(List.of(project));
        when(session.getSettings()).thenReturn(new Settings());

        SuperPomProvider superPomProvider = mock(SuperPomProvider.class);
        when(superPomProvider.getSuperModel("4.0.0")).thenReturn(new Model());

        DefaultMaven maven = new DefaultMaven(
                mock(Lookup.class),
                mock(ExecutionEventCatapult.class),
                mock(LegacySupport.class),
                mock(SessionScope.class),
                mock(RepositorySystemSessionFactory.class),
                mock(GraphBuilder.class),
                mock(BuildResumptionAnalyzer.class),
                mock(BuildResumptionDataRepository.class),
                superPomProvider,
                mock(DefaultSessionFactory.class),
                null);

        assertEquals(Set.of("parent-profile"), maven.getAllProfiles(session));
    }
}
