/*
 * CopyrightPlugin (c) 2004 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.project.inheritance;

import org.apache.maven.MavenTestCase;
import org.apache.maven.artifact.repository.ArtifactRepository;
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
    }

    protected ArtifactRepository getLocalRepository()
    {
        ArtifactRepository r = new ArtifactRepository();

        String s = new File( basedir, "src/test/resources/inheritance-repo/" + getTestSeries() ).getPath();

        r.setUrl( "file://" + s );

        return r;
    }

    protected File projectFile( String name )
    {
        File f = new File( "src/test/resources/inheritance-repo/" + getTestSeries() + "/maven/poms", name + "-1.0.pom" );

        return new File( basedir, f.getPath() );
    }
}
