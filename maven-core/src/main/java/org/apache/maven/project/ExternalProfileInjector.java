package org.apache.maven.project;

import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.profile.activation.ProfileActivationCalculator;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

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

public class ExternalProfileInjector
    extends AbstractLogEnabled
    implements Contextualizable
{

    public static final String ROLE = ExternalProfileInjector.class.getName();

    private ArtifactRepositoryFactory artifactRepositoryFactory;

    private ProfileActivationCalculator profileActivationCalculator;

    private PlexusContainer container;

    public void injectExternalProfiles( MavenProject project, List profiles )
        throws ProjectBuildingException
    {
        List activeProfiles = profileActivationCalculator.calculateActiveProfiles( profiles );
        
        Model model = project.getModel();

        for ( Iterator it = activeProfiles.iterator(); it.hasNext(); )
        {
            Profile profile = (Profile) it.next();
            
            mergeRepositories( project, model, profile );
            
            mergePluginRepositories( project, model, profile );
            
            Properties props = profile.getProperties();
            
            if( props != null )
            {
                project.addProfileConfiguration( props );
            }
        }
    }

    private void mergePluginRepositories( MavenProject project, Model model, Profile profile ) throws ProjectBuildingException
    {
        List repos = profile.getPluginRepositories();
        if( repos != null && !repos.isEmpty() )
        {
            List modelRepos = model.getPluginRepositories();
            if( modelRepos == null )
            {
                modelRepos = new ArrayList();
                
                model.setPluginRepositories( modelRepos );
            }
            
            modelRepos.addAll( repos );
            
            List artifactRepos = ProjectUtils.buildArtifactRepositories( repos, artifactRepositoryFactory, container );
            
            List projectRepos = project.getPluginArtifactRepositories();
            if( projectRepos == null )
            {
                projectRepos = new ArrayList();
                
                project.setPluginArtifactRepositories( projectRepos );
            }
            
            projectRepos.addAll( artifactRepos );
        }
    }

    private void mergeRepositories( MavenProject project, Model model, Profile profile )
        throws ProjectBuildingException
    {
        List repos = profile.getRepositories();
        if( repos != null && !repos.isEmpty() )
        {
            List modelRepos = model.getRepositories();
            if( modelRepos == null )
            {
                modelRepos = new ArrayList();
                
                model.setRepositories( modelRepos );
            }
            
            modelRepos.addAll( repos );
            
            List artifactRepos = ProjectUtils.buildArtifactRepositories( repos, artifactRepositoryFactory, container );
            
            List projectRepos = project.getRemoteArtifactRepositories();
            if( projectRepos == null )
            {
                projectRepos = new ArrayList();
                
                project.setRemoteArtifactRepositories( projectRepos );
            }
            
            projectRepos.addAll( artifactRepos );
        }
    }

    public void contextualize( Context context )
        throws ContextException
    {
        this.container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

}
