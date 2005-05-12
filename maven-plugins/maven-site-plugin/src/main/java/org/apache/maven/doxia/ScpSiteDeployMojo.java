/*
 * Copyright (c) 2005 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.doxia;

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.WagonUtils;
import org.apache.maven.wagon.observers.Debug;
import org.apache.maven.wagon.providers.ssh.ScpWagon;
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
 * Deploys website using scp protocol.
 * First website files are packaged into zip archive,
 * then archive is transfred to remote host, nextly it is un-archived.
 * This method of deployment should normally be much faster
 * then making file by file copy.
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
    private String inputDirectory;

    /**
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private String workingDirectory;

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
        File baseDir = new File( inputDirectory );

        File zipFile = null;

        try
        {
            zipFile = File.createTempFile( "site", ".zip", new File( workingDirectory ) );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Cannot create site archive!", e );
        }

        SshCommandExecutor commandExecutor = null;
        try
        {
            DistributionManagement distributionManagement = project.getDistributionManagement();

            if ( distributionManagement == null )
            {
                String msg = "distributionManagement element is missing in the POM: " + project.getId();

                throw new MojoExecutionException( msg );
            }

            if ( distributionManagement.getSite() == null )
            {
                String msg = "distributionManagement/site element is missing in the POM: " + project.getId();

                throw new MojoExecutionException( msg );

            }

            String url = distributionManagement.getSite().getUrl();

            String id = distributionManagement.getSite().getId();

            if ( url == null )
            {
                String msg = "distributionManagement/site/url element is missing in the POM: " + project.getId();

                throw new MojoExecutionException( msg );

            }

            Repository repository = new Repository( id, url );

            commandExecutor = (SshCommandExecutor) wagonManager.getWagon( "scp" );

            commandExecutor.connect( repository, wagonManager.getAuthenticationInfo( id ) );

            String basedir = repository.getBasedir();

            List files = FileUtils.getFileNames( baseDir, "**/**", "", false );

            createZip( files, zipFile, baseDir );

            Debug debug = new Debug();

            commandExecutor.addSessionListener( debug );

            commandExecutor.addTransferListener( debug );

            String cmd = " mkdir -p " + basedir;

            commandExecutor.executeCommand( cmd );

            commandExecutor.put( zipFile, zipFile.getName() );

            cmd = " cd " + basedir + ";" + unzipCommand + " " + zipFile.getName() + "\n";

            commandExecutor.executeCommand( cmd );

            String rmCommand = "rm  " + basedir + "/" + zipFile.getName();

            commandExecutor.executeCommand( rmCommand );

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
