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
package org.apache.maven.model.building;

import java.util.Properties;

import org.apache.maven.model.Model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for profile activation in external (repository-resolved) model builds, i.e. those at
 * {@link ModelBuildingRequest#VALIDATION_LEVEL_MINIMAL} (dependency POMs, parent POMs, imported BOMs).
 *
 * <h2>Activation rules for external models</h2>
 * <ul>
 *   <li><b>JDK / OS / activeByDefault</b> — always re-activated: they are platform facts.</li>
 *   <li><b>File</b> — suppressed: publisher-local paths don't exist on the consumer's machine.</li>
 *   <li><b>Property (user / -D)</b> — suppressed: consumer flags were not set for that dependency
 *       and must not accidentally activate its profiles.</li>
 *   <li><b>Property (system)</b> — re-activated: system properties are platform facts
 *       ({@code java.version}, {@code os.name}, …).</li>
 *   <li><b>Property (POM-declared, from &lt;properties&gt;)</b> — re-activated: the POM's own
 *       properties are part of the artifact's published identity, not the consumer's environment.
 *       A profile conditioned on a POM-declared property is publisher-intent, not consumer-injected.</li>
 *   <li><b>Negated property (-D absent)</b> — re-activated (default-on): the property is absent from
 *       the sandboxed context, so {@code !foo} evaluates to true, preserving the publisher's intent.</li>
 *   <li><b>Profile repositories</b> — honored from legitimately-active profiles (JDK/OS/activeByDefault
 *       and POM-property-gated); stripping them would break the established
 *       {@code project → dep1 → dep2} pattern. See #13100.</li>
 * </ul>
 */
class ExternalModelProfileActivationTest {

    /**
     * POM with profiles covering all activation types.
     * <p>
     * For the file test, {@code some.dir} is set as a system property pointing to a directory that
     * exists (tmpdir); in a STRICT project build this profile fires, but in external builds file
     * activation is suppressed entirely.
     * <p>
     * For the user-property test, {@code user.gating.prop} must be set via user properties (-D),
     * not system properties, to test the suppression of -D flags in external builds.
     * <p>
     * For the system-property test, {@code sys.gating.prop} is set as a system property and
     * must remain effective in external builds (system properties are platform facts).
     */
    private static final String POM = "<project>\n"
            + "  <modelVersion>4.0.0</modelVersion>\n"
            + "  <groupId>thegroup</groupId>\n"
            + "  <artifactId>withprofiles</artifactId>\n"
            + "  <version>1</version>\n"
            + "  <packaging>pom</packaging>\n"
            + "  <properties>\n"
            + "    <pom.declared.prop>hello</pom.declared.prop>\n"
            + "  </properties>\n"
            + "  <profiles>\n"
            // profile 1: file activation — suppressed in external builds
            + "    <profile>\n"
            + "      <id>file-condition</id>\n"
            + "      <activation>\n"
            + "        <file>\n"
            + "          <exists>${some.dir}</exists>\n"
            + "        </file>\n"
            + "      </activation>\n"
            + "      <properties>\n"
            + "        <profile.file>activated</profile.file>\n"
            + "      </properties>\n"
            + "    </profile>\n"
            // profile 2: user-property activation — suppressed in external builds
            + "    <profile>\n"
            + "      <id>user-property-condition</id>\n"
            + "      <activation>\n"
            + "        <property>\n"
            + "          <name>user.gating.prop</name>\n"
            + "        </property>\n"
            + "      </activation>\n"
            + "      <properties>\n"
            + "        <profile.user.property>activated</profile.user.property>\n"
            + "      </properties>\n"
            + "    </profile>\n"
            // profile 3: system-property activation — must fire in external builds
            + "    <profile>\n"
            + "      <id>system-property-condition</id>\n"
            + "      <activation>\n"
            + "        <property>\n"
            + "          <name>sys.gating.prop</name>\n"
            + "        </property>\n"
            + "      </activation>\n"
            + "      <properties>\n"
            + "        <profile.sys.property>activated</profile.sys.property>\n"
            + "      </properties>\n"
            + "    </profile>\n"
            // profile 4: POM-declared property activation (positive) — must fire in external builds
            + "    <profile>\n"
            + "      <id>pom-declared-property-condition</id>\n"
            + "      <activation>\n"
            + "        <property>\n"
            + "          <name>pom.declared.prop</name>\n"
            + "        </property>\n"
            + "      </activation>\n"
            + "      <properties>\n"
            + "        <profile.pom.property>activated</profile.pom.property>\n"
            + "      </properties>\n"
            + "    </profile>\n"
            // profile 5: negated-property (default-on, opt-out via -D) — must fire in external builds
            + "    <profile>\n"
            + "      <id>negated-property-default-on</id>\n"
            + "      <activation>\n"
            + "        <property>\n"
            + "          <name>!skip.defaults</name>\n"
            + "        </property>\n"
            + "      </activation>\n"
            + "      <properties>\n"
            + "        <profile.negated.property>activated</profile.negated.property>\n"
            + "      </properties>\n"
            + "    </profile>\n"
            // profile 6: JDK activation — always fires
            + "    <profile>\n"
            + "      <id>jdk-condition</id>\n"
            + "      <activation>\n"
            + "        <jdk>[1,)</jdk>\n"
            + "      </activation>\n"
            + "      <properties>\n"
            + "        <profile.jdk>activated</profile.jdk>\n"
            + "      </properties>\n"
            + "      <repositories>\n"
            + "        <repository>\n"
            + "          <id>profile-repo</id>\n"
            + "          <url>https://repo.example.test/profile</url>\n"
            + "        </repository>\n"
            + "      </repositories>\n"
            + "    </profile>\n"
            + "  </profiles>\n"
            + "</project>\n";

    private Model build(int validationLevel, Properties systemProperties, Properties userProperties) throws Exception {
        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();

        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setModelSource(new StringModelSource(POM));
        request.setValidationLevel(validationLevel);
        request.setSystemProperties(systemProperties);
        request.setUserProperties(userProperties);
        request.setModelResolver(new DefaultModelBuilderTest.BaseModelResolver());

        return builder.build(request).getEffectiveModel();
    }

    private Properties systemPropertiesWithTestValues() {
        Properties sp = new Properties();
        sp.putAll(System.getProperties());
        sp.setProperty("some.dir", System.getProperty("java.io.tmpdir"));
        sp.setProperty("sys.gating.prop", "true");
        return sp;
    }

    private Properties userPropertiesWithTestValues() {
        Properties up = new Properties();
        up.setProperty("user.gating.prop", "true");
        return up;
    }

    @Test
    void testProjectBuildEvaluatesAllActivators() throws Exception {
        Model model = build(
                ModelBuildingRequest.VALIDATION_LEVEL_STRICT,
                systemPropertiesWithTestValues(),
                userPropertiesWithTestValues());

        assertEquals("activated", model.getProperties().get("profile.file"), "file profile must fire in project build");
        assertEquals(
                "activated",
                model.getProperties().get("profile.user.property"),
                "user-property profile must fire in project build");
        assertEquals(
                "activated",
                model.getProperties().get("profile.sys.property"),
                "system-property profile must fire in project build");
        // POM-declared property activation is only supported in external (sandbox) builds.
        // In project builds, the PropertyProfileActivator does not check model properties
        // because doing so would cause unintended profile activation when a POM declares
        // a property that also matches a profile's activation condition (see IT proxy profile).
        assertNull(
                model.getProperties().get("profile.pom.property"),
                "POM-declared-property profile must NOT fire in project build (only in external)");
        assertEquals(
                "activated",
                model.getProperties().get("profile.negated.property"),
                "negated-property profile must fire in project build");
        assertEquals("activated", model.getProperties().get("profile.jdk"), "JDK profile must fire in project build");
        assertTrue(
                model.getRepositories().stream().anyMatch(r -> "profile-repo".equals(r.getId())),
                "profile repository must be present in project build");
    }

    @Test
    void testExternalModelFileActivationIsSuppressed() throws Exception {
        // File activation must be suppressed in external model builds regardless of whether the
        // path exists: publisher-local paths don't exist on the consumer's machine.
        Model model = build(
                ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL,
                systemPropertiesWithTestValues(),
                userPropertiesWithTestValues());

        assertNull(model.getProperties().get("profile.file"), "file profile must NOT fire in external model build");
    }

    @Test
    void testExternalModelUserPropertyActivationIsSuppressed() throws Exception {
        // Consumer -D flags must not activate dependency profiles.
        Model model = build(
                ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL,
                systemPropertiesWithTestValues(),
                userPropertiesWithTestValues());

        assertNull(
                model.getProperties().get("profile.user.property"),
                "user-property profile must NOT fire in external model build (consumer -D suppressed)");
    }

    @Test
    void testExternalModelSystemPropertyActivationIsPreserved() throws Exception {
        // System properties are platform facts and must drive activation even in external builds.
        Model model = build(
                ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL, systemPropertiesWithTestValues(), new Properties());

        assertEquals(
                "activated",
                model.getProperties().get("profile.sys.property"),
                "system-property profile must fire in external model build");
    }

    @Test
    void testExternalModelPomDeclaredPropertyActivationIsPreserved() throws Exception {
        // A profile conditioned on the POM's own <properties> must fire in external builds:
        // those properties are part of the artifact's published identity, not the consumer's env.
        Model model = build(
                ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL, systemPropertiesWithTestValues(), new Properties());

        assertEquals(
                "activated",
                model.getProperties().get("profile.pom.property"),
                "POM-declared-property profile must fire in external model build");
    }

    @Test
    void testExternalModelNegatedPropertyDefaultOnIsPreserved() throws Exception {
        // A negated-property profile (!foo) fires when the property is absent.
        // In the sandbox, user properties are suppressed so 'skip.defaults' is absent → fires.
        // This is the common "opt-out flag" pattern (e.g. resteasy-default in JBoss projects).
        Model model = build(
                ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL, systemPropertiesWithTestValues(), new Properties());

        assertEquals(
                "activated",
                model.getProperties().get("profile.negated.property"),
                "negated-property (default-on) profile must fire in external model build");
    }

    @Test
    void testExternalModelSystemPropertySuppressesDefaultOnProfile() throws Exception {
        // A system property named skip.defaults suppresses the default-on negated-property
        // profile (!skip.defaults) even in external model builds. System properties are platform
        // facts that pass through the sandbox unchanged, so the publisher's opt-out mechanism
        // still works when the consumer sets it at the JVM level.
        Properties sp = systemPropertiesWithTestValues();
        sp.setProperty("skip.defaults", "true"); // system-level opt-out
        Model model = build(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL, sp, new Properties());

        assertNull(
                model.getProperties().get("profile.negated.property"),
                "negated-property (default-on) profile must be suppressed when system property 'skip.defaults' is set");
    }

    @Test
    void testExternalModelJdkActivationIsPreserved() throws Exception {
        // JDK activation is a platform fact and must fire in external builds.
        Model model = build(
                ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL, systemPropertiesWithTestValues(), new Properties());

        assertEquals("activated", model.getProperties().get("profile.jdk"), "JDK profile must fire in external build");
    }

    @Test
    void testExternalModelJdkProfileRepositoriesAreHonored() throws Exception {
        // Repositories from legitimately-active profiles (JDK-activated) must be honored:
        // stripping them would break the project → dep1 → dep2 pattern. See #13100, #13116.
        Model model = build(
                ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL, systemPropertiesWithTestValues(), new Properties());

        assertTrue(
                model.getRepositories().stream().anyMatch(r -> "profile-repo".equals(r.getId())),
                "profile repository must be present in external model build (JDK-activated profile)");
    }

    /**
     * An {@code activeByDefault=true} profile in an external dependency model must contribute
     * its repositories — the same guarantee as for JDK/OS-activated profiles (see #13100).
     * Uses an isolated POM with only an activeByDefault profile so the standard Maven rule
     * ("activeByDefault is suppressed when any other profile activates") does not interfere.
     */
    @Test
    void testActiveByDefaultProfileRepositoryHonored() throws Exception {
        // Isolated POM: only one profile (activeByDefault=true), no other profiles that could
        // suppress it.  Without isolation the JDK-activated sibling in POM would cause Maven
        // to skip the activeByDefault profile entirely, making the test vacuous.
        String activeByDefaultPom = "<project>\n"
                + "  <modelVersion>4.0.0</modelVersion>\n"
                + "  <groupId>thegroup</groupId>\n"
                + "  <artifactId>active-by-default-profile</artifactId>\n"
                + "  <version>1</version>\n"
                + "  <packaging>pom</packaging>\n"
                + "  <profiles>\n"
                + "    <profile>\n"
                + "      <id>always-active</id>\n"
                + "      <activation>\n"
                + "        <activeByDefault>true</activeByDefault>\n"
                + "      </activation>\n"
                + "      <repositories>\n"
                + "        <repository>\n"
                + "          <id>always-active-repo</id>\n"
                + "          <url>https://repo.example.test/always-active</url>\n"
                + "        </repository>\n"
                + "      </repositories>\n"
                + "    </profile>\n"
                + "  </profiles>\n"
                + "</project>\n";

        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();

        Properties systemProperties = new Properties();
        systemProperties.putAll(System.getProperties());

        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setModelSource(new StringModelSource(activeByDefaultPom));
        request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        request.setSystemProperties(systemProperties);

        Model model = builder.build(request).getEffectiveModel();

        // The always-active profile activates by default: its repository must survive external
        // model resolution unchanged. See #13100, #13141.
        assertTrue(
                model.getRepositories().stream().anyMatch(r -> "always-active-repo".equals(r.getId())),
                "Repository from activeByDefault profile must be retained in external dependency model");
    }
}
