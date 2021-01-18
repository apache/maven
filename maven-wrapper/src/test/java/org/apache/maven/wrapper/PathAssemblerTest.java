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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Hans Dockter
 */
public class PathAssemblerTest
{
    public static final String TEST_MAVEN_USER_HOME = "someUserHome";

    private PathAssembler pathAssembler = new PathAssembler( Paths.get( TEST_MAVEN_USER_HOME ) );

    final WrapperConfiguration configuration = new WrapperConfiguration();

    @BeforeEach
    public void setup()
    {
        configuration.setDistributionBase( PathAssembler.MAVEN_USER_HOME_STRING );
        configuration.setDistributionPath( "somePath" );
        configuration.setZipBase( PathAssembler.MAVEN_USER_HOME_STRING );
        configuration.setZipPath( "somePath" );
    }

    @Test
    public void distributionDirWithMavenUserHomeBase()
        throws Exception
    {
        configuration.setDistribution( new URI( "http://server/dist/maven-0.9-bin.zip" ) );

        Path distributionDir = pathAssembler.getDistribution( configuration ).getDistributionDir();
        assertThat( distributionDir, is( Paths.get( TEST_MAVEN_USER_HOME, "/somePath/maven-0.9-bin" ) ) );
    }

    @Test
    public void distributionDirWithProjectBase()
        throws Exception
    {
        configuration.setDistributionBase( PathAssembler.PROJECT_STRING );
        configuration.setDistribution( new URI( "http://server/dist/maven-0.9-bin.zip" ) );

        Path distributionDir = pathAssembler.getDistribution( configuration ).getDistributionDir();
        assertThat( distributionDir, equalTo( Paths.get( currentDirPath(), "/somePath/maven-0.9-bin" ) ) );
    }

    @Test
    public void distributionDirWithUnknownBase()
        throws Exception
    {
        configuration.setDistribution( new URI( "http://server/dist/maven-1.0.zip" ) );
        configuration.setDistributionBase( "unknownBase" );

        RuntimeException e =
            assertThrows( RuntimeException.class, () -> pathAssembler.getDistribution( configuration ) );
        assertEquals( "Base: unknownBase is unknown", e.getMessage() );
    }

    @Test
    public void distZipWithMavenUserHomeBase()
        throws Exception
    {
        configuration.setDistribution( new URI( "http://server/dist/maven-1.0.zip" ) );

        Path dist = pathAssembler.getDistribution( configuration ).getZipFile();
        assertThat( dist.getFileName().toString(), equalTo( "maven-1.0.zip" ) );
        assertThat( dist.getParent(), equalTo( Paths.get( TEST_MAVEN_USER_HOME, "/somePath/maven-1.0" ) ) );
    }

    @Test
    public void distZipWithProjectBase()
        throws Exception
    {
        configuration.setZipBase( PathAssembler.PROJECT_STRING );
        configuration.setDistribution( new URI( "http://server/dist/maven-1.0.zip" ) );

        Path dist = pathAssembler.getDistribution( configuration ).getZipFile();
        assertThat( dist.getFileName().toString(), equalTo( "maven-1.0.zip" ) );
        assertThat( dist.getParent(), equalTo( Paths.get( currentDirPath(), "/somePath/maven-1.0" ) ) );
    }

    private String currentDirPath()
    {
        return System.getProperty( "user.dir" );
    }
}
