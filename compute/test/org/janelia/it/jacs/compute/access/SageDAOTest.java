package org.janelia.it.jacs.compute.access;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.util.ResultSetIterator;
import org.janelia.it.jacs.compute.api.support.SageTerm;

import java.util.Map;

/**
 * Tests the {@link SageDAO} class.
 *
 * @author Eric Trautman
 */
public class SageDAOTest extends TestCase {

    private SageDAO sageDao;

    public SageDAOTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(SageDAOTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        sageDao = new SageDAO(LOG);
    }

    @Override
    protected void tearDown() throws Exception {
    }

    public void testGetFlylightImageVocabulary() throws Exception {
        final Map<String, SageTerm> map = sageDao.getFlylightImageVocabulary();
        final String[] expectedKeys = { "id", "name", "path", "line", "data_set",
                                        "slide_code", "capture_date", "created_by" };
        for (String expectedKey : expectedKeys) {
            assertTrue("map is missing key: " + expectedKey, map.containsKey(expectedKey));
        }
    }

    public void testGetImagesByFamily() throws Exception {

        ResultSetIterator iterator = null;
        try {
            iterator = sageDao.getImagesByFamily("flylight_flip");
            validateIteratorResults(iterator, 10);
        } finally {
            if (iterator != null) {
                iterator.close();
            }
        }

    }

    public void testGetImagesByDataSet() throws Exception {
        ResultSetIterator iterator = null;
        try {
            iterator = sageDao.getImagesByDataSet("asoy_mb_polarity_63x_case_2");
            validateIteratorResults(iterator, 10);
        } finally {
            if (iterator != null) {
                iterator.close();
            }
        }
    }

    public void testGetAllImagePropertiesByDataSet() throws Exception {
        ResultSetIterator iterator = null;
        try {
            iterator = sageDao.getAllImagePropertiesByDataSet("asoy_mb_polarity_63x_case_3");
            validateIteratorResults(iterator, 10);
        } finally {
            if (iterator != null) {
                iterator.close();
            }
        }
    }

    public void testGetAllImagePropertiesForEmptyDataSet() throws Exception {
        ResultSetIterator iterator = null;
        try {
            iterator = sageDao.getAllImagePropertiesByDataSet("system_flylight_optic_lobe_tile");
            validateIteratorResults(iterator, 0);
        } finally {
            if (iterator != null) {
                iterator.close();
            }
        }
    }

    private void validateIteratorResults(ResultSetIterator iterator,
                                         int minimumNumberOfRows) {
        int row = 0;
        Map<String, Object> map;
        while ((row < minimumNumberOfRows) && iterator.hasNext()) {
            row++;
            map = iterator.next();
            assertNotNull("age is missing for row " + row, map.get("age"));
        }
        assertEquals("expected to find at least 10 matching rows", minimumNumberOfRows, row);
    }

    private static final Logger LOG = Logger.getLogger(SageDAOTest.class);
}
