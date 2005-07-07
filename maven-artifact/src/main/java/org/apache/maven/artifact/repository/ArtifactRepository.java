package org.apache.maven.artifact.repository;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;

/**
 * TODO: describe
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public interface ArtifactRepository
{
    String SNAPSHOT_POLICY_NEVER = "never";

    String SNAPSHOT_POLICY_ALWAYS = "always";

    String SNAPSHOT_POLICY_DAILY = "daily";

    String SNAPSHOT_POLICY_INTERVAL = "interval";

    String CHECKSUM_POLICY_FAIL = "fail";

    String CHECKSUM_POLICY_WARN = "warn";

    String CHECKSUM_ALGORITHM_SHA1 = "SHA-1";

    String CHECKSUM_ALGORITHM_MD5 = "MD5";

    String pathOf( Artifact artifact );

    String pathOfMetadata( ArtifactMetadata artifactMetadata );
    
    String formatDirectory( String directory );

    String getUrl();

    String getBasedir();

    String getSnapshotPolicy();

    String getProtocol();

    String getId();

    String getChecksumPolicy();

    boolean failOnChecksumMismatch();
}
