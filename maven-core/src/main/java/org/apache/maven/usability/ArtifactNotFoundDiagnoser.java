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
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.usability.diagnostics.DiagnosisUtils;
import org.apache.maven.usability.diagnostics.ErrorDiagnoser;

public class ArtifactNotFoundDiagnoser
    implements ErrorDiagnoser
{

    private WagonManager wagonManager;

    public boolean canDiagnose( Throwable error )
    {
        return DiagnosisUtils.containsInCausality( error, ArtifactNotFoundException.class );
    }

    public String diagnose( Throwable error )
    {
        ArtifactNotFoundException exception =
            (ArtifactNotFoundException) DiagnosisUtils.getFromCausality( error, ArtifactNotFoundException.class );

        StringBuffer message = new StringBuffer();

        message.append( "Failed to resolve artifact.\n" );
        message.append( "\nGroupId: " ).append( exception.getGroupId() );
        message.append( "\nArtifactId: " ).append( exception.getArtifactId() );
        message.append( "\nVersion: " ).append( exception.getVersion() );
        message.append( "\n\n" );
        message.append( "Reason: " ).append( exception.getMessage() );

        if ( !wagonManager.isOnline() )
        {
            message.append( "\n" ).append( SystemWarnings.getOfflineWarning() );
        }

        message.append( "\n" );

        return message.toString();
    }

}
