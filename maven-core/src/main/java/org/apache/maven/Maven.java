package org.apache.maven;

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

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.reactor.MavenExecutionException;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id$
 */
public interface Maven
{
    static String ROLE = Maven.class.getName();

    String POMv4 = "pom.xml";

    String RELEASE_POMv4 = "release-pom.xml";

    // ----------------------------------------------------------------------
    // Logging
    // ----------------------------------------------------------------------

    static final int LOGGING_LEVEL_DEBUG = 0;

    static final int LOGGING_LEVEL_INFO = 1;

    static final int LOGGING_LEVEL_WARN = 2;

    static final int LOGGING_LEVEL_ERROR = 3;

    static final int LOGGING_LEVEL_FATAL = 4;

    static final int LOGGING_LEVEL_DISABLE = 5;

    MavenExecutionResult execute( MavenExecutionRequest request );
}