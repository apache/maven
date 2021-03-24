package org.apache.maven.artifact.resolver.filter;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Exclusion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExclusionArtifactFilterTest
{
    private Artifact artifact;

    @BeforeEach
    public void setup()
    {
        artifact = mock( Artifact.class );
        when( artifact.getGroupId() ).thenReturn( "org.apache.maven" );
        when( artifact.getArtifactId() ).thenReturn( "maven-core" );
    }

    @Test
    public void testExcludeExact()
    {
        Exclusion exclusion = new Exclusion();
        exclusion.setGroupId( "org.apache.maven" );
        exclusion.setArtifactId( "maven-core" );
        ExclusionArtifactFilter filter = new ExclusionArtifactFilter( Collections.singletonList( exclusion ) );

        assertThat( filter.include( artifact ), is( false ) );
    }

    @Test
    public void testExcludeNoMatch()
    {
        Exclusion exclusion = new Exclusion();
        exclusion.setGroupId( "org.apache.maven" );
        exclusion.setArtifactId( "maven-model" );
        ExclusionArtifactFilter filter = new ExclusionArtifactFilter( Collections.singletonList( exclusion ) );

        assertThat( filter.include( artifact ), is( true ) );
    }

    @Test
    public void testExcludeGroupIdWildcard()
    {
        Exclusion exclusion = new Exclusion();
        exclusion.setGroupId( "*" );
        exclusion.setArtifactId( "maven-core" );
        ExclusionArtifactFilter filter = new ExclusionArtifactFilter( Collections.singletonList( exclusion ) );

        assertThat( filter.include( artifact ), is( false ) );
    }


    @Test
    public void testExcludeGroupIdWildcardNoMatch()
    {
        Exclusion exclusion = new Exclusion();
        exclusion.setGroupId( "*" );
        exclusion.setArtifactId( "maven-compat" );
        ExclusionArtifactFilter filter = new ExclusionArtifactFilter( Collections.singletonList( exclusion ) );

        assertThat( filter.include( artifact ), is( true ) );
    }

    @Test
    public void testExcludeArtifactIdWildcard()
    {
        Exclusion exclusion = new Exclusion();
        exclusion.setGroupId( "org.apache.maven" );
        exclusion.setArtifactId( "*" );
        ExclusionArtifactFilter filter = new ExclusionArtifactFilter( Collections.singletonList( exclusion ) );

        assertThat( filter.include( artifact ), is( false ) );
    }

    @Test
    public void testExcludeArtifactIdWildcardNoMatch()
    {
        Exclusion exclusion = new Exclusion();
        exclusion.setGroupId( "org.apache.groovy" );
        exclusion.setArtifactId( "*" );
        ExclusionArtifactFilter filter = new ExclusionArtifactFilter( Collections.singletonList( exclusion ) );

        assertThat( filter.include( artifact ), is( true ) );
    }

    @Test
    public void testExcludeAllWildcard()
    {
        Exclusion exclusion = new Exclusion();
        exclusion.setGroupId( "*" );
        exclusion.setArtifactId( "*" );
        ExclusionArtifactFilter filter = new ExclusionArtifactFilter( Collections.singletonList( exclusion ) );

        assertThat( filter.include( artifact ), is( false ) );
    }

    @Test
    public void testMultipleExclusionsExcludeArtifactIdWildcard()
    {
        Exclusion exclusion1 = new Exclusion();
        exclusion1.setGroupId( "org.apache.groovy" );
        exclusion1.setArtifactId( "*" );

        Exclusion exclusion2 = new Exclusion();
        exclusion2.setGroupId( "org.apache.maven" );
        exclusion2.setArtifactId( "maven-core" );

        ExclusionArtifactFilter filter = new ExclusionArtifactFilter( Arrays.asList( exclusion1, exclusion2 ) );

        assertThat( filter.include( artifact ), is( false ) );
    }

    @Test
    public void testMultipleExclusionsExcludeGroupIdWildcard()
    {
        Exclusion exclusion1 = new Exclusion();
        exclusion1.setGroupId( "*" );
        exclusion1.setArtifactId( "maven-model" );

        Exclusion exclusion2 = new Exclusion();
        exclusion2.setGroupId( "org.apache.maven" );
        exclusion2.setArtifactId( "maven-core" );

        ExclusionArtifactFilter filter = new ExclusionArtifactFilter( Arrays.asList( exclusion1, exclusion2 ) );

        assertThat( filter.include( artifact ), is( false ) );
    }
}