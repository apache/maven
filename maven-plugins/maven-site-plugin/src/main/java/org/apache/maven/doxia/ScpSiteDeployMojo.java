package org.apache.maven.doxia;

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

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Site;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.observers.Debug;
import org.apache.maven.wagon.providers.ssh.SshCommandExecutor;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Deploys website using scp/file protocol.
 * For scp protocol, website files are packaged into zip archive,
 * then archive is transfred to remote host, nextly it is un-archived.
 * This method of deployment should normally be much faster
 * then making file by file copy.  For file protocol, the files are copied
 * directly to the destination directory.
 *
 * @author <a href="mailto:michal@codehaus.org">Michal Maczka</a>
 * @version $Id$
 * @goal deploy
 */
public class ScpSiteDeployMojo
    extends AbstractMojo
{
    /**
     * @parameter alias="siteDirectory" expression="${project.build.directory}/site"
     * @required
     */
    private File inputDirectory;

    /**
     * @parameter
     */
    private String unzipCommand = "unzip -o";

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression="${component.org.apache.maven.artifact.manager.WagonManager}"
     * @required
     * @readonly
     */
    private WagonManager wagonManager;

    public void execute()
        throws MojoExecutionException
    {
        if ( !inputDirectory.exists() )
        {
            throw new MojoExecutionException( "The site does not exist, please run site:site first" );
        }

        File zipFile;

        try
        {
            zipFile = File.createTempFile( "site", ".zip" );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Cannot create site archive!", e );
        }

        DistributionManagement distributionManagement = project.getDistributionManagement();

        if ( distributionManagement == null )
        {
            throw new MojoExecutionException( "Missing distribution management information in the project" );
        }

        Site site = distributionManagement.getSite();

        if ( site == null )
        {
            throw new MojoExecutionException(
                "Missing site information in the distribution management element in the project.." );
        }

        String url = site.getUrl();

        String id = site.getId();

        if ( url == null )
        {
            throw new MojoExecutionException( "The URL to the site is missing in the project descriptor." );
        }

        Repository repository = new Repository( id, url );

        String siteProtocol = repository.getProtocol();

        if ( "scp".equals( siteProtocol ) )
        {
            scpDeploy( zipFile, id, repository );
        }
        else if ( "file".equals( siteProtocol ) )
        {
            File toDir = new File( repository.getBasedir() );
            fileDeploy( toDir );
        }
        else
        {
            throw new MojoExecutionException(
                "The deploy mojo currently only supports site deployment using the 'scp' and 'file' protocols." );
        }
    }


    /**
     * @throws MojoExecutionException
     */
    private void fileDeploy( File toDir )
        throws MojoExecutionException
    {
        try
        {
            FileUtils.copyDirectoryStructure( inputDirectory, toDir );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error transfering site!", e );
        }
    }


    /**
     * @param zipFile
     * @param id
     * @param repository
     * @throws MojoExecutionException
     */
    private void scpDeploy( File zipFile, String id, Repository repository )
        throws MojoExecutionException
    {
        SshCommandExecutor commandExecutor = null;

        try
        {
            commandExecutor = (SshCommandExecutor) wagonManager.getWagon( "scp" );

            commandExecutor.connect( repository, wagonManager.getAuthenticationInfo( id ) );

            String basedir = repository.getBasedir();

            List files = FileUtils.getFileNames( inputDirectory, "**/**", "", false );

            createZip( files, zipFile, inputDirectory );

            Debug debug = new Debug();

            commandExecutor.addSessionListener( debug );

            commandExecutor.addTransferListener( debug );

            String cmd = " mkdir -p " + basedir;

            commandExecutor.executeCommand( cmd );

            commandExecutor.put( zipFile, zipFile.getName() );

            // TODO: cat to file is temporary until the ssh executor is fixed to deal with output
            cmd = " cd " + basedir + ";" + unzipCommand + " " + zipFile.getName() + " >scpdeploymojo.log";

            commandExecutor.executeCommand( cmd );

            if ( !basedir.endsWith( "/" ) )
            {
                basedir = basedir + "/";
            }

            commandExecutor.executeCommand( "rm -f " + basedir + zipFile.getName() + " scpdeploymojo.log" );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error transfering site archive!", e );
        }
        finally
        {
            if ( commandExecutor != null )
            {
                try
                {
                    commandExecutor.disconnect();
                }
                catch ( ConnectionException e )
                {
                    //what to to here?
                }
            }

            if ( !zipFile.delete() )
            {
                zipFile.deleteOnExit();
            }
        }
    }


    public void createZip( List files, File zipName, File basedir )
        throws Exception
    {
        ZipOutputStream zos = new ZipOutputStream( new FileOutputStream( zipName ) );

        try
        {
            for ( int i = 0; i < files.size(); i++ )
            {
                String file = (String) files.get( i );

                writeZipEntry( zos, new File( basedir, file ), file );
            }
        }
        finally
        {
            zos.close();
        }
    }

    private void writeZipEntry( ZipOutputStream jar, File source, String entryName )
        throws Exception
    {
        byte[] buffer = new byte[1024];

        int bytesRead;

        FileInputStream is = new FileInputStream( source );

        try
        {
            ZipEntry entry = new ZipEntry( entryName );

            jar.putNextEntry( entry );

            while ( ( bytesRead = is.read( buffer ) ) != -1 )
            {
                jar.write( buffer, 0, bytesRead );
            }
        }

        finally
        {
            is.close();
        }
    }
}
