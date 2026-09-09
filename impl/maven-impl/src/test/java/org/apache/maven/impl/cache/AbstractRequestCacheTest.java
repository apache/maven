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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.apache.maven.api.ProtoSession;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.cache.BatchRequestException;
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

    /**
     * Tests that re-entrant calls to {@code requests()} do not deadlock.
     * <p>
     * This reproduces the scenario from issue #12445: an outer {@code requests()} call
     * creates CachingSupplier instances that are stored in the cache. During batch resolution
     * (inside the outer call's batch supplier), a nested {@code requests()} call is triggered
     * (e.g., parent POM resolution during artifact resolution). If the inner call hits the
     * same cache entry (same request key), it gets back the CachingSupplier from the outer call.
     * <p>
     * Before the fix, the CachingSupplier wrapped a wait-based supplier that referenced the
     * outer call's {@code nonCachedResults} HashMap. The inner call would wait on that HashMap
     * forever, since the outer call couldn't populate it until the inner call completed.
     */
    @Test
    void testReentrantRequestsDoesNotDeadlock() throws Exception {
        // Use a caching implementation that stores CachingSuppliers in a shared map
        CachingTestRequestCache cachingCache = new CachingTestRequestCache();

        // "parentPom" is the request that will be resolved by both the outer and inner calls
        TestRequest artifact = createTestRequest("artifact");
        TestRequest parentPom = createTestRequest("parentPom");

        // The outer batch supplier resolves requests, but during resolution of "artifact",
        // it triggers a nested requests() call for "parentPom"
        Function<List<TestRequest>, List<TestResult>> outerBatchSupplier = reqs -> {
            List<TestResult> results = new java.util.ArrayList<>();
            for (TestRequest req : reqs) {
                if (req.equals(artifact)) {
                    // Simulate parent POM resolution: re-entrant call for "parentPom"
                    List<TestResult> innerResults = cachingCache.requests(
                            List.of(parentPom),
                            innerReqs -> innerReqs.stream().map(TestResult::new).toList());
                    // After inner call completes, outer resolution succeeds
                    assertEquals(1, innerResults.size());
                }
                results.add(new TestResult(req));
            }
            return results;
        };

        // Execute with a timeout to detect deadlock
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<List<TestResult>> future =
                    executor.submit(() -> cachingCache.requests(List.of(artifact, parentPom), outerBatchSupplier));

            // If this deadlocks, the future will time out
            List<TestResult> results = future.get(5, TimeUnit.SECONDS);

            assertEquals(2, results.size());
            assertEquals(artifact, results.get(0).getRequest());
            assertEquals(parentPom, results.get(1).getRequest());
        } catch (TimeoutException e) {
            throw new AssertionError(
                    "Deadlock detected: re-entrant requests() call did not complete within 5 seconds", e);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Tests that a concurrent singular {@code request()} call waits for an
     * in-progress batch resolution instead of invoking the supplier independently.
     * <p>
     * Thread A starts a batch resolution via {@code requests()} (which marks the
     * CachingSupplier as {@code batchResolving} and registers it on the current thread).
     * While Thread A is still inside its batch supplier, Thread B calls {@code request()}
     * for the same key. Thread B's {@code cs.apply()} should see {@code batchResolving == true},
     * wait for {@code complete()}, and return the batch result without running its own supplier.
     */
    @Test
    void testConcurrentRequestDoesNotDuplicateResolution() throws Exception {
        CachingTestRequestCache cachingCache = new CachingTestRequestCache();

        TestRequest sharedReq = createTestRequest("shared");

        java.util.concurrent.atomic.AtomicInteger resolutionCount = new java.util.concurrent.atomic.AtomicInteger(0);
        CountDownLatch batchStarted = new CountDownLatch(1);
        CountDownLatch proceedWithBatch = new CountDownLatch(1);

        // Thread A's batch supplier: signals when it starts, then waits before completing
        Function<List<TestRequest>, List<TestResult>> slowBatchSupplier = reqs -> {
            resolutionCount.incrementAndGet();
            batchStarted.countDown();
            try {
                proceedWithBatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return reqs.stream().map(TestResult::new).toList();
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // Thread A: starts batch resolution via requests(), pauses inside the supplier
            Future<List<TestResult>> futureA =
                    executor.submit(() -> cachingCache.requests(List.of(sharedReq), slowBatchSupplier));

            // Wait for Thread A's batch supplier to start
            assertTrue(batchStarted.await(5, TimeUnit.SECONDS), "Thread A's batch should have started");

            // Thread B: calls request() (singular) for the same key.
            // It gets the same CachingSupplier from the cache, sees batchResolving == true,
            // and should wait for Thread A's complete() instead of invoking its own supplier.
            Future<TestResult> futureB = executor.submit(() -> cachingCache.request(sharedReq, req -> {
                resolutionCount.incrementAndGet();
                return new TestResult(req);
            }));

            // Give Thread B time to enter apply() and start waiting
            Thread.sleep(200);

            // Let Thread A's batch complete — this calls complete() which wakes Thread B
            proceedWithBatch.countDown();

            // Both should complete
            List<TestResult> resultsA = futureA.get(5, TimeUnit.SECONDS);
            TestResult resultB = futureB.get(5, TimeUnit.SECONDS);

            assertEquals(1, resultsA.size());
            assertNotNull(resultB);

            // The shared request should have been resolved only once (by Thread A's batch).
            // Thread B should have waited for Thread A's complete() call, not invoked its
            // own supplier.
            assertEquals(1, resolutionCount.get(), "Request should be resolved only once, not duplicated");
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Tests that batch resolution is resilient to requests whose {@code hashCode()} changes
     * during resolution (e.g., because {@code RequestTrace} includes mutable
     * {@code ModelBuilderRequest} data).
     * <p>
     * The {@code reqToIndex} map inside {@code requests()} uses {@code IdentityHashMap}
     * specifically to handle this: after the batch supplier mutates request data,
     * lookups still succeed because they compare by reference identity, not by
     * {@code equals()/hashCode()}.
     */
    @Test
    void testBatchResolutionWithUnstableHashCode() {
        // Use identity-based cache to match real DefaultRequestCache behavior
        IdentityCachingTestRequestCache cachingCache = new IdentityCachingTestRequestCache();

        // Create requests with mutable trace data that affects hashCode()
        MutableHashCodeRequest req1 = new MutableHashCodeRequest("req1", "traceA");
        MutableHashCodeRequest req2 = new MutableHashCodeRequest("req2", "traceB");

        int originalHash1 = req1.hashCode();
        int originalHash2 = req2.hashCode();

        java.util.concurrent.atomic.AtomicInteger supplierCallCount = new java.util.concurrent.atomic.AtomicInteger(0);

        Function<List<MutableHashCodeRequest>, List<MutableHashCodeResult>> batchSupplier = reqs -> {
            supplierCallCount.incrementAndGet();
            // Mutate trace data during resolution — this changes hashCode()
            for (MutableHashCodeRequest r : reqs) {
                r.setTraceData(r.getTraceData() + "-mutated");
            }
            return reqs.stream().map(MutableHashCodeResult::new).toList();
        };

        // Resolve the batch — hashCode changes inside the supplier
        List<MutableHashCodeResult> results = cachingCache.requests(List.of(req1, req2), batchSupplier);

        // Verify results were delivered despite hashCode mutation
        assertEquals(2, results.size());
        assertEquals(req1, results.get(0).getRequest());
        assertEquals(req2, results.get(1).getRequest());
        assertEquals(1, supplierCallCount.get());

        // Verify hashCode actually changed
        assertTrue(
                req1.hashCode() != originalHash1 || req2.hashCode() != originalHash2,
                "hashCode should have changed after mutation");
    }

    /**
     * Tests that a concurrent {@code request()} call does not hang when the batch
     * supplier returns fewer results than expected (partial batch).
     * <p>
     * In this scenario, {@link CachingSupplier#complete} is never called for one
     * of the CachingSuppliers. The waiting thread in {@code apply()} must still
     * be unblocked when {@code setBatchResolving(false)} is called in the
     * {@code finally} block, which calls {@code notifyAll()} to wake any waiters.
     * The unblocked thread then falls through to the fallback supplier.
     */
    @Test
    void testConcurrentRequestUnblockedOnPartialBatchResult() throws Exception {
        CachingTestRequestCache cachingCache = new CachingTestRequestCache();

        TestRequest req1 = createTestRequest("req1");
        TestRequest req2 = createTestRequest("req2");

        CountDownLatch batchStarted = new CountDownLatch(1);
        CountDownLatch proceedWithBatch = new CountDownLatch(1);

        // Batch supplier that only resolves req1, "forgetting" req2
        Function<List<TestRequest>, List<TestResult>> partialBatchSupplier = reqs -> {
            batchStarted.countDown();
            try {
                proceedWithBatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // Return only the first result — req2's CachingSupplier never gets complete()
            return List.of(new TestResult(reqs.get(0)));
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // Thread A: starts batch resolution for [req1, req2]
            Future<List<TestResult>> futureA =
                    executor.submit(() -> cachingCache.requests(List.of(req1, req2), partialBatchSupplier));

            // Wait for Thread A's batch supplier to start
            assertTrue(batchStarted.await(5, TimeUnit.SECONDS), "Batch should have started");

            // Thread B: calls request() for req2 — gets the same CachingSupplier,
            // sees batchResolving == true, and waits in apply()
            Future<TestResult> futureB = executor.submit(() -> cachingCache.request(req2, req -> {
                // Fallback supplier — should be invoked after setBatchResolving(false)
                return new TestResult(req);
            }));

            // Give Thread B time to enter apply() and start waiting
            Thread.sleep(200);

            // Let Thread A's batch complete — only resolves req1
            proceedWithBatch.countDown();

            // Thread A should complete (req2 gets resolved via fallback in collection loop)
            List<TestResult> resultsA = futureA.get(5, TimeUnit.SECONDS);
            assertNotNull(resultsA);

            // Thread B should also complete — setBatchResolving(false) + notifyAll()
            // unblocks it, and it falls through to its fallback supplier
            TestResult resultB = futureB.get(5, TimeUnit.SECONDS);
            assertNotNull(resultB);
            assertEquals(req2, resultB.getRequest());
        } catch (TimeoutException e) {
            throw new AssertionError("Thread hung: setBatchResolving(false) did not unblock waiting thread", e);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Tests that two concurrent {@code requests()} (batch) calls with overlapping keys
     * do not deadlock when they share a CachingSupplier through an equals-based cache.
     * <p>
     * This is the exact regression test for issue
     * <a href="https://github.com/apache/maven/issues/12472">#12472</a>:
     * Maven 4.0.0-rc-5 hangs indefinitely during multi-module builds because two threads
     * both call {@code requests()} with overlapping request keys. Through the equals-based
     * cache (old {@code SoftIdentityMap} which used {@code equals()}, not identity), both
     * threads get back the <b>same</b> CachingSupplier for the shared key.
     * <p>
     * Before the fix (commit c6de104bff), the old {@code individualSupplier} lambda would
     * wait on the outer call's {@code IdentityHashMap} using {@code synchronized + wait()}.
     * Thread B, entering the shared CachingSupplier's {@code apply()}, would wait on
     * that map — but since Thread B held a different set of request object references,
     * the identity-based map lookup could never match, creating a permanent deadlock.
     * <p>
     * The fix replaced the wait-on-map pattern with {@link CachingSupplier#complete}
     * (direct value setting + notifyAll) and a ThreadLocal re-entrancy guard, eliminating
     * the cross-thread dependency cycle.
     */
    @Test
    void testConcurrentBatchRequestsWithSharedKeyDoNotDeadlock() throws Exception {
        // Use equals-based cache: two threads will get the same CachingSupplier for matching keys
        CachingTestRequestCache cachingCache = new CachingTestRequestCache();

        TestRequest reqOnlyA = createTestRequest("onlyA");
        TestRequest reqOnlyB = createTestRequest("onlyB");
        // Both threads request "shared" — different objects, but equals() returns true
        TestRequest sharedByA = createTestRequest("shared");
        TestRequest sharedByB = createTestRequest("shared");

        // Barrier ensures both threads are inside their batch supplier simultaneously,
        // maximizing the chance of the deadlock scenario
        CyclicBarrier bothInSupplier = new CyclicBarrier(2);

        Function<List<TestRequest>, List<TestResult>> supplierA = reqs -> {
            try {
                bothInSupplier.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                // Thread B may be blocked waiting on the shared CachingSupplier instead
                // of reaching its supplier — that's what we're testing against
            }
            return reqs.stream().map(TestResult::new).toList();
        };

        Function<List<TestRequest>, List<TestResult>> supplierB = reqs -> {
            try {
                bothInSupplier.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                // Same — Thread A may not reach the barrier
            }
            return reqs.stream().map(TestResult::new).toList();
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<List<TestResult>> futureA =
                    executor.submit(() -> cachingCache.requests(List.of(reqOnlyA, sharedByA), supplierA));
            Future<List<TestResult>> futureB =
                    executor.submit(() -> cachingCache.requests(List.of(reqOnlyB, sharedByB), supplierB));

            // If the old cross-thread deadlock exists, both futures will hang forever
            List<TestResult> resultsA = futureA.get(10, TimeUnit.SECONDS);
            List<TestResult> resultsB = futureB.get(10, TimeUnit.SECONDS);

            assertEquals(2, resultsA.size());
            assertEquals(2, resultsB.size());
        } catch (TimeoutException e) {
            throw new AssertionError(
                    "Cross-thread deadlock detected: two concurrent batch requests() calls "
                            + "with shared keys did not complete within 10 seconds "
                            + "(regression for #12472)",
                    e);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Variant of the #12472 regression scenario where the shared request's
     * {@code equals()}/{@code hashCode()} change <b>during</b> batch resolution
     * (mirroring mutable {@link RequestTrace} data in real requests).
     * <p>
     * Pre-fix, Thread B waits on Thread A's equals-based {@code nonCachedResults}
     * map for a key that no longer matches — forever. The identity-based fix
     * (c6de104bff) eliminates this because lookups use reference identity, not
     * {@code equals()}/{@code hashCode()}.
     * <p>
     * Unlike {@link #testConcurrentBatchRequestsWithSharedKeyDoNotDeadlock()}, this
     * variant actually deadlocks on the pre-fix code (verified empirically by
     * &#064;ascheman in PR review).
     *
     * @see <a href="https://github.com/apache/maven/issues/12472">#12472</a>
     */
    @Test
    void testConcurrentBatchRequestsWithMutatingSharedKeyDoNotDeadlock() throws Exception {
        GenericCachingTestRequestCache cachingCache = new GenericCachingTestRequestCache();

        MutableHashCodeRequest reqOnlyA = new MutableHashCodeRequest("onlyA", "trace");
        MutableHashCodeRequest reqOnlyB = new MutableHashCodeRequest("onlyB", "trace");
        MutableHashCodeRequest sharedByA = new MutableHashCodeRequest("shared", "trace");
        MutableHashCodeRequest sharedByB = new MutableHashCodeRequest("shared", "trace");

        CountDownLatch aInBatch = new CountDownLatch(1);

        Function<List<MutableHashCodeRequest>, List<MutableHashCodeResult>> supplierA = reqs -> {
            aInBatch.countDown();
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // Mutate trace data during resolution — changes equals()/hashCode()
            for (MutableHashCodeRequest r : reqs) {
                r.setTraceData(r.getTraceData() + "-A");
            }
            return reqs.stream().map(MutableHashCodeResult::new).toList();
        };

        Function<List<MutableHashCodeRequest>, List<MutableHashCodeResult>> supplierB = reqs -> {
            for (MutableHashCodeRequest r : reqs) {
                r.setTraceData(r.getTraceData() + "-B");
            }
            return reqs.stream().map(MutableHashCodeResult::new).toList();
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<List<MutableHashCodeResult>> futureA =
                    executor.submit(() -> cachingCache.requests(List.of(reqOnlyA, sharedByA), supplierA));
            assertTrue(aInBatch.await(5, TimeUnit.SECONDS), "Thread A should have entered its batch supplier");
            Future<List<MutableHashCodeResult>> futureB =
                    executor.submit(() -> cachingCache.requests(List.of(reqOnlyB, sharedByB), supplierB));

            List<MutableHashCodeResult> resultsA = futureA.get(10, TimeUnit.SECONDS);
            List<MutableHashCodeResult> resultsB = futureB.get(10, TimeUnit.SECONDS);

            assertEquals(2, resultsA.size());
            assertEquals(2, resultsB.size());
        } catch (TimeoutException e) {
            throw new AssertionError(
                    "Cross-thread deadlock detected: batch requests() with a shared key whose "
                            + "equals()/hashCode() mutate during resolution did not complete (#12472)",
                    e);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Tests that two sequential {@code requests()} calls with overlapping keys
     * correctly deliver results to both callers, when one thread's batch
     * completes before the other begins.
     * <p>
     * This is a timing variant of the #12472 scenario: Thread A starts and completes
     * its batch resolution (setting the shared CachingSupplier's value via
     * {@link CachingSupplier#complete}). Thread B starts its batch call afterwards,
     * finds the shared CachingSupplier already has a value, and should skip resolution
     * for that key. Thread B's unique key still needs fresh resolution.
     */
    @Test
    void testSequentialBatchRequestsWithSharedKeyReuseResult() throws Exception {
        CachingTestRequestCache cachingCache = new CachingTestRequestCache();

        TestRequest reqOnlyA = createTestRequest("onlyA");
        TestRequest reqOnlyB = createTestRequest("onlyB");
        TestRequest sharedByA = createTestRequest("shared");
        TestRequest sharedByB = createTestRequest("shared");

        java.util.concurrent.atomic.AtomicInteger supplierCallCount = new java.util.concurrent.atomic.AtomicInteger(0);

        Function<List<TestRequest>, List<TestResult>> batchSupplier = reqs -> {
            supplierCallCount.incrementAndGet();
            return reqs.stream().map(TestResult::new).toList();
        };

        // Thread A resolves [onlyA, shared] — both get cached
        List<TestResult> resultsA = cachingCache.requests(List.of(reqOnlyA, sharedByA), batchSupplier);
        assertEquals(2, resultsA.size());
        assertEquals(1, supplierCallCount.get());

        // Thread B resolves [onlyB, shared] — "shared" should come from cache;
        // only "onlyB" needs resolution
        List<TestResult> resultsB = cachingCache.requests(List.of(reqOnlyB, sharedByB), batchSupplier);
        assertEquals(2, resultsB.size());
        // Supplier should have been called twice: once for [onlyA, shared], once for [onlyB] only
        assertEquals(2, supplierCallCount.get());
    }

    /**
     * Tests that batch results are properly cached in CachingSupplier instances
     * so subsequent calls return the cached values.
     */
    @Test
    void testBatchResultsAreCached() {
        CachingTestRequestCache cachingCache = new CachingTestRequestCache();

        TestRequest req1 = createTestRequest("req1");
        TestRequest req2 = createTestRequest("req2");

        java.util.concurrent.atomic.AtomicInteger supplierCallCount = new java.util.concurrent.atomic.AtomicInteger(0);

        Function<List<TestRequest>, List<TestResult>> batchSupplier = reqs -> {
            supplierCallCount.incrementAndGet();
            return reqs.stream().map(TestResult::new).toList();
        };

        // First call should invoke the batch supplier
        List<TestResult> results1 = cachingCache.requests(List.of(req1, req2), batchSupplier);
        assertEquals(2, results1.size());
        assertEquals(1, supplierCallCount.get());

        // Second call with same requests should use cached values
        List<TestResult> results2 = cachingCache.requests(List.of(req1, req2), batchSupplier);
        assertEquals(2, results2.size());
        // Supplier should not have been called again
        assertEquals(1, supplierCallCount.get());
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

    /**
     * A cache implementation that stores CachingSupplier instances in a shared map,
     * simulating the real DefaultRequestCache behavior where the same CachingSupplier
     * can be returned for the same request key across different requests() calls.
     */
    static class CachingTestRequestCache extends AbstractRequestCache {
        private final Map<TestRequest, CachingSupplier<?, ?>> cache = new ConcurrentHashMap<>();

        @Override
        @SuppressWarnings("unchecked")
        protected <REQ extends Request<?>, REP extends Result<REQ>> CachingSupplier<REQ, REP> doCache(
                REQ req, Function<REQ, REP> supplier) {
            return (CachingSupplier<REQ, REP>)
                    cache.computeIfAbsent((TestRequest) req, r -> new CachingSupplier<>(supplier));
        }
    }

    /**
     * Equals-based cache that accepts any {@link Request} type (not just {@link TestRequest}).
     * Uses {@link ConcurrentHashMap} so lookups depend on {@code equals()}/{@code hashCode()} —
     * two request objects that are {@code equals()} will share the same {@link CachingSupplier}.
     * This is needed by tests that use {@link MutableHashCodeRequest}.
     */
    static class GenericCachingTestRequestCache extends AbstractRequestCache {
        private final Map<Request<?>, CachingSupplier<?, ?>> cache = new ConcurrentHashMap<>();

        @Override
        @SuppressWarnings("unchecked")
        protected <REQ extends Request<?>, REP extends Result<REQ>> CachingSupplier<REQ, REP> doCache(
                REQ req, Function<REQ, REP> supplier) {
            return (CachingSupplier<REQ, REP>) cache.computeIfAbsent(req, r -> new CachingSupplier<>(supplier));
        }
    }

    /**
     * A request implementation whose hashCode() depends on mutable trace data,
     * simulating ResolverRequest with mutable RequestTrace/ModelBuilderRequest data.
     */
    static class MutableHashCodeRequest implements Request<ProtoSession> {
        private final String id;
        private String traceData;

        MutableHashCodeRequest(String id, String traceData) {
            this.id = id;
            this.traceData = traceData;
        }

        String getTraceData() {
            return traceData;
        }

        void setTraceData(String traceData) {
            this.traceData = traceData;
        }

        @Override
        @Nonnull
        public ProtoSession getSession() {
            return mock(ProtoSession.class);
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
            MutableHashCodeRequest that = (MutableHashCodeRequest) obj;
            return java.util.Objects.equals(id, that.id) && java.util.Objects.equals(traceData, that.traceData);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, traceData);
        }

        @Override
        @Nonnull
        public String toString() {
            return "MutableHashCodeRequest[" + id + ", " + traceData + "]";
        }
    }

    static class MutableHashCodeResult implements Result<MutableHashCodeRequest> {
        private final MutableHashCodeRequest request;

        MutableHashCodeResult(MutableHashCodeRequest request) {
            this.request = request;
        }

        @Override
        @Nonnull
        public MutableHashCodeRequest getRequest() {
            return request;
        }
    }

    /**
     * Cache implementation using identity-based storage (IdentityHashMap).
     * Simulates real DefaultRequestCache behavior where request identity —
     * not equals/hashCode — determines cache hits.
     */
    static class IdentityCachingTestRequestCache extends AbstractRequestCache {
        private final java.util.IdentityHashMap<Object, CachingSupplier<?, ?>> cache =
                new java.util.IdentityHashMap<>();

        @Override
        @SuppressWarnings("unchecked")
        protected <REQ extends Request<?>, REP extends Result<REQ>> CachingSupplier<REQ, REP> doCache(
                REQ req, Function<REQ, REP> supplier) {
            return (CachingSupplier<REQ, REP>) cache.computeIfAbsent(req, r -> new CachingSupplier<>(supplier));
        }
    }

    static class TestRequestCache extends AbstractRequestCache {
        private final java.util.Map<TestRequest, RuntimeException> failures = new java.util.HashMap<>();

        void addFailure(TestRequest request, RuntimeException exception) {
            failures.put(request, exception);
        }

        public CacheStatistics getStatistics() {
            return null; // Not implemented for test
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
