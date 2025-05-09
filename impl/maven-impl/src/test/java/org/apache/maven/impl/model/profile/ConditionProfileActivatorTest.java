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
package org.apache.maven.impl.model.profile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.maven.api.model.Activation;
import org.apache.maven.api.model.ActivationFile;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.services.model.ProfileActivationContext;
import org.apache.maven.api.services.model.RootLocator;
import org.apache.maven.impl.DefaultModelVersionParser;
import org.apache.maven.impl.DefaultVersionParser;
import org.apache.maven.impl.model.DefaultInterpolator;
import org.apache.maven.impl.model.DefaultPathTranslator;
import org.apache.maven.impl.model.DefaultProfileActivationContext;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

class ConditionProfileActivatorTest extends AbstractProfileActivatorTest<ConditionProfileActivator> {

    @TempDir
    Path tempDir;

    @BeforeEach
    @Override
    void setUp() throws Exception {
        activator = new ConditionProfileActivator(
                new DefaultVersionParser(new DefaultModelVersionParser(new GenericVersionScheme())),
                new DefaultInterpolator());

        Path file = tempDir.resolve("file.txt");
        Files.createFile(file);

        Path dir = tempDir.resolve("dir");
        Files.createDirectory(dir);
        Files.createFile(dir.resolve("test.xsd"));
    }

    private Profile newProfile(String condition) {
        Activation a = Activation.newBuilder().condition(condition).build();
        Profile p = Profile.newBuilder().activation(a).build();
        return p;
    }

    private Map<String, String> newJdkProperties(String javaVersion) {
        return Map.of("java.version", javaVersion);
    }

    @Test
    void nullSafe() throws Exception {
        Profile p = Profile.newInstance();

        assertActivation(false, p, newContext(null, null));

        p = p.withActivation(Activation.newInstance());

        assertActivation(false, p, newContext(null, null));
    }

    @Test
    void jdkPrefix() throws Exception {
        Profile profile = newProfile("inrange(${java.version}, '[1.4,1.5)')");

        assertActivation(true, profile, newContext(null, newJdkProperties("1.4")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.4.2")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.4.2_09")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.4.2_09-b03")));

        assertActivation(false, profile, newContext(null, newJdkProperties("1.3")));

        assertActivation(false, profile, newContext(null, newJdkProperties("1.5")));
    }

    @Test
    void jdkPrefixNegated() throws Exception {
        Profile profile = newProfile("not(inrange(${java.version}, '[1.4,1.5)'))");

        assertActivation(false, profile, newContext(null, newJdkProperties("1.4")));
        assertActivation(false, profile, newContext(null, newJdkProperties("1.4.2")));
        assertActivation(false, profile, newContext(null, newJdkProperties("1.4.2_09")));
        assertActivation(false, profile, newContext(null, newJdkProperties("1.4.2_09-b03")));

        assertActivation(true, profile, newContext(null, newJdkProperties("1.3")));

        assertActivation(true, profile, newContext(null, newJdkProperties("1.5")));
    }

    @Test
    void jdkVersionRangeInclusiveBounds() throws Exception {
        Profile profile = newProfile("inrange(${java.version}, '[1.5,1.6.1]')");

        assertActivation(false, profile, newContext(null, newJdkProperties("1.4")));
        assertActivation(false, profile, newContext(null, newJdkProperties("1.4.2")));
        assertActivation(false, profile, newContext(null, newJdkProperties("1.4.2_09")));
        assertActivation(false, profile, newContext(null, newJdkProperties("1.4.2_09-b03")));

        assertActivation(true, profile, newContext(null, newJdkProperties("1.5")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.5.0")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.5.0_09")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.5.0_09-b03")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.5.1")));

        assertActivation(true, profile, newContext(null, newJdkProperties("1.6")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.6.0")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.6.0_09")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.6.0_09-b03")));
    }

    @Test
    void jdkVersionRangeExclusiveBounds() throws Exception {
        Profile profile = newProfile("inrange(${java.version}, '[1.3.1,1.6)')");

        assertActivation(false, profile, newContext(null, newJdkProperties("1.3")));
        assertActivation(false, profile, newContext(null, newJdkProperties("1.3.0")));
        assertActivation(false, profile, newContext(null, newJdkProperties("1.3.0_09")));
        assertActivation(false, profile, newContext(null, newJdkProperties("1.3.0_09-b03")));

        assertActivation(true, profile, newContext(null, newJdkProperties("1.3.1")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.3.1_09")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.3.1_09-b03")));

