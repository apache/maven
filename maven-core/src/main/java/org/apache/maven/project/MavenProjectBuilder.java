package org.apache.maven.project;

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

public interface MavenProjectBuilder
{
    String ROLE = MavenProjectBuilder.class.getName();

    // ----------------------------------------------------------------------

    MavenProject build( File project )
        throws ProjectBuildingException;

    MavenProject build( File project, boolean followTransitiveDeps )
        throws ProjectBuildingException;

    MavenProject build( File mavenHomeLocal, File project )
        throws ProjectBuildingException;

    MavenProject build( File mavenHomeLocal, File project, boolean followTransitiveDeps )
        throws ProjectBuildingException;

    // ----------------------------------------------------------------------

    List getSortedProjects( List projects )
        throws Exception;
}
