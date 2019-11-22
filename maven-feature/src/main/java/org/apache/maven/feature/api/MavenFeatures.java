package org.apache.maven.feature.api;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * <strong>NOTE:</strong> This class is not part of any public api and can be changed or deleted without prior notice.
 *
 * An API to allow making assertions about the presence of specific features in Maven core.
 * Features are identified using string constants and are only intended for use in opt-in experiments.
 * Unknown features will always be reported as disabled.
 */
public interface MavenFeatures
{
    /**
     * Returns {@code true} if and only if the specified feature is enabled.
     *
     * @param context     the context within which to check the feature.
     * @param featureName the name of the feature.
     * @return {@code true} if and only if the specified feature is enabled.
     */
    boolean enabled( MavenFeatureContext context, String featureName );
}
