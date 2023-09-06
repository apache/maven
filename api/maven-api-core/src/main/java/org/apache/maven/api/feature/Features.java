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
import java.util.Properties;

import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Nullable;

/**
 * Centralized class for feature information
 *
 * @since 4.0.0
 */
public final class Features {

    public static final String BUILDCONSUMER = "maven.experimental.buildconsumer";

    private Features() {}

    public static boolean buildConsumer(@Nullable Properties userProperties) {
        return doGet(userProperties, BUILDCONSUMER, true);
    }

    public static boolean buildConsumer(@Nullable Map<String, String> userProperties) {
        return doGet(userProperties, BUILDCONSUMER, true);
    }

    public static boolean buildConsumer(@Nullable Session session) {
        return buildConsumer(session != null ? session.getUserProperties() : null);
    }

    private static boolean doGet(Properties userProperties, String key, boolean def) {
        return doGet(userProperties != null ? userProperties.get(key) : null, def);
    }

    private static boolean doGet(Map<String, ?> userProperties, String key, boolean def) {
        return doGet(userProperties != null ? userProperties.get(key) : null, def);
    }

    private static boolean doGet(Object val, boolean def) {
        if (val instanceof Boolean) {
            return (Boolean) val;
        } else if (val != null) {
            return Boolean.parseBoolean(val.toString());
        } else {
            return def;
        }
    }
}
