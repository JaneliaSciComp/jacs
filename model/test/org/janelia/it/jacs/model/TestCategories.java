package org.janelia.it.jacs.model;

/**
 * Container interface for all {@link org.junit.experimental.categories.Category} marker interfaces.
 *
 * These categories allow other tests to be marked so that they can be
 * explicitly excluded or included in various test runs.
 *
 * @author Eric Trautman
 */
public interface TestCategories {

    /**
     * Parent marker for any test that takes less than a few seconds to run.
     * Typically should be used to identify a test that does not depend upon any
     * external resource like a database or web service.
     */
    public interface FastTests {}

    /** Marks short running tests that integrate multiple components. */
    public interface FastIntegrationTests extends FastTests {}

    /** Parent marker for any test that takes more than a few seconds to run. */
    public interface SlowTests {}

    /** Marks longer running tests that integrate multiple components. */
    public interface SlowIntegrationTests extends SlowTests {}

    /** Marks tests used to verify performance requirements. */
    public interface PerformanceTests extends SlowTests {}
}
