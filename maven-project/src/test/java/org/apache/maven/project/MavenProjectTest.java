package org.apache.maven.project;

/*
 * Copyright 2005 The Apache Software Foundation.
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

import java.io.File;
import java.io.IOException;

import org.apache.maven.model.Model;

public class MavenProjectTest
    extends AbstractMavenProjectTestCase
{
    
    public void testEmptyConstructor()
    {
        MavenProject project = new MavenProject();
        
        assertEquals( MavenProject.EMPTY_PROJECT_GROUP_ID + ":" + MavenProject.EMPTY_PROJECT_ARTIFACT_ID + ":jar:"
            + MavenProject.EMPTY_PROJECT_VERSION, project.getId() );
    }
    
    public void testCopyConstructor() throws Exception
    {
        File f = getFileForClasspathResource( "canonical-pom.xml" );
        MavenProject projectToClone = getProject(f);

        MavenProject clonedProject = new MavenProject(projectToClone);
        assertEquals("maven-core", clonedProject.getArtifactId());
    }
    
    public void testGetModulePathAdjustment() throws IOException
    {
        Model moduleModel = new Model();
        
        MavenProject module = new MavenProject( moduleModel );
        module.setFile( new File( "module-dir/pom.xml" ) );
        
        Model parentModel = new Model();
        parentModel.addModule( "../module-dir" );
        
        MavenProject parent = new MavenProject( parentModel );
        parent.setFile( new File( "parent-dir/pom.xml" ) );
        
        String pathAdjustment = parent.getModulePathAdjustment( module );
        
        assertEquals( "..", pathAdjustment );
    }
}
