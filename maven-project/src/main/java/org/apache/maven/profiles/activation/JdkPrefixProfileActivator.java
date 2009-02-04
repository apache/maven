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

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Activation;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.util.StringUtils;

public class JdkPrefixProfileActivator
    extends DetectedProfileActivator
{
    private static final String JDK_VERSION = System.getProperty( "java.version" );

    public boolean isActive( Profile profile )
        throws ProfileActivationException
    {
        Activation activation = profile.getActivation();

        String jdk = activation.getJdk();

        // null case is covered by canDetermineActivation(), so we can do a straight startsWith() here.
        if ( jdk.startsWith( "[" ) || jdk.startsWith( "(" ) )
        {
            try
            {
                if ( matchJdkVersionRange( jdk ) )
                {
                    return true;
                }
                else
                {
                    return false;
                }
            }
            catch ( InvalidVersionSpecificationException e )
            {
                throw new ProfileActivationException( "Invalid JDK version in profile '" + profile.getId() + "': "
                    + e.getMessage() );
            }
        }

        boolean reverse = false;

        if ( jdk.startsWith( "!" ) )
        {
            reverse = true;
            jdk = jdk.substring( 1 );
        }

        if ( getJdkVersion().startsWith( jdk ) )
        {
            return !reverse;
        }
        else
        {
            return reverse;
        }
    }

    private boolean matchJdkVersionRange( String jdk )
        throws InvalidVersionSpecificationException
    {
        VersionRange jdkVersionRange = VersionRange.createFromVersionSpec( convertJdkToMavenVersion( jdk ) );
        DefaultArtifactVersion jdkVersion = new DefaultArtifactVersion( convertJdkToMavenVersion( getJdkVersion() ) );
        return jdkVersionRange.containsVersion( jdkVersion );
    }

    private String convertJdkToMavenVersion( String jdk )
    {
        return jdk.replaceAll( "_", "-" );
    }

    protected String getJdkVersion()
    {
        return JDK_VERSION;
    }

    protected boolean canDetectActivation( Profile profile )
    {
        return profile.getActivation() != null && StringUtils.isNotEmpty( profile.getActivation().getJdk() );
    }

}
