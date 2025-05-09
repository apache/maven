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

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

/**
 * Tests version range construction.
 *
 */
class VersionRangeTest {
    private static final String CHECK_NUM_RESTRICTIONS = "check number of restrictions";

    private static final String CHECK_UPPER_BOUND = "check upper bound";

    private static final String CHECK_UPPER_BOUND_INCLUSIVE = "check upper bound is inclusive";

    private static final String CHECK_LOWER_BOUND = "check lower bound";

    private static final String CHECK_LOWER_BOUND_INCLUSIVE = "check lower bound is inclusive";

    private static final String CHECK_VERSION_RECOMMENDATION = "check version recommended";

    private static final String CHECK_SELECTED_VERSION_KNOWN = "check selected version known";

    private static final String CHECK_SELECTED_VERSION = "check selected version";

    @Test
    void range() throws InvalidVersionSpecificationException, OverConstrainedVersionException {
        Artifact artifact = null;

        VersionRange range = VersionRange.createFromVersionSpec("(,1.0]");
        List<Restriction> restrictions = range.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        Restriction restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound()).as(CHECK_LOWER_BOUND).isNull();
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isFalse();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.0");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();
        assertThat(range.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        assertThat(range.isSelectedVersionKnown(artifact)).as(CHECK_SELECTED_VERSION_KNOWN).isFalse();
        assertThat(range.getSelectedVersion(artifact)).as(CHECK_SELECTED_VERSION).isNull();

        range = VersionRange.createFromVersionSpec("1.0");
        assertThat(range.getRecommendedVersion().toString()).as(CHECK_VERSION_RECOMMENDATION).isEqualTo("1.0");
        restrictions = range.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound()).as(CHECK_LOWER_BOUND).isNull();
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isFalse();
        assertThat(restriction.getUpperBound()).as(CHECK_UPPER_BOUND).isNull();
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isFalse();
        assertThat(range.isSelectedVersionKnown(artifact)).as(CHECK_SELECTED_VERSION_KNOWN).isTrue();
        assertThat(range.getSelectedVersion(artifact).toString()).as(CHECK_SELECTED_VERSION).isEqualTo("1.0");

