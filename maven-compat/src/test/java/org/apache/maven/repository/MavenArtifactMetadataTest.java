package org.apache.maven.repository;

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

import org.apache.maven.repository.MavenArtifactMetadata;
import org.junit.Assert;
import org.junit.Test;

public class MavenArtifactMetadataTest {

    @Test
    public void toStringOutputNotNull() {

        // Arrange
        final MavenArtifactMetadata objectUnderTest = new MavenArtifactMetadata();
        objectUnderTest.setGroupId("!!!!!!!!!");
        objectUnderTest.setClassifier("AAAAAAAA");
        objectUnderTest.setArtifactId("!!!!!!!!!!");
        objectUnderTest.setScope(null);
        objectUnderTest.setType("!!!!!!!!");
        objectUnderTest.setVersion(" ");
        objectUnderTest.setDatum(null);

        // Act
        final String retval = objectUnderTest.toString();

        // Assert result
        Assert.assertEquals("!!!!!!!!!:!!!!!!!!!!: :AAAAAAAA:!!!!!!!!", retval);
    }

    @Test
    public void toStringOutputNotNull2() {

        // Arrange
        final MavenArtifactMetadata objectUnderTest = new MavenArtifactMetadata();
        objectUnderTest.setGroupId("!!!!!!!!!");
        objectUnderTest.setClassifier("AAAAAAAA");
        objectUnderTest.setArtifactId("!!!!!!!!!!");
        objectUnderTest.setScope(null);
        objectUnderTest.setType(null);
        objectUnderTest.setVersion(" ");
        objectUnderTest.setDatum(null);

        // Act
        final String retval = objectUnderTest.toString();

        // Assert result
        Assert.assertEquals("!!!!!!!!!:!!!!!!!!!!: :AAAAAAAA:jar", retval);
    }

    @Test
    public void toStringOutputNotNull3() {

        // Arrange
        final MavenArtifactMetadata objectUnderTest = new MavenArtifactMetadata();
        objectUnderTest.setGroupId("!!!!!!!!!");
        objectUnderTest.setClassifier(null);
        objectUnderTest.setArtifactId("!!!!!!!!!!");
        objectUnderTest.setScope(null);
        objectUnderTest.setType(null);
        objectUnderTest.setVersion(" ");
        objectUnderTest.setDatum(null);

        // Act
        final String retval = objectUnderTest.toString();

        // Assert result
        Assert.assertEquals("!!!!!!!!!:!!!!!!!!!!: ::jar", retval);
    }

}
