package org.apache.maven.project.interpolation;

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

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;

import java.util.Collections;
import java.util.Map;

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

    public void testShouldInterpolateDependencyVersionToSetSameAsProjectVersion()
        throws ModelInterpolationException
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
        throws ModelInterpolationException
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
        throws ModelInterpolationException
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
        throws ModelInterpolationException
    {
        Model model = new Model();
        model.setVersion( "3.8.1" );
        model.setArtifactId( "foo" );

        Repository repository = new Repository();

        repository.setUrl( "file://localhost/${basedir}/temp-repo" );

        model.addRepository( repository );

        Model out = new RegexBasedModelInterpolator().interpolate( model, context );

        assertEquals( "file://localhost/myBasedir/temp-repo",
                      ( (Repository) out.getRepositories().get( 0 ) ).getUrl() );
    }
}
