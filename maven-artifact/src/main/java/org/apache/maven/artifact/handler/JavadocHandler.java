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

package org.apache.maven.artifact.handler;

import org.apache.maven.artifact.Artifact;

import java.io.File;

/**
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez </a>
 * @version $Id$
 */
public class JavadocHandler
    extends AbstractArtifactHandler
{
    public File source( String basedir, Artifact artifact )
    {
        return new File( basedir, artifact.getArtifactId() + "-" + artifact.getVersion() + "-javadocs." + extension() );
    }

    public String extension()
    {
        return "jar";
    }

    public String directory()
    {
        return "javadocs";
    }
}