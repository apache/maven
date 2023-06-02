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
import java.util.List;

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5935">MNG-5935</a>.
 *
 */
public class MavenITmng5935OptionalLostInTranstiveManagedDependenciesTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng5935OptionalLostInTranstiveManagedDependenciesTest() {
        super("[3.5.1,)");
    }

    @Test
    public void testitMNG5935() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(
                getClass(), "/mng-5935-optional-lost-in-transtive-managed-dependencies");

        Verifier verifier = newVerifier(testDir.getAbsolutePath(), "remote");
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> dependencies = verifier.loadLines("target/dependencies.txt", "UTF-8");
        assertEquals(5, dependencies.size());
        assertEquals("com.mysema.querydsl:querydsl-core:jar:3.4.3 (optional)", dependencies.get(0));
        assertEquals("com.google.guava:guava:jar:17.0 (optional)", dependencies.get(1));
        assertEquals("com.google.code.findbugs:jsr305:jar:2.0.3 (optional)", dependencies.get(2));
        assertEquals("com.mysema.commons:mysema-commons-lang:jar:0.2.4 (optional)", dependencies.get(3));
        assertEquals("com.infradna.tool:bridge-method-annotation:jar:1.13 (optional)", dependencies.get(4));
    }
}
