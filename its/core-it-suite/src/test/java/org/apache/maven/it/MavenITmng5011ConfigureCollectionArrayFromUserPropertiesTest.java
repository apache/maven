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

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5011">MNG-5011</a>.
 */
public class MavenITmng5011ConfigureCollectionArrayFromUserPropertiesTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng5011ConfigureCollectionArrayFromUserPropertiesTest() {
        super("[3.0.3,)");
    }

    /**
     * Verify that plugin parameters of type array/collection can be configured using user properties from the CLI.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-5011");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("-Dconfig.stringParams=");
        verifier.addCliArgument("-Dconfig.fileParams=foo,bar");
        verifier.addCliArgument("-Dconfig.listParam=,two,,four,");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/config.properties");

        assertEquals("0", props.getProperty("stringParams"));

        assertEquals("2", props.getProperty("fileParams"));
        assertEquals(
                new File(testDir, "foo").getCanonicalFile(),
                new File(props.getProperty("fileParams.0")).getCanonicalFile());
        assertEquals(
                new File(testDir, "bar").getCanonicalFile(),
                new File(props.getProperty("fileParams.1")).getCanonicalFile());

        assertEquals("5", props.getProperty("listParam"));
        assertEquals("", props.getProperty("listParam.0", ""));
        assertEquals("two", props.getProperty("listParam.1", ""));
        assertEquals("", props.getProperty("listParam.2", ""));
        assertEquals("four", props.getProperty("listParam.3", ""));
        assertEquals("", props.getProperty("listParam.4", ""));
    }
}
