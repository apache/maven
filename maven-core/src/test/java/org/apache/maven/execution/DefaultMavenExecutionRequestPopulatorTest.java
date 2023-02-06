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
package org.apache.maven.execution;

import javax.inject.Inject;

import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Settings;
import org.eclipse.sisu.launch.InjectedTestCase;

public class DefaultMavenExecutionRequestPopulatorTest extends InjectedTestCase {
    @Inject
    MavenExecutionRequestPopulator testee;

    public void testPluginRepositoryInjection() throws Exception {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();

        Repository r = new Repository();
        r.setId("test");
        r.setUrl("file:///test");

        Profile p = new Profile();
        p.setId("test");
        p.addPluginRepository(r);

        Settings settings = new Settings();
        settings.addProfile(p);
        settings.addActiveProfile(p.getId());

        testee.populateFromSettings(request, settings);

        List<ArtifactRepository> repositories = request.getPluginArtifactRepositories();
        assertEquals(1, repositories.size());
        assertEquals(r.getId(), repositories.get(0).getId());
        assertEquals(r.getUrl(), repositories.get(0).getUrl());
    }
}
