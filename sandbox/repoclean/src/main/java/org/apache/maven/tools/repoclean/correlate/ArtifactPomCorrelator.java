package org.apache.maven.tools.repoclean.correlate;

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

import org.apache.maven.tools.repoclean.report.Reporter;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jdcasey
 */
public class ArtifactPomCorrelator
{

    public static final String ROLE = ArtifactPomCorrelator.class.getName();

    public void correlateArtifactsToPoms( String[] poms, String[] artifacts, Reporter reporter )
    {
        reporter.info( "Starting artifact-to-POM correlation." );

        Map pomMap = new TreeMap();
        for ( int i = 0; i < poms.length; i++ )
        {
            String key = keyOf( poms[i] );

            if ( key == null )
            {
                reporter.error( "Found POM with invalid name: \'" + poms[i] + "\'" );
            }
            else
            {
                pomMap.put( key, poms[i] );
            }
        }

        for ( int i = 0; i < artifacts.length; i++ )
        {
            String key = keyOf( artifacts[i] );

            if ( key == null )
            {
                reporter.error( "Found artifact with invalid name: \'" + artifacts[i] + "\'" );
            }
            else if ( !pomMap.containsKey( key ) )
            {
                reporter.error( "Cannot find POM for artifact: \'" + artifacts[i] + "\'" );
            }
        }

        reporter.info( "Finished artifact-to-POM correlation." );
    }

    private String keyOf( String artifact )
    {
        Pattern keyPattern = Pattern.compile( "(.+)\\.(jar|pom)" );
        Matcher matcher = keyPattern.matcher( artifact );

        String key = null;
        if ( matcher.matches() )
        {
            key = matcher.group( 1 );
        }

        return key;
    }

}