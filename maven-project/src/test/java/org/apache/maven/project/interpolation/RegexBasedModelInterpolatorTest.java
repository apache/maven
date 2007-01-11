package org.apache.maven.project.interpolation;

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

import junit.framework.TestCase;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Scm;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

/**
 * @author jdcasey
 * @version $Id$
 */
public class RegexBasedModelInterpolatorTest
    extends TestCase
{
    private Map context;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        context = Collections.singletonMap( "basedir", "myBasedir" );
    }

    public void testShouldNotThrowExceptionOnReferenceToNonExistentValue()
        throws IOException, ModelInterpolationException
    {
        Model model = new Model();

        Scm scm = new Scm();
        scm.setConnection( "${test}/somepath" );

        model.setScm( scm );

        Model out = new RegexBasedModelInterpolator().interpolate( model, context );

        assertEquals( "${test}/somepath", out.getScm().getConnection() );
    }

    public void testShouldThrowExceptionOnRecursiveScmConnectionReference()
        throws IOException
    {
        Model model = new Model();

        Scm scm = new Scm();
        scm.setConnection( "${project.scm.connection}/somepath" );

        model.setScm( scm );

        try
        {
            Model out = new RegexBasedModelInterpolator().interpolate( model, context );

            fail( "The interpolator should not allow self-referencing expressions in POM." );
        }
        catch ( ModelInterpolationException e )
        {

        }
    }

    public void testShouldNotThrowExceptionOnReferenceToValueContainingNakedExpression()
        throws IOException, ModelInterpolationException
    {
        Model model = new Model();

        Scm scm = new Scm();
        scm.setConnection( "${test}/somepath" );

        model.setScm( scm );

        model.addProperty( "test", "test" );

        Model out = new RegexBasedModelInterpolator().interpolate( model, context );

        assertEquals( "test/somepath", out.getScm().getConnection() );
    }

    public void testShouldInterpolateOrganizationNameCorrectly()
        throws Exception
    {
        String orgName = "MyCo";

        Model model = new Model();
        model.setName( "${pom.organization.name} Tools" );

        Organization org = new Organization();
        org.setName( orgName );

        model.setOrganization( org );

        Model out = new RegexBasedModelInterpolator().interpolate( model, context );

        assertEquals( orgName + " Tools", out.getName() );
    }

    public void testShouldInterpolateDependencyVersionToSetSameAsProjectVersion()
        throws Exception
    {
        Model model = new Model();
        model.setVersion( "3.8.1" );

        Dependency dep = new Dependency();
        dep.setVersion( "${version}" );

        model.addDependency( dep );

        Model out = new RegexBasedModelInterpolator().interpolate( model, context );

        assertEquals( "3.8.1", ( (Dependency) out.getDependencies().get( 0 ) ).getVersion() );
    }

    public void testShouldNotInterpolateDependencyVersionWithInvalidReference()
        throws Exception
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

        Model out = new RegexBasedModelInterpolator().interpolate( model, context );

        assertEquals( "${something}", ( (Dependency) out.getDependencies().get( 0 ) ).getVersion() );
    }

    public void testTwoReferences()
        throws Exception
    {
        Model model = new Model();
        model.setVersion( "3.8.1" );
        model.setArtifactId( "foo" );

        Dependency dep = new Dependency();
        dep.setVersion( "${artifactId}-${version}" );

        model.addDependency( dep );

        Model out = new RegexBasedModelInterpolator().interpolate( model, context );

        assertEquals( "foo-3.8.1", ( (Dependency) out.getDependencies().get( 0 ) ).getVersion() );
    }

    public void testBasedir()
        throws Exception
    {
        Model model = new Model();
        model.setVersion( "3.8.1" );
        model.setArtifactId( "foo" );

        Repository repository = new Repository();

        repository.setUrl( "file://localhost/${basedir}/temp-repo" );

        model.addRepository( repository );

        Model out = new RegexBasedModelInterpolator().interpolate( model, context );

        assertEquals( "file://localhost/myBasedir/temp-repo", ( (Repository) out.getRepositories().get( 0 ) ).getUrl() );
    }

    public void testEnvars()
        throws Exception
    {
        Properties envars = new Properties();

        envars.setProperty( "HOME", "/path/to/home" );

        Model model = new Model();

        Properties modelProperties = new Properties();

        modelProperties.setProperty( "outputDirectory", "${env.HOME}" );

        model.setProperties( modelProperties );

        Model out = new RegexBasedModelInterpolator( envars ).interpolate( model, context );

        assertEquals( out.getProperties().getProperty( "outputDirectory" ), "/path/to/home" );
    }

    public void testEnvarExpressionThatEvaluatesToNullReturnsTheLiteralString()
        throws Exception
    {
        Properties envars = new Properties();

        Model model = new Model();

        Properties modelProperties = new Properties();

        modelProperties.setProperty( "outputDirectory", "${env.DOES_NOT_EXIST}" );

        model.setProperties( modelProperties );

        Model out = new RegexBasedModelInterpolator( envars ).interpolate( model, context );

        assertEquals( out.getProperties().getProperty( "outputDirectory" ), "${env.DOES_NOT_EXIST}" );
    }

    public void testExpressionThatEvaluatesToNullReturnsTheLiteralString()
        throws Exception
    {
        Model model = new Model();

        Properties modelProperties = new Properties();

        modelProperties.setProperty( "outputDirectory", "${DOES_NOT_EXIST}" );

        model.setProperties( modelProperties );

        Model out = new RegexBasedModelInterpolator().interpolate( model, context );

        assertEquals( out.getProperties().getProperty( "outputDirectory" ), "${DOES_NOT_EXIST}" );
    }
}
