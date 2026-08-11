package org.apache.maven.impl.model;

import java.util.List;
import java.util.Map;

import org.apache.maven.api.model.Model;
import org.apache.maven.impl.model.DefaultProfileActivationContext.Record;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParentProfileCacheTest {

    @Test
    void recordOfAnUnactivatedProfileMustNotMatchAnActivatedContext() {
        Record withoutRelease = record(List.of(), "release");
        Record withRelease = record(List.of("release"), "release");

        assertFalse(
                withoutRelease.matches(context(List.of("release"))),
                "a parent assembled without -Prelease must not be reused for a module built with -Prelease");
        assertFalse(
                withRelease.matches(context(List.of())),
                "a parent assembled with -Prelease must not be reused for a module built without it");
        assertTrue(withoutRelease.matches(context(List.of())));
        assertTrue(withRelease.matches(context(List.of("release"))));
    }

    @Test
    void recordOfAnUnsuppressedProfileMustNotMatchASuppressedContext() {
        DefaultProfileActivationContext recording = context(List.of(), List.of()).start();
        recording.isProfileInactive("release");
        Record withoutSuppression = recording.stop();

        assertFalse(
                withoutSuppression.matches(context(List.of(), List.of("release"))),
                "a parent assembled without -!release must not be reused for a module built with -!release");
        assertTrue(withoutSuppression.matches(context(List.of(), List.of())));
    }

    /** Records the activation state of {@code profileId} as seen from a context with {@code activeIds}. */
    private static Record record(List<String> activeIds, String profileId) {
        DefaultProfileActivationContext recording = context(activeIds).start();
        recording.isProfileActive(profileId);
        return recording.stop();
    }

    private static DefaultProfileActivationContext context(List<String> activeIds) {
        return context(activeIds, List.of());
    }

    private static DefaultProfileActivationContext context(List<String> activeIds, List<String> inactiveIds) {
        return new DefaultProfileActivationContext(
                null, null, null, activeIds, inactiveIds, Map.of(), Map.of(), Model.newInstance());
    }
}