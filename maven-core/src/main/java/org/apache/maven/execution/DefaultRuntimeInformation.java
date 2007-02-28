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

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.util.IOUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Describes runtime information about the application.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class DefaultRuntimeInformation
    implements RuntimeInformation, Initializable
{
    private static final String MAVEN_GROUPID = "org.apache.maven";
    
    private static final String MAVEN_PROPERTIES = "META-INF/maven/" + MAVEN_GROUPID + "/maven-core/pom.properties";

    private ArtifactVersion applicationVersion;

    public ArtifactVersion getApplicationVersion()
    {
        return applicationVersion;
    }

    public void initialize()
        throws InitializationException
    {
        InputStream resourceAsStream = null;
        try
        {
            Properties properties = new Properties();
            resourceAsStream = getClass().getClassLoader().getResourceAsStream( MAVEN_PROPERTIES );
            
            if ( resourceAsStream == null )
            {
                throw new IllegalStateException( "Unable to find Maven properties in classpath: " + MAVEN_PROPERTIES );
            }
            properties.load( resourceAsStream );

            String property = properties.getProperty( "version" );
            if ( property == null )
            {
                throw new InitializationException( "maven-core properties did not include the version" );
            }

            applicationVersion = new DefaultArtifactVersion( property );
        }
        catch ( IOException e )
        {
            throw new InitializationException( "Unable to read properties file from maven-core", e );
        }
        finally
        {
            IOUtil.close( resourceAsStream );
        }
    }
}
