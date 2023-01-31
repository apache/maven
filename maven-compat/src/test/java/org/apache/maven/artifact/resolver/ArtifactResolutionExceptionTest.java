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
package org.apache.maven.artifact.resolver;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

/**
 * Test the artifact resolution exception message
 *
 * @author Mauro Talevi
 */
public class ArtifactResolutionExceptionTest extends TestCase {
    private static final String LS = System.lineSeparator();

    public void testMissingArtifactMessageFormat() {
        String message = "Missing artifact";
        String indentation = "  ";
        String groupId = "aGroupId";
        String artifactId = "anArtifactId";
        String version = "aVersion";
        String type = "jar";
        String classifier = "aClassifier";
        String downloadUrl = "http://somewhere.com/download";
        List<String> path = Arrays.asList("dependency1", "dependency2");
        String expected = "Missing artifact" + LS + LS + "  Try downloading the file manually from: " + LS
                + "      http://somewhere.com/download" + LS + LS + "  Then, install it using the command: " + LS
                + "      mvn install:install-file -DgroupId=aGroupId -DartifactId=anArtifactId -Dversion=aVersion "
                + "-Dclassifier=aClassifier -Dpackaging=jar -Dfile=/path/to/file" + LS + LS
                + "  Alternatively, if you host your own repository you can deploy the file there: " + LS
                + "      mvn deploy:deploy-file -DgroupId=aGroupId -DartifactId=anArtifactId"
                + " -Dversion=aVersion -Dclassifier=aClassifier -Dpackaging=jar -Dfile=/path/to/file"
                + " -Durl=[url] -DrepositoryId=[id]" + LS + LS + "  Path to dependency: " + LS + "  \t1) dependency1"
                + LS + "  \t2) dependency2" + LS + LS;
        String actual = AbstractArtifactResolutionException.constructMissingArtifactMessage(
                message, indentation, groupId, artifactId, version, type, classifier, downloadUrl, path);
        assertEquals(expected, actual);
    }
}
