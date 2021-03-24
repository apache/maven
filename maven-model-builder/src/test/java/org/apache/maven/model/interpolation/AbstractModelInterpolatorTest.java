package org.apache.maven.model.interpolation;

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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Resource;
import org.apache.maven.model.Scm;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.SimpleProblemCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author jdcasey
 */
public abstract class AbstractModelInterpolatorTest
{
    private Properties context;

    @BeforeEach
    public void setUp()
    {
        context = new Properties();
        context.put( "basedir", "myBasedir" );
        context.put( "project.baseUri", "myBaseUri" );
    }


    protected void assertProblemFree( SimpleProblemCollector collector )
    {
        assertEquals( 0, collector.getErrors().size(), "Expected no errors" );
        assertEquals( 0, collector.getWarnings().size(), "Expected no warnings" );
        assertEquals( 0, collector.getFatals().size(), "Expected no fatals" );
    }

    protected void assertCollectorState( int numFatals, int numErrors, int numWarnings, SimpleProblemCollector collector )
    {
        assertEquals( numErrors, collector.getErrors().size(), "Errors" );
        assertEquals( numWarnings, collector.getWarnings().size(), "Warnings" );
        assertEquals( numFatals, collector.getFatals().size(), "Fatals" );
    }

    private ModelBuildingRequest createModelBuildingRequest( Properties p )
    {
        ModelBuildingRequest config = new DefaultModelBuildingRequest();
        if ( p != null )
        {
            config.setSystemProperties( p );
        }
        return config;
    }

    @Test
    public void testDefaultBuildTimestampFormatShouldFormatTimeIn24HourFormat()
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone( MavenBuildTimestamp.DEFAULT_BUILD_TIME_ZONE );
        cal.set( Calendar.HOUR, 12 );
        cal.set( Calendar.AM_PM, Calendar.AM );

        // just to make sure all the bases are covered...
        cal.set( Calendar.HOUR_OF_DAY, 0 );
        cal.set( Calendar.MINUTE, 16 );
        cal.set( Calendar.SECOND, 0 );
        cal.set( Calendar.YEAR, 1976 );
        cal.set( Calendar.MONTH, Calendar.NOVEMBER );
        cal.set( Calendar.DATE, 11 );

        Date firstTestDate = cal.getTime();

        cal.set( Calendar.HOUR, 11 );
        cal.set( Calendar.AM_PM, Calendar.PM );

        // just to make sure all the bases are covered...
        cal.set( Calendar.HOUR_OF_DAY, 23 );

        Date secondTestDate = cal.getTime();

