package org.apache.maven.project;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Developer;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Reports;
import org.apache.maven.model.Scm;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * The concern of the project is provide runtime values based on the model. <p/>
 * The values in the model remain untouched but during the process of building a
 * project notions like inheritance and interpolation can be added. This allows
 * to have an entity which is useful in a runtime while preserving the model so
 * that it can be marshalled and unmarshalled without being tainted by runtime
 * requirements. <p/>We need to leave the model intact because we don't want
 * the following:
 * <ol>
 * <li>We don't want interpolated values being written back into the model.
 * <li>We don't want inherited values being written back into the model.
 * </ol>
 */
public class MavenProject
{
    private Model model;

    private MavenProject parent;

    private File file;

    private Set artifacts;

    public MavenProject( Model model )
    {
        this.model = model;
    }

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    //@todo I would like to get rid of this. jvz.
    public Model getModel()
    {
        return model;
    }

    public MavenProject getParent()
    {
        return parent;
    }

    public void setParent( MavenProject parent )
    {
        this.parent = parent;
    }

    public boolean hasParent()
    {
        return getParent() != null;
    }

    public File getFile()
    {
        return file;
    }

    public void setFile( File file )
    {
        this.file = file;
    }

    public File getBasedir()
    {
        return getFile().getParentFile();
    }

    public void setDependencies( List denpendencies )
    {
        model.setDependencies( denpendencies );
    }

    public List getDependencies()
    {
        return model.getDependencies();
    }

    public DependencyManagement getDependencyManagement()
    {
        return model.getDependencyManagement();
    }

    // ----------------------------------------------------------------------
    // Test and compile sourceroots.
    // ----------------------------------------------------------------------

    private List compileSourceRoots = new ArrayList();

    private List testCompileSourceRoots = new ArrayList();

    private List scriptSourceRoots = new ArrayList();

    public void addCompileSourceRoot( String path )
    {
        if ( path != null )
        {
            path = path.trim();
            if ( path.length() != 0 )
            {
                if ( !compileSourceRoots.contains( path ) )
                {
                    compileSourceRoots.add( path );
                }
            }
        }
    }

    public void addScriptSourceRoot( String path )
    {
        if ( path != null )
        {
            path = path.trim();
            if ( path.length() != 0 )
            {
                if ( !scriptSourceRoots.contains( path ) )
                {
                    scriptSourceRoots.add( path );
                }
            }
        }
    }

    public void addTestCompileSourceRoot( String path )
    {
        if ( path != null )
        {
            path = path.trim();
            if ( path.length() != 0 )
            {
                if ( !testCompileSourceRoots.contains( path ) )
                {
                    testCompileSourceRoots.add( path );
                }
            }
        }
    }

    public List getCompileSourceRoots()
    {
        return compileSourceRoots;
    }

    public List getScriptSourceRoots()
    {
        return scriptSourceRoots;
    }

    public List getTestCompileSourceRoots()
    {
        return testCompileSourceRoots;
    }

    public List getCompileClasspathElements()
    {
        List list = new ArrayList( getArtifacts().size() );

        for ( Iterator i = getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();

            // TODO: let the scope handler deal with this
            if ( Artifact.SCOPE_COMPILE.equals( a.getScope() ) )
            {
                list.add( a.getPath() );
            }
        }
        return list;
    }

    public List getTestClasspathElements()
    {
        List list = new ArrayList( getArtifacts().size() + 1 );

        list.add( getBuild().getOutputDirectory() );

        for ( Iterator i = getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();

            if ( isAddedToClasspath( a ) )
            {
                // TODO: let the scope handler deal with this
                if ( Artifact.SCOPE_TEST.equals( a.getScope() ) || Artifact.SCOPE_COMPILE.equals( a.getScope() ) || Artifact.SCOPE_RUNTIME.equals(
                    a.getScope() ) )
                {
                    list.add( a.getPath() );
                }
            }
        }
        return list;
    }

    public List getRuntimeClasspathElements()
    {
        List list = new ArrayList( getArtifacts().size() + 1 );

        list.add( getBuild().getOutputDirectory() );

        for ( Iterator i = getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();

            if ( isAddedToClasspath( a ) )
            {
                // TODO: let the scope handler deal with this
                if ( Artifact.SCOPE_COMPILE.equals( a.getScope() ) || Artifact.SCOPE_RUNTIME.equals( a.getScope() ) )
                {
                    list.add( a.getPath() );
                }
            }
        }
        return list;
    }

