package org.apache.maven.legacy;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import java.io.File;
import java.util.List;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public interface Maven1xIntegration
{
    static String ROLE = Maven1xIntegration.class.getName();

    // ----------------------------------------------------------------------
    // Execution
    // ----------------------------------------------------------------------

    void execute( File project, List goals );

    // ----------------------------------------------------------------------
    // Reactor execution
    // ----------------------------------------------------------------------

    // TODO: perhaps?
    // ExecutionResponse executeReactor( String goal, String includes, String excludes );

    // ----------------------------------------------------------------------
    // Maven home
    // ----------------------------------------------------------------------
    void setMavenHome( String mavenHome );

    String getMavenHome();

    void setMavenHomeLocal( String mavenHomeLocal );

    String getMavenHomeLocal();
}

