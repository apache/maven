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
package org.apache.maven.cli.internal.extension.model;

/**
 * Describes a build extension to utilise.
 *
 * @deprecated Use {@link org.apache.maven.api.cli.extensions.CoreExtension} instead
 */
@Deprecated
public class CoreExtension implements java.io.Serializable {

    private String groupId;

    private String artifactId;

    private String version;

    private String classLoadingStrategy = "self-first";

    /**
     * Gets the group ID of the extension's artifact.
     *
     * @return the group ID
     */
    public String getGroupId() {
        return this.groupId;
    }

    /**
     * Sets the group ID of the extension's artifact.
     *
     * @param groupId the group ID
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * Gets the artifact ID of the extension.
     *
     * @return the artifact ID
     */
    public String getArtifactId() {
        return this.artifactId;
    }

    /**
     * Sets the artifact ID of the extension.
     *
     * @param artifactId the artifact ID
     */
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    /**
     * Gets the version of the extension.
     *
     * @return the version
     */
    public String getVersion() {
        return this.version;
    }

    /**
     * Sets the version of the extension.
     *
     * @param version the version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Gets the class loading strategy.
     *
     * @return the class loading strategy
     */
    public String getClassLoadingStrategy() {
        return this.classLoadingStrategy;
    }

    /**
     * Sets the class loading strategy.
     *
     * @param classLoadingStrategy the class loading strategy
     */
    public void setClassLoadingStrategy(String classLoadingStrategy) {
        this.classLoadingStrategy = classLoadingStrategy;
    }

    /**
     * Gets the identifier of the extension.
     *
     * @return the extension id
     */
    public String getId() {
        return (groupId != null ? groupId : "<null>") + ":" + (artifactId != null ? artifactId : "<null>") + ":"
                + (version != null ? version : "<null>");
    }
}
