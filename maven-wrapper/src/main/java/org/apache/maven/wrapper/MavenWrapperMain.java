package org.apache.maven.wrapper;

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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Main entry point for the Maven Wrapper, delegating wrapper execution to {@link WrapperExecutor}.
 *
 * @author Hans Dockter
 */
public class MavenWrapperMain
{
    private static final String POM_PROPERTIES = "/META-INF/maven/org.apache.maven/maven-wrapper/pom.properties";

    public static final String DEFAULT_MAVEN_USER_HOME = System.getProperty( "user.home" ) + "/.m2";

    public static final String MVNW_VERBOSE = "MVNW_VERBOSE";

    public static final String MVNW_USERNAME = "MVNW_USERNAME";

    public static final String MVNW_PASSWORD = "MVNW_PASSWORD";

    public static final String MVNW_REPOURL = "MVNW_REPOURL";

    public static final String MVN_PATH =
        "org/apache/maven/apache-maven/" + wrapperVersion() + "/apache-maven-" + wrapperVersion() + "-bin.zip";

    public static void main( String[] args )
        throws Exception
    {
        Path wrapperJar = wrapperJar();
        Path propertiesFile = wrapperProperties( wrapperJar );

        String wrapperVersion = wrapperVersion();
        Logger.info( "Apache Maven Wrapper " + wrapperVersion );

        WrapperExecutor wrapperExecutor = WrapperExecutor.forWrapperPropertiesFile( propertiesFile );
        wrapperExecutor.execute( args, new Installer( new DefaultDownloader( "mvnw", wrapperVersion ),
                                                      new PathAssembler( mavenUserHome() ) ),
                                 new BootstrapMainStarter() );
    }

    private static Path wrapperProperties( Path wrapperJar ) throws URISyntaxException
    {
        return wrapperJar().resolveSibling( wrapperJar.getFileName().toString().replaceFirst( "\\.jar$",
                                                                                              ".properties" ) );
    }

    private static Path wrapperJar() throws URISyntaxException
    {
        URI location = MavenWrapperMain.class.getProtectionDomain().getCodeSource().getLocation().toURI();

        return Paths.get( location );
    }

    static String wrapperVersion()
    {
        try ( InputStream resourceAsStream = MavenWrapperMain.class.getResourceAsStream( POM_PROPERTIES ) )
        {
            if ( resourceAsStream == null )
            {
                throw new IllegalStateException( POM_PROPERTIES + " not found." );
            }
            Properties mavenProperties = new Properties();
            mavenProperties.load( resourceAsStream );
            String version = mavenProperties.getProperty( "version" );
            if ( version == null )
            {
                throw new NullPointerException( "No version specified in " + POM_PROPERTIES );
            }
            return version;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Could not determine wrapper version.", e );
        }
    }

    private static Path mavenUserHome()
    {
        return Paths.get( DEFAULT_MAVEN_USER_HOME );
    }
}
