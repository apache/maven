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

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.api.Constants;

/**
 * MavenBuildTimestamp
 */
public class MavenBuildTimestamp {
    // ISO 8601-compliant timestamp for machine readability
    public static final String DEFAULT_BUILD_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";

    private final String formattedTimestamp;

    public MavenBuildTimestamp() {
        this(Instant.now());
    }

    public MavenBuildTimestamp(Instant time) {
        this(time, DEFAULT_BUILD_TIMESTAMP_FORMAT);
    }

    public MavenBuildTimestamp(Instant time, Map<String, String> properties) {
        this(time, properties != null ? properties.get(Constants.MAVEN_BUILD_TIMESTAMP_FORMAT) : null);
    }

    /**
     *
     * @deprecated Use {@link #MavenBuildTimestamp(Instant, Map)} or extract the format and pass it
     *             to {@link #MavenBuildTimestamp(Instant, String)} instead.
     */
    @Deprecated
    public MavenBuildTimestamp(Instant time, Properties properties) {
        this(time, properties != null ? properties.getProperty(Constants.MAVEN_BUILD_TIMESTAMP_FORMAT) : null);
    }

    public MavenBuildTimestamp(Instant time, String timestampFormat) {
        if (timestampFormat == null) {
            timestampFormat = DEFAULT_BUILD_TIMESTAMP_FORMAT;
        }
        if (time == null) {
            time = Instant.now();
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat(timestampFormat);
        dateFormat.setCalendar(new GregorianCalendar());
        formattedTimestamp = dateFormat.format(new Date(time.toEpochMilli()));
    }

    public String formattedTimestamp() {
        return formattedTimestamp;
    }
}
