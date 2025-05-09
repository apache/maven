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
package org.apache.maven.artifact.versioning;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test ComparableVersion.
 */
@SuppressWarnings("unchecked")
class ComparableVersionTest {
    private ComparableVersion newComparable(String version) {
        ComparableVersion ret = new ComparableVersion(version);
        String canonical = ret.getCanonical();
        String parsedCanonical = new ComparableVersion(canonical).getCanonical();

        assertThat(parsedCanonical).as("canonical( " + version + " ) = " + canonical + " -> canonical: " + parsedCanonical).isEqualTo(canonical);

        return ret;
    }

    private static final String[] VERSIONS_QUALIFIER = {
        "1-alpha2snapshot",
        "1-alpha2",
        "1-alpha-123",
        "1-beta-2",
        "1-beta123",
        "1-m2",
        "1-m11",
        "1-rc",
        "1-cr2",
        "1-rc123",
        "1-SNAPSHOT",
        "1",
        "1-sp",
        "1-sp2",
        "1-sp123",
        "1-abc",
        "1-def",
        "1-pom-1",
        "1-1-snapshot",
        "1-1",
        "1-2",
        "1-123"
    };

    private static final String[] VERSIONS_NUMBER = {
        "2.0", "2.0.a", "2-1", "2.0.2", "2.0.123", "2.1.0", "2.1-a", "2.1b", "2.1-c", "2.1-1", "2.1.0.1", "2.2",
        "2.123", "11.a2", "11.a11", "11.b2", "11.b11", "11.m2", "11.m11", "11", "11.a", "11b", "11c", "11m"
    };

    private void checkVersionsOrder(String[] versions) {
        Comparable[] c = new Comparable[versions.length];
        for (int i = 0; i < versions.length; i++) {
            c[i] = newComparable(versions[i]);
        }

        for (int i = 1; i < versions.length; i++) {
            Comparable low = c[i - 1];
            for (int j = i; j < versions.length; j++) {
                Comparable high = c[j];
                assertThat(low.compareTo(high) < 0).as("expected " + low + " < " + high).isTrue();
                assertThat(high.compareTo(low) > 0).as("expected " + high + " > " + low).isTrue();
            }
        }
    }

    private void checkVersionsEqual(String v1, String v2) {
        Comparable c1 = newComparable(v1);
        Comparable c2 = newComparable(v2);
        assertThat(c1.compareTo(c2)).as("expected " + v1 + " == " + v2).isEqualTo(0);
        assertThat(c2.compareTo(c1)).as("expected " + v2 + " == " + v1).isEqualTo(0);
        assertThat(c2.hashCode()).as("expected same hashcode for " + v1 + " and " + v2).isEqualTo(c1.hashCode());
        assertThat(c2).as("expected " + v1 + ".equals( " + v2 + " )").isEqualTo(c1);
        assertThat(c1).as("expected " + v2 + ".equals( " + v1 + " )").isEqualTo(c2);
    }

    private void checkVersionsHaveSameOrder(String v1, String v2) {
        ComparableVersion c1 = new ComparableVersion(v1);
        ComparableVersion c2 = new ComparableVersion(v2);
        assertThat(c1.compareTo(c2)).as("expected " + v1 + " == " + v2).isEqualTo(0);
        assertThat(c2.compareTo(c1)).as("expected " + v2 + " == " + v1).isEqualTo(0);
    }

    private void checkVersionsArrayEqual(String[] array) {
        // compare against each other (including itself)
        for (int i = 0; i < array.length; ++i) {
            for (int j = i; j < array.length; ++j) {
                checkVersionsEqual(array[i], array[j]);
            }
        }
    }

    private void checkVersionsOrder(String v1, String v2) {
        Comparable c1 = newComparable(v1);
        Comparable c2 = newComparable(v2);
        assertThat(c1.compareTo(c2) < 0).as("expected " + v1 + " < " + v2).isTrue();
        assertThat(c2.compareTo(c1) > 0).as("expected " + v2 + " > " + v1).isTrue();
    }

