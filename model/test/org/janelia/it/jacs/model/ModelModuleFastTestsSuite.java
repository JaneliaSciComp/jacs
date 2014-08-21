
package org.janelia.it.jacs.model;

import org.janelia.it.jacs.model.entity.DataSetTest;
import org.janelia.it.jacs.model.entity.EntityTypeTest;
import org.janelia.it.jacs.model.lsm.FileStoragePathPatternTest;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * This test suite generates a {@link org.junit.runner.Runner}
 * for all {@link TestCategories.ModelModuleFastTests} tests.
 *
 * @author Eric Trautman
 */
@RunWith(Categories.class)
@Categories.IncludeCategory(TestCategories.FastTests.class)
@Suite.SuiteClasses({
        DataSetTest.class,
        EntityTypeTest.class,
        FileStoragePathPatternTest.class
})
public class ModelModuleFastTestsSuite {}
