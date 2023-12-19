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
package org.apache.maven.repository.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.Version;
import org.apache.maven.model.version.ModelVersionParser;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

/**
 */
public class VersionTest extends AbstractVersionTest {
    private final ModelVersionParser modelVersionParser = new DefaultModelVersionParser(new GenericVersionScheme());

    protected Version newVersion(String version) {
        return modelVersionParser.parseVersion(version);
    }

    @Test
    void testEmptyVersion() {
        assertOrder(X_EQ_Y, "0", "");
    }

    @Test
    void testNumericOrdering() {
        assertOrder(X_LT_Y, "2", "10");
        assertOrder(X_LT_Y, "1.2", "1.10");
        assertOrder(X_LT_Y, "1.0.2", "1.0.10");
        assertOrder(X_LT_Y, "1.0.0.2", "1.0.0.10");
        assertOrder(X_LT_Y, "1.0.20101206.111434.1", "1.0.20101206.111435.1");
        assertOrder(X_LT_Y, "1.0.20101206.111434.2", "1.0.20101206.111434.10");
    }

    @Test
    void testDelimiters() {
        assertOrder(X_EQ_Y, "1.0", "1-0");
        assertOrder(X_EQ_Y, "1.0", "1_0");
        assertOrder(X_EQ_Y, "1.a", "1a");
    }

    @Test
    void testLeadingZerosAreSemanticallyIrrelevant() {
        assertOrder(X_EQ_Y, "1", "01");
        assertOrder(X_EQ_Y, "1.2", "1.002");
        assertOrder(X_EQ_Y, "1.2.3", "1.2.0003");
        assertOrder(X_EQ_Y, "1.2.3.4", "1.2.3.00004");
    }

    @Test
    void testTrailingZerosAreSemanticallyIrrelevant() {
        assertOrder(X_EQ_Y, "1", "1.0.0.0.0.0.0.0.0.0.0.0.0.0");
        assertOrder(X_EQ_Y, "1", "1-0-0-0-0-0-0-0-0-0-0-0-0-0");
        assertOrder(X_EQ_Y, "1", "1.0-0.0-0.0-0.0-0.0-0.0-0.0");
        assertOrder(X_EQ_Y, "1", "1.0000000000000");
        assertOrder(X_EQ_Y, "1.0", "1.0.0");
    }

    @Test
    void testTrailingZerosBeforeQualifierAreSemanticallyIrrelevant() {
        assertOrder(X_EQ_Y, "1.0-ga", "1.0.0-ga");
        assertOrder(X_EQ_Y, "1.0.ga", "1.0.0.ga");
        assertOrder(X_EQ_Y, "1.0ga", "1.0.0ga");

        assertOrder(X_EQ_Y, "1.0-alpha", "1.0.0-alpha");
        assertOrder(X_EQ_Y, "1.0.alpha", "1.0.0.alpha");
        assertOrder(X_EQ_Y, "1.0alpha", "1.0.0alpha");
        assertOrder(X_EQ_Y, "1.0-alpha-snapshot", "1.0.0-alpha-snapshot");
        assertOrder(X_EQ_Y, "1.0.alpha.snapshot", "1.0.0.alpha.snapshot");

        assertOrder(X_EQ_Y, "1.x.0-alpha", "1.x.0.0-alpha");
        assertOrder(X_EQ_Y, "1.x.0.alpha", "1.x.0.0.alpha");
        assertOrder(X_EQ_Y, "1.x.0-alpha-snapshot", "1.x.0.0-alpha-snapshot");
        assertOrder(X_EQ_Y, "1.x.0.alpha.snapshot", "1.x.0.0.alpha.snapshot");
    }

    @Test
    void testTrailingDelimitersAreSemanticallyIrrelevant() {
        assertOrder(X_EQ_Y, "1", "1.............");
        assertOrder(X_EQ_Y, "1", "1-------------");
        assertOrder(X_EQ_Y, "1.0", "1.............");
        assertOrder(X_EQ_Y, "1.0", "1-------------");
    }

    @Test
    void testInitialDelimiters() {
        assertOrder(X_EQ_Y, "0.1", ".1");
        assertOrder(X_EQ_Y, "0.0.1", "..1");
        assertOrder(X_EQ_Y, "0.1", "-1");
        assertOrder(X_EQ_Y, "0.0.1", "--1");
    }