    @Test
    void versionsQualifier() {
        checkVersionsOrder(VERSIONS_QUALIFIER);
    }

    @Test
    void versionsNumber() {
        checkVersionsOrder(VERSIONS_NUMBER);
    }

    @Test
    void versionsEqual() {
        newComparable("1.0-alpha");
        checkVersionsEqual("1", "1");
        checkVersionsEqual("1", "1.0");
        checkVersionsEqual("1", "1.0.0");
        checkVersionsEqual("1.0", "1.0.0");
        checkVersionsEqual("1", "1-0");
        checkVersionsEqual("1", "1.0-0");
        checkVersionsEqual("1.0", "1.0-0");
        // no separator between number and character
        checkVersionsEqual("1a", "1-a");
        checkVersionsEqual("1a", "1.0-a");
        checkVersionsEqual("1a", "1.0.0-a");
        checkVersionsEqual("1.0a", "1-a");
        checkVersionsEqual("1.0.0a", "1-a");
        checkVersionsEqual("1x", "1-x");
        checkVersionsEqual("1x", "1.0-x");
        checkVersionsEqual("1x", "1.0.0-x");
        checkVersionsEqual("1.0x", "1-x");
        checkVersionsEqual("1.0.0x", "1-x");
        checkVersionsEqual("1cr", "1rc");

        // special "aliases" a, b and m for alpha, beta and milestone
        checkVersionsEqual("1a1", "1-alpha-1");
        checkVersionsEqual("1b2", "1-beta-2");
        checkVersionsEqual("1m3", "1-milestone-3");

        // case insensitive
        checkVersionsEqual("1X", "1x");
        checkVersionsEqual("1A", "1a");
        checkVersionsEqual("1B", "1b");
        checkVersionsEqual("1M", "1m");
        checkVersionsEqual("1Cr", "1Rc");
        checkVersionsEqual("1cR", "1rC");
        checkVersionsEqual("1m3", "1Milestone3");
        checkVersionsEqual("1m3", "1MileStone3");
        checkVersionsEqual("1m3", "1MILESTONE3");
    }

    @Test
    void versionsHaveSameOrderButAreNotEqual() {
        checkVersionsHaveSameOrder("1ga", "1");
        checkVersionsHaveSameOrder("1release", "1");
        checkVersionsHaveSameOrder("1final", "1");
        checkVersionsHaveSameOrder("1Ga", "1");
        checkVersionsHaveSameOrder("1GA", "1");
        checkVersionsHaveSameOrder("1RELEASE", "1");
        checkVersionsHaveSameOrder("1release", "1");
        checkVersionsHaveSameOrder("1RELeaSE", "1");
        checkVersionsHaveSameOrder("1Final", "1");
        checkVersionsHaveSameOrder("1FinaL", "1");
        checkVersionsHaveSameOrder("1FINAL", "1");
    }

    @Test
    void versionComparing() {
        checkVersionsOrder("1", "2");
        checkVersionsOrder("1.5", "2");
        checkVersionsOrder("1", "2.5");
        checkVersionsOrder("1.0", "1.1");
        checkVersionsOrder("1.1", "1.2");
        checkVersionsOrder("1.0.0", "1.1");
        checkVersionsOrder("1.0.1", "1.1");
        checkVersionsOrder("1.1", "1.2.0");

        checkVersionsOrder("1.0-alpha-1", "1.0");
        checkVersionsOrder("1.0-alpha-1", "1.0-alpha-2");
        checkVersionsOrder("1.0-alpha-1", "1.0-beta-1");

        checkVersionsOrder("1.0-beta-1", "1.0-SNAPSHOT");
        checkVersionsOrder("1.0-SNAPSHOT", "1.0");
        checkVersionsOrder("1.0-alpha-1-SNAPSHOT", "1.0-alpha-1");

        checkVersionsOrder("1.0", "1.0-1");
        checkVersionsOrder("1.0-1", "1.0-2");
        checkVersionsOrder("1.0.0", "1.0-1");

        checkVersionsOrder("2.0-1", "2.0.1");
        checkVersionsOrder("2.0.1-klm", "2.0.1-lmn");
        checkVersionsOrder("2.0.1", "2.0.1-xyz");

        checkVersionsOrder("2.0.1", "2.0.1-123");
        checkVersionsOrder("2.0.1-xyz", "2.0.1-123");
    }

