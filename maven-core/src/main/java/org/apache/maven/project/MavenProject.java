package org.apache.maven.project;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Developer;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Scm;

import org.codehaus.plexus.util.StringUtils;

/**
 * The concern of the project is provide runtime values based on the model.
 * <p/>
 * The values in the model remain untouched but during the process of building
 * a project notions like inheritance and interpolation can be added. This allows
 * to have an entity which is useful in a runtime while preserving the model so that
 * it can be marshalled and unmarshalled without being tainted by runtime
 * requirements.
 * <p/>
 * We need to leave the model intact because we don't want the following:
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

    private Map properties;

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

    // ----------------------------------------------------------------------
    // Test and compile sourceroots.
    // ----------------------------------------------------------------------

    //!!! Refactor, collect the list of compile source roots and create a path1:path2
    // type construct from the list instead of the other way around. jvz.

    private static String PS = System.getProperty( "path.separator" );

    private String compileSourceRoots = "";

    private String testCompileSourceRoots = "";

    public void addCompileSourceRoot( String path )
    {
        if ( path != null || path.trim().length() != 0 )
        {
            compileSourceRoots += path + PS;
        }
    }

    public String getCompileSourceRoots()
    {
        // Get rid of any trailing path separators.
        if ( compileSourceRoots.endsWith( PS ) )
        {
            compileSourceRoots = compileSourceRoots.substring( 0, compileSourceRoots.length() - 1 );
        }

        // Always add the build.sourceDirectory
        return getBuild().getSourceDirectory() + PS + compileSourceRoots;
    }

    public List getCompileSourceRootsList()
    {
        String[] s = StringUtils.split( getCompileSourceRoots(), PS );

        List list = new ArrayList();

        for ( int i = 0; i < s.length; i++ )
        {
            list.add( s[i] );
        }

        return list;
    }

    public void addTestCompileSourceRoot( String path )
    {
        if ( path != null || path.trim().length() != 0 )
        {
            testCompileSourceRoots += path + PS;
        }
    }

    public String getTestCompileSourceRoots()
    {
        // Get rid of any trailing path separators.
        if ( testCompileSourceRoots.endsWith( PS ) )
        {
            testCompileSourceRoots = testCompileSourceRoots.substring( 0, testCompileSourceRoots.length() - 1 );
        }

        // Always add the build.unitTestSourceDirectory
        return getBuild().getUnitTestSourceDirectory() + PS + testCompileSourceRoots;
    }

    public List getTestCompileSourceRootsList()
    {
        String[] s = StringUtils.split( getTestCompileSourceRoots(), PS );

        List list = new ArrayList();

        for ( int i = 0; i < s.length; i++ )
        {
            list.add( s[i] );
        }

        return list;
    }

    public String[] getClasspathElements()
    {
        int size = getArtifacts().size();

        String[] classpathElements = new String[size + 1];

        int i = 0;

        for ( Iterator it = getArtifacts().iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();

            if ( isAddedToClasspath( artifact ) )
            {
                classpathElements[i++] = artifact.getPath();
            }
        }

        classpathElements[i] = getBuild().getOutput();

        return classpathElements;
    }

    public boolean isAddedToClasspath( Artifact artifact )
    {
        String type = artifact.getType().trim();

        if ( type.equals( "jar" ) || type.equals( "ejb" ) || type.equals( "test" ) )
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

    public String getType()
    {
        return model.getType();
    }

    public void setType( String type )
    {
         model.setType( type );
    }

    public void setInceptionYear( String inceptionYear )
    {
        model.setInceptionYear( inceptionYear );
    }

    public String getInceptionYear()
    {
        return model.getInceptionYear();
    }

    public void setPackage( String packageName )
    {
        model.setPackage( packageName );
    }

    public String getPackage()
    {
        return model.getPackage();
    }

    public void setUrl( String url )
    {
        model.setUrl( url );
    }

    public String getUrl()
    {
        return model.getUrl();
    }

    public void setLogo( String logo )
    {
        model.setLogo( logo );
    }

    public String getLogo()
    {
        return model.getLogo();
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

    public void setShortDescription( String shortDescription )
    {
        model.setShortDescription( shortDescription );
    }

    public String getShortDescription()
    {
        return model.getShortDescription();
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

    public void setReports( List reports )
    {
        model.setReports( reports );
    }

    public List getReports()
    {
        return model.getReports();
    }

    public void addReports( String report )
    {
        model.addReport( report );
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

    public void setProperty( String key, String value )
    {
        getProperties().put( key, value );
    }

    public void setProperties( Map properties )
    {
        this.properties = properties;
    }

    public Map getProperties()
    {
        return properties;
    }

    public String getProperty( String key )
    {
        String property = (String) properties.get( key );

        if ( property == null && hasParent() )
        {
            property = getParent().getProperty( key );
        }

        return property;
    }

    /**
     * Convert a <code>String</code> property to a
     * <code>Boolean</code> based on its contents.  It would be nice
     * if Jelly would deal with this automatically.
     *
     * @param key The property key to lookup and convert.
     * @return The boolean value of the property if convertiable,
     *         otherwise <code>Boolean.FALSE</code>.
     */
    public boolean getBooleanProperty( String key )
    {
        String value = getProperty( key );

        if ( "true".equalsIgnoreCase( value )
            || "on".equalsIgnoreCase( value )
            || "1".equals( value ) )
        {
            return true;
        }

        return false;
    }

    public List getRepositories()
    {
        return model.getRepositories();
    }
}

