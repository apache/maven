package org.apache.maven.tools.repoclean.patch;

import org.apache.maven.model.v4_0_0.Model;
import org.apache.maven.tools.repoclean.report.Reporter;
import org.codehaus.plexus.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

/**
 * @author jdcasey
 */
public class V4ModelPatcher
{

    public static final String ROLE = V4ModelPatcher.class.getName();

    public void patchModel( Model model, String pomPath, Reporter reporter )
    {
        // TODO: Need to add more test scenarios to the unit test for this pattern.
        // I'm not convinced that this will catch everything.
        Pattern pathInfoPattern = Pattern.compile( "(.+)\\/poms\\/([-a-zA-Z0-9]+)-([0-9]+[-.0-9a-zA-Z]+).pom" );

        Matcher matcher = pathInfoPattern.matcher( pomPath );
        if ( !matcher.matches() )
        {
            reporter.info( "POM path: \'" + pomPath
                + "\' does not match naming convention. Cannot reliably patch model information from POM path." );
        }

        String parsedGroup = matcher.group( 1 );
        String parsedArtifact = matcher.group( 2 );
        String parsedVersion = matcher.group( 3 );

        if ( StringUtils.isEmpty( model.getGroupId() ) )
        {
            reporter.info( "Patching missing Group Id with parsed data: \'" + parsedGroup + "\'" );
            model.setGroupId( parsedGroup );
        }

        if ( StringUtils.isEmpty( model.getArtifactId() ) )
        {
            reporter.info( "Patching missing Artifact Id with parsed data: \'" + parsedArtifact + "\'" );
            model.setArtifactId( parsedArtifact );
        }

        if ( StringUtils.isEmpty( model.getVersion() ) )
        {
            reporter.info( "Patching missing Version with parsed data: \'" + parsedVersion + "\'" );
            model.setVersion( parsedVersion );
        }

    }

}