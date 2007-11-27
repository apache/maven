package org.apache.maven.extension;

import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.interpolation.ModelInterpolationException;

import java.io.File;
import java.io.IOException;

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

public class ExtensionScanningException
    extends Exception
{

    private File pomFile;
    private String extensionId;
    private String modelId;
    private String moduleSubpath;

    public ExtensionScanningException( String message,
                                       File pomFile,
                                       ProjectBuildingException cause )
    {
        super( message, cause );
        this.pomFile = pomFile;
    }

    public ExtensionScanningException( String message,
                                       Model model,
                                       Extension extension,
                                       ExtensionManagerException cause )
    {
        super( message, cause );
        modelId = model.getId();
        extensionId = extension.getGroupId() + ":" + extension.getArtifactId();
    }

    public ExtensionScanningException( String message,
                                       ProjectBuildingException cause )
    {
        super( message, cause );
    }

    public ExtensionScanningException( String message,
                                       File pomFile,
                                       String moduleSubpath,
                                       IOException cause )
    {
        super( message, cause );
        this.pomFile = pomFile;
        this.moduleSubpath = moduleSubpath;
    }

    public ExtensionScanningException( String message,
                                       File pomFile,
                                       ModelInterpolationException cause )
    {
        super( message, cause );
        this.pomFile = pomFile;
    }

    public ExtensionScanningException( String message,
                                       Model model,
                                       Plugin plugin,
                                       ExtensionManagerException cause )
    {
        super( message, cause );
        modelId = model.getId();
        extensionId = plugin.getGroupId() + ":" + plugin.getArtifactId();
    }

    public File getPomFile()
    {
        return pomFile;
    }

    public String getExtensionId()
    {
        return extensionId;
    }

    public String getModelId()
    {
        return modelId;
    }

    public String getModuleSubpath()
    {
        return moduleSubpath;
    }

}
