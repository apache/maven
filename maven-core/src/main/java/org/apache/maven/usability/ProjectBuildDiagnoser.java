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

import org.apache.maven.project.InvalidProjectModelException;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.validation.ModelValidationResult;
import org.apache.maven.usability.diagnostics.DiagnosisUtils;
import org.apache.maven.usability.diagnostics.ErrorDiagnoser;

public class ProjectBuildDiagnoser
    implements ErrorDiagnoser
{

    public boolean canDiagnose( Throwable error )
    {
        return DiagnosisUtils.containsInCausality( error, ProjectBuildingException.class );
    }

    public String diagnose( Throwable error )
    {
        ProjectBuildingException pbe =
            (ProjectBuildingException) DiagnosisUtils.getFromCausality( error, ProjectBuildingException.class );

        StringBuffer message = new StringBuffer();

        message.append( "Error building POM (may not be this project's POM)." ).append( "\n\n" );

        message.append( "\nProject ID: " ).append( pbe.getProjectId() );

        if ( pbe instanceof InvalidProjectModelException )
        {
            InvalidProjectModelException ipme = (InvalidProjectModelException) pbe;

            message.append( "\nPOM Location: " ).append( ipme.getPomLocation() );

            ModelValidationResult result = ipme.getValidationResult();

            if ( result != null )
            {
                message.append( "\nValidation Messages:\n\n" ).append( ipme.getValidationResult().render( "    " ) );
            }
        }

        message.append( "\n\n" ).append( "Reason: " ).append( pbe.getMessage() );

        message.append( "\n\n" );

        return message.toString();
    }

}