    @Test
    void testConsecutiveDelimiters() {
        assertOrder(X_EQ_Y, "1.0.1", "1..1");
        assertOrder(X_EQ_Y, "1.0.0.1", "1...1");
        assertOrder(X_EQ_Y, "1.0.1", "1--1");
        assertOrder(X_EQ_Y, "1.0.0.1", "1---1");
    }

    @Test
    void testUnlimitedNumberOfVersionComponents() {
        assertOrder(X_GT_Y, "1.0.1.2.3.4.5.6.7.8.9.0.1.2.10", "1.0.1.2.3.4.5.6.7.8.9.0.1.2.3");
    }

    @Test
    void testUnlimitedNumberOfDigitsInNumericComponent() {
        assertOrder(X_GT_Y, "1.1234567890123456789012345678901", "1.123456789012345678901234567891");
    }

    @Test
    void testTransitionFromDigitToLetterAndViceVersaIsEqualivantToDelimiter() {
        assertOrder(X_EQ_Y, "1alpha10", "1.alpha.10");
        assertOrder(X_EQ_Y, "1alpha10", "1-alpha-10");

        assertOrder(X_GT_Y, "1.alpha10", "1.alpha2");
        assertOrder(X_GT_Y, "10alpha", "1alpha");
    }

    @Test
    void testWellKnownQualifierOrdering() {
        assertOrder(X_EQ_Y, "1-alpha1", "1-a1");
        assertOrder(X_LT_Y, "1-alpha", "1-beta");
        assertOrder(X_EQ_Y, "1-beta1", "1-b1");
        assertOrder(X_LT_Y, "1-beta", "1-milestone");
        assertOrder(X_EQ_Y, "1-milestone1", "1-m1");
        assertOrder(X_LT_Y, "1-milestone", "1-rc");
        assertOrder(X_EQ_Y, "1-rc", "1-cr");
        assertOrder(X_LT_Y, "1-rc", "1-snapshot");
        assertOrder(X_LT_Y, "1-snapshot", "1");
        assertOrder(X_EQ_Y, "1", "1-ga");
        assertOrder(X_EQ_Y, "1", "1.ga.0.ga");
        assertOrder(X_EQ_Y, "1.0", "1-ga");
        assertOrder(X_EQ_Y, "1", "1-ga.ga");
        assertOrder(X_EQ_Y, "1", "1-ga-ga");
        assertOrder(X_EQ_Y, "A", "A.ga.ga");
        assertOrder(X_EQ_Y, "A", "A-ga-ga");
        assertOrder(X_EQ_Y, "1", "1-final");
        assertOrder(X_EQ_Y, "1", "1-release");
        assertOrder(X_LT_Y, "1", "1-sp");

        assertOrder(X_LT_Y, "A.rc.1", "A.ga.1");
        assertOrder(X_GT_Y, "A.sp.1", "A.ga.1");
        assertOrder(X_LT_Y, "A.rc.x", "A.ga.x");
        assertOrder(X_GT_Y, "A.sp.x", "A.ga.x");
    }

    @Test
    void testWellKnownQualifierVersusUnknownQualifierOrdering() {
        assertOrder(X_GT_Y, "1-abc", "1-alpha");
        assertOrder(X_GT_Y, "1-abc", "1-beta");
        assertOrder(X_GT_Y, "1-abc", "1-milestone");
        assertOrder(X_GT_Y, "1-abc", "1-rc");
        assertOrder(X_GT_Y, "1-abc", "1-snapshot");
        assertOrder(X_GT_Y, "1-abc", "1");
        assertOrder(X_GT_Y, "1-abc", "1-sp");
    }

