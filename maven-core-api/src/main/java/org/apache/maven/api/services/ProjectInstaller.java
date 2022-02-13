package org.apache.maven.api.services;

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

import org.apache.maven.api.Session;
import org.apache.maven.api.Project;

/**
 * This defines the interface to install a single Maven Project.
 * 
 * @author Karl Heinz Marbaise <a href="mailto:khmarbaise@apache.org">khmarbaise@apache.org</a>
 */
public interface ProjectInstaller extends Service
{
    /**
     * This will install a single project which may contain several artifacts. Those artifacts will be installed into
     * the appropriate repository.
     * 
     * <pre class="java">
     *  &#64;Parameter( defaultValue = "${session}", required = true, readonly = true )
     *  private MavenSession session;
     *  &#64;Parameter( defaultValue = "${project}", required = true, readonly = true )
     *  private MavenProject project;
     *  ..
     *  &#64;Component
     *  private ProjectInstaller installer;
     *  ...
     *  public void execute()
     *  {
     *    ProjectInstallerRequest pir =
     *      new ProjectInstallerRequest()
     *         .setProject( mavenProject );
     * 
     *    installer.install( session.getProjectBuildingRequest(), pir );
     *  }
     * </pre>
     * 
     * To set a different local repository than the current one in the Maven session, you can inject an instance of the
     * <code>RepositoryManager</code> and set the path to the local repository, called <code>localRepositoryPath</code>,
     * as such:
     * 
     * <pre class="java">
     * &#64;Component
     * private RepositoryManager repositoryManager;
     * 
     * buildingRequest = repositoryManager.setLocalRepositoryBasedir( buildingRequest, localRepositoryPath );
     * </pre>
     * 
     * @param request {@link ProjectInstallerRequest}
     * @throws ProjectInstallerException In case of problems to install artifacts.
     * @throws IllegalArgumentException in case of parameter <code>projectBuildingRequest</code> is <code>null</code> or
     *             parameter <code>projectInstallerRequest</code> is <code>null</code>.
     */
    void install( ProjectInstallerRequest request )
        throws ProjectInstallerException, IllegalArgumentException;

    default void install( Session session, Project project )
        throws ProjectInstallerException, IllegalArgumentException
    {
        install( ProjectInstallerRequest.build( session, project ) );
    }
}
