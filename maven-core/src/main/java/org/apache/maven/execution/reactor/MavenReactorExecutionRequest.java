package org.apache.maven.execution.reactor;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.AbstractMavenExecutionRequest;

import java.io.File;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MavenReactorExecutionRequest
extends AbstractMavenExecutionRequest
{
    private String includes;
    private String excludes;
    private File baseDirectory;

    public MavenReactorExecutionRequest( ArtifactRepository localRepository, Properties properties, List goals,
                                         String includes, String excludes, File baseDirectory )
    {
        super( localRepository, properties, goals );
        this.includes = includes;
        this.excludes = excludes;
        this.baseDirectory = baseDirectory;
        type = "reactor";
    }

    public String getIncludes()
    {
        return includes;
    }

    public String getExcludes()
    {
        return excludes;
    }

    public File getBaseDirectory()
    {
        return baseDirectory;
    }
}
