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
package org.apache.maven;

import org.apache.maven.exception.ExceptionHandler;
import org.codehaus.plexus.component.annotations.Requirement;

public class MavenTest extends AbstractCoreMavenComponentTestCase {
    @Requirement
    private Maven maven;

    @Requirement
    private ExceptionHandler exceptionHandler;

    protected void setUp() throws Exception {
        super.setUp();
        maven = lookup(Maven.class);
        exceptionHandler = lookup(ExceptionHandler.class);
    }

    @Override
    protected void tearDown() throws Exception {
        maven = null;
        exceptionHandler = null;
        super.tearDown();
    }

    protected String getProjectsDirectory() {
        return "src/test/projects/lifecycle-executor";
    }

    public void testLifecycleExecutionUsingADefaultLifecyclePhase() throws Exception {
        /*
        File pom = getProject( "project-with-additional-lifecycle-elements" );
        MavenExecutionRequest request = createMavenExecutionRequest( pom );
        MavenExecutionResult result = maven.execute( request );
        if ( result.hasExceptions() )
        {
            ExceptionSummary es = exceptionHandler.handleException( result.getExceptions().get( 0 ) );
            System.out.println( es.getMessage() );
            es.getException().printStackTrace();
            fail( "Maven did not execute correctly." );
        }
        */
    }
}
