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
package org.apache.maven.model.building;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelBuildingExceptionTest {

    @Test
    void testMessage() {
        DefaultModelProblem pb1 =
                new DefaultModelProblem("message1", ModelProblem.Severity.ERROR, null, "source", 0, 0, "modelId", null);
        DefaultModelProblem pb2 =
                new DefaultModelProblem("message2", ModelProblem.Severity.ERROR, null, "source", 0, 0, "modelId", null);
        String msg = ModelBuildingException.toMessage("modelId", Arrays.asList(pb1, pb2));
        assertEquals(
                "2 problems were encountered while building the effective model for modelId" + System.lineSeparator()
                        + "    - [ERROR] message1" + System.lineSeparator()
                        + "    - [ERROR] message2",
                msg);
    }
}