        range = VersionRange.createFromVersionSpec("[1.0]");
        restrictions = range.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.0");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.0");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();
        assertThat(range.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        assertThat(range.isSelectedVersionKnown(artifact)).as(CHECK_SELECTED_VERSION_KNOWN).isFalse();
        assertThat(range.getSelectedVersion(artifact)).as(CHECK_SELECTED_VERSION).isNull();

        range = VersionRange.createFromVersionSpec("[1.2,1.3]");
        restrictions = range.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.2");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.3");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();
        assertThat(range.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        assertThat(range.isSelectedVersionKnown(artifact)).as(CHECK_SELECTED_VERSION_KNOWN).isFalse();
        assertThat(range.getSelectedVersion(artifact)).as(CHECK_SELECTED_VERSION).isNull();

        range = VersionRange.createFromVersionSpec("[1.0,2.0)");
        restrictions = range.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.0");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("2.0");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isFalse();
        assertThat(range.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        assertThat(range.isSelectedVersionKnown(artifact)).as(CHECK_SELECTED_VERSION_KNOWN).isFalse();
        assertThat(range.getSelectedVersion(artifact)).as(CHECK_SELECTED_VERSION).isNull();

        range = VersionRange.createFromVersionSpec("[1.5,)");
        restrictions = range.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.5");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound()).as(CHECK_UPPER_BOUND).isNull();
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isFalse();
        assertThat(range.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        assertThat(range.isSelectedVersionKnown(artifact)).as(CHECK_SELECTED_VERSION_KNOWN).isFalse();
        assertThat(range.getSelectedVersion(artifact)).as(CHECK_SELECTED_VERSION).isNull();

        range = VersionRange.createFromVersionSpec("(,1.0],[1.2,)");
        restrictions = range.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(2);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound()).as(CHECK_LOWER_BOUND).isNull();
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isFalse();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.0");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();
        assertThat(range.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restriction = restrictions.get(1);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.2");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound()).as(CHECK_UPPER_BOUND).isNull();
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isFalse();
        assertThat(range.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        assertThat(range.isSelectedVersionKnown(artifact)).as(CHECK_SELECTED_VERSION_KNOWN).isFalse();
        assertThat(range.getSelectedVersion(artifact)).as(CHECK_SELECTED_VERSION).isNull();

        range = VersionRange.createFromVersionSpec("[1.0,)");
        assertThat(range.containsVersion(new DefaultArtifactVersion("1.0-SNAPSHOT"))).isFalse();

        range = VersionRange.createFromVersionSpec("[1.0,1.1-SNAPSHOT]");
        assertThat(range.containsVersion(new DefaultArtifactVersion("1.1-SNAPSHOT"))).isTrue();

        range = VersionRange.createFromVersionSpec("[5.0.9.0,5.0.10.0)");
        assertThat(range.containsVersion(new DefaultArtifactVersion("5.0.9.0"))).isTrue();
    }

    @Test
    void sameUpperAndLowerBoundRoundtrip() throws InvalidVersionSpecificationException {
        VersionRange range = VersionRange.createFromVersionSpec("[1.0]");
        VersionRange range2 = VersionRange.createFromVersionSpec(range.toString());
        assertThat(range2).isEqualTo(range);
    }

    @Test
    void invalidRanges() {
        checkInvalidRange("(1.0)");
        checkInvalidRange("[1.0)");
        checkInvalidRange("(1.0]");
        checkInvalidRange("(1.0,1.0]");
        checkInvalidRange("[1.0,1.0)");
        checkInvalidRange("(1.0,1.0)");
        checkInvalidRange("[1.1,1.0]");
        checkInvalidRange("[1.0,1.2),1.3");
        // overlap
        checkInvalidRange("[1.0,1.2),(1.1,1.3]");
        // overlap
        checkInvalidRange("[1.1,1.3),(1.0,1.2]");
        // ordering
        checkInvalidRange("(1.1,1.2],[1.0,1.1)");
    }

    @Test
    @SuppressWarnings("checkstyle:MethodLength")
    void intersections() throws InvalidVersionSpecificationException {
        VersionRange range1 = VersionRange.createFromVersionSpec("1.0");
        VersionRange range2 = VersionRange.createFromVersionSpec("1.1");
        VersionRange mergedRange = range1.restrict(range2);
        // TODO current policy is to retain the original version - is this correct, do we need strategies or is that
        // handled elsewhere?
        //        assertEquals( "1.1", mergedRange.getRecommendedVersion().toString(), CHECK_VERSION_RECOMMENDATION );
        assertThat(mergedRange.getRecommendedVersion().toString()).as(CHECK_VERSION_RECOMMENDATION).isEqualTo("1.0");
        List<Restriction> restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        Restriction restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound()).as(CHECK_LOWER_BOUND).isNull();
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isFalse();
        assertThat(restriction.getUpperBound()).as(CHECK_UPPER_BOUND).isNull();
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isFalse();

        mergedRange = range2.restrict(range1);
        assertThat(mergedRange.getRecommendedVersion().toString()).as(CHECK_VERSION_RECOMMENDATION).isEqualTo("1.1");
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound()).as(CHECK_LOWER_BOUND).isNull();
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isFalse();
        assertThat(restriction.getUpperBound()).as(CHECK_UPPER_BOUND).isNull();
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isFalse();

        // TODO test reversed restrictions on all below
        range1 = VersionRange.createFromVersionSpec("[1.0,)");
        range2 = VersionRange.createFromVersionSpec("1.1");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion().toString()).as(CHECK_VERSION_RECOMMENDATION).isEqualTo("1.1");
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.0");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound()).as(CHECK_UPPER_BOUND).isNull();
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isFalse();

        range1 = VersionRange.createFromVersionSpec("[1.1,)");
        range2 = VersionRange.createFromVersionSpec("1.1");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion().toString()).as(CHECK_VERSION_RECOMMENDATION).isEqualTo("1.1");
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.1");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound()).as(CHECK_UPPER_BOUND).isNull();
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isFalse();

        range1 = VersionRange.createFromVersionSpec("[1.1]");
        range2 = VersionRange.createFromVersionSpec("1.1");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion().toString()).as(CHECK_VERSION_RECOMMENDATION).isEqualTo("1.1");
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.1");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getLowerBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.1");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();

        range1 = VersionRange.createFromVersionSpec("(1.1,)");
        range2 = VersionRange.createFromVersionSpec("1.1");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.1");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isFalse();
        assertThat(restriction.getUpperBound()).as(CHECK_UPPER_BOUND).isNull();
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isFalse();

        range1 = VersionRange.createFromVersionSpec("[1.2,)");
        range2 = VersionRange.createFromVersionSpec("1.1");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.2");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound()).as(CHECK_UPPER_BOUND).isNull();
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isFalse();

        range1 = VersionRange.createFromVersionSpec("(,1.2]");
        range2 = VersionRange.createFromVersionSpec("1.1");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion().toString()).as(CHECK_VERSION_RECOMMENDATION).isEqualTo("1.1");
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound()).as(CHECK_LOWER_BOUND).isNull();
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isFalse();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.2");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();

        range1 = VersionRange.createFromVersionSpec("(,1.1]");
        range2 = VersionRange.createFromVersionSpec("1.1");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion().toString()).as(CHECK_VERSION_RECOMMENDATION).isEqualTo("1.1");
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound()).as(CHECK_LOWER_BOUND).isNull();
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isFalse();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.1");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();

        range1 = VersionRange.createFromVersionSpec("(,1.1)");
        range2 = VersionRange.createFromVersionSpec("1.1");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound()).as(CHECK_LOWER_BOUND).isNull();
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isFalse();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.1");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isFalse();

        range1 = VersionRange.createFromVersionSpec("(,1.0]");
        range2 = VersionRange.createFromVersionSpec("1.1");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound()).as(CHECK_LOWER_BOUND).isNull();
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isFalse();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.0");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();

        range1 = VersionRange.createFromVersionSpec("(,1.0], [1.1,)");
        range2 = VersionRange.createFromVersionSpec("1.2");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion().toString()).as(CHECK_VERSION_RECOMMENDATION).isEqualTo("1.2");
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(2);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound()).as(CHECK_LOWER_BOUND).isNull();
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isFalse();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.0");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();
        restriction = restrictions.get(1);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.1");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound()).as(CHECK_UPPER_BOUND).isNull();
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isFalse();

        range1 = VersionRange.createFromVersionSpec("(,1.0], [1.1,)");
        range2 = VersionRange.createFromVersionSpec("1.0.5");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(2);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound()).as(CHECK_LOWER_BOUND).isNull();
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isFalse();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.0");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();
        restriction = restrictions.get(1);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.1");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound()).as(CHECK_UPPER_BOUND).isNull();
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isFalse();

        range1 = VersionRange.createFromVersionSpec("(,1.1), (1.1,)");
        range2 = VersionRange.createFromVersionSpec("1.1");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(2);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound()).as(CHECK_LOWER_BOUND).isNull();
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isFalse();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.1");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isFalse();
        restriction = restrictions.get(1);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.1");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isFalse();
        assertThat(restriction.getUpperBound()).as(CHECK_UPPER_BOUND).isNull();
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isFalse();

        range1 = VersionRange.createFromVersionSpec("[1.1,1.3]");
        range2 = VersionRange.createFromVersionSpec("(1.1,)");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.1");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isFalse();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.3");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();

        range1 = VersionRange.createFromVersionSpec("(,1.3)");
        range2 = VersionRange.createFromVersionSpec("[1.2,1.3]");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.2");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.3");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isFalse();

        range1 = VersionRange.createFromVersionSpec("[1.1,1.3]");
        range2 = VersionRange.createFromVersionSpec("[1.2,)");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.2");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.3");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();

        range1 = VersionRange.createFromVersionSpec("(,1.3]");
        range2 = VersionRange.createFromVersionSpec("[1.2,1.4]");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.2");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.3");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();

        range1 = VersionRange.createFromVersionSpec("(1.2,1.3]");
        range2 = VersionRange.createFromVersionSpec("[1.1,1.4]");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.2");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isFalse();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.3");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();

        range1 = VersionRange.createFromVersionSpec("(1.2,1.3)");
        range2 = VersionRange.createFromVersionSpec("[1.1,1.4]");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.2");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isFalse();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.3");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isFalse();

        range1 = VersionRange.createFromVersionSpec("[1.2,1.3)");
        range2 = VersionRange.createFromVersionSpec("[1.1,1.4]");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.2");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.3");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isFalse();

        range1 = VersionRange.createFromVersionSpec("[1.0,1.1]");
        range2 = VersionRange.createFromVersionSpec("[1.1,1.4]");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.1");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.1");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();

        range1 = VersionRange.createFromVersionSpec("[1.0,1.1)");
        range2 = VersionRange.createFromVersionSpec("[1.1,1.4]");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(0);

        range1 = VersionRange.createFromVersionSpec("[1.0,1.2],[1.3,1.5]");
        range2 = VersionRange.createFromVersionSpec("[1.1]");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.1");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.1");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();

        range1 = VersionRange.createFromVersionSpec("[1.0,1.2],[1.3,1.5]");
        range2 = VersionRange.createFromVersionSpec("[1.4]");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.4");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.4");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();

        range1 = VersionRange.createFromVersionSpec("[1.0,1.2],[1.3,1.5]");
        range2 = VersionRange.createFromVersionSpec("[1.1,1.4]");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(2);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.1");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.2");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();
        restriction = restrictions.get(1);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.3");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.4");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();

        range1 = VersionRange.createFromVersionSpec("[1.0,1.2),(1.3,1.5]");
        range2 = VersionRange.createFromVersionSpec("[1.1,1.4]");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(2);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.1");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.2");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isFalse();
        restriction = restrictions.get(1);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.3");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isFalse();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.4");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();

        range1 = VersionRange.createFromVersionSpec("[1.0,1.2],[1.3,1.5]");
        range2 = VersionRange.createFromVersionSpec("(1.1,1.4)");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(2);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.1");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isFalse();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.2");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();
        restriction = restrictions.get(1);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.3");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.4");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isFalse();

        range1 = VersionRange.createFromVersionSpec("[1.0,1.2),(1.3,1.5]");
        range2 = VersionRange.createFromVersionSpec("(1.1,1.4)");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(2);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.1");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isFalse();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.2");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isFalse();
        restriction = restrictions.get(1);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.3");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isFalse();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.4");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isFalse();

        range1 = VersionRange.createFromVersionSpec("(,1.1),(1.4,)");
        range2 = VersionRange.createFromVersionSpec("[1.1,1.4]");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(0);

        range1 = VersionRange.createFromVersionSpec("(,1.1],[1.4,)");
        range2 = VersionRange.createFromVersionSpec("(1.1,1.4)");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(0);

        range1 = VersionRange.createFromVersionSpec("[,1.1],[1.4,]");
        range2 = VersionRange.createFromVersionSpec("[1.2,1.3]");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(0);

        range1 = VersionRange.createFromVersionSpec("[1.0,1.2],[1.3,1.5]");
        range2 = VersionRange.createFromVersionSpec("[1.1,1.4],[1.6,]");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(2);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.1");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.2");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();
        restriction = restrictions.get(1);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.3");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.4");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();

        range1 = VersionRange.createFromVersionSpec("[1.0,1.2],[1.3,1.5]");
        range2 = VersionRange.createFromVersionSpec("[1.1,1.4],[1.5,]");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(3);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.1");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.2");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();
        restriction = restrictions.get(1);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.3");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.4");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();
        restriction = restrictions.get(2);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.5");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.5");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();

        range1 = VersionRange.createFromVersionSpec("[1.0,1.2],[1.3,1.7]");
        range2 = VersionRange.createFromVersionSpec("[1.1,1.4],[1.5,1.6]");
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(3);
        restriction = restrictions.get(0);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.1");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.2");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();
        restriction = restrictions.get(1);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.3");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.4");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();
        restriction = restrictions.get(2);
        assertThat(restriction.getLowerBound().toString()).as(CHECK_LOWER_BOUND).isEqualTo("1.5");
        assertThat(restriction.isLowerBoundInclusive()).as(CHECK_LOWER_BOUND_INCLUSIVE).isTrue();
        assertThat(restriction.getUpperBound().toString()).as(CHECK_UPPER_BOUND).isEqualTo("1.6");
        assertThat(restriction.isUpperBoundInclusive()).as(CHECK_UPPER_BOUND_INCLUSIVE).isTrue();

        // test restricting empty sets
        range1 = VersionRange.createFromVersionSpec("[,1.1],[1.4,]");
        range2 = VersionRange.createFromVersionSpec("[1.2,1.3]");
        range1 = range1.restrict(range2);
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(0);

        range1 = VersionRange.createFromVersionSpec("[,1.1],[1.4,]");
        range2 = VersionRange.createFromVersionSpec("[1.2,1.3]");
        range2 = range1.restrict(range2);
        mergedRange = range1.restrict(range2);
        assertThat(mergedRange.getRecommendedVersion()).as(CHECK_VERSION_RECOMMENDATION).isNull();
        restrictions = mergedRange.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(0);
    }

    @Test
    void releaseRangeBoundsContainsSnapshots() throws InvalidVersionSpecificationException {
        VersionRange range = VersionRange.createFromVersionSpec("[1.0,1.2]");

        assertThat(range.containsVersion(new DefaultArtifactVersion("1.1-SNAPSHOT"))).isTrue();
        assertThat(range.containsVersion(new DefaultArtifactVersion("1.2-SNAPSHOT"))).isTrue();
        assertThat(range.containsVersion(new DefaultArtifactVersion("1.0-SNAPSHOT"))).isFalse();
    }

    @Test
    void snapshotRangeBoundsCanContainSnapshots() throws InvalidVersionSpecificationException {
        VersionRange range = VersionRange.createFromVersionSpec("[1.0,1.2-SNAPSHOT]");

        assertThat(range.containsVersion(new DefaultArtifactVersion("1.1-SNAPSHOT"))).isTrue();
        assertThat(range.containsVersion(new DefaultArtifactVersion("1.2-SNAPSHOT"))).isTrue();

        range = VersionRange.createFromVersionSpec("[1.0-SNAPSHOT,1.2]");

        assertThat(range.containsVersion(new DefaultArtifactVersion("1.0-SNAPSHOT"))).isTrue();
        assertThat(range.containsVersion(new DefaultArtifactVersion("1.1-SNAPSHOT"))).isTrue();
    }

    @Test
    void snapshotSoftVersionCanContainSnapshot() throws InvalidVersionSpecificationException {
        VersionRange range = VersionRange.createFromVersionSpec("1.0-SNAPSHOT");

        assertThat(range.containsVersion(new DefaultArtifactVersion("1.0-SNAPSHOT"))).isTrue();
    }

    private void checkInvalidRange(String version) {
        assertThatExceptionOfType(InvalidVersionSpecificationException.class).as("Version " + version + " should have failed to construct").isThrownBy(() -> VersionRange.createFromVersionSpec(version));
    }

    @Test
    void contains() throws InvalidVersionSpecificationException {
        ArtifactVersion actualVersion = new DefaultArtifactVersion("2.0.5");
        assertThat(enforceVersion("2.0.5", actualVersion)).isTrue();
        assertThat(enforceVersion("2.0.4", actualVersion)).isTrue();
        assertThat(enforceVersion("[2.0.5]", actualVersion)).isTrue();
        assertThat(enforceVersion("[2.0.6,)", actualVersion)).isFalse();
        assertThat(enforceVersion("[2.0.6]", actualVersion)).isFalse();
        assertThat(enforceVersion("[2.0,2.1]", actualVersion)).isTrue();
        assertThat(enforceVersion("[2.0,2.0.3]", actualVersion)).isFalse();
        assertThat(enforceVersion("[2.0,2.0.5]", actualVersion)).isTrue();
        assertThat(enforceVersion("[2.0,2.0.5)", actualVersion)).isFalse();
    }

    public boolean enforceVersion(String requiredVersionRange, ArtifactVersion actualVersion)
            throws InvalidVersionSpecificationException {
        VersionRange vr = VersionRange.createFromVersionSpec(requiredVersionRange);

        return vr.containsVersion(actualVersion);
    }

    @Test
    void cache() throws InvalidVersionSpecificationException {
        VersionRange range = VersionRange.createFromVersionSpec("[1.0,1.2]");
        assertThat(VersionRange.createFromVersionSpec("[1.0,1.2]")).isSameAs(range); // same instance from spec cache

        VersionRange spec = VersionRange.createFromVersionSpec("1.0");
        assertThat(VersionRange.createFromVersionSpec("1.0")).isSameAs(spec); // same instance from spec cache
        List<Restriction> restrictions = spec.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(1);

        VersionRange version = VersionRange.createFromVersion("1.0");
        assertThat(VersionRange.createFromVersion("1.0")).isSameAs(version); // same instance from version cache
        restrictions = version.getRestrictions();
        assertThat(restrictions.size()).as(CHECK_NUM_RESTRICTIONS).isEqualTo(0);

        assertThat(version).as("check !VersionRange.createFromVersionSpec(x).equals(VersionRange.createFromVersion(x))").isNotEqualTo(spec);
    }
}
