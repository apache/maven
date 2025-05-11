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

import org.apache.maven.api.services.Request;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestTraceHelperTest {

    @Test
    void enterWithRequestData() {
        InternalSession session = mock(InternalSession.class);
        Request<?> request = mock(Request.class);
        org.apache.maven.api.services.RequestTrace existingTrace =
                new org.apache.maven.api.services.RequestTrace(null, "test");

        when(request.getTrace()).thenReturn(existingTrace);

        RequestTraceHelper.ResolverTrace result = RequestTraceHelper.enter(session, request);

        assertThat(result).isNotNull();
        assertThat(result.mvnTrace()).isEqualTo(existingTrace);
        verify(session).setCurrentTrace(existingTrace);
    }

    @Test
    void interpretTraceWithArtifactRequest() {
        ArtifactRequest artifactRequest = mock(ArtifactRequest.class);
        RequestTrace trace = RequestTrace.newChild(null, artifactRequest);

        String result = RequestTraceHelper.interpretTrace(false, trace);

        assertThat(result.startsWith("artifact request for ")).isTrue();
    }

    @Test
    void toMavenWithNullTrace() {
        assertThat(RequestTraceHelper.toMaven("test", null)).isNull();
    }

    @Test
    void toResolverWithNullTrace() {
        assertThat(RequestTraceHelper.toResolver(null)).isNull();
    }

    @Test
    void exitResetsParentTrace() {
        InternalSession session = mock(InternalSession.class);
        org.apache.maven.api.services.RequestTrace parentTrace =
                new org.apache.maven.api.services.RequestTrace(null, "parent");
        org.apache.maven.api.services.RequestTrace currentTrace =
                new org.apache.maven.api.services.RequestTrace(parentTrace, "current");

        RequestTraceHelper.ResolverTrace resolverTrace =
                new RequestTraceHelper.ResolverTrace(session, "test", null, currentTrace);

        RequestTraceHelper.exit(resolverTrace);

        verify(session).setCurrentTrace(parentTrace);
    }
}
