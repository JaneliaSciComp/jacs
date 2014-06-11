package org.janelia.it.jacs.compute.service.align;

import org.apache.commons.io.FileUtils;
import org.janelia.it.jacs.model.TestCategories;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link org.janelia.it.jacs.compute.access.SageDAO} class.
 *
 * @author Eric Trautman
 */
public class ConfiguredAlignmentServiceTest {

    private ConfiguredAlignmentService service;
    private File baseTestDir;

    @Before
    public void setUp() throws Exception {
        service = new ConfiguredAlignmentService();
        baseTestDir = new File("testConfiguredAlignmentService").getAbsoluteFile();
    }

    @After
    public void tearDown() throws Exception {
        if (baseTestDir.exists()) {
            FileUtils.deleteDirectory(baseTestDir);
        }
    }

    @Test
    @Category(TestCategories.FastIntegrationTests.class)
    public void testGetPropertiesFiles() throws Exception {
        final String[] relativePaths = {
                "Brains/20x/Aligned20xScale.properties",
                "Brains/63x/Aligned63xScale.properties",
                "Neurons/20x/NeuronAligned20xScale.properties",
                "Neurons/63x/NeuronAligned63xScale.properties"
        };
        List<File> expectedPropertiesFiles = new ArrayList<>();
        File propertiesFile;
        for (String path : relativePaths) {
            propertiesFile = new File(baseTestDir, path);
            createTestFile(propertiesFile);
            expectedPropertiesFiles.add(propertiesFile);
        }

        createTestFile(new File(baseTestDir, "not-a-properties-file.txt"));

        final Collection<File> actualPropertiesFiles = service.getPropertiesFiles(baseTestDir);

        assertEquals("invalid number of files found",
                     expectedPropertiesFiles.size(), actualPropertiesFiles.size());
    }

    private void createTestFile(File file) {

        final File parentFile = file.getParentFile();
        if ((parentFile != null) && (! parentFile.exists())) {
            if (parentFile.mkdirs()) {
                LOG.info("createTestFile: created " + parentFile.getAbsolutePath());
            } else {
                LOG.warn("createTestFile: failed to create " + parentFile.getAbsolutePath());
            }
        }

        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            writer.write("alignment.stack.filename=foo\n");
            LOG.info("createTestFile: created " + file.getAbsolutePath());
        } catch (Exception e) {
            LOG.error("createTestFile: failed to create " + file.getAbsolutePath(), e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {
                    LOG.warn("createTestFile: failed to close " + file.getAbsolutePath(), e);
                }
            }
        }

    }

    private static final Logger LOG = LoggerFactory.getLogger(ConfiguredAlignmentServiceTest.class);

}
