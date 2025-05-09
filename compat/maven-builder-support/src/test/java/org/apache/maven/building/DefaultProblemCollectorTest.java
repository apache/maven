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
package org.apache.maven.building;

import org.apache.maven.building.Problem.Severity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultProblemCollectorTest {

    @Test
    void getProblems() {
        DefaultProblemCollector collector = new DefaultProblemCollector(null);
        assertThat(collector.getProblems()).isNotNull();
        assertThat(collector.getProblems().size()).isEqualTo(0);

        collector.add(null, "MESSAGE1", -1, -1, null);

        Exception e2 = new Exception();
        collector.add(Severity.WARNING, null, 42, 127, e2);

        assertThat(collector.getProblems().size()).isEqualTo(2);

        Problem p1 = collector.getProblems().get(0);
        assertThat(p1.getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(p1.getMessage()).isEqualTo("MESSAGE1");
        assertThat(p1.getLineNumber()).isEqualTo(-1);
        assertThat(p1.getColumnNumber()).isEqualTo(-1);
        assertThat(p1.getException()).isNull();

        Problem p2 = collector.getProblems().get(1);
        assertThat(p2.getSeverity()).isEqualTo(Severity.WARNING);
        assertThat(p2.getMessage()).isEqualTo("");
        assertThat(p2.getLineNumber()).isEqualTo(42);
        assertThat(p2.getColumnNumber()).isEqualTo(127);
        assertThat(p2.getException()).isEqualTo(e2);
    }

    @Test
    void setSource() {
        DefaultProblemCollector collector = new DefaultProblemCollector(null);

        collector.add(null, "PROBLEM1", -1, -1, null);

        collector.setSource("SOURCE_PROBLEM2");
        collector.add(null, "PROBLEM2", -1, -1, null);

        collector.setSource("SOURCE_PROBLEM3");
        collector.add(null, "PROBLEM3", -1, -1, null);

        assertThat(collector.getProblems().get(0).getSource()).isEqualTo("");
        assertThat(collector.getProblems().get(1).getSource()).isEqualTo("SOURCE_PROBLEM2");
        assertThat(collector.getProblems().get(2).getSource()).isEqualTo("SOURCE_PROBLEM3");
    }
}
