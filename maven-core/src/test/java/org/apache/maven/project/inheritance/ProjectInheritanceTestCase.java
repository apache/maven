/*
 * CopyrightPlugin (c) 2004 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.project.inheritance;

import org.apache.maven.MavenTestCase;
import org.apache.maven.project.MavenProjectBuilder;

import java.io.File;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public abstract class ProjectInheritanceTestCase
    extends MavenTestCase
{
    protected MavenProjectBuilder projectBuilder;

    protected String getTestSeries()
    {
        String className = getClass().getPackage().getName();

        return className.substring( className.lastIndexOf( "." ) + 1 );
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();

        projectBuilder = (MavenProjectBuilder) lookup( MavenProjectBuilder.ROLE );

        System.setProperty( "maven.repo.local", getLocalRepository().getAbsolutePath() );
    }

    private File getLocalRepository()
    {
        return getTestFile( "src/test/resources/inheritance-repo/" + getTestSeries() );
    }

    protected File projectFile( String name )
    {
        return new File( getLocalRepository(), "/maven/poms/" + name + "-1.0.pom" );
    }
}
