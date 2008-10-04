package org.apache.maven.execution;

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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.util.IOUtil;

/**
 * Describes runtime information about the application.
 *
 * @author Jason van Zyl
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class DefaultRuntimeInformation
    implements RuntimeInformation, Initializable
{    
    private ApplicationInformation applicationInformation;
    
    public ApplicationInformation getApplicationInformation()
    {
        return applicationInformation;
    }

    /** @deprecated Use getApplicationInformation() */
    public ArtifactVersion getApplicationVersion()
    {
        return applicationInformation.getVersion();
    }    
    
    public void initialize()
        throws InitializationException
    {
        applicationInformation = getVersion( getClass().getClassLoader(), "org.apache.maven", "maven-core" );
    }
    
    public static ApplicationInformation getVersion( ClassLoader loader, String groupId, String artifactId )
    {
        String MAVEN_PROPERTIES = "META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";

        String version = "unknown";
        String builtOn = "unknown";

        InputStream resourceAsStream = null;
        try
        {
            Properties properties = new Properties();
            resourceAsStream = loader.getResourceAsStream( MAVEN_PROPERTIES );
            
            if ( resourceAsStream == null )
            {
                return new ApplicationInformation( new DefaultArtifactVersion( "3.0" ), builtOn );
            }
            
            properties.load( resourceAsStream );

            String property = properties.getProperty( "version" );
            
            if ( property != null )
            {
                version = property;
            }
            
            property = properties.getProperty( "builtOn" );
            
            if ( property != null )
            {
                builtOn = property;
            }
            
            return new ApplicationInformation( new DefaultArtifactVersion( version ), builtOn );
            
        }
        catch ( IOException e )
        {
            return new ApplicationInformation( new DefaultArtifactVersion( version ), builtOn );
        }
        finally
        {
            IOUtil.close( resourceAsStream );
        }        
    }
}
