package org.apache.maven.lifecycle;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class LifecyclePhaseConstants
{
    public static String PHASE_GENERATE_SOURCES = "generate-sources";

    public static String PHASE_PROCESS_SOURCES = "process-sources";

    public static String PHASE_GENERATE_RESOURCES = "generate-resources";

    public static String PHASE_PROCESS_RESOURCES = "process-resources";

    public static String PHASE_COMPILE = "compile";

    public static String PHASE_PROCESS_CLASSES = "process-classes";

    public static String PHASE_GENERATE_TEST_SOURCES = "generate-test-sources";

    public static String PHASE_PROCESS_TEST_SOURCES = "process-test-sources";

    public static String PHASE_GENERATE_TEST_RESOURCES = "generate-test-resources";

    public static String PHASE_PROCESS_TEST_RESOURCES = "process-test-resources";

    public static String PHASE_TEST_COMPILE = "test-compile";

    public static String PHASE_PROCESS_TEST_CLASSES = "process-test-classes";

    public static String PHASE_EXECUTE_TESTS = "execute-tests";

    public static String PHASE_PACKAGE = "package";

    public static String PHASE_EXECUTE_INTEGRATION_TESTS = "execute-integration-tests";

    public static String PHASE_INSTALL = "install";

    public static String PHASE_DEPLOY = "deploy";
}
