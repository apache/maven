package org.apache.maven.artifact.metadata;

import org.apache.maven.artifact.Artifact;

import java.io.Reader;
import java.util.Set;

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

// Currently the only thing we need from the artifact metadata source is the
// dependency information, but i figure I'll just leave this generally as a
// metadata retrieval mechanism so we can retrieve whatever metadata about
// the artifact we may wish to provide in this layer. jvz.

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public interface ArtifactMetadataSource
{
    Set retrieve( Artifact artifact )
        throws ArtifactMetadataRetrievalException;
}
