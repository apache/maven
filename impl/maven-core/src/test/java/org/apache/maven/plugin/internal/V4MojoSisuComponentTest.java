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

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.AbstractCoreMavenComponentTestCase;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.plugin.Log;
import org.apache.maven.api.plugin.Mojo;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.scope.internal.MojoExecutionScope;
import org.apache.maven.execution.scope.internal.MojoExecutionScopeModule;
import org.apache.maven.internal.impl.SisuDiBridgeModule;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.session.scope.internal.SessionScope;
import org.apache.maven.session.scope.internal.SessionScopeModule;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * A mojo written against the Maven 4 API is built by its own {@link org.apache.maven.di.Injector}. A component in the
 * same plugin realm that is indexed for Sisu ({@code META-INF/sisu/javax.inject.Named}), as every library still on
 * JSR-330 and Sisu ships its components, must be injectable into such a mojo, without shadowing what that injector
 * binds itself: the project, session, mojo execution and log.
 */
class V4MojoSisuComponentTest extends AbstractCoreMavenComponentTestCase {

    private static final String GROUP_ID = "org.apache.maven.its";
    private static final String ARTIFACT_ID = "v4-sisu-plugin";
    private static final String VERSION = "1.0";
    private static final String GOAL = "inject";

    @Inject
    private MavenPluginManager pluginManager;

    @TempDir
    Path indexes;

    @Override
    protected String getProjectsDirectory() {
        return null;
    }

    @Test
    void v4MojoCanInjectSisuComponentFromItsPluginRealm() throws Exception {
        ClassRealm pluginRealm = pluginRealm();

        // as DefaultMavenPluginManager.discoverPluginComponents does when it sets up the plugin realm
        ((DefaultPlexusContainer) container)
                .discoverComponents(
                        pluginRealm,
                        new SessionScopeModule(container.lookup(SessionScope.class)),
                        new MojoExecutionScopeModule(container.lookup(MojoExecutionScope.class)),
                        new PluginConfigurationModule(Plugin.newBuilder()
                                .groupId(GROUP_ID)
                                .artifactId(ARTIFACT_ID)
                                .version(VERSION)
                                .build()),
                        new SisuDiBridgeModule(true));
        // the container sees the component, as it does for a Maven 3 mojo
        ClassRealm oldLookupRealm = container.setLookupRealm(pluginRealm);
        try {
            assertInstanceOf(SisuComponentImpl.class, container.lookup(SisuComponent.class, "sisu"));
        } finally {
            container.setLookupRealm(oldLookupRealm);
        }

        MavenSession session = createMavenSession(null);
        // the session maps only a project with a basedir to an API Project
        session.getProjects().get(0).setFile(indexes.resolve("pom.xml").toFile());
        session.setCurrentProject(session.getProjects().get(0));

        InjectingMojo mojo =
                (InjectingMojo) pluginManager.getConfiguredMojo(Mojo.class, session, mojoExecution(pluginRealm));

        assertInstanceOf(SisuComponentImpl.class, mojo.component);
        // what the mojo injector binds itself still wins over the Sisu scopes, even outside a mojo execution scope
        assertSame(session.getSession(), mojo.session);
        assertEquals(session.getCurrentProject().getArtifactId(), mojo.project.getArtifactId());
        assertEquals(GOAL, mojo.execution.getGoal());
        assertNotNull(mojo.log);
    }

    private ClassRealm pluginRealm() throws Exception {
        write("META-INF/sisu/javax.inject.Named", SisuComponentImpl.class.getName());
        write("META-INF/maven/org.apache.maven.api.di.Inject", InjectingMojo.class.getName());
        // like a real plugin realm: classes come in through imports from the core realm, so core's components stay
        // visible, while the realm's resources are its own index files only, not every index on the test class path
        ClassRealm realm = ((DefaultPlexusContainer) container)
                .getClassWorld()
                .newRealm(GROUP_ID + ":" + ARTIFACT_ID + ":" + VERSION, ClassLoader.getPlatformClassLoader());
        realm.importFrom(container.getContainerRealm(), getClass().getPackageName());
        realm.addURL(indexes.toUri().toURL());
        return realm;
    }

    private void write(String index, String className) throws Exception {
        Path file = indexes.resolve(index);
        Files.createDirectories(file.getParent());
        Files.writeString(file, className + "\n");
    }

    private static MojoExecution mojoExecution(ClassRealm pluginRealm) throws Exception {
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId(GROUP_ID);
        pluginDescriptor.setArtifactId(ARTIFACT_ID);
        pluginDescriptor.setVersion(VERSION);
        pluginDescriptor.setClassRealm(pluginRealm);
        MojoDescriptor mojoDescriptor = new MojoDescriptor(
                pluginDescriptor,
                org.apache.maven.api.plugin.descriptor.MojoDescriptor.newBuilder()
                        .goal(GOAL)
                        .implementation(InjectingMojo.class.getName())
                        .build());
        // as setupPluginRealm does, so the descriptor loads the mojo class from the realm
        mojoDescriptor.setRealm(pluginRealm);
        pluginDescriptor.addMojo(mojoDescriptor);
        return new MojoExecution(mojoDescriptor);
    }

    public interface SisuComponent {}

    /**
     * A component as libraries on JSR-330 and Sisu ship it, maven-scm's providers for one.
     */
    @Named("sisu")
    @Singleton
    public static class SisuComponentImpl implements SisuComponent {}

    @org.apache.maven.api.di.Named(GROUP_ID + ":" + ARTIFACT_ID + ":" + VERSION + ":" + GOAL)
    public static class InjectingMojo implements Mojo {
        @org.apache.maven.api.di.Inject
        @org.apache.maven.api.di.Named("sisu")
        SisuComponent component;

        @org.apache.maven.api.di.Inject
        Session session;

        @org.apache.maven.api.di.Inject
        Project project;

        @org.apache.maven.api.di.Inject
        org.apache.maven.api.MojoExecution execution;

        @org.apache.maven.api.di.Inject
        Log log;

        @Override
        public void execute() {}
    }
}
