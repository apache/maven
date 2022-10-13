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
package org.apache.maven.api.plugin.annotations;

import org.apache.maven.api.annotations.Experimental;

/**
 * <a href="/ref/3.0.4/maven-core/lifecycles.html">Lifecycle phases</a>.
 *
 * @since 4.0
 */
@Experimental
public enum LifecyclePhase
{

    VALIDATE( "validate" ),
    INITIALIZE( "initialize" ),
    GENERATE_SOURCES( "generate-sources" ),
    PROCESS_SOURCES( "process-sources" ),
    GENERATE_RESOURCES( "generate-resources" ),
    PROCESS_RESOURCES( "process-resources" ),
    COMPILE( "compile" ),
    PROCESS_CLASSES( "process-classes" ),
    GENERATE_TEST_SOURCES( "generate-test-sources" ),
    PROCESS_TEST_SOURCES( "process-test-sources" ),
    GENERATE_TEST_RESOURCES( "generate-test-resources" ),
    PROCESS_TEST_RESOURCES( "process-test-resources" ),
    TEST_COMPILE( "test-compile" ),
    PROCESS_TEST_CLASSES( "process-test-classes" ),
    TEST( "test" ),
    PREPARE_PACKAGE( "prepare-package" ),
    PACKAGE( "package" ),
    PRE_INTEGRATION_TEST( "pre-integration-test" ),
    INTEGRATION_TEST( "integration-test" ),
    POST_INTEGRATION_TEST( "post-integration-test" ),
    VERIFY( "verify" ),
    INSTALL( "install" ),
    DEPLOY( "deploy" ),

    PRE_CLEAN( "pre-clean" ),
    CLEAN( "clean" ),
    POST_CLEAN( "post-clean" ),

    PRE_SITE( "pre-site" ),
    SITE( "site" ),
    POST_SITE( "post-site" ),
    SITE_DEPLOY( "site-deploy" ),

    NONE( "" );

    private final String id;

    LifecyclePhase( String id )
    {
        this.id = id;
    }

    public String id()
    {
        return this.id;
    }

}
