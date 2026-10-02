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
package org.apache.maven.api.feature;

import java.util.Map;

import org.apache.maven.api.Constants;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.services.ModelBuilder;

/**
 * Centralized class for Maven Core feature information.
 * Features configured are supposed to be final in a given maven session.
 *
 * @since 4.0.0
 */
public final class Features {

    private Features() {}

    /**
     * Check if the personality is "maven3".
     */
    public static boolean mavenMaven3Personality(@Nullable Map<String, ?> userProperties) {
        return doGet(userProperties, Constants.MAVEN_MAVEN3_PERSONALITY, false);
    }

    /**
     * Check if the consumer POM feature is active.
     */
    public static boolean consumerPom(@Nullable Map<String, ?> userProperties) {
        return doGet(userProperties, Constants.MAVEN_CONSUMER_POM, !mavenMaven3Personality(userProperties));
    }

    /**
     * Check if consumer POM flattening is enabled.
     */
    public static boolean consumerPomFlatten(@Nullable Map<String, ?> userProperties) {
        return doGet(userProperties, Constants.MAVEN_CONSUMER_POM_FLATTEN, false);
    }

    /**
     * Check if unused managed dependency removal is enabled during consumer POM flattening.
     */
    public static boolean consumerPomRemoveUnusedManagedDependencies(@Nullable Map<String, ?> userProperties) {
        return doGet(userProperties, Constants.MAVEN_CONSUMER_POM_REMOVE_UNUSED_MANAGED_DEPENDENCIES, true);
    }

    /**
     * Check if consumer POM repository sanitization is enabled: when active, only repositories
     * declared in the project's own POM file are published in the consumer POM, while repositories
     * inherited from parent POMs or injected by active settings.xml profiles are removed.
     */
    public static boolean consumerPomSanitizeRepositories(@Nullable Map<String, ?> userProperties) {
        return doGet(userProperties, Constants.MAVEN_CONSUMER_POM_SANITIZE_REPOSITORIES, true);
    }

    /**
     * Check if automatic subproject discovery is enabled for POM-packaged projects.
     */
    public static boolean discoverSubprojects(@Nullable Map<String, ?> userProperties) {
        return doGet(userProperties, Constants.MAVEN_PROJECT_DISCOVER_SUBPROJECTS, true);
    }

    /**
     * Check if build POM deployment is enabled.
     */
    public static boolean deployBuildPom(@Nullable Map<String, ?> userProperties) {
        return doGet(userProperties, Constants.MAVEN_DEPLOY_BUILD_POM, true);
    }

    /**
     * Check if the session's declared model version ({@code session.modelVersion}) implies
     * Maven 3-compatible defaults.
     * <p>
     * Returns {@code true} when <em>either</em>:
     * <ul>
     *   <li>{@link #mavenMaven3Personality(Map)} is {@code true}, or</li>
     *   <li>{@code session.modelVersion} is explicitly set to {@code "4.0.0"}.</li>
     * </ul>
     * This is the single place to check whether resolver and model-builder defaults should
     * revert to the Maven 3 / POM 4.0.0 behaviour (MNG-7984).
     *
     * @param userProperties the merged user/system/profile properties map
     * @return {@code true} if Maven 3-compatible model defaults should be used
     * @since 4.2.0
     */
    public static boolean maven3CompatModelVersion(@Nullable Map<String, ?> userProperties) {
        if (mavenMaven3Personality(userProperties)) {
            return true;
        }
        Object mv = userProperties != null ? userProperties.get(Constants.MAVEN_SESSION_MODEL_VERSION) : null;
        return mv != null && ModelBuilder.MODEL_VERSION_4_0_0.equals(mv.toString());
    }

    private static boolean doGet(Map<String, ?> userProperties, String key, boolean def) {
        return doGet(userProperties != null ? userProperties.get(key) : null, def);
    }

    private static boolean doGet(Object val, boolean def) {
        if (val instanceof Boolean bool) {
            return bool;
        } else if (val != null) {
            return Boolean.parseBoolean(val.toString());
        } else {
            return def;
        }
    }
}
