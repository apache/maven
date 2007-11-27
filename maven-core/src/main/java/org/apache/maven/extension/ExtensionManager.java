package org.apache.maven.extension;

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

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

import java.util.List;

/**
 * Used to locate extensions.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public interface ExtensionManager
{
    void addExtension( Extension extension,
                       MavenProject project,
                       MavenExecutionRequest request )
        throws ExtensionManagerException;

    void registerWagons();

    void addExtension( Extension extension,
                       Model originatingModel,
                       List remoteRepositories,
                       MavenExecutionRequest request )
        throws ExtensionManagerException;

    void addPluginAsExtension( Plugin plugin,
                               Model originatingModel,
                               List remoteRepositories,
                               MavenExecutionRequest request )
        throws ExtensionManagerException;
}
