package org.apache.maven.converter;

/*
 * Copyright (c) 2004, Jason van Zyl and Trygve Laugstøl
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Developer;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Resource;
import org.apache.maven.model.Scm;
import org.apache.maven.model.Site;
import org.apache.maven.model.SourceModification;
import org.apache.maven.model.UnitTest;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.model.v300.io.xpp3.MavenXpp3Reader;

import org.codehaus.plexus.util.FileUtils;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class PomV3ToV4Converter
{
    private org.apache.maven.model.v300.Model v3ParentModel;

    public static void main( String[] args )
    {
        PomV3ToV4Converter converter = new PomV3ToV4Converter();

        try
        {
            converter.work( args );

            System.exit( 0 );
        }
        catch( Exception ex )
        {
            ex.printStackTrace( System.err );

            System.exit( -1 );
        }
    }

    public void work( String[] args )
        throws Exception
    {
        if ( args.length != 1 )
        {
            throw new Exception( "Usage: converter <file> | <directory" );
        }

        File input = new File( args[0] );

        List files;

        if ( !input.isDirectory() )
        {
            if ( !input.isFile() )
            {
                throw new Exception( "The input file isn't a file nor a directory: '" + input.getAbsolutePath() + "'." );
            }

            files = new LinkedList();

            files.add( input );
        }
        else
        {
            files = FileUtils.getFiles( input, "**/project.xml", "**/xdocs/**" );
        }

        for ( Iterator it = files.iterator(); it.hasNext(); )
        {
            input = (File) it.next();

            try
            {
                convertFile( input, new File( input.getParentFile(), "pom.xml" ) );

                info( "Converted: " + input.getAbsolutePath() );
            }
            catch( Exception ex )
            {
                info( "Could not convert: " + input.getAbsolutePath() );

                throw ex;
            }
        }
    }

    public void convertFile( File input, File outputFile )
        throws Exception
    {
        Model model = convertFile( input );

        MavenXpp3Writer v4Writer = new MavenXpp3Writer();

        // dump to console
        Writer output = new OutputStreamWriter( System.out );

        v4Writer.write( output, model );

        output.flush();

        // write the new pom.xml
//        File outputFile = new File( currentFile.getParentFile(), "pom.xml" );

        System.err.println( "Writing new pom to: " + outputFile.getAbsolutePath() );

        output = new FileWriter( outputFile );

        v4Writer.write( output, model );

        output.close();
    }

    public Model convertFile( File input )
        throws Exception
    {
        org.apache.maven.model.v300.Model v3Model = loadV3Pom( input );

        return convertModel( input, v3Model );
    }

    public Model convertModel( File file, org.apache.maven.model.v300.Model v3Model )
        throws Exception
    {
        Model v4Model = populateModel( file, v3Model );

        return v4Model;
    }

    private  Model populateModel( File file, org.apache.maven.model.v300.Model v3Model )
        throws Exception
    {
        Model v4Model = new Model();

        v4Model.setModelVersion( "4.0.0" );

        v4Model.setParent( getParent( file, v3Model ) );

        // Group id
        String groupId = v3Model.getGroupId();

        String parentGroupId = null;

        if ( v3ParentModel != null )
        {
            parentGroupId = v3ParentModel.getGroupId();
        }

        if( isEmpty( groupId ) && isEmpty( parentGroupId ) )
        {
            throw new Exception( "Missing 'groupId' from both pom and the extended pom." );
        }

        v4Model.setGroupId( groupId );

        // Artifact id
        String artifactId = v3Model.getArtifactId();

        if ( isEmpty( artifactId ) )
        {
//            throw new Exception( "Missing element 'artifactId'." );
            v4Model.setArtifactId( groupId );
        }
        else
        {
            v4Model.setArtifactId( artifactId );
        }

        // Version
        String version = v3Model.getCurrentVersion();

        if( isEmpty( version ) && (v3ParentModel == null || isEmpty( v3ParentModel.getCurrentVersion() ) ) )
        {
            throw new Exception( "Missing 'currentVersion' from both pom and the extended pom." );
        }

        v4Model.setVersion( version );

        v4Model.setName( v3Model.getName() );

        v4Model.setShortDescription( v3Model.getShortDescription() );

        v4Model.setDescription( v3Model.getDescription() );

        v4Model.setUrl( v3Model.getUrl() );

        v4Model.setOrganization( getOrganization( v3Model ) );

        v4Model.setLogo( v3Model.getLogo() );

        v4Model.setIssueManagement( getIssueManagement( v3Model ) );

        v4Model.setCiManagement( getCiManagement( v3Model ) );

        v4Model.setInceptionYear( v3Model.getInceptionYear() );

        if ( !isEmpty( v3Model.getGumpRepositoryId() ) )
        {
            warn( "The 'gumpRepositoryId' is removed from the version 4 pom." );
        }

        v4Model.setRepositories( getRepositories( v3Model ) );

        v4Model.setDevelopers( getDevelopers( v3Model ) );

        v4Model.setContributors( getContributors( v3Model ) );

        v4Model.setDependencies( getDependencies( v3Model ) );

        v4Model.setLicenses( getLicenses( v3Model ) );

        List versions = v3Model.getVersions();

        if ( versions != null )
        {
            warn( "The <versions> list is removed in the version 4 of the pom." );
        }

        List branches = v3Model.getBranches();

        if ( branches != null )
        {
            warn( "The <branches> list is removed in the version 4 of the pom." );
        }

        v4Model.setReports( returnList( v3Model.getReports() ) );

        v4Model.setScm( getScm( v3Model ) );

        v4Model.setBuild( getBuild( v3Model ) );

        v4Model.setDistributionManagement( getDistributionManagement( v3Model ) );

        v4Model.setPackage( v3Model.getPackage() );

        return v4Model;
    }

    private Parent getParent( File file, org.apache.maven.model.v300.Model v3Model )
        throws Exception
    {
        Parent parent = new Parent();

        String extend = v3Model.getExtend();

        if ( isEmpty( extend ) )
        {
            return null;
        }

        final String basedir = "${basedir}";

        int i = extend.indexOf( basedir );

        if ( i >= 0 )
        {
            extend = extend.substring( 0, i ) +
                     file.getParentFile() + File.separator +
                     extend.substring( i + basedir.length() + 1 );
        }

        File extendFile = new File( extend );

        if ( !extendFile.isAbsolute() )
        {
            extendFile = new File( file.getParentFile(), extend );
        }

        if ( !extendFile.isFile() )
        {
            throw new FileNotFoundException( "Could not find the file the pom extends: '" + extendFile.getAbsolutePath() + "' is not a file." );
        }

        // try to find the parent pom.
        v3ParentModel = loadV3Pom( extendFile );

        String groupId = v3ParentModel.getGroupId();

        if ( isEmpty( groupId ) )
        {
            throw new Exception( "Missing groupId from the extended pom." );
        }

        parent.setGroupId( groupId );

        String artifactId = v3ParentModel.getArtifactId();

        if ( isEmpty( artifactId ) )
        {
            throw new Exception( "Missing 'artifactId' from the extended pom." );
        }

        parent.setArtifactId( artifactId );

        String version = v3ParentModel.getCurrentVersion();

        if ( isEmpty( version ) )
        {
            throw new Exception( "Missing 'currentVersion' from the extended pom." );
        }

        parent.setVersion( version );

        return parent;
    }

    private Organization getOrganization( org.apache.maven.model.v300.Model v3Model )
    {
        Organization organization = new Organization();

        if ( v3Model.getOrganization() == null )
        {
            return null;
        }

        organization.setName( v3Model.getOrganization().getName() );

        organization.setUrl( v3Model.getOrganization().getUrl() );

        organization.setLogo( v3Model.getOrganization().getLogo() );

        return organization;
    }

    private IssueManagement getIssueManagement( org.apache.maven.model.v300.Model v3Model )
    {
        String issueTrackingUrl = v3Model.getIssueTrackingUrl();

        if ( isEmpty( issueTrackingUrl ) )
        {
            return null;
        }

        IssueManagement issueManagement = new IssueManagement();

        issueManagement.setUrl( issueTrackingUrl );

        return issueManagement;
    }

    private CiManagement getCiManagement( org.apache.maven.model.v300.Model v3Model )
    {
        if ( v3Model.getBuild() == null )
        {
            return null;
        }

        String nagEmailAddress = v3Model.getBuild().getNagEmailAddress();

        if ( isEmpty( nagEmailAddress ) )
        {
            return null;
        }

        CiManagement ciManagement = new CiManagement();

        ciManagement.setNagEmailAddress( nagEmailAddress );

        return ciManagement;
    }

    // TODO:
    // note: these are not SCM repositories but rather artifact repositories
    private List getRepositories( org.apache.maven.model.v300.Model v3Model )
    {
        List repositories = new ArrayList( 1 );

//        warn( "" );

        return returnList( repositories );
    }

    // TODO:
    private List getMailingLists( org.apache.maven.model.v300.Model v3Model )
    {
        List mailingLists = new ArrayList();

        List v3MailingLists = v3Model.getMailingLists();

        if ( isEmpty( v3MailingLists ) )
        {
            return null;
        }

        for ( Iterator it = v3MailingLists.iterator(); it.hasNext(); )
        {
            org.apache.maven.model.v300.MailingList v3MailingList =
                (org.apache.maven.model.v300.MailingList) it.next();

            MailingList mailingList = new MailingList();

            mailingList.setName( v3MailingList.getName() );

            mailingList.setSubscribe( v3MailingList.getSubscribe() );

            mailingList.setUnsubscribe( v3MailingList.getUnsubscribe() );

            mailingList.setArchive( v3MailingList.getArchive() );

            mailingLists.add( mailingList );
        }

        return mailingLists;
    }

    private List getDevelopers( org.apache.maven.model.v300.Model v3Model )
    {
        List developers = new ArrayList();

        List v3Developers = v3Model.getDevelopers();

        if ( isEmpty( v3Developers ) )
        {
            return null;
        }

        for ( Iterator it = v3Developers.iterator(); it.hasNext(); )
        {
            org.apache.maven.model.v300.Developer v3Developer =
                (org.apache.maven.model.v300.Developer) it.next();

            Developer developer = new Developer();

            developer.setId( nullIfEmpty( v3Developer.getId() ) );

            developer.setName( nullIfEmpty( v3Developer.getName() ) );

            developer.setEmail( nullIfEmpty( v3Developer.getEmail() ) );

            developer.setOrganization( nullIfEmpty( v3Developer.getOrganization() ) );

            developer.setTimezone( nullIfEmpty( v3Developer.getTimezone() ) );

            developer.setUrl( nullIfEmpty( v3Developer.getUrl() ) );

            developer.setRoles( returnList( v3Developer.getRoles() ) );

            developers.add( developer );
        }

        return developers;
    }

    private List getContributors( org.apache.maven.model.v300.Model v3Model )
    {
        List contributors = new ArrayList();

        List v3Contributors = v3Model.getContributors();

        if ( isEmpty( v3Contributors ) )
        {
            return null;
        }

        for ( Iterator it = v3Contributors.iterator(); it.hasNext(); )
        {
            org.apache.maven.model.v300.Contributor v3Contributor =
                (org.apache.maven.model.v300.Contributor) it.next();

            Contributor contributor = new Contributor();

            contributor.setName( nullIfEmpty( v3Contributor.getName() ) );

            contributor.setEmail( nullIfEmpty( v3Contributor.getEmail() ) );

            contributor.setOrganization( nullIfEmpty( v3Contributor.getOrganization() ) );

            contributor.setTimezone( nullIfEmpty( v3Contributor.getTimezone() ) );

            contributor.setUrl( nullIfEmpty( v3Contributor.getUrl() ) );

            contributor.setRoles( returnList( v3Contributor.getRoles() ) );

            contributors.add( contributor );
        }

        return contributors;
    }

    private List getDependencies( org.apache.maven.model.v300.Model v3Model )
        throws Exception
    {
        List dependencies = new ArrayList();

        List v3Dependencies = v3Model.getDependencies();

        if ( isEmpty( v3Dependencies ) )
        {
            return null;
        }

        for ( Iterator it = v3Dependencies.iterator(); it.hasNext(); )
        {
            org.apache.maven.model.v300.Dependency v3Dependency =
                (org.apache.maven.model.v300.Dependency) it.next();

            Dependency dependency = new Dependency();

            String id = nullIfEmpty( v3Dependency.getId() );

            String groupId = nullIfEmpty( v3Dependency.getGroupId() );

            String artifactId = nullIfEmpty( v3Dependency.getArtifactId() );

            // old school
            if ( !isEmpty( id ) )
            {
                dependency.setGroupId( id );

                dependency.setArtifactId( id );

                if ( !isEmpty( groupId ) )
                {
                    warn( "Both <dependency.id> and <dependency.groupId> is set, using <groupId> (id: " + id + ", groupId: " + groupId + ")." );
                }

                if ( !isEmpty( artifactId ) )
                {
                    warn( "Both <dependency.id> and <dependency.artifactId> is set, using <artifactId> (id: " + id + ", artifactId: " + artifactId + ")." );
                }
            }
            // new school
            else
            {
                if ( isEmpty( groupId ) )
                {
                    fatal( "Missing dependency.groupId." );
                }

                if ( isEmpty( artifactId ) )
                {
                    fatal( "Missing dependency.artifactId." );
                }

                dependency.setGroupId( groupId );

                dependency.setArtifactId( artifactId );
            }

            dependency.setType( nullIfEmpty( v3Dependency.getType() ) );

            dependency.setVersion( nullIfEmpty( v3Dependency.getVersion() ) );

            dependency.setUrl( nullIfEmpty( v3Dependency.getUrl() ) );

            dependency.setProperties( v3Dependency.getProperties() );

            dependencies.add( dependency );
        }

        return dependencies;
    }

    private List getLicenses( org.apache.maven.model.v300.Model v3Model )
    {
        List licenses = new ArrayList();

        List v3Licenses = v3Model.getLicenses();

        if ( isEmpty( v3Licenses ) )
        {
            return null;
        }

        for ( Iterator it = v3Licenses.iterator(); it.hasNext(); )
        {
            org.apache.maven.model.v300.License v3License =
                (org.apache.maven.model.v300.License) it.next();

            License license = new License();

            license.setName( nullIfEmpty( v3License.getName() ) );

            license.setUrl( nullIfEmpty( v3License.getUrl() ) );

            license.setComments( nullIfEmpty( v3License.getComments() ) );

            licenses.add( license );
        }

        return licenses;
    }

    private Scm getScm( org.apache.maven.model.v300.Model v3Model )
    {
        if ( v3Model.getRepository() == null )
        {
            return null;
        }

        Scm scm = new Scm();

//        warn( "connection: " + v3Model.getRepository().getConnection() );

//        warn( "developerConnection: " + v3Model.getRepository().getDeveloperConnection() );

//        warn( "url: " + v3Model.getRepository().getUrl() );

        scm.setConnection( v3Model.getRepository().getConnection() );

        scm.setDeveloperConnection( v3Model.getRepository().getDeveloperConnection() );

        scm.setUrl( v3Model.getRepository().getUrl() );

        return scm;
    }

    private Build getBuild( org.apache.maven.model.v300.Model v3Model )
    {
        org.apache.maven.model.v300.Build v3Build = v3Model.getBuild();

        if ( v3Build == null )
        {
            return null;
        }

        Build build = new Build();

        build.setSourceDirectory( v3Build.getSourceDirectory() );

        List v3SourceModifications = v3Build.getSourceModifications();

        if ( v3SourceModifications != null && v3SourceModifications.size() > 0 )
        {
            List sourceModifications = new ArrayList();

            for ( Iterator it = v3SourceModifications.iterator(); it.hasNext(); )
            {
                org.apache.maven.model.v300.SourceModification v3SourceModification =
                    (org.apache.maven.model.v300.SourceModification) it.next();

                SourceModification sourceModification = new SourceModification();

                sourceModification.setClassName( v3SourceModification.getClassName() );

                sourceModification.setIncludes( getIncludes( v3SourceModification.getIncludes() ) );

                sourceModification.setExcludes( getExcludes( v3SourceModification.getExcludes() ) );

                sourceModifications.add( sourceModification );
            }

            build.setSourceModifications( sourceModifications );
        }

        build.setUnitTestSourceDirectory( v3Build.getUnitTestSourceDirectory() );

        build.setAspectSourceDirectory( v3Build.getAspectSourceDirectory() );

        org.apache.maven.model.v300.UnitTest v3UnitTest = v3Build.getUnitTest();

        if ( v3UnitTest != null )
        {
            UnitTest unitTest = new UnitTest();

            unitTest.setIncludes( getIncludes( v3UnitTest.getIncludes() ) );

            unitTest.setExcludes( getExcludes( v3UnitTest.getExcludes() ) );

            unitTest.setResources( convertResources( v3UnitTest.getResources() ) );

            build.setUnitTest( unitTest );
        }

        build.setResources( convertResources( v3Build.getResources() ) );

        return build;
    }

    private DistributionManagement getDistributionManagement( org.apache.maven.model.v300.Model v3Model )
        throws Exception
    {
        DistributionManagement distributionManagement = new DistributionManagement();

        Site site = null;

        String siteAddress = v3Model.getSiteAddress();

        String siteDirectory = v3Model.getSiteDirectory();

        if ( isEmpty( siteAddress ) )
        {
            if ( !isEmpty( siteDirectory ) )
            {
                site = new Site();

                site.setId( "default" );

                site.setName( "Default Site" );

                site.setUrl( "file://" + siteDirectory );
            }
        }
        else
        {
            if ( isEmpty( siteDirectory ) )
            {
                throw new Exception( "Missing 'siteDirectory': Both siteAddress and siteDirectory must be set at the same time." );
            }

            site = new Site();

            site.setId( "default" );

            site.setName( "Default Site" );

            site.setUrl( "scp://" + siteAddress + "/" + siteDirectory );
        }

        distributionManagement.setSite( site );

        String distributionSite = v3Model.getDistributionSite();

        String distributionDirectory = v3Model.getDistributionDirectory();

        Repository repository = null;

        if ( isEmpty( distributionSite ) )
        {
            if ( !isEmpty( distributionDirectory ) )
            {
                repository = new Repository();

                repository.setId( "default" );

                repository.setName( "Default Repository" );

                repository.setUrl( "file://" + distributionDirectory );
//                throw new Exception( "Missing 'distributionSite': Both distributionSite and distributionDirectory must be set." );
            }
        }
        else
        {
            if ( isEmpty( distributionDirectory ) )
            {
                throw new Exception( "Missing 'distributionDirectory': must be set is 'distributionSite' is set." );
            }

            repository = new Repository();

            repository.setId( "default" );

            repository.setName( "Default Repository" );

            repository.setUrl( distributionSite + "/" + distributionDirectory );
        }

        distributionManagement.setRepository( repository );

        if ( site == null && repository == null )
        {
            return null;
        }

        return distributionManagement;
    }

    private org.apache.maven.model.v300.Model loadV3Pom( File inputFile )
        throws Exception
    {
        MavenXpp3Reader v3Reader = new MavenXpp3Reader();

        org.apache.maven.model.v300.Model model;

        model = v3Reader.read( new FileReader( inputFile ) );

        SAXReader r = new SAXReader();

        Document d = r.read( new FileReader( inputFile ) );

        Element root = d.getRootElement();

        Element idElement = root.element( "id" );

        String id = null;

        if ( idElement != null )
        {
            id = idElement.getText();
        }
//        String id = model.getId();

        String groupId = model.getGroupId();

        String artifactId = model.getArtifactId();

        if ( !isEmpty( id ) )
        {
            int i = id.indexOf( "+" );

            int j = id.indexOf( ":" );

            if ( i > 0 )
            {
                model.setGroupId( id.substring( 0, i ) );

                model.setArtifactId( id.replace( '+', '-' ) );
            }
            else if ( j > 0 )
            {
                model.setGroupId( id.substring( 0, j ) );

                model.setArtifactId( id.substring( j + 1 ) );
            }
            else
            {
                model.setGroupId( id );

                model.setArtifactId( id );
            }

            if ( !isEmpty( groupId ) )
            {
                warn( "Both <id> and <groupId> is set, using <groupId>." );

                model.setGroupId( groupId );
            }

            if ( !isEmpty( artifactId ) )
            {
                warn( "Both <id> and <artifactId> is set, using <artifactId>." );

                model.setArtifactId( artifactId );
            }
        }
/**/
        return model;
    }

    private List getIncludes( List includes )
    {
        if ( includes == null || includes.size() == 0 )
        {
            return null;
        }

        return includes;
    }

    private List getExcludes( List excludes )
    {
        if ( excludes == null || excludes.size() == 0 )
        {
            return null;
        }

        return excludes;
    }

    private List convertResources( List v3Resources )
    {
        List resources = new ArrayList();

        if ( v3Resources == null || v3Resources.size() == 0 )
        {
            return null;
        }

        for ( Iterator it = v3Resources.iterator(); it.hasNext(); )
        {
            org.apache.maven.model.v300.Resource v3Resource =
                (org.apache.maven.model.v300.Resource) it.next();

            Resource resource = new Resource();

            resource.setDirectory( v3Resource.getDirectory() );

            resource.setTargetPath( v3Resource.getTargetPath() );

            resource.setIncludes( getIncludes( v3Resource.getIncludes() ) );

            resource.setExcludes( getExcludes( v3Resource.getExcludes() ) );

            resources.add( resource );
        }

        return resources;
    }
/*
    private void assertNotEmpty( String fieldName, String value )
        throws Exception
    {
        if ( value == null || value.trim().length() == 0 )
        {
            throw new Exception( "Missing required field: '" + fieldName + "'." );
        }
    }
*/
    private List returnList( List list )
    {
        if ( list == null || list.size() == 0 )
            return null;

        return list;
    }

    private boolean isEmpty( String value )
    {
        return value == null || value.trim().length() == 0;
    }

    private boolean isEmpty( List list )
    {
        return list == null || list.size() == 0;
    }

    private String nullIfEmpty( String string )
    {
        if ( string == null || string.trim().length() == 0 )
        {
            return null;
        }

        return string;
    }

    private void fatal( String msg )
        throws Exception
    {
        System.err.println( "[FATAL] " + msg );

        throw new Exception( msg );
    }

    private void warn( String msg )
    {
        System.err.println( "[WARN] " + msg );

        System.err.flush();
    }

    private void info( String msg )
    {
        System.err.println( msg );

        System.err.flush();
    }
}
