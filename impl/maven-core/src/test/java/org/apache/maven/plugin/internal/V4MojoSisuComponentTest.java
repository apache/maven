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
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.AbstractCoreMavenComponentTestCase;
import org.apache.maven.api.model.Plugin;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A mojo written against the Maven 4 API is built by its own {@link org.apache.maven.di.Injector}. A component that
 * its plugin realm ships for Sisu ({@code META-INF/sisu/javax.inject.Named}), as every library still on JSR-330 and Sisu
 * does, must be injectable into such a mojo, without shadowing what that injector binds itself: the project, session,
 * mojo execution and log.
 * <p>
 * The plugin classes are compiled at test time into the plugin realm's own directory, without annotation processing:
 * classes nested in this test would be indexed into the test class path, and the container would then find the
 * components in the core realm rather than in the plugin realm.
 */
class V4MojoSisuComponentTest extends AbstractCoreMavenComponentTestCase {

    private static final String GROUP_ID = "org.apache.maven.its";
    private static final String ARTIFACT_ID = "v4-sisu-plugin";
    private static final String VERSION = "1.0";
    private static final String GOAL = "inject";
    private static final String PACKAGE = "org.apache.maven.its.v4sisu";

    private static final Map<String, String> PLUGIN_SOURCES = Map.of(
            "SisuComponent",
            "public interface SisuComponent {}",
            "SisuComponentImpl",
            """
            @javax.inject.Named("sisu")
            @javax.inject.Singleton
            public class SisuComponentImpl implements SisuComponent {}
            """,
            "OtherComponent",
            "public interface OtherComponent {}",
            // a bare @Named on a Default* class is Sisu's "default" component, maven-scm's DefaultScmManager for one
            "DefaultComponent",
            """
            @javax.inject.Named
            @javax.inject.Singleton
            public class DefaultComponent implements OtherComponent {}
            """,
            "InjectingMojo",
            """
            import org.apache.maven.api.di.Inject;
            import org.apache.maven.api.di.Named;

            @Named("%s")
            public class InjectingMojo implements org.apache.maven.api.plugin.Mojo {
                @Inject @Named("sisu") SisuComponent component;
                @Inject OtherComponent defaultComponent;
                @Inject @Named("default") OtherComponent namedDefaultComponent;
                @Inject org.apache.maven.api.Session session;
                @Inject org.apache.maven.api.Project project;
                @Inject org.apache.maven.api.MojoExecution execution;
                @Inject org.apache.maven.api.plugin.Log log;

                public void execute() {}
            }
            """.formatted(GROUP_ID + ":" + ARTIFACT_ID + ":" + VERSION + ":" + GOAL));

    @Inject
    private MavenPluginManager pluginManager;

    @TempDir
    Path pluginDir;

    @Override
    protected String getProjectsDirectory() {
        return null;
    }

    @Test
    void v4MojoCanInjectSisuComponentFromItsPluginRealm() throws Exception {
        ClassRealm pluginRealm = pluginRealm();
        Class<?> sisuComponent = pluginRealm.loadClass(PACKAGE + ".SisuComponent");

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
        // the plugin classes live in the plugin realm only
        assertSame(pluginRealm, sisuComponent.getClassLoader());
        assertThrows(
                ClassNotFoundException.class,
                () -> container.getContainerRealm().loadClass(PACKAGE + ".SisuComponentImpl"));
        // the container finds the component from the plugin realm, as for a Maven 3 mojo
        ClassRealm oldLookupRealm = container.setLookupRealm(pluginRealm);
        try {
            assertEquals(
                    PACKAGE + ".SisuComponentImpl",
                    container.lookup(sisuComponent, "sisu").getClass().getName());
        } finally {
            container.setLookupRealm(oldLookupRealm);
        }

        MavenSession session = createMavenSession(null);
        // the session maps only a project with a basedir to an API Project
        session.getProjects().get(0).setFile(pluginDir.resolve("pom.xml").toFile());
        session.setCurrentProject(session.getProjects().get(0));

        Mojo mojo = pluginManager.getConfiguredMojo(Mojo.class, session, mojoExecution(pluginRealm));

        assertEquals(PACKAGE + ".SisuComponentImpl", className(field(mojo, "component")));
        assertEquals(PACKAGE + ".DefaultComponent", className(field(mojo, "defaultComponent")));
        assertEquals(PACKAGE + ".DefaultComponent", className(field(mojo, "namedDefaultComponent")));
        // what the mojo injector binds itself still wins over the Sisu scopes, even outside a mojo execution scope
        assertSame(session.getSession(), field(mojo, "session"));
        assertEquals(
                session.getCurrentProject().getArtifactId(),
                ((org.apache.maven.api.Project) field(mojo, "project")).getArtifactId());
        assertEquals(GOAL, ((org.apache.maven.api.MojoExecution) field(mojo, "execution")).getGoal());
        assertNotNull(field(mojo, "log"));
    }

    /**
     * A realm like a real plugin realm: its own classes and index files, and the API packages imported from core.
     */
    private ClassRealm pluginRealm() throws Exception {
        Path classes = pluginDir.resolve("classes");
        compile(classes);
        write(
                classes.resolve("META-INF/sisu/javax.inject.Named"),
                PACKAGE + ".SisuComponentImpl",
                PACKAGE + ".DefaultComponent");
        write(classes.resolve("META-INF/maven/org.apache.maven.api.di.Inject"), PACKAGE + ".InjectingMojo");

        ClassRealm realm = ((DefaultPlexusContainer) container)
                .getClassWorld()
                .newRealm(GROUP_ID + ":" + ARTIFACT_ID + ":" + VERSION, ClassLoader.getPlatformClassLoader());
        realm.importFrom(container.getContainerRealm(), "org.apache.maven.api");
        realm.importFrom(container.getContainerRealm(), "javax.inject");
        realm.addURL(classes.toUri().toURL());
        return realm;
    }

    private void compile(Path classes) throws Exception {
        Path sources = pluginDir.resolve("sources");
        List<String> arguments = new ArrayList<>(List.of(
                "-proc:none", "-d", classes.toString(), "-classpath", classPath(), "-sourcepath", sources.toString()));
        for (Map.Entry<String, String> source : PLUGIN_SOURCES.entrySet()) {
            Path file = sources.resolve(PACKAGE.replace('.', '/') + "/" + source.getKey() + ".java");
            write(file, "package " + PACKAGE + ";\n\n" + source.getValue());
            arguments.add(file.toString());
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertEquals(0, compiler.run(null, null, null, arguments.toArray(new String[0])), "plugin sources compile");
    }

    private static String classPath() throws Exception {
        Set<String> entries = new LinkedHashSet<>();
        for (Class<?> type : List.of(
                javax.inject.Named.class,
                org.apache.maven.api.di.Inject.class,
                org.apache.maven.api.Project.class,
                Mojo.class)) {
            entries.add(Path.of(type.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI())
                    .toString());
        }
        return String.join(File.pathSeparator, entries);
    }

    private static void write(Path file, String... lines) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, String.join("\n", lines) + "\n");
    }

    private static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private static String className(Object object) {
        return object == null ? null : object.getClass().getName();
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
                        .implementation(PACKAGE + ".InjectingMojo")
                        .build());
        // as setupPluginRealm does, so the descriptor loads the mojo class from the realm
        mojoDescriptor.setRealm(pluginRealm);
        pluginDescriptor.addMojo(mojoDescriptor);
        return new MojoExecution(mojoDescriptor);
    }
}
