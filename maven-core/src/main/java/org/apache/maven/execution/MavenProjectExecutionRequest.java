package org.apache.maven.execution;

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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MavenProjectExecutionRequest
extends AbstractMavenExecutionRequest
{
    private File pom;

    public MavenProjectExecutionRequest( ArtifactRepository localRepository,
                                         Properties properties,
                                         List goals,
                                         File pom )
    {
        super( localRepository, properties, goals );

        this.pom = pom;

        type = "project";
    }

    public File getPom()
    {
        return pom;
    }

    public List getProjectFiles()
    {
        List files = new ArrayList();

        files.add( pom );

        return files;
    }
}
