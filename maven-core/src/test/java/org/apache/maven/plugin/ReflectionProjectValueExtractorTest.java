/*
 * Copyright (c) 2004 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.plugin;

import java.io.File;
import java.util.List;

import org.apache.maven.MavenTestCase;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.util.introspection.ReflectionValueExtractor;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class ReflectionProjectValueExtractorTest
    extends MavenTestCase
{
    private MavenProject project;

    private MavenProjectBuilder builder;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        builder = (MavenProjectBuilder) lookup( MavenProjectBuilder.ROLE );

        File f =  getTestFile( "src/test/resources/pom.xml" );

        project = getProject( f );
    }

    public void testValueExtraction()
        throws Exception
    {
        // ----------------------------------------------------------------------
        // Top level values
        // ----------------------------------------------------------------------

        assertEquals( "4.0.0", ReflectionValueExtractor.evaluate( "project.modelVersion", project ) );

        assertEquals( "maven", ReflectionValueExtractor.evaluate( "project.groupId", project ) );

        assertEquals( "maven-core", ReflectionValueExtractor.evaluate( "project.artifactId", project ) );

        assertEquals( "Maven", ReflectionValueExtractor.evaluate( "project.name", project ) );

        assertEquals( "2.0-SNAPSHOT", ReflectionValueExtractor.evaluate( "project.version", project ) );

        // ----------------------------------------------------------------------
        // SCM
        // ----------------------------------------------------------------------

        assertEquals( "scm-connection", ReflectionValueExtractor.evaluate( "project.scm.connection", project ) );

        // ----------------------------------------------------------------------
        // Dependencies
        // ----------------------------------------------------------------------

        List dependencies = (List) ReflectionValueExtractor.evaluate( "project.dependencies", project );

        assertNotNull( dependencies );

        assertEquals( 2, dependencies.size() );

        // ----------------------------------------------------------------------
        // Build
        // ----------------------------------------------------------------------

        Build build = (Build) ReflectionValueExtractor.evaluate( "project.build", project );

        assertNotNull( build );
    }
}
