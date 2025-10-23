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
@SuppressWarnings("all")
public class CoreExtension implements java.io.Serializable {

    // --------------------------/
    // - Class/Member Variables -/
    // --------------------------/

    /**
     * The group ID of the extension's artifact.
     */
    private String groupId;

    /**
     * The artifact ID of the extension.
     */
    private String artifactId;

    /**
     * The version of the extension.
     */
    private String version;

    /**
     * The class loading strategy: 'self-first' (the default),
     * 'parent-first' (loads classes from the parent, then from the
     * extension) or 'plugin' (follows the rules from extensions
     * defined as plugins).
     */
    private String classLoadingStrategy = "self-first";

    // -----------/
    // - Methods -/
    // -----------/

    /**
     * Get the artifact ID of the extension.
     *
     * @return String
     */
    public String getArtifactId() {
        return this.artifactId;
    } // -- String getArtifactId()

    /**
     * Get the class loading strategy: 'self-first' (the default),
     * 'parent-first' (loads classes from the parent, then from the
     * extension) or 'plugin' (follows the rules from extensions
     * defined as plugins).
     *
     * @return String
     */
    public String getClassLoadingStrategy() {
        return this.classLoadingStrategy;
    } // -- String getClassLoadingStrategy()

    /**
     * Get the group ID of the extension's artifact.
     *
     * @return String
     */
    public String getGroupId() {
        return this.groupId;
    } // -- String getGroupId()

    /**
     * Get the version of the extension.
     *
     * @return String
     */
    public String getVersion() {
        return this.version;
    } // -- String getVersion()

    /**
     * Set the artifact ID of the extension.
     *
     * @param artifactId a artifactId object.
     */
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    } // -- void setArtifactId( String )

    /**
     * Set the class loading strategy: 'self-first' (the default),
     * 'parent-first' (loads classes from the parent, then from the
     * extension) or 'plugin' (follows the rules from extensions
     * defined as plugins).
     *
     * @param classLoadingStrategy a classLoadingStrategy object.
     */
    public void setClassLoadingStrategy(String classLoadingStrategy) {
        this.classLoadingStrategy = classLoadingStrategy;
    } // -- void setClassLoadingStrategy( String )

    /**
     * Set the group ID of the extension's artifact.
     *
     * @param groupId a groupId object.
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    } // -- void setGroupId( String )

    /**
     * Set the version of the extension.
     *
     * @param version a version object.
     */
    public void setVersion(String version) {
        this.version = version;
    } // -- void setVersion( String )

    /**
     * Gets the identifier of the extension.
     *
     * @return The extension id in the form {@code <groupId>:<artifactId>:<version>}, never {@code null}.
     */
    public String getId() {
        StringBuilder id = new StringBuilder(128);

        id.append((getGroupId() == null) ? "[unknown-group-id]" : getGroupId());
        id.append(":");
        id.append((getArtifactId() == null) ? "[unknown-artifact-id]" : getArtifactId());
        id.append(":");
        id.append((getVersion() == null) ? "[unknown-version]" : getVersion());

        return id.toString();
    }
}