    @Test
    void leadingZeroes() {
        checkVersionsOrder("0.7", "2");
        checkVersionsOrder("0.2", "1.0.7");
    }

    @Test
    void digitGreaterThanNonAscii() {
        ComparableVersion c1 = new ComparableVersion("1");
        ComparableVersion c2 = new ComparableVersion("é");
        assertThat(c1.compareTo(c2) > 0).as("expected " + "1" + " > " + "\uD835\uDFE4").isTrue();
        assertThat(c2.compareTo(c1) < 0).as("expected " + "\uD835\uDFE4" + " < " + "1").isTrue();
    }

    @Test
    void digitGreaterThanNonBmpCharacters() {
        ComparableVersion c1 = new ComparableVersion("1");
        // MATHEMATICAL SANS-SERIF DIGIT TWO
        ComparableVersion c2 = new ComparableVersion("\uD835\uDFE4");
        assertThat(c1.compareTo(c2) > 0).as("expected " + "1" + " > " + "\uD835\uDFE4").isTrue();
        assertThat(c2.compareTo(c1) < 0).as("expected " + "\uD835\uDFE4" + " < " + "1").isTrue();
    }

    @Test
    void getCanonical() {
        // MNG-7700
        newComparable("0.x");
        newComparable("0-x");
        newComparable("0.rc");
        newComparable("0-1");

        ComparableVersion version = new ComparableVersion("0.x");
        assertThat(version.getCanonical()).isEqualTo("x");
        ComparableVersion version2 = new ComparableVersion("0.2");
        assertThat(version2.getCanonical()).isEqualTo("0.2");
    }

    @Test
    void lexicographicASCIISortOrder() { // Required by Semver 1.0
        ComparableVersion lower = new ComparableVersion("1.0.0-alpha1");
        ComparableVersion upper = new ComparableVersion("1.0.0-ALPHA1");
        // Lower case is equal to upper case. This is *NOT* what Semver 1.0
        // specifies. Here we are explicitly deviating from Semver 1.0.
        assertThat(upper.compareTo(lower)).as("expected 1.0.0-ALPHA1 == 1.0.0-alpha1").isEqualTo(0);
        assertThat(lower.compareTo(upper)).as("expected 1.0.0-alpha1 == 1.0.0-ALPHA1").isEqualTo(0);
    }

    @Test
    void compareLowerCaseToUpperCaseASCII() {
        ComparableVersion lower = new ComparableVersion("1.a");
        ComparableVersion upper = new ComparableVersion("1.A");
        // Lower case is equal to upper case
        assertThat(upper.compareTo(lower)).as("expected 1.A == 1.a").isEqualTo(0);
        assertThat(lower.compareTo(upper)).as("expected 1.a == 1.A").isEqualTo(0);
    }

    @Test
    void compareLowerCaseToUpperCaseNonASCII() {
        ComparableVersion lower = new ComparableVersion("1.é");
        ComparableVersion upper = new ComparableVersion("1.É");
        // Lower case is equal to upper case
        assertThat(upper.compareTo(lower)).as("expected 1.É < 1.é").isEqualTo(0);
        assertThat(lower.compareTo(upper)).as("expected 1.é > 1.É").isEqualTo(0);
    }

