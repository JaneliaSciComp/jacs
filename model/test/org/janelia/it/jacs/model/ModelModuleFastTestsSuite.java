
package org.janelia.it.jacs.model;

import org.janelia.it.jacs.model.domain.DomainObjectTests;
import org.janelia.it.jacs.model.domain.DomainUtilsTest;
import org.janelia.it.jacs.model.domain.OntologyTests;
import org.janelia.it.jacs.model.domain.SampleTests;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * This test suite generates a {@link org.junit.runner.Runner}
 * for all {@link TestCategories.FastTests} tests.
 *
 * @author Eric Trautman
 */
@RunWith(Categories.class)
@Categories.IncludeCategory(TestCategories.FastTests.class)
@Suite.SuiteClasses({
        DomainObjectTests.class,
        DomainUtilsTest.class,
        OntologyTests.class,
        SampleTests.class
})
public class ModelModuleFastTestsSuite {}
