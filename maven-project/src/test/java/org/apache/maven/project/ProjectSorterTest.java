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

import junit.framework.TestCase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.dag.CycleDetectedException;

import java.util.ArrayList;
import java.util.List;

/**
 * Test sorting projects by dependencies.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class ProjectSorterTest
    extends TestCase
{
    public void testMatchingArtifactIdsDifferentGroupIds()
        throws CycleDetectedException
    {
        List projects = new ArrayList();
        MavenProject project1 = createProject( "groupId1", "artifactId", "1.0" );
        projects.add( project1 );
        MavenProject project2 = createProject( "groupId2", "artifactId", "1.0" );
        projects.add( project2 );
        project1.getDependencies().add( createDependency( project2 ) );

        projects = ProjectSorter.getSortedProjects( projects );

        assertEquals( project2, projects.get( 0 ) );
        assertEquals( project1, projects.get( 1 ) );
    }

    public void testMatchingGroupIdsDifferentArtifactIds()
        throws CycleDetectedException
    {
        List projects = new ArrayList();
        MavenProject project1 = createProject( "groupId", "artifactId1", "1.0" );
        projects.add( project1 );
        MavenProject project2 = createProject( "groupId", "artifactId2", "1.0" );
        projects.add( project2 );
        project1.getDependencies().add( createDependency( project2 ) );

        projects = ProjectSorter.getSortedProjects( projects );

        assertEquals( project2, projects.get( 0 ) );
        assertEquals( project1, projects.get( 1 ) );
    }

    private Dependency createDependency( MavenProject project )
    {
        Dependency depdendency = new Dependency();
        depdendency.setArtifactId( project.getArtifactId() );
        depdendency.setGroupId( project.getGroupId() );
        depdendency.setVersion( project.getVersion() );
        return depdendency;
    }

    private static MavenProject createProject( String groupId, String artifactId, String version )
    {
        Model model = new Model();
        model.setGroupId( groupId );
        model.setArtifactId( artifactId );
        model.setVersion( version );
        return new MavenProject( model );
    }
}
