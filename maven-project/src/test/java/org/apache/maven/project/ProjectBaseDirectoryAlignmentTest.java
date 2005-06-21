package org.apache.maven.project;

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

import org.apache.maven.model.Build;
import org.apache.maven.model.Resource;

import java.io.File;

public class ProjectBaseDirectoryAlignmentTest
    extends MavenProjectTestCase
{
    private String dir = "src/test/resources/projects/base-directory-alignment/";

    public void testProjectDirectoryBaseDirectoryAlignment()
        throws Exception
    {
        File f = getTestFile( dir + "project-which-needs-directory-alignment.xml" );

        MavenProject project = getProject( f );

        assertNotNull( "Test project can't be null!", project );

        assertTrue( project.getBuild().getSourceDirectory().startsWith( getBasedir() ) );

        assertTrue( project.getBuild().getTestSourceDirectory().startsWith( getBasedir() ) );

        Build build = project.getBuild();

        Resource resource = (Resource) build.getResources().get( 0 );

        assertTrue( resource.getDirectory().startsWith( getBasedir() ) );
    }
}
