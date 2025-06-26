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

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.apache.maven.api.ProtoSession;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.cache.BatchRequestException;
import org.apache.maven.api.cache.CacheStatistics;
import org.apache.maven.api.cache.RequestResult;
import org.apache.maven.api.services.Request;
import org.apache.maven.api.services.RequestTrace;
import org.apache.maven.api.services.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AbstractRequestCacheTest {

    private TestRequestCache cache;

    @BeforeEach
    void setUp() {
        cache = new TestRequestCache();
    }

    @Test
    void testBatchRequestExceptionIncludesSuppressedExceptions() {
        // Create mock requests and results
        TestRequest request1 = createTestRequest("request1");
        TestRequest request2 = createTestRequest("request2");
        TestRequest request3 = createTestRequest("request3");

        // Create specific exceptions with different messages and stack traces
        RuntimeException exception1 = new RuntimeException("Error processing request1");
        IllegalArgumentException exception2 = new IllegalArgumentException("Invalid argument in request2");
        IllegalStateException exception3 = new IllegalStateException("Invalid state in request3");

        // Set up the cache to return failures for all requests
        cache.addFailure(request1, exception1);
        cache.addFailure(request2, exception2);
        cache.addFailure(request3, exception3);

        List<TestRequest> requests = Arrays.asList(request1, request2, request3);

        // Create a supplier that should not be called since we're simulating cached failures
        Function<List<TestRequest>, List<TestResult>> supplier = reqs -> {
            throw new AssertionError("Supplier should not be called in this test");
        };

        // Execute the batch request and expect BatchRequestException
        BatchRequestException batchException =
                assertThrows(BatchRequestException.class, () -> cache.requests(requests, supplier));

        // Verify the main exception message
        assertEquals("One or more requests failed", batchException.getMessage());

        // Verify that all individual exceptions are included as suppressed exceptions
        Throwable[] suppressedExceptions = batchException.getSuppressed();
        assertNotNull(suppressedExceptions);
        assertEquals(3, suppressedExceptions.length);

        // Verify each suppressed exception
        assertTrue(Arrays.asList(suppressedExceptions).contains(exception1));
        assertTrue(Arrays.asList(suppressedExceptions).contains(exception2));
        assertTrue(Arrays.asList(suppressedExceptions).contains(exception3));

        // Verify the results contain the correct error information
        List<RequestResult<?, ?>> results = batchException.getResults();
        assertEquals(3, results.size());

        for (RequestResult<?, ?> result : results) {
            assertNotNull(result.error());
            assertInstanceOf(RuntimeException.class, result.error());
        }
    }

    @Test
    void testBatchRequestWithMixedSuccessAndFailure() {
        TestRequest successRequest = createTestRequest("success");
        TestRequest failureRequest = createTestRequest("failure");

        RuntimeException failureException = new RuntimeException("Processing failed");

        // Set up mixed success/failure scenario
        cache.addFailure(failureRequest, failureException);

        List<TestRequest> requests = Arrays.asList(successRequest, failureRequest);

        Function<List<TestRequest>, List<TestResult>> supplier = reqs -> {
            // Only the success request should reach the supplier
            assertEquals(1, reqs.size());
            assertEquals(successRequest, reqs.get(0));
            return List.of(new TestResult(successRequest));
        };

        BatchRequestException batchException =
                assertThrows(BatchRequestException.class, () -> cache.requests(requests, supplier));

        // Verify only the failure exception is suppressed
        Throwable[] suppressedExceptions = batchException.getSuppressed();
        assertEquals(1, suppressedExceptions.length);
        assertEquals(failureException, suppressedExceptions[0]);

        // Verify results: one success, one failure
        List<RequestResult<?, ?>> results = batchException.getResults();
        assertEquals(2, results.size());

        RequestResult<?, ?> result1 = results.get(0);
        RequestResult<?, ?> result2 = results.get(1);

        // One should be success, one should be failure
        boolean hasSuccess = (result1.error() == null) || (result2.error() == null);
        boolean hasFailure = (result1.error() != null) || (result2.error() != null);

        assertTrue(hasSuccess);
        assertTrue(hasFailure);
    }

    @Test
    void testSuccessfulBatchRequestDoesNotThrowException() {
        TestRequest request1 = createTestRequest("success1");
        TestRequest request2 = createTestRequest("success2");

        List<TestRequest> requests = Arrays.asList(request1, request2);

        Function<List<TestRequest>, List<TestResult>> supplier =
                reqs -> reqs.stream().map(TestResult::new).toList();

        // Should not throw any exception
        List<TestResult> results = cache.requests(requests, supplier);

        assertEquals(2, results.size());
        assertEquals(request1, results.get(0).getRequest());
        assertEquals(request2, results.get(1).getRequest());
    }

    // Helper methods and test classes

    private TestRequest createTestRequest(String id) {
        ProtoSession session = mock(ProtoSession.class);
        return new TestRequestImpl(id, session);
    }

    // Test implementations

    interface TestRequest extends Request<ProtoSession> {}

    static class TestRequestImpl implements TestRequest {
        private final String id;
        private final ProtoSession session;

        TestRequestImpl(String id, ProtoSession session) {
            this.id = id;
            this.session = session;
        }

        @Override
        @Nonnull
        public ProtoSession getSession() {
            return session;
        }

        @Override
        public RequestTrace getTrace() {
            return null;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            TestRequestImpl that = (TestRequestImpl) obj;
            return java.util.Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(id);
        }

        @Override
        @Nonnull
        public String toString() {
            return "TestRequest[" + id + "]";
        }
    }

    static class TestResult implements Result<TestRequest> {
        private final TestRequest request;

        TestResult(TestRequest request) {
            this.request = request;
        }

        @Override
        @Nonnull
        public TestRequest getRequest() {
            return request;
        }
    }

    static class TestRequestCache extends AbstractRequestCache {
        private final java.util.Map<TestRequest, RuntimeException> failures = new java.util.HashMap<>();

        void addFailure(TestRequest request, RuntimeException exception) {
            failures.put(request, exception);
        }

        @Override
        protected <REQ extends Request<?>, REP extends Result<REQ>> CachingSupplier<REQ, REP> doCache(
                REQ req, Function<REQ, REP> supplier) {
            // Check if we have a pre-configured failure for this request
            RuntimeException failure = failures.get(req);
            if (failure != null) {
                // Return a pre-cached failure by creating a supplier that always throws
                return new PreCachedFailureCachingSupplier<>(failure);
            }

            // For non-failure cases, return a normal caching supplier
            return new CachingSupplier<>(supplier);
        }

        @Override
        public CacheStatistics getStatistics() {
            return null; // Not implemented for test
        }

        // Custom CachingSupplier that simulates a pre-cached failure
        private static class PreCachedFailureCachingSupplier<REQ, REP> extends CachingSupplier<REQ, REP> {
            PreCachedFailureCachingSupplier(RuntimeException failure) {
                super(null); // No supplier needed
                // Pre-populate the value with the failure
                this.value = new AltRes(failure);
            }
        }
    }
}
