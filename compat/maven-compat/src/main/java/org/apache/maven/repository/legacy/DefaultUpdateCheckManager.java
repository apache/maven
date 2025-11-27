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
package org.apache.maven.repository.legacy;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.Authentication;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.repository.Proxy;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.internal.impl.TrackingFileManager;

/**
 * DefaultUpdateCheckManager
 */
@Named
@Singleton
@Deprecated
public class DefaultUpdateCheckManager extends AbstractLogEnabled implements UpdateCheckManager {
    private final TrackingFileManager trackingFileManager;

    private static final String ERROR_KEY_SUFFIX = ".error";

    @Inject
    public DefaultUpdateCheckManager(TrackingFileManager trackingFileManager) {
        this.trackingFileManager = trackingFileManager;
    }

    /**
     * For testing purposes.
     */
    public DefaultUpdateCheckManager(Logger logger, TrackingFileManager trackingFileManager) {
        enableLogging(logger);
        this.trackingFileManager = trackingFileManager;
    }

    public static final String LAST_UPDATE_TAG = ".lastUpdated";

    private static final String TOUCHFILE_NAME = "resolver-status.properties";

    @Override
    public boolean isUpdateRequired(Artifact artifact, ArtifactRepository repository) {
        File file = artifact.getFile();

        ArtifactRepositoryPolicy policy = artifact.isSnapshot() ? repository.getSnapshots() : repository.getReleases();

        if (!policy.isEnabled()) {
            if (getLogger().isDebugEnabled()) {
                getLogger()
                        .debug("Skipping update check for " + artifact + " (" + file + ") from " + repository.getId()
                                + " (" + repository.getUrl() + ")");
            }

            return false;
        }

        if (getLogger().isDebugEnabled()) {
            getLogger()
                    .debug("Determining update check for " + artifact + " (" + file + ") from " + repository.getId()
                            + " (" + repository.getUrl() + ")");
        }

        if (file == null) {
            // TODO throw something instead?
            return true;
        }

        Date lastCheckDate;

        if (file.exists()) {
            lastCheckDate = new Date(file.lastModified());
        } else {
            File touchfile = getTouchfile(artifact);
            lastCheckDate = readLastUpdated(touchfile, getRepositoryKey(repository));
        }

        return (lastCheckDate == null) || policy.checkOutOfDate(lastCheckDate);
    }

    @Override
    public boolean isUpdateRequired(RepositoryMetadata metadata, ArtifactRepository repository, File file) {
        // Here, we need to determine which policy to use. Release updateInterval will be used when
        // the metadata refers to a release artifact or meta-version, and snapshot updateInterval will be used when
        // it refers to a snapshot artifact or meta-version.
        // NOTE: Release metadata includes version information about artifacts that have been released, to allow
        // meta-versions like RELEASE and LATEST to resolve, and also to allow retrieval of the range of valid, released
        // artifacts available.
        ArtifactRepositoryPolicy policy = metadata.getPolicy(repository);

        if (!policy.isEnabled()) {
            if (getLogger().isDebugEnabled()) {
                getLogger()
                        .debug("Skipping update check for " + metadata.getKey() + " (" + file + ") from "
                                + repository.getId() + " (" + repository.getUrl() + ")");
            }

            return false;
        }

        if (getLogger().isDebugEnabled()) {
            getLogger()
                    .debug("Determining update check for " + metadata.getKey() + " (" + file + ") from "
                            + repository.getId() + " (" + repository.getUrl() + ")");
        }

        if (file == null) {
            // TODO throw something instead?
            return true;
        }

        Date lastCheckDate = readLastUpdated(metadata, repository, file);

        return (lastCheckDate == null) || policy.checkOutOfDate(lastCheckDate);
    }

    private Date readLastUpdated(RepositoryMetadata metadata, ArtifactRepository repository, File file) {
        File touchfile = getTouchfile(metadata, file);

        String key = getMetadataKey(repository, file);

        return readLastUpdated(touchfile, key);
    }

    @Override
    public String getError(Artifact artifact, ArtifactRepository repository) {
        File touchFile = getTouchfile(artifact);
        return getError(touchFile, getRepositoryKey(repository));
    }

    @Override
    public void touch(Artifact artifact, ArtifactRepository repository, String error) {
        File file = artifact.getFile();

        File touchfile = getTouchfile(artifact);

        if (file.exists()) {
            trackingFileManager.delete(touchfile);
        } else {
            writeLastUpdated(touchfile, getRepositoryKey(repository), error);
        }
    }

    @Override
    public void touch(RepositoryMetadata metadata, ArtifactRepository repository, File file) {
        File touchfile = getTouchfile(metadata, file);

        String key = getMetadataKey(repository, file);

        writeLastUpdated(touchfile, key, null);
    }

    String getMetadataKey(ArtifactRepository repository, File file) {
        return repository.getId() + '.' + file.getName() + LAST_UPDATE_TAG;
    }

    String getRepositoryKey(ArtifactRepository repository) {
        StringBuilder buffer = new StringBuilder(256);

        Proxy proxy = repository.getProxy();
        if (proxy != null) {
            if (proxy.getUserName() != null) {
                int hash = (proxy.getUserName() + proxy.getPassword()).hashCode();
                buffer.append(hash).append('@');
            }
            buffer.append(proxy.getHost()).append(':').append(proxy.getPort()).append('>');
        }

        // consider the username&password because a repo manager might block artifacts depending on authorization
        Authentication auth = repository.getAuthentication();
        if (auth != null) {
            int hash = (auth.getUsername() + auth.getPassword()).hashCode();
            buffer.append(hash).append('@');
        }

        // consider the URL (instead of the id) as this most closely relates to the contents in the repo
        buffer.append(repository.getUrl());

        return buffer.toString();
    }

    private void writeLastUpdated(File touchfile, String key, String error) {
        HashMap<String, String> update = new HashMap<>();
        update.put(key, Long.toString(System.currentTimeMillis()));
        update.put(key + ERROR_KEY_SUFFIX, error); // error==null => remove mapping
        trackingFileManager.update(touchfile, update);
    }

    Date readLastUpdated(File touchfile, String key) {
        getLogger().debug("Searching for " + key + " in resolution tracking file.");

        Properties props = read(touchfile);
        if (props != null) {
            String rawVal = props.getProperty(key);
            if (rawVal != null) {
                try {
                    return new Date(Long.parseLong(rawVal));
                } catch (NumberFormatException e) {
                    getLogger().debug("Cannot parse lastUpdated date: '" + rawVal + "'. Ignoring.", e);
                }
            }
        }
        return null;
    }

    private String getError(File touchFile, String key) {
        Properties props = read(touchFile);
        if (props != null) {
            return props.getProperty(key + ERROR_KEY_SUFFIX);
        }
        return null;
    }

    private Properties read(File touchfile) {
        return trackingFileManager.read(touchfile);
    }

    File getTouchfile(Artifact artifact) {
        StringBuilder sb = new StringBuilder(128);
        sb.append(artifact.getArtifactId());
        sb.append('-').append(artifact.getBaseVersion());
        if (artifact.getClassifier() != null) {
            sb.append('-').append(artifact.getClassifier());
        }
        sb.append('.').append(artifact.getType()).append(LAST_UPDATE_TAG);
        return new File(artifact.getFile().getParentFile(), sb.toString());
    }

    File getTouchfile(RepositoryMetadata metadata, File file) {
        return new File(file.getParent(), TOUCHFILE_NAME);
    }
}
