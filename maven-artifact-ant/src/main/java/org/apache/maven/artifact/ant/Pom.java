package org.apache.maven.artifact.ant;

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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Build;
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Reports;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.codehaus.plexus.util.introspection.ReflectionValueExtractor;

import java.io.File;
import java.util.Collections;

/**
 * A POM typedef.
 *
 * Also an Ant Task that registers a handler called POMPropertyHelper
 * that intercepts all calls to property value resolution and replies instead
 * of Ant to properties that start with the id of the pom.
 *
 * Example:
 * ${maven.project:artifactId}
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:nicolaken@apache.org">Nicola Ken Barozzi</a>
 * @version $Id$
 */
public class Pom
    extends AbstractArtifactTask
{
    private String refid;

    private String antId;

    private MavenProject mavenProject;

    private File file;

    /**
     * The property interceptor.
     */
    private final POMPropertyHelper helper = new POMPropertyHelper();

    public String getRefid()
    {
        return refid;
    }

    public void setRefid( String refid )
    {
        this.refid = refid;
    }

    public void setId( String id )
    {
        this.antId = id;
    }

    protected Pom getInstance()
    {
        Pom instance = this;
        if ( refid != null )
        {
            instance = (Pom) getProject().getReference( refid );
        }
        return instance;
    }

    public File getFile()
    {
        return getInstance().file;
    }

    public void setFile( File file )
    {
        this.file = file;
    }

    void initialise( MavenProjectBuilder builder, ArtifactRepository localRepository )
    {
        // TODO: should this be in execute() too? Would that work when it is used as a type?
        if ( file != null )
        {
            try
            {
                // TODO: should the profiles be constructed and passed in here? From Ant, or perhaps settings?
                mavenProject = builder.build( file, localRepository, Collections.EMPTY_LIST );
            }
            catch ( ProjectBuildingException e )
            {
                throw new BuildException( "Unable to build project: " + file, e );
            }
            catch ( ArtifactResolutionException e )
            {
                throw new BuildException( "Unable to build project: " + file, e );
            }
        }
        else if ( refid != null )
        {
            getInstance().initialise( builder, localRepository );
        }
    }

    protected MavenProject getMavenProject()
    {
        return getInstance().mavenProject;
    }

    public String getArtifactId()
    {
        return getMavenProject().getArtifactId();
    } //-- String getArtifactId()

    public Build getBuild()
    {
        return getMavenProject().getBuild();
    } //-- Build getBuild()

    public CiManagement getCiManagement()
    {
        return getMavenProject().getCiManagement();
    } //-- CiManagement getCiManagement()

    public java.util.List getContributors()
    {
        return getMavenProject().getContributors();
    } //-- java.util.List getContributors()

    public java.util.List getDependencies()
    {
        return getMavenProject().getDependencies();
    } //-- java.util.List getDependencies()

    public DependencyManagement getDependencyManagement()
    {
        return getMavenProject().getDependencyManagement();
    } //-- DependencyManagement getDependencyManagement()

    public String getDescription()
    {
        return getMavenProject().getDescription();
    } //-- String getDescription()

    public java.util.List getDevelopers()
    {
        return getMavenProject().getDevelopers();
    } //-- java.util.List getDevelopers()

    public DistributionManagement getDistributionManagement()
    {
        return getMavenProject().getDistributionManagement();
    } //-- DistributionManagement getDistributionManagement()

    public String getGroupId()
    {
        return getMavenProject().getGroupId();
    } //-- String getGroupId()

    public String getInceptionYear()
    {
        return getMavenProject().getInceptionYear();
    } //-- String getInceptionYear()

    public IssueManagement getIssueManagement()
    {
        return getMavenProject().getIssueManagement();
    } //-- IssueManagement getIssueManagement()

    public java.util.List getLicenses()
    {
        return getMavenProject().getLicenses();
    } //-- java.util.List getLicenses()

    public java.util.List getMailingLists()
    {
        return getMavenProject().getMailingLists();
    } //-- java.util.List getMailingLists()

    public String getModelVersion()
    {
        return getMavenProject().getModelVersion();
    } //-- String getModelVersion()

    public java.util.List getModules()
    {
        return getMavenProject().getModules();
    } //-- java.util.List getModules()

    public String getName()
    {
        return getMavenProject().getName();
    } //-- String getName()

    public Organization getOrganization()
    {
        return getMavenProject().getOrganization();
    } //-- Organization getOrganization()

    public String getPackaging()
    {
        return getMavenProject().getPackaging();
    } //-- String getPackaging()

    public java.util.List getPluginRepositories()
    {
        return getMavenProject().getPluginRepositories();
    } //-- java.util.List getPluginRepositories()

    public Reports getReports()
    {
        return getMavenProject().getReports();
    } //-- Reports getReports()

    public java.util.List getRepositories()
    {
        return getMavenProject().getRepositories();
    } //-- java.util.List getRepositories()

    public Scm getScm()
    {
        return getMavenProject().getScm();
    } //-- Scm getScm()

    public String getUrl()
    {
        return getMavenProject().getUrl();
    } //-- String getUrl()

    public String getVersion()
    {
        return getMavenProject().getVersion();
    } //-- String getVersion()

    public String getId()
    {
        return getMavenProject().getId();
    }

    /**
     * Registers POMPropertyHelper as a property interceptor
     */
    public void execute()
    {
        ArtifactRepository localRepo = createLocalArtifactRepository();
        MavenProjectBuilder projectBuilder = (MavenProjectBuilder) lookup( MavenProjectBuilder.ROLE );
        initialise( projectBuilder, localRepo );

        Project project = getProject();

        // Add a reference to this task/type
        project.addReference( antId, this );

        // Register the property interceptor
        PropertyHelper phelper = PropertyHelper.getPropertyHelper( project );
        helper.setNext( phelper.getNext() );
        helper.setProject( project );
        phelper.setNext( helper );
    }

    /**
     * The property interceptor that handles the calls for "pom." properties
     */
    private class POMPropertyHelper
        extends PropertyHelper
    {
        /**
         * The method that gets called by Ant with every request of property
         */
        public Object getPropertyHook( String ns, String name, boolean user )
        {

            String prefix = antId + ".";

            if ( !name.startsWith( prefix ) )
            {
                // pass on to next interceptor
                return super.getPropertyHook( ns, name, user );
            }
            try
            {
                // else handle the property resolution
                String expression = name.substring( prefix.length() );
                return getPOMValue( "project." + expression );

            }
            catch ( Exception ex )
            {
                ex.printStackTrace();
                return null;
            }
        }

        private Object getPOMValue( String expression )
        {
            Object value = null;

            try
            {
                value = ReflectionValueExtractor.evaluate( expression, getMavenProject() );
            }
            catch ( Exception e )
            {
                throw new BuildException( "Error extracting expression from POM", e );
            }

            return value;
        }

    }

}
