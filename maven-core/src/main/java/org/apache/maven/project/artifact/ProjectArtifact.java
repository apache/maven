package org.apache.maven.project.artifact;

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

import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.project.MavenProject;

public class ProjectArtifact
    extends DefaultArtifact
    implements ArtifactWithDependencies
{
    private MavenProject project;

    public ProjectArtifact( MavenProject project )
    {
        super( project.getGroupId(), project.getArtifactId(), project.getVersion(), null, "pom", null,
               new PomArtifactHandler() );
        this.project = project;
        setFile( project.getFile() );
        setResolved( true );
    }

    public MavenProject getProject()
    {
        return project;
    }

    public List<Dependency> getDependencies()
    {
        return project.getDependencies();
    }

    public List<Dependency> getManagedDependencies()
    {
        DependencyManagement depMngt = project.getDependencyManagement();
        return ( depMngt != null )
                   ? Collections.unmodifiableList( depMngt.getDependencies() )
                   : Collections.<Dependency>emptyList();

    }

    static class PomArtifactHandler
        implements ArtifactHandler
    {
        public String getClassifier()
        {
            return null;
        }

        public String getDirectory()
        {
            return null;
        }

        public String getExtension()
        {
            return "pom";
        }

        public String getLanguage()
        {
            return "none";
        }

        public String getPackaging()
        {
            return "pom";
        }

        public boolean isAddedToClasspath()
        {
            return false;
        }

        public boolean isIncludesDependencies()
        {
            return false;
        }
    }
}
