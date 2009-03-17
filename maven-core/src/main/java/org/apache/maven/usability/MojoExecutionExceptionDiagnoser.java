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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.usability.diagnostics.DiagnosisUtils;
import org.apache.maven.usability.diagnostics.ErrorDiagnoser;

public class MojoExecutionExceptionDiagnoser
    implements ErrorDiagnoser
{

    public boolean canDiagnose( Throwable error )
    {
        return DiagnosisUtils.containsInCausality( error, MojoExecutionException.class );
    }

    public String diagnose( Throwable error )
    {
        MojoExecutionException mee =
            (MojoExecutionException) DiagnosisUtils.getFromCausality( error, MojoExecutionException.class );

        StringBuffer message = new StringBuffer();

        Object source = mee.getSource();
        if ( source != null )
        {
            message.append( ": " ).append( mee.getSource() ).append( "\n" );
        }

        String shortMessage = mee.getMessage();
        if ( shortMessage != null )
        {
            message.append( shortMessage );
        }

        String longMessage = mee.getLongMessage();
        
        // the indexOf bit is very strange, but the compiler output for 1.5 source compiled using JDK 1.4 presents this case!
        // duplicating here for consistency, just in case there's another goofy plugin output I don't know of.
        if ( longMessage != null && !longMessage.equals( shortMessage ) && shortMessage.indexOf( longMessage ) < 0 )
        {
            message.append( "\n\n" ).append( longMessage );
        }

        Throwable directCause = mee.getCause();

        if ( directCause != null )
        {
            message.append( "\n" );

            String directCauseMessage = directCause.getMessage();

            String meeMessage = mee.getMessage();
            if ( ( directCauseMessage != null ) && ( meeMessage != null ) && meeMessage.indexOf( directCauseMessage ) < 0 )
            {
                message.append( "\nEmbedded error: " ).append( directCauseMessage );
            }

            DiagnosisUtils.appendRootCauseIfPresentAndUnique( directCause, message, false );
        }

        return message.toString();
    }

}
