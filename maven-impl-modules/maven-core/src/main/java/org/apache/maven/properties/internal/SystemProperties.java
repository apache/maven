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
package org.apache.maven.properties.internal;

import java.util.Properties;

/**
 * @since 3.2.3
 */
public class SystemProperties {
    /**
     * Thread-safe System.properties copy implementation.
     */
    public static void addSystemProperties(Properties props) {
        props.putAll(getSystemProperties());
    }

    /**
     * Returns a copy of {@link System#getProperties()} in a thread-safe manner.
     *
     * @return {@link System#getProperties()} obtained in a thread-safe manner.
     */
    public static Properties getSystemProperties() {
        return copyProperties(System.getProperties());
    }

    /**
     * Copies the given {@link Properties} object into a new {@link Properties} object, in a thread-safe manner.
     * @param properties Properties to copy.
     * @return Copy of the given properties.
     */
    public static Properties copyProperties(Properties properties) {
        final Properties copyProperties = new Properties();
        // guard against modification/removal of keys in the given properties (MNG-5670, MNG-6053, MNG-6105)
        synchronized (properties) {
            copyProperties.putAll(properties);
        }
        return copyProperties;
    }
}
