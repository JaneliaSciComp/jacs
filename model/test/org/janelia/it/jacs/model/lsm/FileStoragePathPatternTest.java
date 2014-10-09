
package org.janelia.it.jacs.model.lsm;

import org.janelia.it.jacs.model.TestCategories;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;

/**
 * Tests the {@link FileStoragePathPattern} class.
 *
 * @author Eric Trautman
 */
@Category(TestCategories.FastTests.class)
public class FileStoragePathPatternTest {

    @Test
    public void testValidPatterns() throws Exception {

        final Object[][] testData = {
                // patternString                             basePath
                { "/fs/lab/confocalStacks/{yyyy}/{MM}/{dd}", "/fs/lab/confocalStacks", 3 },
                { "/fs/lab",                                 "/fs/lab",                0 },
                { "/fs/lab/confocalStacks/{yyyy}",           "/fs/lab/confocalStacks", 1 },
                { "/fs/lab/confocalStacks/{}/{}",            "/fs/lab/confocalStacks", 2 }
        };

        String patternString;
        String basePath;
        int numberOfParentDirectories;
        FileStoragePathPattern pathPattern;
        for (Object[] test : testData) {
            patternString = (String) test[0];
            basePath = (String) test[1];
            numberOfParentDirectories = (Integer) test[2];
            pathPattern = new FileStoragePathPattern(patternString);

            Assert.assertEquals("invalid base path derived for '" + patternString + "'",
                                basePath, pathPattern.getBasePath());

            Assert.assertEquals("invalid number of parent directories derived for '" + patternString + "'",
                                numberOfParentDirectories,
                                pathPattern.getNumberOfParentDirectoriesToIncludeInRelativePath());
        }

    }

    @Test
    public void testInvalidPatterns() throws Exception {

        final String[] invalidPatterns = {
                "../confocalStacks/{yyyy}/{MM}/{dd}",
                "confocalStacks",
                "/confocalStacks/{yyyy}/",
                "/confocalStacks/../{yyyy}",
                "/confocalStacks/{}{}",
                "/confocalStacks/{..}",
                "",
                null
        };

        for (String patternString : invalidPatterns) {
            try {
                new FileStoragePathPattern(patternString);
                Assert.fail("'" + patternString + "' pattern string should have caused exception");
            } catch (IllegalArgumentException e) {
                Assert.assertTrue("test passed", true);
            }
        }

    }

    @Test
    public void testPathDerivation() throws Exception {

        final String sourceBasePath = "/tier2/projtechres/confocalStacks";
        final String targetBasePath = "/groups/rubin/data0/screen/confocalStacks";
        FileStoragePathPattern pathPattern =  new FileStoragePathPattern(targetBasePath + "/{yyyy}/{MM}/{dd}");

        final String[] relativeSourcePaths = {
                "1999/01/02/JPTR_1111_2.lsm",
                "2111/33/44/JPTR_5555_6.lsm"
        };

        File sourceFile;
        File storageFile;
        String expectedStoragePath;
        for (String relativeSourcePath : relativeSourcePaths) {
            sourceFile = new File(sourceBasePath + '/' + relativeSourcePath);

            Assert.assertEquals("invalid relative path derived for " + sourceFile.getAbsolutePath(),
                                relativeSourcePath, pathPattern.getRelativePath(sourceFile));

            expectedStoragePath = targetBasePath + '/' + relativeSourcePath;
            storageFile = pathPattern.getStorageFile(sourceFile);
            Assert.assertEquals("invalid storage path derived for " + sourceFile.getAbsolutePath(),
                                expectedStoragePath, storageFile.getAbsolutePath());
        }

    }

    @Test
    public void testIsLocationDifferent() throws Exception {

        final String sourceBasePath = "/tier2/projtechres/confocalStacks";
        final String targetBasePath = "/groups/rubin/data0/screen/confocalStacks";
        final String relativeSourcePath = "1999/01/02/JPTR_1111_2.lsm";
        FileStoragePathPattern pathPattern =  new FileStoragePathPattern(targetBasePath + "/{yyyy}/{MM}/{dd}");

        File sourceFile = new File(sourceBasePath + '/' + relativeSourcePath);
        Assert.assertTrue("different location should be identified for " + sourceFile.getAbsolutePath(),
                          pathPattern.isLocationDifferent(sourceFile));

        sourceFile = new File(targetBasePath + '/' + relativeSourcePath);
        Assert.assertFalse("different location should NOT be identified for " + sourceFile.getAbsolutePath(),
                           pathPattern.isLocationDifferent(sourceFile));
    }

}