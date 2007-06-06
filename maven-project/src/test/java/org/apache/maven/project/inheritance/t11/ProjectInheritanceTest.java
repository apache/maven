package org.apache.maven.project.inheritance.t11;

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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

import org.apache.maven.model.Build;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.inheritance.AbstractProjectInheritanceTestCase;
import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.logging.LoggerManager;
import org.codehaus.plexus.logging.Logger;

/**
 * Verifies scope of root project is preserved regardless of parent depenedency management. See
 * {@link http://jira.codehaus.org/browse/MNG-2919}
 * 
 * @author <a href="mailto:pschneider@gmail.com">Patrick Schneider</a>
 * @version $Id$
 */
public class ProjectInheritanceTest extends AbstractProjectInheritanceTestCase
{
    // ----------------------------------------------------------------------
    //
    // p1 inherits from p0
    // p0 inhertis from super model
    //
    // or we can show it graphically as:
    //
    // p1 ---> p0 --> super model
    //
    // ----------------------------------------------------------------------

    public void testDependencyManagementOverridesTransitiveDependencyVersion() throws Exception
    {
        File localRepo = getLocalRepositoryPath();

        File pom0 = new File( localRepo, "p0/pom.xml" );
        File pom0Basedir = pom0.getParentFile();
        File pom1 = new File( pom0Basedir, "p1/pom.xml" );

        // load the child project, which inherits from p0...
        // MavenProject project0 = getProjectWithDependencies( pom0 );
        MavenProject project1 = getProjectWithDependencies( pom1 );

        assertEquals( pom0Basedir, project1.getParent().getBasedir() );
        assertNull( "dependencyManagement has overwritten the scope of a child project",
                     project1.getArtifact().getScope() );
    }
}