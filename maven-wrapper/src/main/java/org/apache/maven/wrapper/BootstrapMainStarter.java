/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.maven.wrapper;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Hans Dockter
 */
public class BootstrapMainStarter
{
    public void start( String[] args, File mavenHome )
        throws Exception
    {
        File mavenJar = findLauncherJar( mavenHome );
        URLClassLoader contextClassLoader =
            new URLClassLoader( new URL[] { mavenJar.toURI().toURL() }, ClassLoader.getSystemClassLoader().getParent() );
        Thread.currentThread().setContextClassLoader( contextClassLoader );
        Class<?> mainClass = contextClassLoader.loadClass( "org.codehaus.plexus.classworlds.launcher.Launcher" );

        System.setProperty( "maven.home", mavenHome.getAbsolutePath() );
        System.setProperty( "classworlds.conf", new File( mavenHome, "/bin/m2.conf" ).getAbsolutePath() );

        Method mainMethod = mainClass.getMethod( "main", String[].class );
        mainMethod.invoke( null, new Object[] { args } );
    }

    private File findLauncherJar( File mavenHome )
    {
        for ( File file : new File( mavenHome, "boot" ).listFiles() )
        {
            if ( file.getName().matches( "plexus-classworlds-.*\\.jar" ) )
            {
                return file;
            }
        }
        throw new RuntimeException(
                                    String.format( "Could not locate the Maven launcher JAR in Maven distribution '%s'.",
                                                   mavenHome ) );
    }
}
