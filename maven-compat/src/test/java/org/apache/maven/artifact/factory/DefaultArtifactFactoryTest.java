package org.apache.maven.artifact.factory;

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

import javax.inject.Inject;

import org.codehaus.plexus.testing.PlexusTest;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.VersionRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@PlexusTest
public class DefaultArtifactFactoryTest
{

    @Inject
    ArtifactFactory factory;

    @Test
    public void testPropagationOfSystemScopeRegardlessOfInheritedScope()
    {
        Artifact artifact = factory.createDependencyArtifact( "test-grp", "test-artifact", VersionRange.createFromVersion("1.0"), "type", null, "system", "provided" );
        Artifact artifact2 = factory.createDependencyArtifact( "test-grp", "test-artifact-2", VersionRange.createFromVersion("1.0"), "type", null, "system", "test" );
        Artifact artifact3 = factory.createDependencyArtifact( "test-grp", "test-artifact-3", VersionRange.createFromVersion("1.0"), "type", null, "system", "runtime" );
        Artifact artifact4 = factory.createDependencyArtifact( "test-grp", "test-artifact-4", VersionRange.createFromVersion("1.0"), "type", null, "system", "compile" );

        // this one should never happen in practice...
        Artifact artifact5 = factory.createDependencyArtifact( "test-grp", "test-artifact-5", VersionRange.createFromVersion("1.0"), "type", null, "system", "system" );

        assertEquals( "system", artifact.getScope() );
        assertEquals( "system", artifact2.getScope() );
        assertEquals( "system", artifact3.getScope() );
        assertEquals( "system", artifact4.getScope() );
        assertEquals( "system", artifact5.getScope() );
    }

}
