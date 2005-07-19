package org.apache.maven.artifact.versioning;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import junit.framework.TestCase;

import java.util.List;

/**
 * Tests version range construction.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class VersionRangeTest
    extends TestCase
{
    private static final String CHECK_NUM_RESTRICTIONS = "check number of restrictions";

    private static final String CHECK_UPPER_BOUND = "check upper bound";

    private static final String CHECK_UPPER_BOUND_INCLUSIVE = "check upper bound is inclusive";

    private static final String CHECK_LOWER_BOUND = "check lower bound";

    private static final String CHECK_LOWER_BOUND_INCLUSIVE = "check lower bound is inclusive";

    private static final String CHECK_VERSION_RECOMMENDATION = "check version recommended";

    public void testRange()
        throws InvalidVersionSpecificationException
    {
        VersionRange range = VersionRange.createFromVersionSpec( "(,1.0]" );
        List restrictions = range.getRestrictions();
        assertEquals( CHECK_NUM_RESTRICTIONS, 1, restrictions.size() );
        Restriction restriction = (Restriction) restrictions.get( 0 );
        assertNull( CHECK_LOWER_BOUND, restriction.getLowerBound() );
        assertFalse( CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive() );
        assertEquals( CHECK_UPPER_BOUND, "1.0", restriction.getUpperBound() );
        assertTrue( CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive() );
        assertNull( CHECK_VERSION_RECOMMENDATION, range.getRecommendedVersion() );

        range = VersionRange.createFromVersionSpec( "1.0" );
        restrictions = range.getRestrictions();
        assertEquals( CHECK_NUM_RESTRICTIONS, 0, restrictions.size() );
        assertEquals( CHECK_VERSION_RECOMMENDATION, "1.0", range.getRecommendedVersion() );

        range = VersionRange.createFromVersionSpec( "[1.0]" );
        restrictions = range.getRestrictions();
        assertEquals( CHECK_NUM_RESTRICTIONS, 1, restrictions.size() );
        restriction = (Restriction) restrictions.get( 0 );
        assertEquals( CHECK_LOWER_BOUND, "1.0", restriction.getLowerBound() );
        assertTrue( CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive() );
        assertEquals( CHECK_UPPER_BOUND, "1.0", restriction.getUpperBound() );
        assertTrue( CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive() );
        assertNull( CHECK_VERSION_RECOMMENDATION, range.getRecommendedVersion() );

        range = VersionRange.createFromVersionSpec( "[1.2,1.3]" );
        restrictions = range.getRestrictions();
        assertEquals( CHECK_NUM_RESTRICTIONS, 1, restrictions.size() );
        restriction = (Restriction) restrictions.get( 0 );
        assertEquals( CHECK_LOWER_BOUND, "1.2", restriction.getLowerBound() );
        assertTrue( CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive() );
        assertEquals( CHECK_UPPER_BOUND, "1.3", restriction.getUpperBound() );
        assertTrue( CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive() );
        assertNull( CHECK_VERSION_RECOMMENDATION, range.getRecommendedVersion() );

        range = VersionRange.createFromVersionSpec( "[1.0,2.0)" );
        restrictions = range.getRestrictions();
        assertEquals( CHECK_NUM_RESTRICTIONS, 1, restrictions.size() );
        restriction = (Restriction) restrictions.get( 0 );
        assertEquals( CHECK_LOWER_BOUND, "1.0", restriction.getLowerBound() );
        assertTrue( CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive() );
        assertEquals( CHECK_UPPER_BOUND, "2.0", restriction.getUpperBound() );
        assertFalse( CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive() );
        assertNull( CHECK_VERSION_RECOMMENDATION, range.getRecommendedVersion() );

        range = VersionRange.createFromVersionSpec( "[1.5,)" );
        restrictions = range.getRestrictions();
        assertEquals( CHECK_NUM_RESTRICTIONS, 1, restrictions.size() );
        restriction = (Restriction) restrictions.get( 0 );
        assertEquals( CHECK_LOWER_BOUND, "1.5", restriction.getLowerBound() );
        assertTrue( CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive() );
        assertNull( CHECK_UPPER_BOUND, restriction.getUpperBound() );
        assertFalse( CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive() );
        assertNull( CHECK_VERSION_RECOMMENDATION, range.getRecommendedVersion() );

        range = VersionRange.createFromVersionSpec( "(,1.0],[1.2,)" );
        restrictions = range.getRestrictions();
        assertEquals( CHECK_NUM_RESTRICTIONS, 2, restrictions.size() );
        restriction = (Restriction) restrictions.get( 0 );
        assertNull( CHECK_LOWER_BOUND, restriction.getLowerBound() );
        assertFalse( CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive() );
        assertEquals( CHECK_UPPER_BOUND, "1.0", restriction.getUpperBound() );
        assertTrue( CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive() );
        assertNull( CHECK_VERSION_RECOMMENDATION, range.getRecommendedVersion() );
        restriction = (Restriction) restrictions.get( 1 );
        assertEquals( CHECK_LOWER_BOUND, "1.2", restriction.getLowerBound() );
        assertTrue( CHECK_LOWER_BOUND_INCLUSIVE, restriction.isLowerBoundInclusive() );
        assertNull( CHECK_UPPER_BOUND, restriction.getUpperBound() );
        assertFalse( CHECK_UPPER_BOUND_INCLUSIVE, restriction.isUpperBoundInclusive() );
        assertNull( CHECK_VERSION_RECOMMENDATION, range.getRecommendedVersion() );
    }

    public void testInvalidRanges()
    {
        checkInvalidRange( "(1.0)" );
        checkInvalidRange( "[1.0)" );
        checkInvalidRange( "(1.0]" );
        checkInvalidRange( "(1.0,1.0]" );
        checkInvalidRange( "[1.0,1.0)" );
        checkInvalidRange( "(1.0,1.0)" );
        checkInvalidRange( "[1.0,1.2),1.3" );
/* TODO: not testing this presently
        // overlap
        checkInvalidRange( "[1.0,1.2),(1.1,1.3]" );
        // overlap
        checkInvalidRange( "[1.1,1.3),(1.0,1.2]" );
*/
    }

    private void checkInvalidRange( String version )
    {
        try
        {
            VersionRange.createFromVersionSpec( version );
            fail( "Version " + version + " should have failed to construct" );
        }
        catch ( InvalidVersionSpecificationException expected )
        {
            // expected
        }
    }
}
