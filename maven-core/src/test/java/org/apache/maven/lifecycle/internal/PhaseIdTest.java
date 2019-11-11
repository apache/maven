package org.apache.maven.lifecycle.internal;

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

import junit.framework.TestCase;

import java.util.Arrays;

public class PhaseIdTest
    extends TestCase
{
    public void testStaticDefaultPhasesParse()
        throws Exception
    {
        for ( String phase : Arrays.asList( "validate", "initialize", "generate-sources", "process-sources",
                                            "generate-resources", "process-resources", "compile", "process-classes",
                                            "generate-test-sources", "process-test-sources", "generate-test-resources",
                                            "process-test-resources", "test-compile", "process-test-classes", "test",
                                            "prepare-package", "package", "pre-integration-test", "integration-test",
                                            "post-integration-test", "verify", "install", "deploy" ) )
        {
            assertEquals( phase, PhaseId.of( phase ).phase() );
            assertEquals( 0, PhaseId.of( phase ).priority() );
            assertEquals( PhaseExecutionPoint.AS, PhaseId.of( phase ).executionPoint() );
        }
    }

    public void testStaticSitePhasesParse()
        throws Exception
    {
        for ( String phase : Arrays.asList( "pre-site", "site", "post-site", "site-deploy" ) )
        {
            assertEquals( phase, PhaseId.of( phase ).phase() );
            assertEquals( 0, PhaseId.of( phase ).priority() );
            assertEquals( PhaseExecutionPoint.AS, PhaseId.of( phase ).executionPoint() );
        }
    }

    public void testStaticCleanPhasesParse()
        throws Exception
    {
        for ( String phase : Arrays.asList( "pre-clean", "clean", "post-clean" ) )
        {
            assertEquals( phase, PhaseId.of( phase ).phase() );
            assertEquals( 0, PhaseId.of( phase ).priority() );
            assertEquals( PhaseExecutionPoint.AS, PhaseId.of( phase ).executionPoint() );
        }
    }

    public void testDynamicPhasesParse()
    {
        assertEquals( "verify", PhaseId.of( "before:verify[1000]" ).phase() );
        assertEquals( "verify", PhaseId.of( "after:verify[1000]" ).phase() );
        assertEquals( "verify", PhaseId.of( "verify[1000]" ).phase() );
        assertEquals( PhaseExecutionPoint.BEFORE, PhaseId.of( "before:verify[1000]" ).executionPoint() );
        assertEquals( PhaseExecutionPoint.AFTER, PhaseId.of( "after:verify[1000]" ).executionPoint() );
        assertEquals( PhaseExecutionPoint.AS, PhaseId.of( "verify[1000]" ).executionPoint() );
        assertEquals( 1000, PhaseId.of( "before:verify[1000]" ).priority() );
        assertEquals( 1000, PhaseId.of( "after:verify[1000]" ).priority() );
        assertEquals( 1000, PhaseId.of( "verify[1000]" ).priority() );
        assertEquals( -1000, PhaseId.of( "before:verify[-1000]" ).priority() );
        assertEquals( -1000, PhaseId.of( "after:verify[-1000]" ).priority() );
        assertEquals( -1000, PhaseId.of( "verify[-1000]" ).priority() );
        assertEquals( "verify", PhaseId.of( "before:verify" ).phase() );
        assertEquals( "verify", PhaseId.of( "after:verify" ).phase() );
        assertEquals( PhaseExecutionPoint.BEFORE, PhaseId.of( "before:verify" ).executionPoint() );
        assertEquals( PhaseExecutionPoint.AFTER, PhaseId.of( "after:verify" ).executionPoint() );
        assertEquals( 0, PhaseId.of( "before:verify" ).priority() );
        assertEquals( 0, PhaseId.of( "after:verify" ).priority() );
    }

    public void testInvalidPhasesParseAsUnknown()
    {
        for ( String phase : Arrays.asList( "pre:verify", "before-verify", "BEFORE:verify", "verify(1000)",
                                            "verify[1000", "verify(1000]", "verify[1.0]", "verify[[1000]",
                                            "verify[1000]]", "[before]verify[1000]", "[1000]verify:before" ) )
        {
            assertEquals( phase, PhaseId.of( phase ).phase() );
            assertEquals( PhaseExecutionPoint.AS, PhaseId.of( phase ).executionPoint() );
            assertEquals( 0, PhaseId.of( phase ).priority() );
        }
        // a valid prefix gets extracted even if the priority fails to parse
        for ( String phase : Arrays.asList( "before:verify[1000", "before:verify[1.0]", "before:verify(1000]" ) )
        {
            assertEquals( phase.substring( 7 ), PhaseId.of( phase ).phase() );
            assertEquals( PhaseExecutionPoint.BEFORE, PhaseId.of( phase ).executionPoint() );
            assertEquals( 0, PhaseId.of( phase ).priority() );
        }
        // a valid priority gets extracted even if the prefix fails to parse
        for ( String phase : Arrays.asList( "beFore:verify[1000]", "before-verify[1000]", "pre:verify[1000]" ) )
        {
            assertEquals( phase.replace( "[1000]", "" ), PhaseId.of( phase ).phase() );
            assertEquals( PhaseExecutionPoint.AS, PhaseId.of( phase ).executionPoint() );
            assertEquals( 1000, PhaseId.of( phase ).priority() );
        }
    }
}
