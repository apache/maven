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
package org.apache.maven.repository.internal;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteSnapshotMetadataTest {
    private Locale defaultLocale;

    private static final Pattern DATE_FILTER = Pattern.compile("\\..*");

    @BeforeEach
    void setLocaleToUseBuddhistCalendar() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(new Locale("th", "TH"));
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(defaultLocale);
    }

    static String gregorianDate() {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
        df.setCalendar(new GregorianCalendar());
        df.setTimeZone(RemoteSnapshotMetadata.DEFAULT_SNAPSHOT_TIME_ZONE);
        return df.format(new Date());
    }

    @Test
    void gregorianCalendarIsUsed() {
        String dateBefore = gregorianDate();

        RemoteSnapshotMetadata metadata =
                new RemoteSnapshotMetadata(new DefaultArtifact("a:b:1-SNAPSHOT"), new Date(), null);
        metadata.merge(new Metadata());

        String dateAfter = gregorianDate();

        String ts = metadata.metadata.getVersioning().getSnapshot().getTimestamp();
        String datePart = DATE_FILTER.matcher(ts).replaceAll("");

        /* Allow for this test running across midnight */
        Set<String> expected = new HashSet<>(Arrays.asList(dateBefore, dateAfter));
        assertTrue(expected.contains(datePart), "Expected " + datePart + " to be in " + expected);
    }

    @Test
    void buildNumberNotSet() {
        RemoteSnapshotMetadata metadata =
                new RemoteSnapshotMetadata(new DefaultArtifact("a:b:1-SNAPSHOT"), new Date(), null);
        metadata.merge(new Metadata());

        int buildNumber = metadata.metadata.getVersioning().getSnapshot().getBuildNumber();
        assertEquals(1, buildNumber);
    }

    @Test
    void buildNumberSet() {
        RemoteSnapshotMetadata metadata =
                new RemoteSnapshotMetadata(new DefaultArtifact("a:b:1-SNAPSHOT"), new Date(), 42);
        metadata.merge(new Metadata());

        int buildNumber = metadata.metadata.getVersioning().getSnapshot().getBuildNumber();
        assertEquals(42, buildNumber);
    }
}
