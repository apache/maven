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
package org.apache.maven.impl;

import org.apache.maven.api.Version;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
abstract class AbstractVersionTest {

    protected static final int X_LT_Y = -1;

    protected static final int X_EQ_Y = 0;

    protected static final int X_GT_Y = 1;

    protected abstract Version newVersion(String version);

    protected void assertOrder(int expected, String version1, String version2) {
        Version v1 = newVersion(version1);
        Version v2 = newVersion(version2);

        if (expected > 0) {
            assertThat(Integer.signum(v1.compareTo(v2))).as("expected " + v1 + " > " + v2).isEqualTo(1);
            assertThat(Integer.signum(v2.compareTo(v1))).as("expected " + v2 + " < " + v1).isEqualTo(-1);
            assertThat(v2).as("expected " + v1 + " != " + v2).isNotEqualTo(v1);
            assertThat(v1).as("expected " + v2 + " != " + v1).isNotEqualTo(v2);
        } else if (expected < 0) {
            assertThat(Integer.signum(v1.compareTo(v2))).as("expected " + v1 + " < " + v2).isEqualTo(-1);
            assertThat(Integer.signum(v2.compareTo(v1))).as("expected " + v2 + " > " + v1).isEqualTo(1);
            assertThat(v2).as("expected " + v1 + " != " + v2).isNotEqualTo(v1);
            assertThat(v1).as("expected " + v2 + " != " + v1).isNotEqualTo(v2);
        } else {
            assertThat(v1.compareTo(v2)).as("expected " + v1 + " == " + v2).isEqualTo(0);
            assertThat(v2.compareTo(v1)).as("expected " + v2 + " == " + v1).isEqualTo(0);
            assertThat(v2).as("expected " + v1 + " == " + v2).isEqualTo(v1);
            assertThat(v1).as("expected " + v2 + " == " + v1).isEqualTo(v2);
            assertThat(v2.hashCode()).as("expected #(" + v1 + ") == #(" + v1 + ")").isEqualTo(v1.hashCode());
        }
    }

    protected void assertSequence(String... versions) {
        for (int i = 0; i < versions.length - 1; i++) {
            for (int j = i + 1; j < versions.length; j++) {
                assertOrder(X_LT_Y, versions[i], versions[j]);
            }
        }
    }
}
