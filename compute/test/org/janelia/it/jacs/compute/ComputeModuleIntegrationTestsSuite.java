
package org.janelia.it.jacs.compute;

import org.janelia.it.jacs.compute.access.SageDAOTest;
import org.janelia.it.jacs.model.TestCategories;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Categories.class)
@Categories.IncludeCategory(TestCategories.SlowIntegrationTests.class)
@Suite.SuiteClasses({
        SageDAOTest.class
})
public class ComputeModuleIntegrationTestsSuite {}

// Legacy test classes that potentially could be converted and included here:
//
// BlastTestErrors.class
// BlastTestBasic.class
// BlastTestMultiFasta.class
// BlastTestMultiFastaXMLPersist.class
// BlastTestMultiPartitionQDataNode.class
// ComputeBeanImplTest.class
// ComputeDAOTest.class
// JacsAccessionSearchResultBuilderTest.class
// AccessionSearcherTest.class