    @Test
    void testWellKnownSingleCharQualifiersOnlyRecognizedIfImmediatelyFollowedByNumber() {
        assertOrder(X_GT_Y, "1.0a", "1.0");
        assertOrder(X_GT_Y, "1.0-a", "1.0");
        assertOrder(X_GT_Y, "1.0.a", "1.0");
        assertOrder(X_GT_Y, "1.0b", "1.0");
        assertOrder(X_GT_Y, "1.0-b", "1.0");
        assertOrder(X_GT_Y, "1.0.b", "1.0");
        assertOrder(X_GT_Y, "1.0m", "1.0");
        assertOrder(X_GT_Y, "1.0-m", "1.0");
        assertOrder(X_GT_Y, "1.0.m", "1.0");

        assertOrder(X_LT_Y, "1.0a1", "1.0");
        assertOrder(X_LT_Y, "1.0-a1", "1.0");
        assertOrder(X_LT_Y, "1.0.a1", "1.0");
        assertOrder(X_LT_Y, "1.0b1", "1.0");
        assertOrder(X_LT_Y, "1.0-b1", "1.0");
        assertOrder(X_LT_Y, "1.0.b1", "1.0");
        assertOrder(X_LT_Y, "1.0m1", "1.0");
        assertOrder(X_LT_Y, "1.0-m1", "1.0");
        assertOrder(X_LT_Y, "1.0.m1", "1.0");

        assertOrder(X_GT_Y, "1.0a.1", "1.0");
        assertOrder(X_GT_Y, "1.0a-1", "1.0");
        assertOrder(X_GT_Y, "1.0b.1", "1.0");
        assertOrder(X_GT_Y, "1.0b-1", "1.0");
        assertOrder(X_GT_Y, "1.0m.1", "1.0");
        assertOrder(X_GT_Y, "1.0m-1", "1.0");
    }

    @Test
    void testUnknownQualifierOrdering() {
        assertOrder(X_LT_Y, "1-abc", "1-abcd");
        assertOrder(X_LT_Y, "1-abc", "1-bcd");
        assertOrder(X_GT_Y, "1-abc", "1-aac");
    }

    @Test
    void testCaseInsensitiveOrderingOfQualifiers() {
        assertOrder(X_EQ_Y, "1.alpha", "1.ALPHA");
        assertOrder(X_EQ_Y, "1.alpha", "1.Alpha");

        assertOrder(X_EQ_Y, "1.beta", "1.BETA");
        assertOrder(X_EQ_Y, "1.beta", "1.Beta");

        assertOrder(X_EQ_Y, "1.milestone", "1.MILESTONE");
        assertOrder(X_EQ_Y, "1.milestone", "1.Milestone");

        assertOrder(X_EQ_Y, "1.rc", "1.RC");
        assertOrder(X_EQ_Y, "1.rc", "1.Rc");
        assertOrder(X_EQ_Y, "1.cr", "1.CR");
        assertOrder(X_EQ_Y, "1.cr", "1.Cr");

        assertOrder(X_EQ_Y, "1.snapshot", "1.SNAPSHOT");
        assertOrder(X_EQ_Y, "1.snapshot", "1.Snapshot");

        assertOrder(X_EQ_Y, "1.ga", "1.GA");
        assertOrder(X_EQ_Y, "1.ga", "1.Ga");
        assertOrder(X_EQ_Y, "1.final", "1.FINAL");
        assertOrder(X_EQ_Y, "1.final", "1.Final");
        assertOrder(X_EQ_Y, "1.release", "1.RELEASE");
        assertOrder(X_EQ_Y, "1.release", "1.Release");

        assertOrder(X_EQ_Y, "1.sp", "1.SP");
        assertOrder(X_EQ_Y, "1.sp", "1.Sp");

        assertOrder(X_EQ_Y, "1.unknown", "1.UNKNOWN");
        assertOrder(X_EQ_Y, "1.unknown", "1.Unknown");
    }

    @Test
    void testCaseInsensitiveOrderingOfQualifiersIsLocaleIndependent() {
        Locale orig = Locale.getDefault();
        try {
            Locale[] locales = {Locale.ENGLISH, new Locale("tr")};
            for (Locale locale : locales) {
                Locale.setDefault(locale);
                assertOrder(X_EQ_Y, "1-abcdefghijklmnopqrstuvwxyz", "1-ABCDEFGHIJKLMNOPQRSTUVWXYZ");
            }
        } finally {
            Locale.setDefault(orig);
        }
    }

    @Test
    void testQualifierVersusNumberOrdering() {
        assertOrder(X_LT_Y, "1-ga", "1-1");
        assertOrder(X_LT_Y, "1.ga", "1.1");
        assertOrder(X_EQ_Y, "1-ga", "1.0");
        assertOrder(X_EQ_Y, "1.ga", "1.0");

        assertOrder(X_LT_Y, "1-ga-1", "1-0-1");
        assertOrder(X_LT_Y, "1.ga.1", "1.0.1");

        assertOrder(X_GT_Y, "1.sp", "1.0");
        assertOrder(X_LT_Y, "1.sp", "1.1");

        assertOrder(X_LT_Y, "1-abc", "1-1");
        assertOrder(X_LT_Y, "1.abc", "1.1");

        assertOrder(X_LT_Y, "1-xyz", "1-1");
        assertOrder(X_LT_Y, "1.xyz", "1.1");
    }

