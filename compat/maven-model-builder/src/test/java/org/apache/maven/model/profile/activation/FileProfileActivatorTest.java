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
package org.apache.maven.model.profile.activation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.maven.api.model.Activation;
import org.apache.maven.api.model.ActivationFile;
import org.apache.maven.api.model.Profile;
import org.apache.maven.model.path.DefaultPathTranslator;
import org.apache.maven.model.path.ProfileActivationFilePathInterpolator;
import org.apache.maven.model.profile.DefaultProfileActivationContext;
import org.apache.maven.model.root.DefaultRootLocator;
import org.apache.maven.model.root.RootLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link FileProfileActivator}.
 *
 */
@Deprecated
class FileProfileActivatorTest extends AbstractProfileActivatorTest<FileProfileActivator> {

    @TempDir
    Path tempDir;

    private final DefaultProfileActivationContext context = new DefaultProfileActivationContext();

    @BeforeEach
    @Override
    void setUp() throws Exception {
        activator = new FileProfileActivator()
                .setProfileActivationFilePathInterpolator(new ProfileActivationFilePathInterpolator()
                        .setPathTranslator(new DefaultPathTranslator())
                        .setRootLocator(new DefaultRootLocator() {
                            @Override
                            public Path findRoot(Path basedir) {
                                return basedir;
                            }
                        }));

        context.setProjectDirectory(tempDir.toFile());

        File file = new File(tempDir.resolve("file.txt").toString());
        if (!file.createNewFile()) {
            throw new IOException("Can't create " + file);
        }
    }

    @Test
    void testRootDirectoryWithNull() {
        context.setProjectDirectory(null);

        IllegalStateException e = assertThrows(
                IllegalStateException.class,
                () -> assertActivation(false, newExistsProfile("${project.rootDirectory}"), context));
        assertEquals(RootLocator.UNABLE_TO_FIND_ROOT_PROJECT_MESSAGE, e.getMessage());
    }

    @Test
    void testRootDirectory() {
        assertActivation(false, newExistsProfile("${project.rootDirectory}/someFile.txt"), context);
        assertActivation(true, newMissingProfile("${project.rootDirectory}/someFile.txt"), context);
        assertActivation(true, newExistsProfile("${project.rootDirectory}"), context);
        assertActivation(true, newExistsProfile("${project.rootDirectory}/" + "file.txt"), context);
        assertActivation(false, newMissingProfile("${project.rootDirectory}"), context);
        assertActivation(false, newMissingProfile("${project.rootDirectory}/" + "file.txt"), context);
    }

    @Test
    void testCustomRootDirectoryPropertyIsNotShadowed() throws Exception {
        // GH-13883: a custom ${rootDirectory} property must not be shadowed by ${project.rootDirectory}
        Properties modelProperties = new Properties();
        modelProperties.setProperty("rootDirectory", "${project.basedir}");
        DefaultProfileActivationContext moduleContext = newModuleContext(modelProperties);

        assertActivation(true, newExistsProfile("${rootDirectory}/module-file.txt"), moduleContext);
        assertActivation(false, newExistsProfile("${rootDirectory}/file.txt"), moduleContext);
        assertActivation(true, newExistsProfile("${project.rootDirectory}/file.txt"), moduleContext);
        assertActivation(false, newExistsProfile("${project.rootDirectory}/module-file.txt"), moduleContext);
    }

    @Test
    void testUnprefixedRootDirectoryIsNotInterpolated() throws Exception {
        // GH-13883: only ${project.rootDirectory} is supported, ${rootDirectory} is not an alias
        DefaultProfileActivationContext moduleContext = newModuleContext(new Properties());

        assertActivation(false, newExistsProfile("${rootDirectory}/file.txt"), moduleContext);
        assertActivation(true, newMissingProfile("${rootDirectory}/file.txt"), moduleContext);
    }

    private DefaultProfileActivationContext newModuleContext(Properties modelProperties) throws IOException {
        Path moduleDir = Files.createDirectories(tempDir.resolve("module"));
        Files.createFile(moduleDir.resolve("module-file.txt"));

        activator = new FileProfileActivator()
                .setProfileActivationFilePathInterpolator(new ProfileActivationFilePathInterpolator()
                        .setPathTranslator(new DefaultPathTranslator())
                        .setRootLocator(new DefaultRootLocator() {
                            @Override
                            public Path findRoot(Path basedir) {
                                return tempDir;
                            }
                        }));

        return new DefaultProfileActivationContext()
                .setProjectDirectory(moduleDir.toFile())
                .setProjectProperties(modelProperties);
    }

    @Test
    void testIsActiveNoFileWithShortBasedir() {
        assertActivation(false, newExistsProfile(null), context);
        assertActivation(false, newExistsProfile("someFile.txt"), context);
        assertActivation(false, newExistsProfile("${basedir}/someFile.txt"), context);

        assertActivation(false, newMissingProfile(null), context);
        assertActivation(true, newMissingProfile("someFile.txt"), context);
        assertActivation(true, newMissingProfile("${basedir}/someFile.txt"), context);
    }

    @Test
    void testIsActiveNoFile() {
        assertActivation(false, newExistsProfile(null), context);
        assertActivation(false, newExistsProfile("someFile.txt"), context);
        assertActivation(false, newExistsProfile("${project.basedir}/someFile.txt"), context);

        assertActivation(false, newMissingProfile(null), context);
        assertActivation(true, newMissingProfile("someFile.txt"), context);
        assertActivation(true, newMissingProfile("${project.basedir}/someFile.txt"), context);
    }

    @Test
    void testIsActiveExistsFileExists() {
        assertActivation(true, newExistsProfile("file.txt"), context);
        assertActivation(true, newExistsProfile("${project.basedir}"), context);
        assertActivation(true, newExistsProfile("${project.basedir}/" + "file.txt"), context);

        assertActivation(false, newMissingProfile("file.txt"), context);
        assertActivation(false, newMissingProfile("${project.basedir}"), context);
        assertActivation(false, newMissingProfile("${project.basedir}/" + "file.txt"), context);
    }

    @Test
    void testIsActiveExistsLeavesFileUnchanged() {
        Profile profile = newExistsProfile("file.txt");
        assertEquals("file.txt", profile.getActivation().getFile().getExists());

        assertActivation(true, profile, context);

        assertEquals("file.txt", profile.getActivation().getFile().getExists());
    }

    private Profile newExistsProfile(String filePath) {
        ActivationFile activationFile =
                ActivationFile.newBuilder().exists(filePath).build();
        return newProfile(activationFile);
    }

    private Profile newMissingProfile(String filePath) {
        ActivationFile activationFile =
                ActivationFile.newBuilder().missing(filePath).build();
        return newProfile(activationFile);
    }

    private Profile newProfile(ActivationFile activationFile) {
        Activation activation = Activation.newBuilder().file(activationFile).build();
        return Profile.newBuilder().activation(activation).build();
    }
}
