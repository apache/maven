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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4633">MNG-4633</a>.
 *
 *
 * @author Kristian Rosenvold
 */
@Disabled
public class MavenITmng4633DualCompilerExecutionsWeaveModeTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4633DualCompilerExecutionsWeaveModeTest() {
        super("[3.0-beta-2,)");
    }

    /**
     * Submodule2 depends on compiler output from submodule1, but dependency is in generate-resources phase in
     * submodule2. This effectively tests the module-locking of the project artifact.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-4633");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.addCliArgument("-T");
        verifier.addCliArgument("2W");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
