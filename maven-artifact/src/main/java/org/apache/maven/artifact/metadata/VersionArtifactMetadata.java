package org.apache.maven.artifact.metadata;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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
import java.util.Date;

/**
 * Contains metadata about a versioned artifact.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public interface VersionArtifactMetadata
    extends ArtifactMetadata, Comparable
{
    /**
     * Determine if the metadata is considered newer than a given date.
     * @return whether it is newer
     */
    boolean checkedSinceDate( Date date );

    /**
     * Determine if the metadata is considered newer than a given file.
     * @return whether it is newer
     */
    boolean newerThanFile( File file );

    /**
     * Get the resolved version from the metadata.
     * @return the resolved version
     */
    String constructVersion();
}
