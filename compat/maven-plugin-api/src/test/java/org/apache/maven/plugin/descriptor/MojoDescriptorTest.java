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
package org.apache.maven.plugin.descriptor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MojoDescriptorTest {
    @Test
    void getParameterMap() throws DuplicateParameterException {
        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        Parameter param1 = new Parameter();
        param1.setName("param1");
        param1.setDefaultValue("value1");
        mojoDescriptor.addParameter(param1);

        assertThat(mojoDescriptor.getParameters().size()).isEqualTo(1);

        assertThat(mojoDescriptor.getParameterMap().size()).isEqualTo(mojoDescriptor.getParameters().size());

        Parameter param2 = new Parameter();
        param2.setName("param2");
        param2.setDefaultValue("value2");
        mojoDescriptor.addParameter(param2);

        assertThat(mojoDescriptor.getParameters().size()).isEqualTo(2);
        assertThat(mojoDescriptor.getParameterMap().size()).isEqualTo(mojoDescriptor.getParameters().size());
    }
}
