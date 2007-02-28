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

import org.apache.maven.profiles.activation.ProfileActivationException;
import org.apache.maven.usability.diagnostics.DiagnosisUtils;
import org.apache.maven.usability.diagnostics.ErrorDiagnoser;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

public class ProfileActivationDiagnoser
    implements ErrorDiagnoser
{

    public boolean canDiagnose( Throwable error )
    {
        return DiagnosisUtils.containsInCausality( error, ProfileActivationException.class );
    }

    public String diagnose( Throwable error )
    {
        ProfileActivationException activationException =
            (ProfileActivationException) DiagnosisUtils.getFromCausality( error, ProfileActivationException.class );

        StringBuffer messageBuffer = new StringBuffer();

        messageBuffer.append( "Error activating profiles." );
        messageBuffer.append( "\n\nReason: " ).append( activationException.getMessage() );

        if ( DiagnosisUtils.containsInCausality( activationException, ComponentLookupException.class ) )
        {
            ComponentLookupException cle = (ComponentLookupException) DiagnosisUtils.getFromCausality(
                activationException, ComponentLookupException.class );

            messageBuffer.append( "\n\nThere was a problem retrieving one or more profile activators." );
            messageBuffer.append( "\n" ).append( cle.getMessage() );
        }

        messageBuffer.append( "\n" );

        return messageBuffer.toString();
    }

}
