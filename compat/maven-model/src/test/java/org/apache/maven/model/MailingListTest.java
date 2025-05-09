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
 * Tests {@code MailingList}.
 *
 */
class MailingListTest {

    @Test
    void hashCodeNullSafe() {
        new MailingList().hashCode();
    }

    @Test
    void equalsNullSafe() {
        assertThat(new MailingList()).isNotEqualTo(null);

        new MailingList().equals(new MailingList());
    }

    @Test
    void equalsIdentity() {
        MailingList thing = new MailingList();
        assertThat(thing).isEqualTo(thing);
    }

    @Test
    void toStringNullSafe() {
        assertThat(new MailingList().toString()).isNotNull();
    }

    @Test
    void toStringNotNonsense() {
        MailingList list = new MailingList();
        list.setName("modello-dev");

        String s = list.toString();

        assertThat(s).isEqualTo("MailingList {name=modello-dev, archive=null}");
    }
}
