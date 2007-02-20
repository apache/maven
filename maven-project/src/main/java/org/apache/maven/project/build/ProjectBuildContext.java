package org.apache.maven.project.build;

import org.apache.maven.context.BuildContext;
import org.apache.maven.context.BuildContextManager;
import org.apache.maven.context.ManagedBuildData;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.build.model.ModelLineage;

import java.util.HashMap;
import java.util.Map;

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

/**
 * Build context information available for use during profile activation, which supplies information
 * about the current project and lineage from the current project back through parent poms to the
 * POM that declared that profile (where the activator is used). Lineage may not be accessible in
 * all cases, and will usually be incomplete (not stretching all the way back to the common super-POM).
 * This could enable custom profile activators that trigger based on model properties, etc.
 */
public class ProjectBuildContext
    implements ManagedBuildData
{
    
    public static final String BUILD_CONTEXT_KEY = ProjectBuildContext.class.getName();

    private static final String MODEL_LINEAGE = "model-lineage";

    private static final String CURRENT_PROJECT = "current-project";
    
    private ModelLineage modelLineage;
    
    private MavenProject currentProject;
    
    public ProjectBuildContext()
    {
    }

    public ModelLineage getModelLineage()
    {
        return modelLineage;
    }

    public void setModelLineage( ModelLineage modelLineage )
    {
        this.modelLineage = modelLineage;
    }

    public MavenProject getCurrentProject()
    {
        return currentProject;
    }

    public void setCurrentProject( MavenProject currentProject )
    {
        this.currentProject = currentProject;
    }

    public String getStorageKey()
    {
        return BUILD_CONTEXT_KEY;
    }
    
    public static ProjectBuildContext getProjectBuildContext( BuildContextManager buildContextManager, boolean create )
    {
        BuildContext buildContext = buildContextManager.readBuildContext( false );
        
        ProjectBuildContext projectContext = new ProjectBuildContext();
        
        if ( buildContext != null )
        {
            if ( !buildContext.retrieve( projectContext ) && !create )
            {
                return null;
            }
        }
        
        return projectContext;
    }
    
    public void store( BuildContextManager buildContextManager )
    {
        BuildContext buildContext = buildContextManager.readBuildContext( true );
        
        buildContext.store( this );
        
        buildContextManager.storeBuildContext( buildContext );
    }

    public Map getData()
    {
        Map data = new HashMap( 2 );
        
        data.put( MODEL_LINEAGE, modelLineage );
        data.put( CURRENT_PROJECT, currentProject );
        
        return data;
    }

    public void setData( Map data )
    {
        this.modelLineage = (ModelLineage) data.get( MODEL_LINEAGE );
        this.currentProject = (MavenProject) data.get( CURRENT_PROJECT );
    }
}
