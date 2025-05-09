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

class DefaultProblemTest {

    @Test
    void getSeverity() {
        DefaultProblem problem = new DefaultProblem(null, null, null, -1, -1, null);
        assertThat(problem.getSeverity()).isEqualTo(Severity.ERROR);

        problem = new DefaultProblem(null, Severity.FATAL, null, -1, -1, null);
        assertThat(problem.getSeverity()).isEqualTo(Severity.FATAL);

        problem = new DefaultProblem(null, Severity.ERROR, null, -1, -1, null);
        assertThat(problem.getSeverity()).isEqualTo(Severity.ERROR);

        problem = new DefaultProblem(null, Severity.WARNING, null, -1, -1, null);
        assertThat(problem.getSeverity()).isEqualTo(Severity.WARNING);
    }

    @Test
    void getLineNumber() {
        DefaultProblem problem = new DefaultProblem(null, null, null, -1, -1, null);
        assertThat(problem.getLineNumber()).isEqualTo(-1);

        problem = new DefaultProblem(null, null, null, 42, -1, null);
        assertThat(problem.getLineNumber()).isEqualTo(42);

        problem = new DefaultProblem(null, null, null, Integer.MAX_VALUE, -1, null);
        assertThat(problem.getLineNumber()).isEqualTo(Integer.MAX_VALUE);

        // this case is not specified, might also return -1
        problem = new DefaultProblem(null, null, null, Integer.MIN_VALUE, -1, null);
        assertThat(problem.getLineNumber()).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    void getColumnNumber() {
        DefaultProblem problem = new DefaultProblem(null, null, null, -1, -1, null);
        assertThat(problem.getColumnNumber()).isEqualTo(-1);

        problem = new DefaultProblem(null, null, null, -1, 42, null);
        assertThat(problem.getColumnNumber()).isEqualTo(42);

        problem = new DefaultProblem(null, null, null, -1, Integer.MAX_VALUE, null);
        assertThat(problem.getColumnNumber()).isEqualTo(Integer.MAX_VALUE);

        // this case is not specified, might also return -1
        problem = new DefaultProblem(null, null, null, -1, Integer.MIN_VALUE, null);
        assertThat(problem.getColumnNumber()).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    void getException() {
        DefaultProblem problem = new DefaultProblem(null, null, null, -1, -1, null);
        assertThat(problem.getException()).isNull();

        Exception e = new Exception();
        problem = new DefaultProblem(null, null, null, -1, -1, e);
        assertThat(problem.getException()).isSameAs(e);
    }

    @Test
    void getSource() {
        DefaultProblem problem = new DefaultProblem(null, null, null, -1, -1, null);
        assertThat(problem.getSource()).isEqualTo("");

        problem = new DefaultProblem(null, null, "", -1, -1, null);
        assertThat(problem.getSource()).isEqualTo("");

        problem = new DefaultProblem(null, null, "SOURCE", -1, -1, null);
        assertThat(problem.getSource()).isEqualTo("SOURCE");
    }

    @Test
    void getLocation() {
        DefaultProblem problem = new DefaultProblem(null, null, null, -1, -1, null);
        assertThat(problem.getLocation()).isEqualTo("");

        problem = new DefaultProblem(null, null, "SOURCE", -1, -1, null);
        assertThat(problem.getLocation()).isEqualTo("SOURCE");

        problem = new DefaultProblem(null, null, null, 42, -1, null);
        assertThat(problem.getLocation()).isEqualTo("line 42");

        problem = new DefaultProblem(null, null, null, -1, 127, null);
        assertThat(problem.getLocation()).isEqualTo("column 127");

        problem = new DefaultProblem(null, null, "SOURCE", 42, 127, null);
        assertThat(problem.getLocation()).isEqualTo("SOURCE, line 42, column 127");
    }

    @Test
    void getMessage() {
        DefaultProblem problem = new DefaultProblem("MESSAGE", null, null, -1, -1, null);
        assertThat(problem.getMessage()).isEqualTo("MESSAGE");

        problem = new DefaultProblem(null, null, null, -1, -1, new Exception());
        assertThat(problem.getMessage()).isEqualTo("");

        problem = new DefaultProblem(null, null, null, -1, -1, new Exception("EXCEPTION MESSAGE"));
        assertThat(problem.getMessage()).isEqualTo("EXCEPTION MESSAGE");
    }
}
