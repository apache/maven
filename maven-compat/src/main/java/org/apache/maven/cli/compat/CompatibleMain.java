package org.apache.maven.cli.compat;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.cli.MavenCli;
import org.codehaus.classworlds.ClassWorld;

/**
 * Main class used to shield the user from the rest of Maven in the event the user is using JDK < 1.5.
 * 
 * @since 2.2.0
 */
public class CompatibleMain
{

    public static void main( String[] args )
    {
        ClassWorld classWorld = new ClassWorld( "plexus.core", Thread.currentThread().getContextClassLoader() );

        int result = main( args, classWorld );

        System.exit( result );
    }

    /**
     * @noinspection ConfusingMainMethod
     */
    public static int main( String[] args, ClassWorld classWorld )
    {
        // ----------------------------------------------------------------------
        // Setup the command line parser
        // ----------------------------------------------------------------------
        
        String javaVersion = System.getProperty( "java.specification.version", "1.5" );
        if ( "1.4".equals( javaVersion ) || "1.3".equals( javaVersion ) 
             || "1.2".equals( javaVersion )  || "1.1".equals( javaVersion ) )
        {
	        System.out.println( "Java specification version: " + javaVersion );
            System.err.println( "This release of Maven requires Java version 1.5 or greater." );
            return 1;
        }
        
        return MavenCli.main( args, classWorld );
    }
}
