/*
 * CopyrightPlugin (c) 2004 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.project.inheritance.t01;

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.inheritance.ProjectInheritanceTestCase;

/**
 * A test which demonstrates maven's recursive inheritance where
 * we are testing to make sure that elements stated in a model are
 * not clobbered by the same elements elsewhere in the lineage. 
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class ProjectInheritanceTest
    extends ProjectInheritanceTestCase
{
    // ----------------------------------------------------------------------
    //
    // p4 inherits from p3
    // p3 inherits from p2
    // p2 inherits from p1
    // p1 inherits from p0
    // p0 inhertis from super model
    //
    // or we can show it graphically as:
    //
    // p4 ---> p3 ---> p2 ---> p1 ---> p0 --> super model
    //
    // ----------------------------------------------------------------------

    public void testProjectInheritance()
        throws Exception
    {
        // ----------------------------------------------------------------------
        // Check p0 value for org name
        // ----------------------------------------------------------------------

        MavenProject p0 = projectBuilder.build( projectFile( "p0" ) );

        assertEquals( "p0-org", p0.getOrganization().getName() );

        // ----------------------------------------------------------------------
        // Check p1 value for org name
        // ----------------------------------------------------------------------

        MavenProject p1 = projectBuilder.build( projectFile( "p1" ) );

        assertEquals( "p1-org", p1.getOrganization().getName() );

        // ----------------------------------------------------------------------
        // Check p2 value for org name
        // ----------------------------------------------------------------------

        MavenProject p2 = projectBuilder.build( projectFile( "p2" ) );

        assertEquals( "p2-org", p2.getOrganization().getName() );

        // ----------------------------------------------------------------------
        // Check p2 value for org name
        // ----------------------------------------------------------------------

        MavenProject p3 = projectBuilder.build( projectFile( "p3" ) );

        assertEquals( "p3-org", p3.getOrganization().getName() );

        // ----------------------------------------------------------------------
        // Check p4 value for org name
        // ----------------------------------------------------------------------

        MavenProject p4 = projectBuilder.build( projectFile( "p4" ) );

        assertEquals( "p4-org", p4.getOrganization().getName() );
    }
}
