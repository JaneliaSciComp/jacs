package org.janelia.it.jacs.compute.access;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.util.ResultSetIterator;
import org.janelia.it.jacs.shared.solr.SageTerm;
import org.janelia.it.jacs.model.TestCategories;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests the {@link SageDAO} class.
 *
 * @author Eric Trautman
 */
public class SageDAOTest {

    private SageDAO sageDao;

    @Before
    public void setUp() throws Exception {
        sageDao = new SageDAO(LOG);
    }

    @Test
    @Category(TestCategories.FastIntegrationTests.class)
    public void testGetSageImageVocabulary() throws Exception {
        final Map<String, SageTerm> map = sageDao.getSageVocabulary();
        final String[] expectedKeys = { "light_imagery_line", "light_imagery_data_set",
                                        "light_imagery_slide_code", "light_imagery_capture_date", "light_imagery_created_by" };
        for (String expectedKey : expectedKeys) {
            assertTrue("map is missing key: " + expectedKey, map.containsKey(expectedKey));
        }
    }

    @Test
    @Category(TestCategories.SlowIntegrationTests.class)
    public void testGetImagesByFamily() throws Exception {

        ResultSetIterator iterator = null;
        try {
            iterator = sageDao.getImagesByFamily("flylight_flip");
            validateIteratorResults(iterator, 10, "light_imagery_age");
        } finally {
            if (iterator != null) {
                iterator.close();
            }
        }

    }

    @Test
    @Category(TestCategories.SlowIntegrationTests.class)
    public void testGetImagesByDataSet() throws Exception {
        ResultSetIterator iterator = null;
        try {
            iterator = sageDao.getImagesByDataSet("asoy_mb_polarity_case_2");
            validateIteratorResults(iterator, 10, "light_imagery_age");
        } finally {
            if (iterator != null) {
                iterator.close();
            }
        }
    }

    @Test
    @Category(TestCategories.SlowIntegrationTests.class)
    public void testGetAllImagePropertiesByDataSet() throws Exception {
        ResultSetIterator iterator = null;
        try {
            iterator = sageDao.getAllImagePropertiesByDataSet("asoy_mb_polarity_case_3");
            validateIteratorResults(iterator, 10, "light_imagery_age");
        } finally {
            if (iterator != null) {
                iterator.close();
            }
        }
    }

    @Test
    @Category(TestCategories.SlowIntegrationTests.class)
    public void testGetAllLineProperties() throws Exception {
        ResultSetIterator iterator = null;
        try {
            iterator = sageDao.getAllLineProperties();
            validateIteratorResults(iterator, 10, "line_genotype");
        } finally {
            if (iterator != null) {
                iterator.close();
            }
        }
    }

    @Test
    @Category(TestCategories.FastIntegrationTests.class)
    public void testGetAllImagePropertiesForEmptyDataSet() throws Exception {
        ResultSetIterator iterator = null;
        try {
            iterator = sageDao.getAllImagePropertiesByDataSet("system_flylight_optic_lobe_tile");
            validateIteratorResults(iterator, 0, "light_imagery_age");
        } finally {
            if (iterator != null) {
                iterator.close();
            }
        }
    }

    private void validateIteratorResults(ResultSetIterator iterator,
                                         int minimumNumberOfRows,
                                         String targetAttribute) {
        int row = 0;
        Map<String, Object> map;
        while ((row < minimumNumberOfRows) && iterator.hasNext()) {
            row++;
            map = iterator.next();
            assertNotNull(targetAttribute + " is missing for row " + row, map.get(targetAttribute));
        }
        assertEquals("expected to find at least 10 matching rows", minimumNumberOfRows, row);
    }

    private static final Logger LOG = Logger.getLogger(SageDAOTest.class);
}