        assertActivation(true, profile, newContext(null, newJdkProperties("1.5")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.5.0")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.5.0_09")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.5.0_09-b03")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.5.1")));

        assertActivation(false, profile, newContext(null, newJdkProperties("1.6")));
    }

    @Test
    void jdkVersionRangeInclusiveLowerBound() throws Exception {
        Profile profile = newProfile("inrange(${java.version}, '[1.5,)')");

        assertActivation(false, profile, newContext(null, newJdkProperties("1.4")));
        assertActivation(false, profile, newContext(null, newJdkProperties("1.4.2")));
        assertActivation(false, profile, newContext(null, newJdkProperties("1.4.2_09")));
        assertActivation(false, profile, newContext(null, newJdkProperties("1.4.2_09-b03")));

        assertActivation(true, profile, newContext(null, newJdkProperties("1.5")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.5.0")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.5.0_09")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.5.0_09-b03")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.5.1")));

        assertActivation(true, profile, newContext(null, newJdkProperties("1.6")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.6.0")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.6.0_09")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.6.0_09-b03")));
    }

    @Test
    void jdkVersionRangeExclusiveUpperBound() throws Exception {
        Profile profile = newProfile("inrange(${java.version}, '(,1.6)')");

        assertActivation(true, profile, newContext(null, newJdkProperties("1.5")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.5.0")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.5.0_09")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.5.0_09-b03")));
        assertActivation(true, profile, newContext(null, newJdkProperties("1.5.1")));

        assertActivation(false, profile, newContext(null, newJdkProperties("1.6")));
        assertActivation(false, profile, newContext(null, newJdkProperties("1.6.0")));
        assertActivation(false, profile, newContext(null, newJdkProperties("1.6.0_09")));
        assertActivation(false, profile, newContext(null, newJdkProperties("1.6.0_09-b03")));
    }

    @Disabled
    @Test
    void jdkRubbishJavaVersion() {
        Profile profile = newProfile("inrange(${java.version}, '[1.8,)')");

        assertActivationWithProblems(profile, newContext(null, newJdkProperties("Pūteketeke")), "invalid JDK version");
        assertActivationWithProblems(profile, newContext(null, newJdkProperties("rubbish")), "invalid JDK version");
        assertActivationWithProblems(profile, newContext(null, newJdkProperties("1.a.0_09")), "invalid JDK version");
        assertActivationWithProblems(profile, newContext(null, newJdkProperties("1.a.2.b")), "invalid JDK version");
    }

    private void assertActivationWithProblems(
            Profile profile, ProfileActivationContext context, String warningContains) {
        SimpleProblemCollector problems = new SimpleProblemCollector();

        assertThat(activator.isActive(profile, context, problems)).isFalse();

        assertThat(problems.getErrors().size()).as(problems.getErrors().toString()).isEqualTo(0);
        assertThat(problems.getWarnings().size()).as(problems.getWarnings().toString()).isEqualTo(1);
        assertThat(problems.getWarnings().get(0).contains(warningContains)).as(problems.getWarnings().toString()).isTrue();
    }

    private Map<String, String> newOsProperties(String osName, String osVersion, String osArch) {
        return Map.of("os.name", osName, "os.version", osVersion, "os.arch", osArch);
    }

    @Test
    void osVersionStringComparison() throws Exception {
        Profile profile = newProfile("inrange(${os.version}, '[6.5.0-1014-aws,6.6)')");

        assertActivation(true, profile, newContext(null, newOsProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(true, profile, newContext(null, newOsProperties("windows", "6.5.0-1014-aws", "aarch64")));

        assertActivation(false, profile, newContext(null, newOsProperties("linux", "3.1.0", "amd64")));
    }

    @Test
    void osVersionRegexMatching() throws Exception {
        Profile profile = newProfile("matches(${os.version}, '.*aws')");

        assertActivation(true, profile, newContext(null, newOsProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(true, profile, newContext(null, newOsProperties("windows", "6.5.0-1014-aws", "aarch64")));

        assertActivation(false, profile, newContext(null, newOsProperties("linux", "3.1.0", "amd64")));
    }

    @Test
    void osName() {
        Profile profile = newProfile("${os.name} == 'windows'");

        assertActivation(false, profile, newContext(null, newOsProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(true, profile, newContext(null, newOsProperties("windows", "6.5.0-1014-aws", "aarch64")));
    }

    @Test
    void osNegatedName() {
        Profile profile = newProfile("${os.name} != 'windows'");

        assertActivation(true, profile, newContext(null, newOsProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(false, profile, newContext(null, newOsProperties("windows", "6.5.0-1014-aws", "aarch64")));
    }

    @Test
    void osArch() {
        Profile profile = newProfile("${os.arch} == 'amd64'");

        assertActivation(true, profile, newContext(null, newOsProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(false, profile, newContext(null, newOsProperties("windows", "6.5.0-1014-aws", "aarch64")));
    }

    @Test
    void osNegatedArch() {
        Profile profile = newProfile("${os.arch} != 'amd64'");

        assertActivation(false, profile, newContext(null, newOsProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(true, profile, newContext(null, newOsProperties("windows", "6.5.0-1014-aws", "aarch64")));
    }

    @Test
    void osAllConditions() {
        Profile profile =
                newProfile("${os.name} == 'windows' && ${os.arch} != 'amd64' && inrange(${os.version}, '[99,)')");

        assertActivation(false, profile, newContext(null, newOsProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(false, profile, newContext(null, newOsProperties("windows", "1", "aarch64")));
        assertActivation(false, profile, newContext(null, newOsProperties("windows", "99", "amd64")));
        assertActivation(true, profile, newContext(null, newOsProperties("windows", "99", "aarch64")));
    }

    @Test
    void osCapitalName() {
        Profile profile = newProfile("lower(${os.name}) == 'mac os x'");

        assertActivation(false, profile, newContext(null, newOsProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(false, profile, newContext(null, newOsProperties("windows", "1", "aarch64")));
        assertActivation(false, profile, newContext(null, newOsProperties("windows", "99", "amd64")));
        assertActivation(true, profile, newContext(null, newOsProperties("Mac OS X", "14.5", "aarch64")));
    }

    private Map<String, String> newPropProperties(String key, String value) {
        return Map.of(key, value);
    }

    @Test
    void propWithNameOnlyUserProperty() throws Exception {
        Profile profile = newProfile("${prop}");

        assertActivation(true, profile, newContext(newPropProperties("prop", "value"), null));
        assertActivation(false, profile, newContext(newPropProperties("prop", ""), null));
        assertActivation(false, profile, newContext(newPropProperties("other", "value"), null));
    }

    @Test
    void propWithNameOnlySystemProperty() throws Exception {
        Profile profile = newProfile("${prop}");

        assertActivation(true, profile, newContext(null, newPropProperties("prop", "value")));
        assertActivation(false, profile, newContext(null, newPropProperties("prop", "")));
        assertActivation(false, profile, newContext(null, newPropProperties("other", "value")));
    }

    @Test
    void propWithNegatedNameOnlyUserProperty() throws Exception {
        Profile profile = newProfile("not(${prop})");

        assertActivation(false, profile, newContext(newPropProperties("prop", "value"), null));
        assertActivation(true, profile, newContext(newPropProperties("prop", ""), null));
        assertActivation(true, profile, newContext(newPropProperties("other", "value"), null));
    }

    @Test
    void propWithNegatedNameOnlySystemProperty() throws Exception {
        Profile profile = newProfile("not(${prop})");

        assertActivation(false, profile, newContext(null, newPropProperties("prop", "value")));
        assertActivation(true, profile, newContext(null, newPropProperties("prop", "")));
        assertActivation(true, profile, newContext(null, newPropProperties("other", "value")));
    }

    @Test
    void propWithValueUserProperty() throws Exception {
        Profile profile = newProfile("${prop} == 'value'");

        assertActivation(true, profile, newContext(newPropProperties("prop", "value"), null));
        assertActivation(false, profile, newContext(newPropProperties("prop", "other"), null));
        assertActivation(false, profile, newContext(newPropProperties("prop", ""), null));
    }

    @Test
    void propWithValueSystemProperty() throws Exception {
        Profile profile = newProfile("${prop} == 'value'");

        assertActivation(true, profile, newContext(null, newPropProperties("prop", "value")));
        assertActivation(false, profile, newContext(null, newPropProperties("prop", "other")));
        assertActivation(false, profile, newContext(null, newPropProperties("other", "")));
    }

    @Test
    void propWithNegatedValueUserProperty() throws Exception {
        Profile profile = newProfile("${prop} != 'value'");

        assertActivation(false, profile, newContext(newPropProperties("prop", "value"), null));
        assertActivation(true, profile, newContext(newPropProperties("prop", "other"), null));
        assertActivation(true, profile, newContext(newPropProperties("prop", ""), null));
    }

    @Test
    void propWithNegatedValueSystemProperty() throws Exception {
        Profile profile = newProfile("${prop} != 'value'");

        assertActivation(false, profile, newContext(null, newPropProperties("prop", "value")));
        assertActivation(true, profile, newContext(null, newPropProperties("prop", "other")));
        assertActivation(true, profile, newContext(null, newPropProperties("other", "")));
    }

    @Test
    void propWithValueUserPropertyDominantOverSystemProperty() throws Exception {
        Profile profile = newProfile("${prop} == 'value'");

        Map<String, String> props1 = newPropProperties("prop", "value");
        Map<String, String> props2 = newPropProperties("prop", "other");

        assertActivation(true, profile, newContext(props1, props2));
        assertActivation(false, profile, newContext(props2, props1));
    }

    @Test
    @Disabled
    void fileRootDirectoryWithNull() {
        IllegalStateException e = assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> assertActivation(false, newProfile("exists('${project.rootDirectory}')"), newFileContext(null))).actual();
        assertThat(e.getMessage()).isEqualTo(RootLocator.UNABLE_TO_FIND_ROOT_PROJECT_MESSAGE);
    }

    @Test
    void fileRootDirectory() {
        assertActivation(false, newProfile("exists('${project.rootDirectory}/someFile.txt')"), newFileContext());
        assertActivation(true, newProfile("missing('${project.rootDirectory}/someFile.txt')"), newFileContext());
        assertActivation(true, newProfile("exists('${project.rootDirectory}')"), newFileContext());
        assertActivation(true, newProfile("exists('${project.rootDirectory}/file.txt')"), newFileContext());
        assertActivation(false, newProfile("missing('${project.rootDirectory}')"), newFileContext());
        assertActivation(false, newProfile("missing('${project.rootDirectory}/file.txt')"), newFileContext());
    }

    @Test
    @Disabled
    void fileWilcards() {
        assertActivation(true, newProfile("exists('${project.rootDirectory}/**/*.xsd')"), newFileContext());
        assertActivation(true, newProfile("exists('${project.basedir}/**/*.xsd')"), newFileContext());
        assertActivation(true, newProfile("exists('${project.basedir}/**/*.xsd')"), newFileContext());
        assertActivation(true, newProfile("exists('**/*.xsd')"), newFileContext());
        assertActivation(true, newProfile("missing('**/*.xml')"), newFileContext());
    }

    @Test
    void fileIsActiveNoFileWithShortBasedir() {
        assertActivation(false, newExistsProfile(null), newFileContext());
        assertActivation(false, newProfile("exists('someFile.txt')"), newFileContext());
        assertActivation(false, newProfile("exists('${basedir}/someFile.txt')"), newFileContext());

        assertActivation(false, newMissingProfile(null), newFileContext());
        assertActivation(true, newProfile("missing('someFile.txt')"), newFileContext());
        assertActivation(true, newProfile("missing('${basedir}/someFile.txt')"), newFileContext());
    }

    @Test
    void fileIsActiveNoFile() {
        assertActivation(false, newExistsProfile(null), newFileContext());
        assertActivation(false, newProfile("exists('someFile.txt')"), newFileContext());
        assertActivation(false, newProfile("exists('${project.basedir}/someFile.txt')"), newFileContext());

        assertActivation(false, newMissingProfile(null), newFileContext());
        assertActivation(true, newProfile("missing('someFile.txt')"), newFileContext());
        assertActivation(true, newProfile("missing('${project.basedir}/someFile.txt')"), newFileContext());
    }

    @Test
    void fileIsActiveExistsFileExists() {
        assertActivation(true, newProfile("exists('file.txt')"), newFileContext());
        assertActivation(true, newProfile("exists('${project.basedir}')"), newFileContext());
        assertActivation(true, newProfile("exists('${project.basedir}/file.txt')"), newFileContext());

        assertActivation(false, newProfile("missing('file.txt')"), newFileContext());
        assertActivation(false, newProfile("missing('${project.basedir}')"), newFileContext());
        assertActivation(false, newProfile("missing('${project.basedir}/file.txt')"), newFileContext());
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

    protected ProfileActivationContext newFileContext(Path path) {
        DefaultProfileActivationContext context = new DefaultProfileActivationContext(
                new DefaultPathTranslator(), new FakeRootLocator(), new DefaultInterpolator());

        context.setModel(Model.newBuilder().pomFile(path.resolve("pom.xml")).build());
        return context;
    }

    protected ProfileActivationContext newFileContext() {
        return newFileContext(tempDir);
    }
}
