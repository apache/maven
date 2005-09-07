package org.apache.maven.plugin.source;

/*
 * Copyright 2005 The Apache Software Foundation.
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

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.archiver.Archiver;

import java.io.File;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class SourceBundlerTest
    extends PlexusTestCase
{
    public void testNormalProject()
        throws Exception
    {
        SourceBundler sourceBundler = new SourceBundler();

        Archiver archiver = (Archiver) lookup( Archiver.ROLE, "jar" );

        File outputFile = getTestFile( "target/source-bundler-test/normal.jar" );

        File sourceDirectories[] = {
            getTestFile( "src/test/projects/normal/src/main/java" ),
            getTestFile( "src/test/projects/normal/src/main/resources" ),
            getTestFile( "src/test/projects/normal/src/test/java" ),
            getTestFile( "src/test/projects/normal/src/test/resources" ),
        };

        if ( outputFile.exists() )
        {
            assertTrue( "Could not delete output file: " + outputFile.getAbsolutePath(), outputFile.delete() );
        }

        sourceBundler.makeSourceBundle( outputFile, sourceDirectories, archiver );

        assertTrue( "Missing output file: " + outputFile.getAbsolutePath(), outputFile.isFile() );
    }
}
