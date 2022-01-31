package org.apache.maven.artifact.versioning;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests version range construction.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class VersionRangeTest
{
    private static final String CHECK_NUM_RESTRICTIONS = "check number of restrictions";

    private static final String CHECK_UPPER_BOUND = "check upper bound";

    private static final String CHECK_UPPER_BOUND_INCLUSIVE = "check upper bound is inclusive";

    private static final String CHECK_LOWER_BOUND = "check lower bound";

    private static final String CHECK_LOWER_BOUND_INCLUSIVE = "check lower bound is inclusive";

    private static final String CHECK_VERSION_RECOMMENDATION = "check version recommended";

    private static final String CHECK_SELECTED_VERSION_KNOWN = "check selected version known";

    private static final String CHECK_SELECTED_VERSION = "check selected version";

    @Test
    public void testRange()
        throws InvalidVersionSpecificationException, OverConstrainedVersionException
    {
        Artifact artifact = null;

        VersionRange range = VersionRange.createFromVersionSpec( "(,1.0]" );
        List<Restriction> restrictions = range.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        Restriction restriction = restrictions.get( 0 );
        assertNull( restriction.getLowerBound(), CHECK_LOWER_BOUND );
        assertFalse( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.0", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );
        assertNull( range.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        assertFalse( range.isSelectedVersionKnown( artifact ), CHECK_SELECTED_VERSION_KNOWN );
        assertNull( range.getSelectedVersion( artifact ), CHECK_SELECTED_VERSION );

        range = VersionRange.createFromVersionSpec( "1.0" );
        assertEquals( "1.0", range.getRecommendedVersion().toString(), CHECK_VERSION_RECOMMENDATION );
        restrictions = range.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertNull( restriction.getLowerBound(), CHECK_LOWER_BOUND );
        assertFalse( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertNull( restriction.getUpperBound(), CHECK_UPPER_BOUND );
        assertFalse( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );
        assertTrue( range.isSelectedVersionKnown( artifact ), CHECK_SELECTED_VERSION_KNOWN );
        assertEquals( "1.0", range.getSelectedVersion( artifact ).toString(), CHECK_SELECTED_VERSION );

        range = VersionRange.createFromVersionSpec( "[1.0]" );
        restrictions = range.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertEquals( "1.0", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.0", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );
        assertNull( range.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        assertFalse( range.isSelectedVersionKnown( artifact ), CHECK_SELECTED_VERSION_KNOWN );
        assertNull( range.getSelectedVersion( artifact ), CHECK_SELECTED_VERSION );

        range = VersionRange.createFromVersionSpec( "[1.2,1.3]" );
        restrictions = range.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertEquals( "1.2", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.3", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );
        assertNull( range.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        assertFalse( range.isSelectedVersionKnown( artifact ), CHECK_SELECTED_VERSION_KNOWN );
        assertNull( range.getSelectedVersion( artifact ), CHECK_SELECTED_VERSION );

        range = VersionRange.createFromVersionSpec( "[1.0,2.0)" );
        restrictions = range.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertEquals( "1.0", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "2.0", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertFalse( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );
        assertNull( range.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        assertFalse( range.isSelectedVersionKnown( artifact ), CHECK_SELECTED_VERSION_KNOWN );
        assertNull( range.getSelectedVersion( artifact ), CHECK_SELECTED_VERSION );

        range = VersionRange.createFromVersionSpec( "[1.5,)" );
        restrictions = range.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertEquals( "1.5", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertNull( restriction.getUpperBound(), CHECK_UPPER_BOUND );
        assertFalse( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );
        assertNull( range.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        assertFalse( range.isSelectedVersionKnown( artifact ), CHECK_SELECTED_VERSION_KNOWN );
        assertNull( range.getSelectedVersion( artifact ), CHECK_SELECTED_VERSION );

        range = VersionRange.createFromVersionSpec( "(,1.0],[1.2,)" );
        restrictions = range.getRestrictions();
        assertEquals( 2, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertNull( restriction.getLowerBound(), CHECK_LOWER_BOUND );
        assertFalse( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.0", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );
        assertNull( range.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restriction = restrictions.get( 1 );
        assertEquals( "1.2", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertNull( restriction.getUpperBound(), CHECK_UPPER_BOUND );
        assertFalse( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );
        assertNull( range.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        assertFalse( range.isSelectedVersionKnown( artifact ), CHECK_SELECTED_VERSION_KNOWN );
        assertNull( range.getSelectedVersion( artifact ), CHECK_SELECTED_VERSION );

        range = VersionRange.createFromVersionSpec( "[1.0,)" );
        assertFalse( range.containsVersion( new DefaultArtifactVersion( "1.0-SNAPSHOT" ) ) );

        range = VersionRange.createFromVersionSpec( "[1.0,1.1-SNAPSHOT]" );
        assertTrue( range.containsVersion( new DefaultArtifactVersion( "1.1-SNAPSHOT" ) ) );

        range = VersionRange.createFromVersionSpec( "[5.0.9.0,5.0.10.0)" );
        assertTrue( range.containsVersion( new DefaultArtifactVersion( "5.0.9.0" ) ) );
    }

    @Test
    public void testInvalidRanges()
    {
        checkInvalidRange( "(1.0)" );
        checkInvalidRange( "[1.0)" );
        checkInvalidRange( "(1.0]" );
        checkInvalidRange( "(1.0,1.0]" );
        checkInvalidRange( "[1.0,1.0)" );
        checkInvalidRange( "(1.0,1.0)" );
        checkInvalidRange( "[1.1,1.0]" );
        checkInvalidRange( "[1.0,1.2),1.3" );
        // overlap
        checkInvalidRange( "[1.0,1.2),(1.1,1.3]" );
        // overlap
        checkInvalidRange( "[1.1,1.3),(1.0,1.2]" );
        // ordering
        checkInvalidRange( "(1.1,1.2],[1.0,1.1)" );
    }

    @Test
    public void testIntersections()
        throws InvalidVersionSpecificationException
    {
        VersionRange range1 = VersionRange.createFromVersionSpec( "1.0" );
        VersionRange range2 = VersionRange.createFromVersionSpec( "1.1" );
        VersionRange mergedRange = range1.restrict( range2 );
        // TODO current policy is to retain the original version - is this correct, do we need strategies or is that handled elsewhere?
//        assertEquals( "1.1", mergedRange.getRecommendedVersion().toString(), CHECK_VERSION_RECOMMENDATION );
        assertEquals( "1.0", mergedRange.getRecommendedVersion().toString(), CHECK_VERSION_RECOMMENDATION );
        List<Restriction> restrictions = mergedRange.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        Restriction restriction = restrictions.get( 0 );
        assertNull( restriction.getLowerBound(), CHECK_LOWER_BOUND );
        assertFalse( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertNull( restriction.getUpperBound(), CHECK_UPPER_BOUND );
        assertFalse( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        mergedRange = range2.restrict( range1 );
        assertEquals( "1.1", mergedRange.getRecommendedVersion().toString(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertNull( restriction.getLowerBound(), CHECK_LOWER_BOUND );
        assertFalse( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertNull( restriction.getUpperBound(), CHECK_UPPER_BOUND );
        assertFalse( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        // TODO test reversed restrictions on all below
        range1 = VersionRange.createFromVersionSpec( "[1.0,)" );
        range2 = VersionRange.createFromVersionSpec( "1.1" );
        mergedRange = range1.restrict( range2 );
        assertEquals( "1.1", mergedRange.getRecommendedVersion().toString(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertEquals( "1.0", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertNull( restriction.getUpperBound(), CHECK_UPPER_BOUND );
        assertFalse( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "[1.1,)" );
        range2 = VersionRange.createFromVersionSpec( "1.1" );
        mergedRange = range1.restrict( range2 );
        assertEquals( "1.1", mergedRange.getRecommendedVersion().toString(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertEquals( "1.1", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertNull( restriction.getUpperBound(), CHECK_UPPER_BOUND );
        assertFalse( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "[1.1]" );
        range2 = VersionRange.createFromVersionSpec( "1.1" );
        mergedRange = range1.restrict( range2 );
        assertEquals( "1.1", mergedRange.getRecommendedVersion().toString(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertEquals( "1.1", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.1", restriction.getLowerBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "(1.1,)" );
        range2 = VersionRange.createFromVersionSpec( "1.1" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertEquals( "1.1", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertFalse( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertNull( restriction.getUpperBound(), CHECK_UPPER_BOUND );
        assertFalse( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "[1.2,)" );
        range2 = VersionRange.createFromVersionSpec( "1.1" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertEquals( "1.2", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertNull( restriction.getUpperBound(), CHECK_UPPER_BOUND );
        assertFalse( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "(,1.2]" );
        range2 = VersionRange.createFromVersionSpec( "1.1" );
        mergedRange = range1.restrict( range2 );
        assertEquals( "1.1", mergedRange.getRecommendedVersion().toString(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertNull( restriction.getLowerBound(), CHECK_LOWER_BOUND );
        assertFalse( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.2", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "(,1.1]" );
        range2 = VersionRange.createFromVersionSpec( "1.1" );
        mergedRange = range1.restrict( range2 );
        assertEquals( "1.1", mergedRange.getRecommendedVersion().toString(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertNull( restriction.getLowerBound(), CHECK_LOWER_BOUND );
        assertFalse( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.1", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "(,1.1)" );
        range2 = VersionRange.createFromVersionSpec( "1.1" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertNull( restriction.getLowerBound(), CHECK_LOWER_BOUND );
        assertFalse( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.1", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertFalse( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "(,1.0]" );
        range2 = VersionRange.createFromVersionSpec( "1.1" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertNull( restriction.getLowerBound(), CHECK_LOWER_BOUND );
        assertFalse( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.0", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "(,1.0], [1.1,)" );
        range2 = VersionRange.createFromVersionSpec( "1.2" );
        mergedRange = range1.restrict( range2 );
        assertEquals( "1.2", mergedRange.getRecommendedVersion().toString(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 2, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertNull( restriction.getLowerBound(), CHECK_LOWER_BOUND );
        assertFalse( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.0", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );
        restriction = restrictions.get( 1 );
        assertEquals( "1.1", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertNull( restriction.getUpperBound(), CHECK_UPPER_BOUND );
        assertFalse( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "(,1.0], [1.1,)" );
        range2 = VersionRange.createFromVersionSpec( "1.0.5" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 2, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertNull( restriction.getLowerBound(), CHECK_LOWER_BOUND );
        assertFalse( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.0", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );
        restriction = restrictions.get( 1 );
        assertEquals( "1.1", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertNull( restriction.getUpperBound(), CHECK_UPPER_BOUND );
        assertFalse( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "(,1.1), (1.1,)" );
        range2 = VersionRange.createFromVersionSpec( "1.1" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 2, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertNull( restriction.getLowerBound(), CHECK_LOWER_BOUND );
        assertFalse( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.1", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertFalse( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );
        restriction = restrictions.get( 1 );
        assertEquals( "1.1", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertFalse( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertNull( restriction.getUpperBound(), CHECK_UPPER_BOUND );
        assertFalse( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "[1.1,1.3]" );
        range2 = VersionRange.createFromVersionSpec( "(1.1,)" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertEquals( "1.1", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertFalse( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.3", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "(,1.3)" );
        range2 = VersionRange.createFromVersionSpec( "[1.2,1.3]" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertEquals( "1.2", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.3", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertFalse( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "[1.1,1.3]" );
        range2 = VersionRange.createFromVersionSpec( "[1.2,)" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertEquals( "1.2", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.3", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "(,1.3]" );
        range2 = VersionRange.createFromVersionSpec( "[1.2,1.4]" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertEquals( "1.2", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.3", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "(1.2,1.3]" );
        range2 = VersionRange.createFromVersionSpec( "[1.1,1.4]" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertEquals( "1.2", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertFalse( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.3", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "(1.2,1.3)" );
        range2 = VersionRange.createFromVersionSpec( "[1.1,1.4]" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertEquals( "1.2", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertFalse( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.3", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertFalse( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "[1.2,1.3)" );
        range2 = VersionRange.createFromVersionSpec( "[1.1,1.4]" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertEquals( "1.2", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.3", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertFalse( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "[1.0,1.1]" );
        range2 = VersionRange.createFromVersionSpec( "[1.1,1.4]" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertEquals( "1.1", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.1", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "[1.0,1.1)" );
        range2 = VersionRange.createFromVersionSpec( "[1.1,1.4]" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 0, restrictions.size(), CHECK_NUM_RESTRICTIONS );

        range1 = VersionRange.createFromVersionSpec( "[1.0,1.2],[1.3,1.5]" );
        range2 = VersionRange.createFromVersionSpec( "[1.1]" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertEquals( "1.1", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.1", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "[1.0,1.2],[1.3,1.5]" );
        range2 = VersionRange.createFromVersionSpec( "[1.4]" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertEquals( "1.4", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.4", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "[1.0,1.2],[1.3,1.5]" );
        range2 = VersionRange.createFromVersionSpec( "[1.1,1.4]" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 2, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertEquals( "1.1", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.2", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );
        restriction = restrictions.get( 1 );
        assertEquals( "1.3", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.4", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "[1.0,1.2),(1.3,1.5]" );
        range2 = VersionRange.createFromVersionSpec( "[1.1,1.4]" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 2, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertEquals( "1.1", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.2", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertFalse( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );
        restriction = restrictions.get( 1 );
        assertEquals( "1.3", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertFalse( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.4", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "[1.0,1.2],[1.3,1.5]" );
        range2 = VersionRange.createFromVersionSpec( "(1.1,1.4)" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 2, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertEquals( "1.1", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertFalse( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.2", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );
        restriction = restrictions.get( 1 );
        assertEquals( "1.3", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.4", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertFalse( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "[1.0,1.2),(1.3,1.5]" );
        range2 = VersionRange.createFromVersionSpec( "(1.1,1.4)" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 2, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertEquals( "1.1", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertFalse( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.2", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertFalse( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );
        restriction = restrictions.get( 1 );
        assertEquals( "1.3", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertFalse( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.4", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertFalse( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "(,1.1),(1.4,)" );
        range2 = VersionRange.createFromVersionSpec( "[1.1,1.4]" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 0, restrictions.size(), CHECK_NUM_RESTRICTIONS );

        range1 = VersionRange.createFromVersionSpec( "(,1.1],[1.4,)" );
        range2 = VersionRange.createFromVersionSpec( "(1.1,1.4)" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 0, restrictions.size(), CHECK_NUM_RESTRICTIONS );

        range1 = VersionRange.createFromVersionSpec( "[,1.1],[1.4,]" );
        range2 = VersionRange.createFromVersionSpec( "[1.2,1.3]" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 0, restrictions.size(), CHECK_NUM_RESTRICTIONS );

        range1 = VersionRange.createFromVersionSpec( "[1.0,1.2],[1.3,1.5]" );
        range2 = VersionRange.createFromVersionSpec( "[1.1,1.4],[1.6,]" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 2, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertEquals( "1.1", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.2", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );
        restriction = restrictions.get( 1 );
        assertEquals( "1.3", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.4", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "[1.0,1.2],[1.3,1.5]" );
        range2 = VersionRange.createFromVersionSpec( "[1.1,1.4],[1.5,]" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 3, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertEquals( "1.1", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.2", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );
        restriction = restrictions.get( 1 );
        assertEquals( "1.3", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.4", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );
        restriction = restrictions.get( 2 );
        assertEquals( "1.5", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.5", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        range1 = VersionRange.createFromVersionSpec( "[1.0,1.2],[1.3,1.7]" );
        range2 = VersionRange.createFromVersionSpec( "[1.1,1.4],[1.5,1.6]" );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 3, restrictions.size(), CHECK_NUM_RESTRICTIONS );
        restriction = restrictions.get( 0 );
        assertEquals( "1.1", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.2", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );
        restriction = restrictions.get( 1 );
        assertEquals( "1.3", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.4", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );
        restriction = restrictions.get( 2 );
        assertEquals( "1.5", restriction.getLowerBound().toString(), CHECK_LOWER_BOUND );
        assertTrue( restriction.isLowerBoundInclusive(), CHECK_LOWER_BOUND_INCLUSIVE );
        assertEquals( "1.6", restriction.getUpperBound().toString(), CHECK_UPPER_BOUND );
        assertTrue( restriction.isUpperBoundInclusive(), CHECK_UPPER_BOUND_INCLUSIVE );

        // test restricting empty sets
        range1 = VersionRange.createFromVersionSpec( "[,1.1],[1.4,]" );
        range2 = VersionRange.createFromVersionSpec( "[1.2,1.3]" );
        range1 = range1.restrict( range2 );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 0, restrictions.size(), CHECK_NUM_RESTRICTIONS );

        range1 = VersionRange.createFromVersionSpec( "[,1.1],[1.4,]" );
        range2 = VersionRange.createFromVersionSpec( "[1.2,1.3]" );
        range2 = range1.restrict( range2 );
        mergedRange = range1.restrict( range2 );
        assertNull( mergedRange.getRecommendedVersion(), CHECK_VERSION_RECOMMENDATION );
        restrictions = mergedRange.getRestrictions();
        assertEquals( 0, restrictions.size(), CHECK_NUM_RESTRICTIONS );
    }

    @Test
    public void testReleaseRangeBoundsContainsSnapshots()
        throws InvalidVersionSpecificationException
    {
        VersionRange range = VersionRange.createFromVersionSpec( "[1.0,1.2]" );

        assertTrue( range.containsVersion( new DefaultArtifactVersion( "1.1-SNAPSHOT" ) ) );
        assertTrue( range.containsVersion( new DefaultArtifactVersion( "1.2-SNAPSHOT" ) ) );
        assertFalse( range.containsVersion( new DefaultArtifactVersion( "1.0-SNAPSHOT" ) ) );
    }

    @Test
    public void testSnapshotRangeBoundsCanContainSnapshots()
        throws InvalidVersionSpecificationException
    {
        VersionRange range = VersionRange.createFromVersionSpec( "[1.0,1.2-SNAPSHOT]" );

        assertTrue( range.containsVersion( new DefaultArtifactVersion( "1.1-SNAPSHOT" ) ) );
        assertTrue( range.containsVersion( new DefaultArtifactVersion( "1.2-SNAPSHOT" ) ) );

        range = VersionRange.createFromVersionSpec( "[1.0-SNAPSHOT,1.2]" );

        assertTrue( range.containsVersion( new DefaultArtifactVersion( "1.0-SNAPSHOT" ) ) );
        assertTrue( range.containsVersion( new DefaultArtifactVersion( "1.1-SNAPSHOT" ) ) );
    }

    @Test
    public void testSnapshotSoftVersionCanContainSnapshot()
        throws InvalidVersionSpecificationException
    {
        VersionRange range = VersionRange.createFromVersionSpec( "1.0-SNAPSHOT" );

        assertTrue( range.containsVersion( new DefaultArtifactVersion( "1.0-SNAPSHOT" ) ) );
    }

    private void checkInvalidRange( String version )
    {
        assertThrows(
                InvalidVersionSpecificationException.class,
                () -> VersionRange.createFromVersionSpec( version ),
                "Version " + version + " should have failed to construct" );
    }

    @Test
    public void testContains() throws InvalidVersionSpecificationException
    {
        ArtifactVersion actualVersion = new DefaultArtifactVersion( "2.0.5" );
        assertTrue( enforceVersion( "2.0.5", actualVersion ) );
        assertTrue( enforceVersion( "2.0.4", actualVersion ) );
        assertTrue( enforceVersion( "[2.0.5]", actualVersion ) );
        assertFalse( enforceVersion( "[2.0.6,)", actualVersion ) );
        assertFalse( enforceVersion( "[2.0.6]", actualVersion ) );
        assertTrue( enforceVersion( "[2.0,2.1]", actualVersion ) );
        assertFalse( enforceVersion( "[2.0,2.0.3]", actualVersion ) );
        assertTrue( enforceVersion( "[2.0,2.0.5]", actualVersion ) );
        assertFalse( enforceVersion( "[2.0,2.0.5)", actualVersion ) );
    }

    public boolean enforceVersion( String requiredVersionRange, ArtifactVersion actualVersion )
        throws InvalidVersionSpecificationException
    {
        VersionRange vr = null;

        vr = VersionRange.createFromVersionSpec( requiredVersionRange );

        return vr.containsVersion( actualVersion );
    }

    @Test
    public void testOrder0()
    {
        // assertTrue( new DefaultArtifactVersion( "1.0-alpha10" ).compareTo( new DefaultArtifactVersion( "1.0-alpha1" ) ) > 0 );
    }

    @Test
    public void testCache()
        throws InvalidVersionSpecificationException
    {
        VersionRange range = VersionRange.createFromVersionSpec( "[1.0,1.2]" );
        assertSame( range, VersionRange.createFromVersionSpec( "[1.0,1.2]" ) ); // same instance from spec cache

        VersionRange spec = VersionRange.createFromVersionSpec( "1.0" );
        assertSame( spec, VersionRange.createFromVersionSpec( "1.0" ) ); // same instance from spec cache
        List<Restriction> restrictions = spec.getRestrictions();
        assertEquals( 1, restrictions.size(), CHECK_NUM_RESTRICTIONS );

        VersionRange version = VersionRange.createFromVersion( "1.0" );
        assertSame( version, VersionRange.createFromVersion( "1.0" ) ); // same instance from version cache
        restrictions = version.getRestrictions();
        assertEquals( 0, restrictions.size(), CHECK_NUM_RESTRICTIONS );

        assertFalse( spec.equals( version ), "check !VersionRange.createFromVersionSpec(x).equals(VersionRange.createFromVersion(x))" );
    }
}
