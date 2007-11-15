package org.apache.maven.profiles.activation;

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

import org.apache.maven.model.Activation;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class JdkPrefixProfileActivator
    extends DetectedProfileActivator
{

    public static final String JDK_VERSION = "java.version";

    public boolean isActive( Profile profile, ProfileActivationContext context )
    {
        Activation activation = profile.getActivation();

        String jdk = activation.getJdk();

        Properties props = context.getExecutionProperties();
        String javaVersion = props.getProperty( JDK_VERSION );
        if ( javaVersion == null )
        {
            getLogger().warn( "Cannot locate java version property: " + JDK_VERSION + ". NOT enabling profile: " + profile.getId() );
            return false;
        }

        return isActive( javaVersion, jdk );
    }

    public boolean isActive( String jdkVersion, String expression )
    {
        boolean reverse = false;

        if ( expression.startsWith( "!" ) )
        {
            reverse = true;
            expression = expression.substring( 1 );
        }

        // null case is covered by canDetermineActivation(), so we can do a straight startsWith() here.
        boolean result = false;
        if ( expression.endsWith( "+" ) )
        {
            result = compareTo( asIntArray( jdkVersion ), asIntArray( expression ) ) >= 0;
        }
        else if ( expression.endsWith( "-" ) )
        {
            result = compareTo( asIntArray( jdkVersion ), asIntArray( expression ) ) <= 0;
        }
        else
        {
            // null case is covered by canDetermineActivation(), so we can do a straight startsWith() here.
            result = jdkVersion.startsWith( expression );
        }

        if ( reverse )
        {
            return !result;
        }
        else
        {
            return result;
        }
    }

    protected boolean canDetectActivation( Profile profile, ProfileActivationContext context )
    {
        return ( profile.getActivation() != null ) && StringUtils.isNotEmpty( profile.getActivation().getJdk() );
    }

    private static void parseNum( List pList, StringBuffer pBuffer )
    {
        if ( pBuffer.length() > 0 )
        {
            pList.add( new Integer( pBuffer.toString() ) );
            pBuffer.setLength( 0 );
        }
    }

    /** This method transforms a string like "1.5.0_06" into
     * new int[]{1, 5, 0, 6}.
     */
    private static int[] asIntArray( String pVersion )
    {
        List nums = new ArrayList();
        StringBuffer sb = new StringBuffer();
        while ( pVersion.length() > 0 )
        {
            char c = pVersion.charAt( 0 );
            pVersion = pVersion.substring( 1 );
            if ( Character.isDigit( c ) )
            {
                sb.append( c );
            }
            else
            {
                parseNum( nums, sb );
            }
        }
        parseNum( nums, sb );
        int[] result = new int[nums.size()];
        for ( int i = 0; i < result.length; i++ )
        {
            result[i] = ( (Integer) nums.get( i ) ).intValue();
        }
        return result;
    }

    /** This method compares to integer arrays, as created
     * by {@link #asIntArray(String)}.
     */
    private static int compareTo( int[] pVersion1, int[] pVersion2 )
    {
        int len = Math.max( pVersion1.length, pVersion2.length );
        for ( int i = 0; i < len; i++ )
        {
            int n1 = pVersion1.length > i ? pVersion1[i] : 0;
            int n2 = pVersion2.length > i ? pVersion2[i] : 0;
            int result = n1 - n2;
            if ( result != 0 )
            {
                return result;
            }
        }
        return 0;
    }

}
