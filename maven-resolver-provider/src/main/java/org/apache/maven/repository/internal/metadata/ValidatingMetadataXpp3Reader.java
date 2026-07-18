/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.repository.internal.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.util.PathUtils;

/**
 * Validating metadata reader.
 *
 * @since 3.10.0
 */
public final class ValidatingMetadataXpp3Reader {
    private final MetadataXpp3Reader mr = new MetadataXpp3Reader();

    /**
     * Delegates to {@link MetadataXpp3Reader#read(Reader, boolean)}
     */
    public Metadata read(Reader reader, boolean strict) throws IOException, XmlPullParserException {
        return validate(mr.read(reader, strict));
    }

    /**
     * Delegates to {@link MetadataXpp3Reader#read(InputStream, boolean)}
     */
    public Metadata read(InputStream in, boolean strict) throws IOException, XmlPullParserException {
        return validate(mr.read(in, strict));
    }

    /**
     * Validates {@link Metadata}.
     */
    public static Metadata validate(Metadata metadata) {
        if (metadata != null) {
            PathUtils.validatePathComponent(metadata.getVersion(), "version");
            Versioning versioning = metadata.getVersioning();
            if (versioning != null) {
                PathUtils.validatePathComponent(versioning.getLatest(), "versioning/latest");
                PathUtils.validatePathComponent(versioning.getRelease(), "versioning/release");
                for (int i = 0; i < versioning.getVersions().size(); i++) {
                    PathUtils.validatePathComponent(versioning.getVersions().get(i), "versioning/versions[" + i + "]");
                }
                for (int i = 0; i < versioning.getSnapshotVersions().size(); i++) {
                    PathUtils.validatePathComponent(
                            versioning.getSnapshotVersions().get(i).getVersion(),
                            "versioning/snapshotVersions[" + i + "]/version");
                }
            }
        }
        return metadata;
    }
}
