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
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Hans Dockter
 */
public class BootstrapMainStarter
{
    public void start( String[] args, Path mavenHome )
        throws Exception
    {
        Path mavenJar = findLauncherJar( mavenHome );
        URLClassLoader contextClassLoader = new URLClassLoader( new URL[] { mavenJar.toUri().toURL() },
                                                                ClassLoader.getSystemClassLoader().getParent() );
        Thread.currentThread().setContextClassLoader( contextClassLoader );
        Class<?> mainClass = contextClassLoader.loadClass( "org.codehaus.plexus.classworlds.launcher.Launcher" );

        System.setProperty( "maven.home", mavenHome.toAbsolutePath().toString() );
        System.setProperty( "classworlds.conf", mavenHome.resolve( "bin/m2.conf" ).toAbsolutePath().toString() );

        Method mainMethod = mainClass.getMethod( "main", String[].class );
        mainMethod.invoke( null, new Object[] { args } );
    }

    private Path findLauncherJar( Path mavenHome ) throws RuntimeException, IOException
    {
        return Files.list( mavenHome.resolve( "boot" ) )
                        .filter( p -> p.getFileName().toString().matches( "plexus-classworlds-.*\\.jar" )  )
                        .findFirst()
                        .orElseThrow( () -> new RuntimeException(
                                  String.format( "Couldn't locate the Maven launcher JAR in Maven distribution '%s'.",
                                   mavenHome ) ) );
    }
}
