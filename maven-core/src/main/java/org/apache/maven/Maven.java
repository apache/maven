package org.apache.maven;

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

import org.apache.maven.lifecycle.goal.GoalNotFoundException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public interface Maven
{
    static String ROLE = Maven.class.getName();

    // ----------------------------------------------------------------------
    // Execution
    // ----------------------------------------------------------------------

    ExecutionResponse execute( List goals )
        throws GoalNotFoundException;

    ExecutionResponse execute( MavenProject project, List goals )
        throws GoalNotFoundException;

    ExecutionResponse execute( File project, List goals )
        throws ProjectBuildingException, GoalNotFoundException;

    // ----------------------------------------------------------------------
    // Reactor execution
    // ----------------------------------------------------------------------

    ExecutionResponse executeReactor( String goal, String includes, String excludes )
        throws ReactorException, GoalNotFoundException;

    // ----------------------------------------------------------------------
    // Goal descriptors
    // ----------------------------------------------------------------------

    Map getMojoDescriptors();

    MojoDescriptor getMojoDescriptor( String goalId );

    // ----------------------------------------------------------------------
    // Maven home
    // ----------------------------------------------------------------------

    void setMavenHome( String mavenHome );

    String getMavenHome();

    void setMavenHomeLocal( String mavenHomeLocal );

    String getMavenHomeLocal();

    // ----------------------------------------------------------------------
    // Maven project handling
    // ----------------------------------------------------------------------

    MavenProject getProject( File project )
        throws ProjectBuildingException;
}
