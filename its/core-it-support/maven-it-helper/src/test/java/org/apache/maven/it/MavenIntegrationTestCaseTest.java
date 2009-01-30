package org.apache.maven.it;

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
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.it.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import junit.framework.TestCase;

public class MavenIntegrationTestCaseTest
    extends TestCase
{
    public void testRemovePattern()
    {
        AbstractMavenIntegrationTestCase test = new AbstractMavenIntegrationTestCase( "[2.0,)" ) {};
        assertEquals( "2.1.0-M1", test.removePattern( new DefaultArtifactVersion( "2.1.0-M1" ) ).toString() );
        assertEquals( "2.1.0-M1", test.removePattern( new DefaultArtifactVersion( "2.1.0-M1-SNAPSHOT" ) ).toString() );
        assertEquals( "2.1.0-M1", test.removePattern( new DefaultArtifactVersion( "2.1.0-M1-RC1" ) ).toString() );
        assertEquals( "2.1.0-M1", test.removePattern( new DefaultArtifactVersion( "2.1.0-M1-RC1-SNAPSHOT" ) ).toString() );
        assertEquals( "2.0.10", test.removePattern( new DefaultArtifactVersion( "2.0.10" ) ).toString() );
        assertEquals( "2.0.10", test.removePattern( new DefaultArtifactVersion( "2.0.10-SNAPSHOT" ) ).toString() );
        assertEquals( "2.0.10", test.removePattern( new DefaultArtifactVersion( "2.0.10-RC1" ) ).toString() );
        assertEquals( "2.0.10", test.removePattern( new DefaultArtifactVersion( "2.0.10-RC1-SNAPSHOT" ) ).toString() );
    }
}
