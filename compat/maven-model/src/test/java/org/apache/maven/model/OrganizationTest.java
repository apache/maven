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
package org.apache.maven.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@code Organization}.
 *
 */
class OrganizationTest {

    @Test
    void hashCodeNullSafe() {
        new Organization().hashCode();
    }

    @Test
    void equalsNullSafe() {
        assertThat(new Organization()).isNotEqualTo(null);

        new Organization().equals(new Organization());
    }

    @Test
    void equalsIdentity() {
        Organization thing = new Organization();
        assertThat(thing).isEqualTo(thing);
    }

    @Test
    void toStringNullSafe() {
        assertThat(new Organization().toString()).isNotNull();
    }

    @Test
    void toStringNotNonsense11() {
        Organization org = new Organization();
        org.setName("Testing Maven Unit");
        org.setUrl("https://maven.localdomain");

        assertThat(org.toString()).isEqualTo("Organization {name=Testing Maven Unit, url=https://maven.localdomain}");
    }

    @Test
    void toStringNotNonsense10() {
        Organization org = new Organization();
        org.setName("Testing Maven Unit");

        assertThat(org.toString()).isEqualTo("Organization {name=Testing Maven Unit, url=null}");
    }

    @Test
    void toStringNotNonsense01() {
        Organization org = new Organization();
        org.setUrl("https://maven.localdomain");

        assertThat(org.toString()).isEqualTo("Organization {name=null, url=https://maven.localdomain}");
    }

    @Test
    void toStringNotNonsense00() {
        Organization org = new Organization();

        assertThat(org.toString()).isEqualTo("Organization {name=null, url=null}");
    }
}
