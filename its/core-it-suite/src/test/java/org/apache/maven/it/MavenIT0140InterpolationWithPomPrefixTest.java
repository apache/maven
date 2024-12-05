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
package org.apache.maven.it;

import java.io.File;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <a href="https://issues.apache.org/jira/browse/MNG-7244">MNG-7244</a> removes the deprecation of
 * <code>pom.X</code>.
 * See {@link MavenITmng7244IgnorePomPrefixInExpressions}.
 *
 * @author Benjamin Bentmann
 */
public class MavenIT0140InterpolationWithPomPrefixTest extends AbstractMavenIntegrationTestCase {
    public MavenIT0140InterpolationWithPomPrefixTest() {
        super("[2.0,4.0.0-alpha-1)");
    }

    /**
     * Test that expressions of the form ${pom.*} resolve correctly to POM values.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit0140() throws Exception {
        File testDir = extractResources("/it0140");
        File child = new File(testDir, "child");

        Verifier verifier = newVerifier(child.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("initialize");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/interpolated.properties");
        String prefix = "project.properties.";

        assertEquals(child.getCanonicalFile(), new File(props.getProperty(prefix + "projectDir")).getCanonicalFile());

        assertEquals("org.apache.maven.its.it0140.child", props.getProperty(prefix + "projectGroupId"));
        assertEquals("child", props.getProperty(prefix + "projectArtifactId"));
        assertEquals("2.0-alpha-1", props.getProperty(prefix + "projectVersion"));
        assertEquals("jar", props.getProperty(prefix + "projectPackaging"));

        assertEquals("child-name", props.getProperty(prefix + "projectName"));
        assertEquals("child-desc", props.getProperty(prefix + "projectDesc"));
        assertEquals("http://child.org/", props.getProperty(prefix + "projectUrl"));
        assertEquals("2008", props.getProperty(prefix + "projectYear"));
        assertEquals("child-org-name", props.getProperty(prefix + "projectOrgName"));

        assertEquals("2.0.0", props.getProperty(prefix + "projectPrereqMvn"));
        assertEquals("http://scm.org/", props.getProperty(prefix + "projectScmUrl"));
        assertEquals("http://issue.org/", props.getProperty(prefix + "projectIssueUrl"));
        assertEquals("http://ci.org/", props.getProperty(prefix + "projectCiUrl"));
        assertEquals("child-dist-repo", props.getProperty(prefix + "projectDistRepoName"));

        assertEquals("org.apache.maven.its.it0140", props.getProperty(prefix + "parentGroupId"));
        assertEquals("parent", props.getProperty(prefix + "parentArtifactId"));
        assertEquals("1.0", props.getProperty(prefix + "parentVersion"));

        /*
         * NOTE: We intentionally do not check whether the build paths have been basedir aligned, that's another
         * story...
         */
        if (matchesVersionRange("(2.0.8,)")) {
            assertTrue(props.getProperty(prefix + "projectBuildOut").endsWith("bin"));
        }
        assertTrue(props.getProperty(prefix + "projectSiteOut").endsWith("doc"));
    }
}
