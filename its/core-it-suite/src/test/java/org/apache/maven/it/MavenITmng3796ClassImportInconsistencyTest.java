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
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3796">MNG-3796</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3796ClassImportInconsistencyTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng3796ClassImportInconsistencyTest() {
        super("(2.0.2,)");
    }

    /**
     * Verify that classes shared with the Maven core realm are properly imported into the plugin realm.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG3796() throws Exception {
        File testDir = extractResources("/mng-3796");
        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties pclProps = verifier.loadProperties("target/pcl.properties");
        assertNotNull(pclProps.getProperty("org.codehaus.plexus.util.xml.Xpp3Dom"));

        Properties tcclProps = verifier.loadProperties("target/tccl.properties");
        assertEquals(pclProps, tcclProps);
    }
}
