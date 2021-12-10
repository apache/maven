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
package org.apache.maven.caching.its;

import org.apache.maven.caching.its.junit.IntegrationTest;
import org.apache.maven.caching.its.junit.Test;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;

@IntegrationTest( "src/test/projects/core-extension" )
public class CoreExtensionTest
{

    @Test
    void simple( Verifier verifier ) throws VerificationException
    {
        verifier.setAutoclean( false );

        verifier.setLogFileName( "../log-1.txt" );
        verifier.executeGoal( "verify" );
        verifier.verifyErrorFreeLog();

        verifier.setLogFileName( "../log-2.txt" );
        verifier.executeGoal( "verify" );
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog( "Found cached build, restoring from cache" );
    }

}