    @Test
    void compareDigitToLetter() {
        ComparableVersion seven = new ComparableVersion("7");
        ComparableVersion capitalJ = new ComparableVersion("J");
        ComparableVersion lowerCaseC = new ComparableVersion("c");
        // Digits are greater than letters
        assertThat(seven.compareTo(capitalJ) > 0).as("expected 7 > J").isTrue();
        assertThat(capitalJ.compareTo(seven) < 0).as("expected J < 1").isTrue();
        assertThat(seven.compareTo(lowerCaseC) > 0).as("expected 7 > c").isTrue();
        assertThat(lowerCaseC.compareTo(seven) < 0).as("expected c < 7").isTrue();
    }

    @Test
    void nonAsciiDigits() { // These should not be treated as digits.
        ComparableVersion asciiOne = new ComparableVersion("1");
        ComparableVersion arabicEight = new ComparableVersion("\u0668");
        ComparableVersion asciiNine = new ComparableVersion("9");
        assertThat(asciiOne.compareTo(arabicEight) > 0).as("expected " + "1" + " > " + "\u0668").isTrue();
        assertThat(arabicEight.compareTo(asciiOne) < 0).as("expected " + "\u0668" + " < " + "1").isTrue();
        assertThat(asciiNine.compareTo(arabicEight) > 0).as("expected " + "9" + " > " + "\u0668").isTrue();
        assertThat(arabicEight.compareTo(asciiNine) < 0).as("expected " + "\u0668" + " < " + "9").isTrue();
    }

    @Test
    void lexicographicOrder() {
        ComparableVersion aardvark = new ComparableVersion("aardvark");
        ComparableVersion zebra = new ComparableVersion("zebra");
        assertThat(zebra.compareTo(aardvark) > 0).isTrue();
        assertThat(aardvark.compareTo(zebra) < 0).isTrue();

        // Greek zebra
        ComparableVersion greek = new ComparableVersion("ζέβρα");
        assertThat(greek.compareTo(zebra) > 0).isTrue();
        assertThat(zebra.compareTo(greek) < 0).isTrue();
    }

    /**
     * Test <a href="https://issues.apache.org/jira/browse/MNG-5568">MNG-5568</a> edge case
     * which was showing transitive inconsistency: since A &gt; B and B &gt; C then we should have A &gt; C
     * otherwise sorting a list of ComparableVersions() will in some cases throw runtime exception;
     * see Netbeans issues <a href="https://netbeans.org/bugzilla/show_bug.cgi?id=240845">240845</a> and
     * <a href="https://netbeans.org/bugzilla/show_bug.cgi?id=226100">226100</a>
     */
    @Test
    void mng5568() {
        String a = "6.1.0";
        String b = "6.1.0rc3";
        String c = "6.1H.5-beta"; // this is the unusual version string, with 'H' in the middle

        checkVersionsOrder(b, a); // classical
        checkVersionsOrder(b, c); // now b < c, but before MNG-5568, we had b > c
        checkVersionsOrder(a, c);
    }

    /**
     * Test <a href="https://jira.apache.org/jira/browse/MNG-6572">MNG-6572</a> optimization.
     */
    @Test
    void mng6572() {
        String a = "20190126.230843"; // resembles a SNAPSHOT
        String b = "1234567890.12345"; // 10 digit number
        String c = "123456789012345.1H.5-beta"; // 15 digit number
        String d = "12345678901234567890.1H.5-beta"; // 20 digit number

        checkVersionsOrder(a, b);
        checkVersionsOrder(b, c);
        checkVersionsOrder(a, c);
        checkVersionsOrder(c, d);
        checkVersionsOrder(b, d);
        checkVersionsOrder(a, d);
    }

    /**
     * Test all versions are equal when starting with many leading zeroes regardless of string length
     * (related to MNG-6572 optimization)
     */
    @Test
    void versionEqualWithLeadingZeroes() {
        // versions with string lengths from 1 to 19
        String[] arr = new String[] {
            "0000000000000000001",
            "000000000000000001",
            "00000000000000001",
            "0000000000000001",
            "000000000000001",
            "00000000000001",
            "0000000000001",
            "000000000001",
            "00000000001",
            "0000000001",
            "000000001",
            "00000001",
            "0000001",
            "000001",
            "00001",
            "0001",
            "001",
            "01",
            "1"
        };

        checkVersionsArrayEqual(arr);
    }