    @Test
    void testVersionEvolution() {
        assertSequence(
                "0.9.9-SNAPSHOT",
                "0.9.9",
                "0.9.10-SNAPSHOT",
                "0.9.10",
                "1.0-alpha-2-SNAPSHOT",
                "1.0-alpha-2",
                "1.0-alpha-10-SNAPSHOT",
                "1.0-alpha-10",
                "1.0-beta-1-SNAPSHOT",
                "1.0-beta-1",
                "1.0-rc-1-SNAPSHOT",
                "1.0-rc-1",
                "1.0-SNAPSHOT",
                "1.0",
                "1.0-sp-1-SNAPSHOT",
                "1.0-sp-1",
                "1.0.1-alpha-1-SNAPSHOT",
                "1.0.1-alpha-1",
                "1.0.1-beta-1-SNAPSHOT",
                "1.0.1-beta-1",
                "1.0.1-rc-1-SNAPSHOT",
                "1.0.1-rc-1",
                "1.0.1-SNAPSHOT",
                "1.0.1",
                "1.1-SNAPSHOT",
                "1.1");

        assertSequence("1.0-alpha", "1.0", "1.0-1");
        assertSequence("1.0.alpha", "1.0", "1.0-1");
        assertSequence("1.0-alpha", "1.0", "1.0.1");
        assertSequence("1.0.alpha", "1.0", "1.0.1");
    }

    @Test
    void testMinimumSegment() {
        assertOrder(X_LT_Y, "1.min", "1.0-alpha-1");
        assertOrder(X_LT_Y, "1.min", "1.0-SNAPSHOT");
        assertOrder(X_LT_Y, "1.min", "1.0");
        assertOrder(X_LT_Y, "1.min", "1.9999999999");

        assertOrder(X_EQ_Y, "1.min", "1.MIN");

        assertOrder(X_GT_Y, "1.min", "0.99999");
        assertOrder(X_GT_Y, "1.min", "0.max");
    }

    @Test
    void testMaximumSegment() {
        assertOrder(X_GT_Y, "1.max", "1.0-alpha-1");
        assertOrder(X_GT_Y, "1.max", "1.0-SNAPSHOT");
        assertOrder(X_GT_Y, "1.max", "1.0");
        assertOrder(X_GT_Y, "1.max", "1.9999999999");

        assertOrder(X_EQ_Y, "1.max", "1.MAX");

        assertOrder(X_LT_Y, "1.max", "2.0-alpha-1");
        assertOrder(X_LT_Y, "1.max", "2.min");
    }

    /**
     * UT for <a href="https://issues.apache.org/jira/browse/MRESOLVER-314">MRESOLVER-314</a>.
     *
     * Generates random UUID string based versions and tries to sort them. While this test is not as reliable
     * as {@link #testCompareUuidVersionStringStream()}, it covers broader range and in case it fails it records
     * the failed array, so we can investigate more.
     */
    @Test
    void testCompareUuidRandom() {
        for (int j = 0; j < 32; j++) {
            ArrayList<Version> versions = new ArrayList<>();
            for (int i = 0; i < 64; i++) {
                versions.add(newVersion(UUID.randomUUID().toString()));
            }
            try {
                Collections.sort(versions);
            } catch (Exception e) {
                e.printStackTrace(System.err);
                System.err.println("The UUIDs used");
                System.err.println(versions.stream().map(Version::toString).collect(Collectors.joining("\n")));
                fail("unexpected exception");
            }
        }
    }

    /**
     * UT for <a href="https://issues.apache.org/jira/browse/MRESOLVER-314">MRESOLVER-314</a>.
     *
     * Works on known set that failed before fix, provided by {@link #uuidVersionStringStream()}.
     */
    @Test
    void testCompareUuidVersionStringStream() {
        // this operation below fails with IAEx if comparison is unstable
        uuidVersionStringStream().map(this::newVersion).sorted().collect(toList());
    }

