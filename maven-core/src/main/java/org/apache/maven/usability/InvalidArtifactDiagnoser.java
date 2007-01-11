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

import org.apache.maven.artifact.InvalidArtifactRTException;
import org.apache.maven.usability.diagnostics.ErrorDiagnoser;

public class InvalidArtifactDiagnoser
    implements ErrorDiagnoser
{

    public boolean canDiagnose( Throwable error )
    {
        return error instanceof InvalidArtifactRTException;
    }

    public String diagnose( Throwable error )
    {
        StringBuffer diagnosis = new StringBuffer();

        InvalidArtifactRTException e = (InvalidArtifactRTException) error;

        diagnosis.append( "An invalid artifact was detected.\n\n" )
            .append( "This artifact might be in your project's POM, " )
            .append( "or it might have been included transitively during the resolution process. " )
            .append( "Here is the information we do have for this artifact:\n" )
            .append( "\n    o GroupID:     " ).append( maybeFlag( e.getGroupId() ) )
            .append( "\n    o ArtifactID:  " ).append( maybeFlag( e.getArtifactId() ) )
            .append( "\n    o Version:     " ).append( maybeFlag( e.getVersion() ) )
            .append( "\n    o Type:        " ).append( maybeFlag( e.getType() ) )
            .append( "\n" );

        return diagnosis.toString();
    }

    private String maybeFlag( String value )
    {
        if ( value == null || value.trim().length() < 1 )
        {
            return "<<< MISSING >>>";
        }
        else
        {
            return value;
        }
    }

}