    /**
     * Test all "0" versions are equal when starting with many leading zeroes regardless of string length
     * (related to MNG-6572 optimization)
     */
    @Test
    void versionZeroEqualWithLeadingZeroes() {
        // versions with string lengths from 1 to 19
        String[] arr = new String[] {
            "0000000000000000000",
            "000000000000000000",
            "00000000000000000",
            "0000000000000000",
            "000000000000000",
            "00000000000000",
            "0000000000000",
            "000000000000",
            "00000000000",
            "0000000000",
            "000000000",
            "00000000",
            "0000000",
            "000000",
            "00000",
            "0000",
            "000",
            "00",
            "0"
        };

        checkVersionsArrayEqual(arr);
    }

    /**
     * Test <a href="https://issues.apache.org/jira/browse/MNG-6964">MNG-6964</a> edge cases
     * for qualifiers that start with "-0.", which was showing A == C and B == C but A &lt; B.
     */
    @Test
    void mng6964() {
        String a = "1-0.alpha";
        String b = "1-0.beta";
        String c = "1";

        checkVersionsOrder(a, c); // Now a < c, but before MNG-6964 they were equal
        checkVersionsOrder(b, c); // Now b < c, but before MNG-6964 they were equal
        checkVersionsOrder(a, b); // Should still be true
    }

    @Test
    void localeIndependent() {
        Locale orig = Locale.getDefault();
        Locale[] locales = {Locale.ENGLISH, new Locale("tr"), Locale.getDefault()};
        try {
            for (Locale locale : locales) {
                Locale.setDefault(locale);
                checkVersionsEqual("1-abcdefghijklmnopqrstuvwxyz", "1-ABCDEFGHIJKLMNOPQRSTUVWXYZ");
            }
        } finally {
            Locale.setDefault(orig);
        }
    }

    @Test
    void reuse() {
        ComparableVersion c1 = new ComparableVersion("1");
        c1.parseVersion("2");

        Comparable<?> c2 = newComparable("2");

        assertThat(c2).as("reused instance should be equivalent to new instance").isEqualTo(c1);
    }

    /**
     * Test <a href="https://issues.apache.org/jira/browse/MNG-7644">MNG-7644</a> edge cases
     * 1.0.0.RC1 &lt; 1.0.0-RC2 and more generally:
     * 1.0.0.X1 &lt; 1.0.0-X2 for any string X
     */
    @Test
    void mng7644() {
        for (String x : new String[] {"abc", "alpha", "a", "beta", "b", "def", "milestone", "m", "RC"}) {
            // 1.0.0.X1 < 1.0.0-X2 for any string x
            checkVersionsOrder("1.0.0." + x + "1", "1.0.0-" + x + "2");
            // 2.0.X == 2-X == 2.0.0.X for any string x
            checkVersionsEqual("2-" + x, "2.0." + x); // previously ordered, now equals
            checkVersionsEqual("2-" + x, "2.0.0." + x); // previously ordered, now equals
            checkVersionsEqual("2.0." + x, "2.0.0." + x); // previously ordered, now equals
        }
    }

    @Test
    void mng7714() {
        ComparableVersion f = new ComparableVersion("1.0.final-redhat");
        ComparableVersion sp1 = new ComparableVersion("1.0-sp1-redhat");
        ComparableVersion sp2 = new ComparableVersion("1.0-sp-1-redhat");
        ComparableVersion sp3 = new ComparableVersion("1.0-sp.1-redhat");
        assertThat(f.compareTo(sp1) < 0).as("expected " + f + " < " + sp1).isTrue();
        assertThat(f.compareTo(sp2) < 0).as("expected " + f + " < " + sp2).isTrue();
        assertThat(f.compareTo(sp3) < 0).as("expected " + f + " < " + sp3).isTrue();
    }
}
