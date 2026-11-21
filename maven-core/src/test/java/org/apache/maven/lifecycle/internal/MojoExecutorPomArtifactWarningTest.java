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
package org.apache.maven.lifecycle.internal;

import java.io.File;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * MNG-8138: warn when a mojo sets a file on the project artifact that has the POM's coordinates.
 */
class MojoExecutorPomArtifactWarningTest {

    private static final File POM = new File("pom.xml").getAbsoluteFile();

    @Test
    void warnsWhenJarIsSetOnPomArtifact() {
        MavenProject project = project("pom", new File("target/test-1.0.jar"));

        String warning = MojoExecutor.pomArtifactFileWarning(project, null, jarJar());

        assertEquals(
                "Plugin org.apache.maven.plugins:maven-jar-plugin:3.5.1:jar set the file of"
                        + " org.example:test:pom:1.0 to " + new File("target/test-1.0.jar")
                        + ", but that artifact is the project's POM (" + POM
                        + "). Install and deploy may publish this file in place of the POM;"
                        + " attach it with a classifier instead.",
                warning);
    }

    @Test
    void silentWhenPomArtifactHasThePom() {
        assertNull(MojoExecutor.pomArtifactFileWarning(project("pom", POM), null, jarJar()));
    }

    @Test
    void silentWhenPomArtifactHasNoFile() {
        assertNull(MojoExecutor.pomArtifactFileWarning(project("pom", null), null, jarJar()));
    }

    @Test
    void silentForJarProject() {
        assertNull(
                MojoExecutor.pomArtifactFileWarning(project("jar", new File("target/test-1.0.jar")), null, jarJar()));
    }

    @Test
    void silentForLaterMojosOnceTheFileWasAlreadySet() {
        File jar = new File("target/test-1.0.jar");
        assertNull(MojoExecutor.pomArtifactFileWarning(project("pom", jar), jar, jarJar()));
    }

    private static MavenProject project(String extension, File artifactFile) {
        MavenProject project = new MavenProject();
        project.setGroupId("org.example");
        project.setArtifactId("test");
        project.setVersion("1.0");
        project.setPackaging(extension);
        project.setFile(POM);
        DefaultArtifact artifact = new DefaultArtifact(
                "org.example", "test", "1.0", null, extension, null, new DefaultArtifactHandler(extension));
        artifact.setFile(artifactFile);
        project.setArtifact(artifact);
        return project;
    }

    private static MojoExecution jarJar() {
        Plugin plugin = new Plugin();
        plugin.setGroupId("org.apache.maven.plugins");
        plugin.setArtifactId("maven-jar-plugin");
        plugin.setVersion("3.5.1");
        return new MojoExecution(plugin, "jar", "default-cli");
    }
}
