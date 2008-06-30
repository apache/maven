package org.apache.maven.project;

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

import org.apache.maven.model.Build;
import org.apache.maven.model.Resource;

import java.io.File;

public class ProjectBaseDirectoryAlignmentTest
    extends AbstractMavenProjectTestCase
{
    private String dir = "src/test/resources/projects/base-directory-alignment/";

    public void testProjectDirectoryBaseDirectoryAlignment()
        throws Exception
    {
        File f = getTestFile( dir + "project-which-needs-directory-alignment.xml" );

        MavenProject project = getProject( f );
        projectBuilder.calculateConcreteState( project, new DefaultProjectBuilderConfiguration() );

        assertNotNull( "Test project can't be null!", project );

        File basedirFile = new File( getBasedir() );
        File sourceDirectoryFile = new File( project.getBuild().getSourceDirectory() );
        File testSourceDirectoryFile = new File( project.getBuild().getTestSourceDirectory() );

        assertEquals( basedirFile.getCanonicalPath(), sourceDirectoryFile.getCanonicalPath().substring( 0, getBasedir().length() ) );

        assertEquals( basedirFile.getCanonicalPath(), testSourceDirectoryFile.getCanonicalPath().substring( 0, getBasedir().length() ) );

        Build build = project.getBuild();

        Resource resource = (Resource) build.getResources().get( 0 );

        assertTrue( resource.getDirectory().startsWith( getBasedir() ) );
    }
}
