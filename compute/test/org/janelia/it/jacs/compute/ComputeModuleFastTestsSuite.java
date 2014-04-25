
package org.janelia.it.jacs.compute;

import org.janelia.it.jacs.compute.access.SageDAOTest;
import org.janelia.it.jacs.model.TestCategories;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Categories.class)
@Categories.IncludeCategory(TestCategories.FastTests.class)
@Suite.SuiteClasses({
        SageDAOTest.class
})
public class ComputeModuleFastTestsSuite {}