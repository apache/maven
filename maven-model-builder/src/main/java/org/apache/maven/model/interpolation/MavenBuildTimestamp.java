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
package org.apache.maven.model.interpolation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.TimeZone;

/**
 * MavenBuildTimestamp
 */
public class MavenBuildTimestamp {
    // ISO 8601-compliant timestamp for machine readability
    public static final String DEFAULT_BUILD_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    public static final String BUILD_TIMESTAMP_FORMAT_PROPERTY = "maven.build.timestamp.format";

    public static final TimeZone DEFAULT_BUILD_TIME_ZONE = TimeZone.getTimeZone("Etc/UTC");

    private String formattedTimestamp;

    public MavenBuildTimestamp() {
        this(new Date());
    }

    public MavenBuildTimestamp(Date time) {
        this(time, DEFAULT_BUILD_TIMESTAMP_FORMAT);
    }

    public MavenBuildTimestamp(Date time, Properties properties) {
        this(time, properties != null ? properties.getProperty(BUILD_TIMESTAMP_FORMAT_PROPERTY) : null);
    }

    public MavenBuildTimestamp(Date time, String timestampFormat) {
        if (timestampFormat == null) {
            timestampFormat = DEFAULT_BUILD_TIMESTAMP_FORMAT;
        }
        if (time == null) {
            time = new Date();
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat(timestampFormat);
        dateFormat.setCalendar(new GregorianCalendar());
        dateFormat.setTimeZone(DEFAULT_BUILD_TIME_ZONE);
        formattedTimestamp = dateFormat.format(time);
    }

    public String formattedTimestamp() {
        return formattedTimestamp;
    }
}
