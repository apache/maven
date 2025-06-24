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
package org.apache.maven.impl.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.Constants;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.cache.CacheRetention;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.services.ModelTransformer;
import org.apache.maven.api.services.Request;
import org.apache.maven.api.services.RequestTrace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for cache configuration functionality.
 */
class CacheConfigurationTest {

    @Mock
    private Session session;

    @Mock
    private Request<?> request;

    @Mock
    private ModelBuilderRequest modelBuilderRequest;

    private Map<String, String> userProperties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userProperties = new HashMap<>();
        when(session.getUserProperties()).thenReturn(userProperties);
        when(request.getSession()).thenReturn(session);
        when(modelBuilderRequest.getSession()).thenReturn(session);
    }

    @Test
    void testDefaultConfiguration() {
        CacheConfig config = CacheConfigurationResolver.resolveConfig(request, session);
        assertEquals(CacheRetention.REQUEST_SCOPED, config.scope());
        assertEquals(Cache.ReferenceType.SOFT, config.referenceType());
    }

    @Test
    void testParseSimpleSelector() {
        String configString = "ModelBuilderRequest { scope: session, ref: hard }";
        List<CacheSelector> selectors = CacheSelectorParser.parse(configString);

        assertEquals(1, selectors.size());
        CacheSelector selector = selectors.get(0);
        assertEquals("ModelBuilderRequest", selector.requestType());
        assertNull(selector.parentRequestType());
        assertEquals(CacheRetention.SESSION_SCOPED, selector.config().scope());
        assertEquals(Cache.ReferenceType.HARD, selector.config().referenceType());
    }

    @Test
    void testParseParentChildSelector() {
        String configString = "ModelBuildRequest ModelBuilderRequest { ref: weak }";
        List<CacheSelector> selectors = CacheSelectorParser.parse(configString);

        assertEquals(1, selectors.size());
        CacheSelector selector = selectors.get(0);
        assertEquals("ModelBuilderRequest", selector.requestType());
        assertEquals("ModelBuildRequest", selector.parentRequestType());
        assertNull(selector.config().scope()); // not specified
        assertEquals(Cache.ReferenceType.WEAK, selector.config().referenceType());
    }

    @Test
    void testParseWildcardSelector() {
        String configString = "* ModelBuilderRequest { scope: persistent }";
        List<CacheSelector> selectors = CacheSelectorParser.parse(configString);

        assertEquals(1, selectors.size());
        CacheSelector selector = selectors.get(0);
        assertEquals("ModelBuilderRequest", selector.requestType());
        assertEquals("*", selector.parentRequestType());
        assertEquals(CacheRetention.PERSISTENT, selector.config().scope());
        assertNull(selector.config().referenceType()); // not specified
    }

    @Test
    void testParseMultipleSelectors() {
        String configString =
                """
            ModelBuilderRequest { scope: session, ref: soft }
            ArtifactResolutionRequest { scope: request, ref: hard }
            * VersionRangeRequest { ref: weak }
            """;
        List<CacheSelector> selectors = CacheSelectorParser.parse(configString);

        assertEquals(3, selectors.size());

        // Check first selector
        CacheSelector first = selectors.get(0);
        assertEquals("VersionRangeRequest", first.requestType());
        assertEquals("*", first.parentRequestType());

        // Check second selector
        CacheSelector second = selectors.get(1);
        assertEquals("ModelBuilderRequest", second.requestType());
        assertNull(second.parentRequestType());

        // Check third selector
        CacheSelector third = selectors.get(2);
        assertEquals("ArtifactResolutionRequest", third.requestType());
        assertNull(third.parentRequestType());
    }

    @Test
    void testConfigurationResolution() {
        userProperties.put(Constants.MAVEN_CACHE_CONFIG_PROPERTY, "ModelBuilderRequest { scope: session, ref: hard }");

        when(modelBuilderRequest.getClass()).thenReturn((Class) ModelBuilderRequest.class);

        CacheConfig config = CacheConfigurationResolver.resolveConfig(modelBuilderRequest, session);
        assertEquals(CacheRetention.SESSION_SCOPED, config.scope());
        assertEquals(Cache.ReferenceType.HARD, config.referenceType());
    }

    @Test
    void testSelectorMatching() {
        PartialCacheConfig config =
                PartialCacheConfig.complete(CacheRetention.SESSION_SCOPED, Cache.ReferenceType.HARD);
        CacheSelector selector = CacheSelector.forRequestType("ModelBuilderRequest", config);

        when(modelBuilderRequest.getClass()).thenReturn((Class) ModelBuilderRequest.class);
        when(modelBuilderRequest.getTrace()).thenReturn(null);

        assertTrue(selector.matches(modelBuilderRequest));
    }

    @Test
    void testInterfaceMatching() {
        // Test that selectors match against implemented interfaces, not just class names
        PartialCacheConfig config =
                PartialCacheConfig.complete(CacheRetention.SESSION_SCOPED, Cache.ReferenceType.HARD);
        CacheSelector selector = CacheSelector.forRequestType("ModelBuilderRequest", config);

        // Create a test request instance that implements ModelBuilderRequest interface
        TestRequestImpl testRequest = new TestRequestImpl();

        // Should match because TestRequestImpl implements ModelBuilderRequest
        assertTrue(selector.matches(testRequest));

        // Test with a selector for a different interface
        CacheSelector requestSelector = CacheSelector.forRequestType("Request", config);
        assertTrue(requestSelector.matches(testRequest)); // Should match Request interface
    }

    // Test implementation class that implements ModelBuilderRequest
    private static class TestRequestImpl implements ModelBuilderRequest {
        @Override
        public Session getSession() {
            return null;
        }

        @Override
        public RequestTrace getTrace() {
            return null;
        }

        @Override
        public RequestType getRequestType() {
            return RequestType.BUILD_PROJECT;
        }

        @Override
        public boolean isLocationTracking() {
            return false;
        }

        @Override
        public boolean isRecursive() {
            return false;
        }

        @Override
        public ModelSource getSource() {
            return null;
        }

        @Override
        public java.util.Collection<Profile> getProfiles() {
            return java.util.List.of();
        }

        @Override
        public java.util.List<String> getActiveProfileIds() {
            return java.util.List.of();
        }

        @Override
        public java.util.List<String> getInactiveProfileIds() {
            return java.util.List.of();
        }

        @Override
        public java.util.Map<String, String> getSystemProperties() {
            return java.util.Map.of();
        }

        @Override
        public java.util.Map<String, String> getUserProperties() {
            return java.util.Map.of();
        }

        @Override
        public RepositoryMerging getRepositoryMerging() {
            return RepositoryMerging.POM_DOMINANT;
        }

        @Override
        public java.util.List<RemoteRepository> getRepositories() {
            return java.util.List.of();
        }

        @Override
        public ModelTransformer getLifecycleBindingsInjector() {
            return null;
        }
    }

    @Test
    void testInvalidConfiguration() {
        String configString = "InvalidSyntax without braces";
        List<CacheSelector> selectors = CacheSelectorParser.parse(configString);
        assertTrue(selectors.isEmpty());
    }

    @Test
    void testEmptyConfiguration() {
        String configString = "";
        List<CacheSelector> selectors = CacheSelectorParser.parse(configString);
        assertTrue(selectors.isEmpty());
    }

    @Test
    void testPartialConfigurationMerging() {
        userProperties.put(
                Constants.MAVEN_CACHE_CONFIG_PROPERTY,
                """
            ModelBuilderRequest { scope: session }
            * ModelBuilderRequest { ref: hard }
            """);

        when(modelBuilderRequest.getClass()).thenReturn((Class) ModelBuilderRequest.class);
        when(modelBuilderRequest.getTrace()).thenReturn(null);

        CacheConfig config = CacheConfigurationResolver.resolveConfig(modelBuilderRequest, session);
        assertEquals(CacheRetention.SESSION_SCOPED, config.scope()); // from first selector
        assertEquals(Cache.ReferenceType.HARD, config.referenceType()); // from second selector
    }

    @Test
    void testPartialConfigurationScopeOnly() {
        String configString = "ModelBuilderRequest { scope: persistent }";
        List<CacheSelector> selectors = CacheSelectorParser.parse(configString);

        assertEquals(1, selectors.size());
        CacheSelector selector = selectors.get(0);
        assertEquals(CacheRetention.PERSISTENT, selector.config().scope());
        assertNull(selector.config().referenceType());

        // Test conversion to complete config
        CacheConfig complete = selector.config().toComplete();
        assertEquals(CacheRetention.PERSISTENT, complete.scope());
        assertEquals(Cache.ReferenceType.SOFT, complete.referenceType()); // default
    }

    @Test
    void testPartialConfigurationRefOnly() {
        String configString = "ModelBuilderRequest { ref: weak }";
        List<CacheSelector> selectors = CacheSelectorParser.parse(configString);

        assertEquals(1, selectors.size());
        CacheSelector selector = selectors.get(0);
        assertNull(selector.config().scope());
        assertEquals(Cache.ReferenceType.WEAK, selector.config().referenceType());

        // Test conversion to complete config
        CacheConfig complete = selector.config().toComplete();
        assertEquals(CacheRetention.REQUEST_SCOPED, complete.scope()); // default
        assertEquals(Cache.ReferenceType.WEAK, complete.referenceType());
    }

    @Test
    void testPartialConfigurationMergeLogic() {
        PartialCacheConfig base = PartialCacheConfig.withScope(CacheRetention.SESSION_SCOPED);
        PartialCacheConfig override = PartialCacheConfig.withReferenceType(Cache.ReferenceType.HARD);

        PartialCacheConfig merged = base.mergeWith(override);
        assertEquals(CacheRetention.SESSION_SCOPED, merged.scope());
        assertEquals(Cache.ReferenceType.HARD, merged.referenceType());

        // Test override precedence
        PartialCacheConfig override2 = PartialCacheConfig.complete(CacheRetention.PERSISTENT, Cache.ReferenceType.WEAK);
        PartialCacheConfig merged2 = base.mergeWith(override2);
        assertEquals(CacheRetention.SESSION_SCOPED, merged2.scope()); // base takes precedence
        assertEquals(Cache.ReferenceType.WEAK, merged2.referenceType()); // from override2
    }

    @Test
    void testParentInterfaceMatching() {
        // Test that parent request matching works with interfaces
        PartialCacheConfig config =
                PartialCacheConfig.complete(CacheRetention.SESSION_SCOPED, Cache.ReferenceType.HARD);
        CacheSelector selector = CacheSelector.forParentAndRequestType("ModelBuilderRequest", "Request", config);

        // Create a child request with a parent that implements ModelBuilderRequest
        TestRequestImpl childRequest = new TestRequestImpl();
        TestRequestImpl parentRequest = new TestRequestImpl();

        // Mock the trace to simulate parent-child relationship
        RequestTrace parentTrace = mock(RequestTrace.class);
        RequestTrace childTrace = mock(RequestTrace.class);

        when(parentTrace.data()).thenReturn(parentRequest);
        when(childTrace.parent()).thenReturn(parentTrace);
        when(childRequest.getTrace()).thenReturn(childTrace);

        // Should match because parent implements ModelBuilderRequest interface
        assertTrue(selector.matches(childRequest));
    }
}
