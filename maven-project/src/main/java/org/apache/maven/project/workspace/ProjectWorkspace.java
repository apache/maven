package org.apache.maven.project.workspace;

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

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ModelAndFile;

import java.io.File;

public interface ProjectWorkspace
{

    String PROJECT_INSTANCE_BYFILE_KEY = "maven:project:project:file";

    String MODEL_AND_FILE_BYFILE_KEY = "maven:project:modelAndFile:file";

    String PROJECT_INSTANCE_BYGAV_KEY = "maven:project:project:GAV";

    String MODEL_AND_FILE_BYGAV_KEY = "maven:project:modelAndFile:GAV";

    MavenProject getProject( File projectFile );

    MavenProject getProject( String groupId, String artifactId, String version );

    void storeProjectByFile( MavenProject project );

    void storeProjectByCoordinate( MavenProject project );

    ModelAndFile getModelAndFile( String groupId, String artifactId, String version );

    ModelAndFile getModelAndFile( File modelFile );

    void storeModelAndFile( ModelAndFile modelAndFile );
}
