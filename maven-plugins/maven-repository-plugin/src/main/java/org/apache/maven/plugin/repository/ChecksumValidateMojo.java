package org.apache.maven.plugin.repository;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;
import org.apache.maven.repository.ChecksumValidator;
import org.apache.maven.repository.RepositoryTools;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.observers.ChecksumObserver;

/**
 * @goal checksumvalidate
 * 
 * @description validates checksums in local repository
 * 
 * @parameter
 *  name="localRepository"
 *  type="org.apache.maven.artifact.repository.ArtifactRepository"
 *  required="true"
 *  validator=""
 *  expression="#localRepository"
 *  description=""
 * 
 * @parameter
 *  name="wagonManager"
 *  type="org.apache.maven.artifact.manager.WagonManager"
 *  required="true"
 *  validator=""
 *  expression="#component.org.apache.maven.artifact.manager.WagonManager"
 *  description=""
 */
public class ChecksumValidateMojo
    extends AbstractPlugin
{
    
    public void execute( PluginExecutionRequest request, PluginExecutionResponse response ) throws Exception
    {
        ArtifactRepository localRepository = (ArtifactRepository) request.getParameter( "localRepository" );

        WagonManager wagonManager = (WagonManager) request.getParameter( "wagonManager" );

        //ChecksumValidator checksumValidator = new ChecksumValidator();

        List artifacts = RepositoryTools.getAllArtifacts( localRepository );

        Iterator it = artifacts.iterator();

        while ( it.hasNext() )
        {
            Artifact artifact = (Artifact) it.next();
            
            System.out.println( artifact );
            boolean b = isValidChecksum( wagonManager, artifact, localRepository );
            
            if (!b) System.out.println( "NOT VALID" );
            
//            if ( !artifact.getChecksumFile().exists() )
//            {
//                // System.out.println( artifact );
//            }
//            else if ( !isValidChecksum( wagonManager, artifact, localRepository ) )
//            {
//                System.out.println( artifact );
//            }
        }
    }

    public boolean isValidChecksum( WagonManager wagonManager, Artifact artifact, ArtifactRepository localRepository )
        throws TransferFailedException, UnsupportedProtocolException
    {
        ChecksumObserver checksumObserver = new ChecksumObserver();

        ArtifactRepository tempRepository = new ArtifactRepository();

        File f = new File( "target/test-classes/temp/" );

        tempRepository.setUrl( "file://" + f.getPath() );

        Set set = new HashSet();

        set.add( localRepository );

        Wagon wagon = wagonManager.getWagon( "file://" );

        wagon.addTransferListener( checksumObserver );

        wagonManager.get( artifact, set, tempRepository );

        // File file = artifact.getFile();
        //
        // TransferEvent transferEvent = new TransferEvent( wagon, new
        // Resource(), TransferEvent.TRANSFER_COMPLETED,
        // TransferEvent.REQUEST_GET );
        //
        // checksumObserver.transferStarted( transferEvent );
        // checksumObserver.transferProgress(transferEvent, file);

        return true;
    }

}
