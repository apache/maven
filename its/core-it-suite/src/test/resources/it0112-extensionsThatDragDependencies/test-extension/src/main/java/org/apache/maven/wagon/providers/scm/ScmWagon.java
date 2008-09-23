package org.apache.maven.wagon.providers.scm;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmResult;
import org.apache.maven.scm.command.add.AddScmResult;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.command.list.ListScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.provider.ScmProviderRepositoryWithHost;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;

/**
 * Wagon provider to get and put files form and to SCM systems, using Maven-SCM as underlying transport.
 * <p/>
 * TODO it probably creates problems if the same wagon is used in two different SCM protocols, as
 * instance variables can keep incorrect state.
 *
 * @author <a href="brett@apache.org">Brett Porter</a>
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public class ScmWagon
    extends AbstractWagon
{
    private ScmManager scmManager;

    private File checkoutDirectory;

    /**
     * Get the {@link ScmManager} used in this Wagon
     *
     * @return rhe {@link ScmManager}
     */
    public ScmManager getScmManager()
    {
        return scmManager;
    }

    /**
     * Set the {@link ScmManager} used in this Wagon
     *
     * @param scmManager
     */
    public void setScmManager( ScmManager scmManager )
    {
        this.scmManager = scmManager;
    }

    /**
     * Get the directory where Wagon will checkout files from SCM.
     * This directory will be deleted!
     *
     * @return directory
     */
    public File getCheckoutDirectory()
    {
        return checkoutDirectory;
    }

    /**
     * Set the directory where Wagon will checkout files from SCM.
     * This directory will be deleted!
     *
     * @param checkoutDirectory
     */
    public void setCheckoutDirectory( File checkoutDirectory )
    {
        this.checkoutDirectory = checkoutDirectory;
    }

    /**
     * Convenience method to get the {@link ScmProvider} implementation to handle the provided SCM type
     *
     * @param scmType type of SCM, eg. <code>svn</code>, <code>cvs</code>
     * @return the {@link ScmProvider} that will handle provided SCM type
     * @throws NoSuchScmProviderException if there is no {@link ScmProvider} able to handle that SCM type
     */
    public ScmProvider getScmProvider( String scmType )
        throws NoSuchScmProviderException
    {
        return getScmManager().getProviderByType( scmType );
    }

    /**
     * This will cleanup the checkout directory
     */
    public void openConnection()
        throws ConnectionException
    {
        if ( checkoutDirectory.exists() )
        {
            try
            {
                FileUtils.deleteDirectory( checkoutDirectory );
            }
            catch ( IOException e )
            {
                throw new ConnectionException( "Unable to cleanup checkout directory", e );
            }
        }
        checkoutDirectory.mkdirs();
    }

    private ScmRepository getScmRepository( String url )
        throws TransferFailedException
    {
        String username = null;

        String password = null;

        String privateKey = null;

        String passphrase = null;

        if ( authenticationInfo != null )
        {
            username = authenticationInfo.getUserName();

            password = authenticationInfo.getPassword();

            privateKey = authenticationInfo.getPrivateKey();

            passphrase = authenticationInfo.getPassphrase();
        }

        ScmRepository scmRepository;

        try
        {
            scmRepository = getScmManager().makeScmRepository( url );
        }
        catch ( ScmRepositoryException e )
        {
            throw new TransferFailedException( "Error initialising SCM repository", e );
        }
        catch ( NoSuchScmProviderException e )
        {
            throw new TransferFailedException( "Unknown SCM type", e );
        }

        ScmProviderRepository providerRepository = scmRepository.getProviderRepository();

        if ( StringUtils.isNotEmpty( username ) )
        {
            providerRepository.setUser( username );
        }

        if ( StringUtils.isNotEmpty( password ) )
        {
            providerRepository.setPassword( password );
        }

        if ( providerRepository instanceof ScmProviderRepositoryWithHost )
        {
            ScmProviderRepositoryWithHost providerRepo = (ScmProviderRepositoryWithHost) providerRepository;

            if ( StringUtils.isNotEmpty( privateKey ) )
            {
                providerRepo.setPrivateKey( privateKey );
            }

            if ( StringUtils.isNotEmpty( passphrase ) )
            {
                providerRepo.setPassphrase( passphrase );
            }
        }

        return scmRepository;
    }

    public void put( File source, String resourceName )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        if ( source.isDirectory() )
        {
            throw new IllegalArgumentException( "Source is a directory: " + source );
        }
        putInternal( source, resourceName );
    }

    /**
     * Puts both files and directories
     *
     * @param source
     * @param resourceName
     * @throws TransferFailedException
     */
    private void putInternal( File source, String resourceName )
        throws TransferFailedException
    {
        Resource resource = new Resource( resourceName );

        firePutInitiated( resource, source );

        String url = getRepository().getUrl();

        ScmRepository scmRepository = getScmRepository( url );

        firePutStarted( resource, source );

        try
        {
            File basedir = checkoutDirectory;

            String msg = "Wagon: Adding " + source.getName() + " to repository";

            ScmProvider scmProvider = getScmProvider( scmRepository.getProvider() );

            ScmResult result;

            File newCheckoutDirectory = mkdirs( scmProvider, scmRepository, basedir );

            File scmFile = new File( newCheckoutDirectory, resourceName );

            boolean fileAlreadyInScm = scmFile.exists();

            if ( !scmFile.equals( source ) )
            {
                if ( source.isDirectory() )
                {
                    FileUtils.copyDirectoryStructure( source, scmFile );
                }
                else
                {
                    FileUtils.copyFile( source, scmFile );
                }
            }

            if ( !fileAlreadyInScm || scmFile.isDirectory() )
            {
                int addedFiles = addFiles( scmProvider, scmRepository, newCheckoutDirectory, scmFile.getName() );

                if ( !fileAlreadyInScm && addedFiles == 0 )
                {
                    throw new TransferFailedException(
                        "Unable to add file to SCM: " + scmFile + "; see error messages above for more information" );
                }
            }

            result = scmProvider.checkIn( scmRepository,
                                          new ScmFileSet( newCheckoutDirectory, scmFile.getName(), null ), null, msg );

            checkScmResult( result );

        }
        catch ( ScmException e )
        {
            throw new TransferFailedException( "Error interacting with SCM", e );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Error interacting with SCM", e );
        }

        if ( source.isFile() )
        {
            postProcessListeners( resource, source, TransferEvent.REQUEST_PUT );
        }

        firePutCompleted( resource, source );
    }

    /**
     * Add a file or directory to a SCM repository.
     * If it's a directory all its contents are added recursively.
     * <p/>
     * TODO this is less than optimal, SCM API should provide a way to add a directory recursively
     *
     * @param scmProvider   SCM provider
     * @param scmRepository SCM repository
     * @param basedir       local directory corresponding to scmRepository
     * @param scmFilePath   path of the file or directory to add, relative to basedir
     * @return the number of files added.
     * @throws ScmException
     */
    private int addFiles( ScmProvider scmProvider, ScmRepository scmRepository, File basedir, String scmFilePath )
        throws ScmException, TransferFailedException
    {
        File scmFile = new File( basedir, scmFilePath );

        AddScmResult result = scmProvider.add( scmRepository, new ScmFileSet( basedir, new File( scmFilePath ) ) );

        /* 
         * TODO dirty fix to work around files with property svn:eol-style=native
         * if a file has that property, first time file is added it fails, second time it succeeds
         * the solution is check if the scm provider is svn and unset that property
         * when the SCM API allows it 
         */
        if ( !result.isSuccess() )
        {
            result = scmProvider.add( scmRepository, new ScmFileSet( basedir, new File( scmFilePath ) ) );
        }

        int addedFiles = result.getAddedFiles().size();

        String reservedScmFile = scmProvider.getScmSpecificFilename();

        if ( scmFile.isDirectory() )
        {
            File[] files = scmFile.listFiles();

            for ( int i = 0; i < files.length; i++ )
            {
                if ( reservedScmFile != null && !reservedScmFile.equals( files[i].getName() ) )
                {
                    addedFiles +=
                        addFiles( scmProvider, scmRepository, basedir, scmFilePath + "/" + files[i].getName() );
                }
            }
        }

        return addedFiles;
    }

    /**
     * Make the necessary directories in the SCM repository to commit the files in the place asked
     *
     * @param scmProvider
     * @param repository
     * @param basedir
     * @return the new checkout directory. Will be <code>null</code> if it does not need to change.
     * @throws ScmException
     * @throws TransferFailedException
     */
    private File mkdirs( ScmProvider scmProvider, ScmRepository repository, File basedir )
        throws ScmException, TransferFailedException
    {
        ScmProviderRepository baseProviderRepository = repository.getProviderRepository();

        ScmFileSet fileSet = new ScmFileSet( basedir, new File( "." ) );

        ListScmResult listScmResult;

        ScmRepository baseRepository = repository;

        ScmProviderRepository lastBaseProviderRepository = baseProviderRepository;

        while ( baseProviderRepository != null )
        {
            listScmResult = scmProvider.list( baseRepository, fileSet, false, null );

            if ( listScmResult.isSuccess() )
            {
                break;
            }

            lastBaseProviderRepository = baseProviderRepository;

            baseProviderRepository = baseProviderRepository.getParent();

            baseRepository = new ScmRepository( repository.getProvider(), baseProviderRepository );
        }

        if ( baseProviderRepository != null )
        {
            String relativePath = repository.getProviderRepository().getRelativePath( baseProviderRepository );

            if ( relativePath != null )
            {
                scmProvider.checkOut( baseRepository, new ScmFileSet( basedir ), null );

                File path = new File( basedir, relativePath );

                path.mkdirs();

                String folderNameToCommit = lastBaseProviderRepository.getRelativePath( baseProviderRepository );

                int addedFiles = addFiles( scmProvider, baseRepository, basedir, folderNameToCommit );

                if ( addedFiles == 0 )
                {
                    throw new TransferFailedException(
                        "Unable to add folder to SCM: " + new File( basedir, folderNameToCommit ) );
                }

                scmProvider.checkIn( baseRepository, new ScmFileSet( basedir, new File( folderNameToCommit ) ), null,
                                     "Adding required folders for Wagon.put" );

                return path;
            }
            else
            {
                /* folder already in SCM */

                CheckOutScmResult result = scmProvider.checkOut( repository, new ScmFileSet( basedir ), null );

                checkScmResult( result );

                return basedir;
            }
        }

        throw new TransferFailedException(
            "Unable to create directories in the remote repository: " + repository.getProviderRepository() );
    }

    /**
     * @return true
     */
    public boolean supportsDirectoryCopy()
    {
        return true;
    }

    public void putDirectory( File sourceDirectory, String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        if ( !sourceDirectory.isDirectory() )
        {
            throw new IllegalArgumentException( "Source is not a directory: " + sourceDirectory );
        }

        putInternal( sourceDirectory, destinationDirectory );
    }

    /**
     * Check that the ScmResult was a successful operation
     *
     * @param result
     * @throws TransferFailedException if result was not a successful operation
     */
    private void checkScmResult( ScmResult result )
        throws TransferFailedException
    {
        if ( !result.isSuccess() )
        {
            throw new TransferFailedException(
                "Unable to commit file. " + result.getProviderMessage() + " " + result.getCommandOutput() );
        }
    }

    public void closeConnection()
    {
    }

    /**
     * Not implemented
     *
     * @throws UnsupportedOperationException always
     */
    public boolean getIfNewer( String resourceName, File destination, long timestamp )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        throw new UnsupportedOperationException( "Not currently supported: getIfNewer" );
    }

    public void get( String resourceName, File destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        Resource resource = new Resource( resourceName );

        fireGetInitiated( resource, destination );

        String url = getRepository().getUrl() + "/" + resourceName;

        // remove the file
        url = url.substring( 0, url.lastIndexOf( '/' ) );

        ScmRepository scmRepository = getScmRepository( url );

        fireGetStarted( resource, destination );

        // TODO: limitations:
        //  - destination filename must match that in the repository - should allow the "-d" CVS equiv to be passed in
        //  - we don't get granular exceptions from SCM (ie, auth, not found)
        //  - need to make it non-recursive to save time
        //  - exists() check doesn't test if it is in SCM already

        try
        {
            File scmFile = new File( checkoutDirectory, resourceName );
            File basedir = scmFile.getParentFile();

            ScmProvider scmProvider = getScmProvider( scmRepository.getProvider() );

            String reservedScmFile = scmProvider.getScmSpecificFilename();

            if ( reservedScmFile != null && new File( basedir, reservedScmFile ).exists() )
            {
                scmProvider.update( scmRepository, new ScmFileSet( basedir ), null );
            }
            else
            {
                // TODO: this should be checking out a full hierachy (requires the -d equiv)
                basedir.mkdirs();

                scmProvider.checkOut( scmRepository, new ScmFileSet( basedir ), null );
            }

            if ( !scmFile.exists() )
            {
                throw new ResourceDoesNotExistException( "Unable to find resource " + destination + " after checkout" );
            }

            if ( !scmFile.equals( destination ) )
            {
                FileUtils.copyFile( scmFile, destination );
            }
        }
        catch ( ScmException e )
        {
            throw new TransferFailedException( "Error getting file from SCM", e );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Error getting file from SCM", e );
        }

        postProcessListeners( resource, destination, TransferEvent.REQUEST_GET );

        fireGetCompleted( resource, destination );
    }

}
