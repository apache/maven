package org.apache.maven.artifact.versioning;

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
import java.io.InterruptedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ComparableVersionIT
{

    @Test
    public void test()
        throws Exception
    {
        Files.walkFileTree( Paths.get( "target" ), new SimpleFileVisitor<Path>()
        {
            Pattern mavenArtifactJar = Pattern.compile( "maven-artifact-[\\d.]+(-SNAPSHOT)?\\.jar" );

            @Override
            public FileVisitResult visitFile( Path file, BasicFileAttributes attrs )
                throws IOException
            {
                String filename = file.getFileName().toString();
                if ( mavenArtifactJar.matcher( filename ).matches() )
                {
                    Process p = Runtime.getRuntime().exec( new String[] {
                        Paths.get( System.getProperty( "java.home" ), "bin/java" ).toString(),
                        "-jar",
                        file.toAbsolutePath().toString(),
                        "5.32",
                        "5.27" } );

                    try
                    {
                        assertEquals( 0, p.waitFor(), "Unexpected exit code" );
                    }
                    catch ( InterruptedException e )
                    {
                        throw new InterruptedIOException( e.toString() );
                    }
                    return FileVisitResult.TERMINATE;
                }
                else
                {
                    return FileVisitResult.CONTINUE;
                }
            }

            @Override
            public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs )
                throws IOException
            {
                if ( Paths.get( "target" ).equals( dir ) )
                {
                    return FileVisitResult.CONTINUE;
                }
                else
                {
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }
        } );
    }

}
