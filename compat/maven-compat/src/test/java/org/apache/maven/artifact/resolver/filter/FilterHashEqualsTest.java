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
package org.apache.maven.artifact.resolver.filter;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
class FilterHashEqualsTest {

    @Test
    void includesExcludesArtifactFilter() {
        List<String> patterns = Arrays.asList("c", "d", "e");

        IncludesArtifactFilter f1 = new IncludesArtifactFilter(patterns);

        IncludesArtifactFilter f2 = new IncludesArtifactFilter(patterns);

        assertThat(f2).isEqualTo(f1);
        assertThat(f1).isEqualTo(f2);
        assertThat(f2.hashCode()).isEqualTo(f1.hashCode());

        IncludesArtifactFilter f3 = new IncludesArtifactFilter(Arrays.asList("d", "c", "e"));
        assertThat(f3).isEqualTo(f1);
        assertThat(f3.hashCode()).isEqualTo(f1.hashCode());
    }
}
