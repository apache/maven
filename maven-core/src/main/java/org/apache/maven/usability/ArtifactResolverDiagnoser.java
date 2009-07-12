package org.apache.maven.usability;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.usability.diagnostics.DiagnosisUtils;
import org.apache.maven.usability.diagnostics.ErrorDiagnoser;

import java.io.IOException;

public class ArtifactResolverDiagnoser
    implements ErrorDiagnoser
{

    private WagonManager wagonManager;

    public boolean canDiagnose( Throwable error )
    {
        return DiagnosisUtils.containsInCausality( error, ArtifactResolutionException.class );
    }

    public String diagnose( Throwable error )
    {
        ArtifactResolutionException exception =
            (ArtifactResolutionException) DiagnosisUtils.getFromCausality( error, ArtifactResolutionException.class );

        StringBuffer message = new StringBuffer();

        message.append( "Failed to resolve artifact." );
        message.append( "\n\n" );
        message.append( exception.getMessage() );

        IOException ioe = (IOException) DiagnosisUtils.getFromCausality( exception, IOException.class );

        if ( ioe != null && ioe.getMessage() != null && exception.getMessage().indexOf( ioe.getMessage() ) < 0 )
        {
            message.append( "\n\nCaused by I/O exception: " ).append( ioe.getMessage() );
        }

        if ( !wagonManager.isOnline() )
        {
            message.append( "\n" ).append( SystemWarnings.getOfflineWarning() );
        }

        message.append( "\n" );

        return message.toString();
    }

}
