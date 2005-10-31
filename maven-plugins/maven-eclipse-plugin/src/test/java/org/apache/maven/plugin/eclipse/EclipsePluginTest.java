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

package org.apache.maven.plugin.eclipse;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class EclipsePluginTest
    extends AbstractEclipsePluginTestCase
{
    public void testProject1()
        throws Exception
    {
        testProject( "project-1", null );
    }

    public void testProject2()
        throws Exception
    {
        testProject( "project-2", null );
    }

    public void testProject3()
        throws Exception
    {
        testProject( "project-3", null );
    }

    public void testProject4()
        throws Exception
    {
        testProject( "project-4", getTestFile( "target/project-4-test/" ) );
    }

    public void testProject5()
        throws Exception
    {
        testProject( "project-5", null );
    }

    public void testProject6()
        throws Exception
    {
        testProject( "project-6", null );
    }

    public void testProject7()
        throws Exception
    {
        testProject( "project-7", null );
    }

    // @todo testcase for MNG-1324 "System" dependencies path non correctly added to eclipse buildpath
    //    public void testProject8()
    //        throws Exception
    //    {
    //        testProject( "project-8", null );
    //    }

}