    private static boolean isAddedToClasspath( Artifact artifact )
    {
        String type = artifact.getType();

        // TODO: utilise type handler
        if ( "jar".equals( type ) || "ejb".equals( type ) )
        {
            return true;
        }

        return false;
    }

    // ----------------------------------------------------------------------
    // Delegate to the model
    // ----------------------------------------------------------------------

    public void setModelVersion( String pomVersion )
    {
        model.setModelVersion( pomVersion );
    }

    public String getModelVersion()
    {
        return model.getModelVersion();
    }

    public String getId()
    {
        return model.getId();
    }

    public void setGroupId( String groupId )
    {
        model.setGroupId( groupId );
    }

    public String getGroupId()
    {
        return model.getGroupId();
    }

    public void setArtifactId( String artifactId )
    {
        model.setArtifactId( artifactId );
    }

    public String getArtifactId()
    {
        return model.getArtifactId();
    }

    public void setName( String name )
    {
        model.setName( name );
    }

    public String getName()
    {
        return model.getName();
    }

    public void setVersion( String version )
    {
        model.setVersion( version );
    }

    public String getVersion()
    {
        return model.getVersion();
    }

    public String getPackaging()
    {
        return model.getPackaging();
    }

    public void setPackaging( String packaging )
    {
        model.setPackaging( packaging );
    }

    public void setInceptionYear( String inceptionYear )
    {
        model.setInceptionYear( inceptionYear );
    }

    public String getInceptionYear()
    {
        return model.getInceptionYear();
    }

    public void setUrl( String url )
    {
        model.setUrl( url );
    }

    public String getUrl()
    {
        return model.getUrl();
    }

    public void setIssueManagement( IssueManagement issueManagement )
    {
        model.setIssueManagement( issueManagement );
    }

    public CiManagement getCiManagement()
    {
        return model.getCiManagement();
    }

    public void setCiManagement( CiManagement ciManagement )
    {
        model.setCiManagement( ciManagement );
    }

    public IssueManagement getIssueManagement()
    {
        return model.getIssueManagement();
    }

    public void setDistributionManagement( DistributionManagement distributionManagement )
    {
        model.setDistributionManagement( distributionManagement );
    }

    public DistributionManagement getDistributionManagement()
    {
        return model.getDistributionManagement();
    }

    public void setDescription( String description )
    {
        model.setDescription( description );
    }

    public String getDescription()
    {
        return model.getDescription();
    }

    public void setOrganization( Organization organization )
    {
        model.setOrganization( organization );
    }

    public Organization getOrganization()
    {
        return model.getOrganization();
    }

    public void setScm( Scm scm )
    {
        model.setScm( scm );
    }

    public Scm getScm()
    {
        return model.getScm();
    }

    public void setMailingLists( List mailingLists )
    {
        model.setMailingLists( mailingLists );
    }

    public List getMailingLists()
    {
        return model.getMailingLists();
    }

    public void addMailingList( MailingList mailingList )
    {
        model.addMailingList( mailingList );
    }

    public void setDevelopers( List developers )
    {
        model.setDevelopers( developers );
    }

    public List getDevelopers()
    {
        return model.getDevelopers();
    }

    public void addDeveloper( Developer developer )
    {
        model.addDeveloper( developer );
    }

    public void setContributors( List contributors )
    {
        model.setContributors( contributors );
    }

    public List getContributors()
    {
        return model.getContributors();
    }

    public void addContributor( Contributor contributor )
    {
        model.addContributor( contributor );
    }

    public void setBuild( Build build )
    {
        model.setBuild( build );
    }

    public Build getBuild()
    {
        return model.getBuild();
    }

    public void setReports( Reports reports )
    {
        model.setReports( reports );
    }

    public Reports getReports()
    {
        return model.getReports();
    }

    public void setLicenses( List licenses )
    {
        model.setLicenses( licenses );
    }

    public List getLicenses()
    {
        return model.getLicenses();
    }

    public void addLicense( License license )
    {
        model.addLicense( license );
    }

    public void setArtifacts( Set artifacts )
    {
        this.artifacts = artifacts;
    }

    public Set getArtifacts()
    {
        return artifacts;
    }

    public List getRepositories()
    {
        return model.getRepositories();
    }

    // ----------------------------------------------------------------------
    // Plugins
    // ----------------------------------------------------------------------

    public List getPlugins()
    {
        if ( model.getBuild() == null )
        {
            return null;
        }
        return model.getBuild().getPlugins();
    }
}