    private Stream<String> uuidVersionStringStream() {
        return Stream.of(
                "e3f6b227-e09d-4461-a030-b8c1755834f7",
                "dfdf5e15-b047-4fee-94e5-3ddf6fe90a0c",
                "bcc15412-6817-4b64-acef-169d048626f6",
                "76093f07-ab1c-4cdd-ae92-9bb500ceed84",
                "7ca8dc9f-4e73-459b-8f30-06aa7972f486",
                "93fee46b-2715-4abd-877a-4197eb8601aa",
                "0379da36-84ee-4d06-9388-83d3aa6536b5",
                "4bb2c7a8-cf68-4ca5-8024-72dc93506da9",
                "9dcc4cd1-34d2-4499-8dab-3ef8bca9680d",
                "ea53d552-83ab-4f7d-852d-98951201083d",
                "0bc420d2-4089-468b-bc54-0a4e2835feed",
                "318d2433-fe40-4f28-9f3a-4e3d66d9b5fb",
                "447b456c-81a4-4f24-9d2e-e5091c39cd19",
                "85741f6e-26fe-40d0-a73a-283315409ab2",
                "3165b9b2-9f8e-4117-ac70-87056eb45745",
                "9d534bf3-a3b0-4a19-9809-670934c10752",
                "86d78bba-d84e-4349-aea6-850721e78188",
                "06392b8c-e26c-4a83-8ec2-085415bc513d",
                "1fb13754-90be-42cb-bc7f-9b9211494e92",
                "3018965c-3330-402a-8075-caa7613ec4fa",
                "7ecc912b-4938-4411-895e-8ca7cf22ce02",
                "6580ada2-4764-45a2-9789-98217d7cf5b6",
                "be9d0de4-4ba7-4fdd-8f76-cb579168c549",
                "7a8236d6-6bec-4176-b6a1-f869c02183c3",
                "089f4195-881c-4f9e-8bc1-124531dee977",
                "46ffda62-768a-4864-9581-cc75eafe1a67",
                "1d6226f6-dacc-42a9-bd88-7aab1f59df74",
                "0948ed55-c25e-4319-9801-5f817bac09b5",
                "2fd52f5e-b856-47ad-9e58-45c1d0ba437b",
                "6c325bd0-ac6b-4391-a5c5-caa160972fa2",
                "d213f6be-f56b-42d2-abda-4300742e0add",
                "efaae115-cc21-4b2e-a150-fb4e0d807736",
                "30f872e8-9cb5-4b22-b65c-6819ca7a14ba",
                "d8e5fb54-6e90-4f74-adb3-451abfbe76a8",
                "b47d62b8-9256-47a1-8e21-21ba9639c212",
                "b25da555-e1f7-4bc5-92fe-4c895d9c70d8",
                "088f0de7-5973-4c10-a7ff-9f3cd7718572",
                "b161de76-e5d5-4224-883b-a749b147d63d",
                "19b7de96-09fa-4276-843d-c0fbdaf07767",
                "e0503f73-33fd-4f9c-812f-8cae3a128c28",
                "b8c57488-a42c-43ed-bfb9-acd112d6b68f",
                "25997299-0825-4c9b-b0ed-75f935c63fd7",
                "2b2e2fcd-3988-45af-855b-7646c0cdbfb5",
                "4e6e16b9-2ae4-4593-b907-1febaf3988dc",
                "ac8bd519-7fd4-4b85-8154-9dbb87f6cd4f",
                "61473b39-b620-468b-abcf-16fe6adfd5cb",
                "18e7a548-3f0b-492b-bc19-dce3eec736fa",
                "c4d82839-3c46-4eff-b10c-ec0b5bcc600b",
                "48f6e90f-924b-4859-9763-3ffe661f5af6",
                "48852d79-ba23-475e-b675-a413b989a2a7",
                "f7ee0915-ff00-4404-9e9a-6e753d5ff767",
                "d6462359-a4e2-45ab-aedc-3b1849b0e6ca",
                "e66228de-d1ed-4973-a108-c181d5059fdb",
                "d49672a7-177d-475d-aad0-aab0ff4a11b7",
                "bfa9337a-0489-4cba-b2db-e0d9d2424e4f",
                "dc9bbe34-3c54-4c0f-a3cd-00e96604ae23",
                "a8119cf1-9694-4b24-923a-3fc729b5f809",
                "5d29cf45-3b9c-4697-85b8-86c81c6ec0c9",
                "e3dcb4c2-a867-40f7-a3b1-fb1058a041e5",
                "ae240754-2ea2-409a-a92c-648fc7a7b70b",
                "8c187383-d59b-4e49-8dfd-98aa5f01925a",
                "9b100ee6-71ed-4746-92c2-b5fb02af7ebd",
                "f95e94f7-2443-4b2f-a10d-059d8d224dd9",
                "b558af80-78bc-43c7-b916-d635a23cc4b5");
    }
}
