package org.apache.maven.project.interpolation;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import junit.framework.TestCase;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;

/**
 * @author jdcasey
 *
 * Created on Feb 3, 2005
 */
public class RegexBasedProjectInterpolatorTest
    extends TestCase
{

    public void testShouldInterpolateDependencyVersionToSetSameAsProjectVersion()
        throws ProjectInterpolationException
    {
        Model model = new Model();
        model.setVersion( "3.8.1" );

        Dependency dep = new Dependency();
        dep.setVersion( "#version" );

        model.addDependency( dep );

        MavenProject in = new MavenProject( model );
        MavenProject out = new RegexBasedProjectInterpolator().interpolate( in );

        assertEquals( "3.8.1", ((Dependency) out.getDependencies().get( 0 )).getVersion() );
    }

    public void testShouldNotInterpolateDependencyVersionWithInvalidReference()
        throws ProjectInterpolationException
    {
        Model model = new Model();
        model.setVersion( "3.8.1" );

        Dependency dep = new Dependency();
        dep.setVersion( "#something" );

        model.addDependency( dep );

        MavenProject in = new MavenProject( model );
        MavenProject out = new RegexBasedProjectInterpolator().interpolate( in );

        assertEquals( "#something", ((Dependency) out.getDependencies().get( 0 )).getVersion() );
    }

}