        SimpleDateFormat format = new SimpleDateFormat( MavenBuildTimestamp.DEFAULT_BUILD_TIMESTAMP_FORMAT );
        format.setTimeZone( MavenBuildTimestamp.DEFAULT_BUILD_TIME_ZONE );
        assertEquals( "1976-11-11T00:16:00Z", format.format( firstTestDate ) );
        assertEquals( "1976-11-11T23:16:00Z", format.format( secondTestDate ) );
    }

    @Test
    public void testDefaultBuildTimestampFormatWithLocalTimeZoneMidnightRollover()
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone( TimeZone.getTimeZone( "Europe/Berlin" ) );

        cal.set( Calendar.HOUR_OF_DAY, 1 );
        cal.set( Calendar.MINUTE, 16 );
        cal.set( Calendar.SECOND, 0 );
        cal.set( Calendar.YEAR, 2014 );
        cal.set( Calendar.MONTH, Calendar.JUNE );
        cal.set( Calendar.DATE, 16 );

        Date firstTestDate = cal.getTime();

        cal.set( Calendar.MONTH, Calendar.NOVEMBER );

        Date secondTestDate = cal.getTime();

        SimpleDateFormat format = new SimpleDateFormat( MavenBuildTimestamp.DEFAULT_BUILD_TIMESTAMP_FORMAT );
        format.setTimeZone( MavenBuildTimestamp.DEFAULT_BUILD_TIME_ZONE );
        assertEquals( "2014-06-15T23:16:00Z", format.format( firstTestDate ) );
        assertEquals( "2014-11-16T00:16:00Z", format.format( secondTestDate ) );
    }

    @Test
    public void testShouldNotThrowExceptionOnReferenceToNonExistentValue() throws Exception
    {
        Model model = new Model();

        Scm scm = new Scm();
        scm.setConnection( "${test}/somepath" );

        model.setScm( scm );

        ModelInterpolator interpolator = createInterpolator();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel( model, new File( "." ), createModelBuildingRequest( context ),
                collector );

        assertProblemFree( collector );
        assertEquals( "${test}/somepath", out.getScm().getConnection() );
    }

    @Test
    public void testShouldThrowExceptionOnRecursiveScmConnectionReference() throws Exception
    {
        Model model = new Model();

        Scm scm = new Scm();
        scm.setConnection( "${project.scm.connection}/somepath" );

        model.setScm( scm );

        ModelInterpolator interpolator = createInterpolator();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        interpolator.interpolateModel( model, null, createModelBuildingRequest( context ), collector );
        assertCollectorState(  0, 1, 0, collector );
    }

    @Test
    public void testShouldNotThrowExceptionOnReferenceToValueContainingNakedExpression() throws Exception
    {
        Model model = new Model();

        Scm scm = new Scm();
        scm.setConnection( "${test}/somepath" );

        model.setScm( scm );

        model.addProperty( "test", "test" );

        ModelInterpolator interpolator = createInterpolator();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel( model, new File( "." ), createModelBuildingRequest( context ),
                collector );

        assertProblemFree( collector );

        assertEquals( "test/somepath", out.getScm().getConnection() );
    }

    @Test
    public void shouldInterpolateOrganizationNameCorrectly() throws Exception
    {
        String orgName = "MyCo";

        Model model = new Model();
        model.setName( "${pom.organization.name} Tools" );

        Organization org = new Organization();
        org.setName( orgName );

        model.setOrganization( org );

        ModelInterpolator interpolator = createInterpolator();

        Model out = interpolator.interpolateModel( model, new File( "." ), createModelBuildingRequest( context ),
                new SimpleProblemCollector() );

        assertEquals( orgName + " Tools", out.getName() );
    }

    @Test
    public void shouldInterpolateDependencyVersionToSetSameAsProjectVersion() throws Exception
    {
        Model model = new Model();
        model.setVersion( "3.8.1" );

        Dependency dep = new Dependency();
        dep.setVersion( "${version}" );

        model.addDependency( dep );

        ModelInterpolator interpolator = createInterpolator();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel( model, new File( "." ), createModelBuildingRequest( context ),
                collector );
        assertCollectorState( 0, 0, 1, collector );

        assertEquals( "3.8.1", ( out.getDependencies().get( 0 ) ).getVersion() );
    }

    @Test
    public void testShouldNotInterpolateDependencyVersionWithInvalidReference() throws Exception
    {
        Model model = new Model();
        model.setVersion( "3.8.1" );

        Dependency dep = new Dependency();
        dep.setVersion( "${something}" );

        model.addDependency( dep );

        /*
         // This is the desired behaviour, however there are too many crappy poms in the repo and an issue with the
         // timing of executing the interpolation

         try
         {
         new RegexBasedModelInterpolator().interpolate( model, context );
         fail( "Should have failed to interpolate with invalid reference" );
         }
         catch ( ModelInterpolationException expected )
         {
         assertTrue( true );
         }
         */

        ModelInterpolator interpolator = createInterpolator();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel( model, new File( "." ), createModelBuildingRequest( context ),
                collector );
        assertProblemFree( collector );

        assertEquals( "${something}", ( out.getDependencies().get( 0 ) ).getVersion() );
    }

    @Test
    public void testTwoReferences() throws Exception
    {
        Model model = new Model();
        model.setVersion( "3.8.1" );
        model.setArtifactId( "foo" );

        Dependency dep = new Dependency();
        dep.setVersion( "${artifactId}-${version}" );

        model.addDependency( dep );

        ModelInterpolator interpolator = createInterpolator();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel( model, new File( "." ), createModelBuildingRequest( context ),
                collector );
        assertCollectorState( 0, 0, 2, collector );

        assertEquals( "foo-3.8.1", ( out.getDependencies().get( 0 ) ).getVersion() );
    }

    @Test
    public void testBasedir() throws Exception
    {
        Model model = new Model();
        model.setVersion( "3.8.1" );
        model.setArtifactId( "foo" );

        Repository repository = new Repository();

        repository.setUrl( "file://localhost/${basedir}/temp-repo" );

        model.addRepository( repository );

        ModelInterpolator interpolator = createInterpolator();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel( model, null, createModelBuildingRequest( context ), collector );
        assertProblemFree( collector );

        assertEquals( "file://localhost/myBasedir/temp-repo", ( out.getRepositories().get( 0 ) ).getUrl() );
    }

    @Test
    public void testBaseUri() throws Exception
    {
        Model model = new Model();
        model.setVersion( "3.8.1" );
        model.setArtifactId( "foo" );

        Repository repository = new Repository();

        repository.setUrl( "${project.baseUri}/temp-repo" );

        model.addRepository( repository );

        ModelInterpolator interpolator = createInterpolator();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel( model, null, createModelBuildingRequest( context ), collector );
        assertProblemFree( collector );

        assertEquals( "myBaseUri/temp-repo", ( out.getRepositories().get( 0 ) ).getUrl() );
    }

    @Test
    public void testEnvars() throws Exception
    {
        Properties context = new Properties();

        context.put( "env.HOME", "/path/to/home" );

        Model model = new Model();

        Properties modelProperties = new Properties();

        modelProperties.setProperty( "outputDirectory", "${env.HOME}" );

        model.setProperties( modelProperties );

        ModelInterpolator interpolator = createInterpolator();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel( model, new File( "." ), createModelBuildingRequest( context ),
                collector );
        assertProblemFree( collector );

        assertEquals( "/path/to/home", out.getProperties().getProperty( "outputDirectory" ) );
    }

    @Test
    public void envarExpressionThatEvaluatesToNullReturnsTheLiteralString() throws Exception
    {
        Model model = new Model();

        Properties modelProperties = new Properties();

        modelProperties.setProperty( "outputDirectory", "${env.DOES_NOT_EXIST}" );

        model.setProperties( modelProperties );

        ModelInterpolator interpolator = createInterpolator();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel( model, new File( "." ), createModelBuildingRequest( context ),
                collector );
        assertProblemFree( collector );

        assertEquals( out.getProperties().getProperty( "outputDirectory" ), "${env.DOES_NOT_EXIST}" );
    }

    @Test
    public void expressionThatEvaluatesToNullReturnsTheLiteralString() throws Exception
    {
        Model model = new Model();

        Properties modelProperties = new Properties();

        modelProperties.setProperty( "outputDirectory", "${DOES_NOT_EXIST}" );

        model.setProperties( modelProperties );

        ModelInterpolator interpolator = createInterpolator();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel( model, new File( "." ), createModelBuildingRequest( context ),
                collector );
        assertProblemFree( collector );

        assertEquals( out.getProperties().getProperty( "outputDirectory" ), "${DOES_NOT_EXIST}" );
    }

    @Test
    public void shouldInterpolateSourceDirectoryReferencedFromResourceDirectoryCorrectly() throws Exception
    {
        Model model = new Model();

        Build build = new Build();
        build.setSourceDirectory( "correct" );

        Resource res = new Resource();
        res.setDirectory( "${project.build.sourceDirectory}" );

        build.addResource( res );

        Resource res2 = new Resource();
        res2.setDirectory( "${pom.build.sourceDirectory}" );

        build.addResource( res2 );

        Resource res3 = new Resource();
        res3.setDirectory( "${build.sourceDirectory}" );

        build.addResource( res3 );

        model.setBuild( build );

        ModelInterpolator interpolator = createInterpolator();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel( model, null, createModelBuildingRequest( context ), collector );
        assertCollectorState( 0, 0, 2, collector );


        List<Resource> outResources = out.getBuild().getResources();
        Iterator<Resource> resIt = outResources.iterator();

        assertEquals( build.getSourceDirectory(), resIt.next().getDirectory() );
        assertEquals( build.getSourceDirectory(), resIt.next().getDirectory() );
        assertEquals( build.getSourceDirectory(), resIt.next().getDirectory() );
    }

    @Test
    public void shouldInterpolateUnprefixedBasedirExpression() throws Exception
    {
        File basedir = new File( "/test/path" );
        Model model = new Model();
        Dependency dep = new Dependency();
        dep.setSystemPath( "${basedir}/artifact.jar" );

        model.addDependency( dep );

        ModelInterpolator interpolator = createInterpolator();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model result = interpolator.interpolateModel( model, basedir, createModelBuildingRequest( context ),
                collector );
        assertProblemFree( collector );


        List<Dependency> rDeps = result.getDependencies();
        assertNotNull( rDeps );
        assertEquals( 1, rDeps.size() );
        assertEquals( new File( basedir, "artifact.jar" ).getAbsolutePath(),
                new File( rDeps.get( 0 ).getSystemPath() ).getAbsolutePath() );
    }

    @Test
    public void testRecursiveExpressionCycleNPE() throws Exception
    {
        Properties props = new Properties();
        props.setProperty( "aa", "${bb}" );
        props.setProperty( "bb", "${aa}" );
        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();

        Model model = new Model();
        model.setProperties( props );

        SimpleProblemCollector collector = new SimpleProblemCollector();
        ModelInterpolator interpolator = createInterpolator();
        interpolator.interpolateModel( model, null, request, collector );

        assertCollectorState( 0, 2, 0, collector );
        assertTrue( collector.getErrors().get( 0 ).contains( "Detected the following recursive expression cycle" ) );
    }

    @Test
    public void testRecursiveExpressionCycleBaseDir() throws Exception
    {
        Properties props = new Properties();
        props.setProperty( "basedir", "${basedir}" );
        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();

        Model model = new Model();
        model.setProperties( props );

        SimpleProblemCollector collector = new SimpleProblemCollector();
        ModelInterpolator interpolator = createInterpolator();
        interpolator.interpolateModel( model, null, request, collector );

        assertCollectorState( 0, 1, 0, collector );
        assertEquals(
                "Resolving expression: '${basedir}': Detected the following recursive expression cycle in 'basedir': [basedir]",
                collector.getErrors().get( 0 ) );
    }

    protected abstract ModelInterpolator createInterpolator() throws Exception;

}
