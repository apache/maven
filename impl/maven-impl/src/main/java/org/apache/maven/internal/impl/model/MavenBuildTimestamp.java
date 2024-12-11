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
package org.apache.maven.internal.impl.model;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.maven.api.Constants;
import org.apache.maven.api.MonotonicTime;

/**
 * MavenBuildTimestamp
 */
public class MavenBuildTimestamp {
    // ISO 8601-compliant timestamp for machine readability
    public static final String DEFAULT_BUILD_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";

    public static final TimeZone DEFAULT_BUILD_TIME_ZONE = TimeZone.getTimeZone("Etc/UTC");

    private final String formattedTimestamp;

    public MavenBuildTimestamp() {
        this(MonotonicTime.now());
    }

    public MavenBuildTimestamp(MonotonicTime time) {
        this(time, DEFAULT_BUILD_TIMESTAMP_FORMAT);
    }

    public MavenBuildTimestamp(MonotonicTime time, Map<String, String> properties) {
        this(time, properties != null ? properties.get(Constants.MAVEN_BUILD_TIMESTAMP_FORMAT) : null);
    }

    /**
     *
     * @deprecated Use {@link #MavenBuildTimestamp(MonotonicTime, Map)} or extract the format and pass it
     *             to {@link #MavenBuildTimestamp(MonotonicTime, String)} instead.
     */
    @Deprecated
    public MavenBuildTimestamp(MonotonicTime time, Properties properties) {
        this(time, properties != null ? properties.getProperty(Constants.MAVEN_BUILD_TIMESTAMP_FORMAT) : null);
    }

    public MavenBuildTimestamp(MonotonicTime time, String timestampFormat) {
        if (timestampFormat == null) {
            timestampFormat = DEFAULT_BUILD_TIMESTAMP_FORMAT;
        }
        if (time == null) {
            time = MonotonicTime.now();
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timestampFormat);
        formattedTimestamp = formatter.format(time);
    }

    public String formattedTimestamp() {
        return formattedTimestamp;
    }
}
