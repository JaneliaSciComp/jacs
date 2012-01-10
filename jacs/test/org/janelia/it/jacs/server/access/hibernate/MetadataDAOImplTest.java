
package org.janelia.it.jacs.server.access.hibernate;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.hibernate.Session;
import org.hibernate.collection.GWTEntityCleaner;
import org.janelia.it.jacs.model.metadata.*;
import org.janelia.it.jacs.server.access.MetadataDAO;
import org.janelia.it.jacs.test.JacswebTestCase;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 8, 2007
 * Time: 11:15:53 AM
 *
 */
public class MetadataDAOImplTest extends JacswebTestCase {

    private MetadataDAO metadataDAO;

    public MetadataDAO getMetadataDAO() {
        return metadataDAO;
    }

    public void setMetadataDAO(MetadataDAO metadataDAO) {
        this.metadataDAO = metadataDAO;
    }

    public MetadataDAOImplTest() {
        super(MetadataDAOImplTest.class.getName());
    }

    public void testMetadataDAO() {
        assertNotNull(metadataDAO);
    }

    public void testGetSamplesByLibraryId() {
        List sampleList = null;
        try {
            sampleList = metadataDAO.getSamplesByLibraryId(281621618L);
            assertNotNull(sampleList);
            assertTrue(sampleList.size() > 0);
            Sample sample = (Sample) sampleList.get(0);
            assertTrue(sample.getSampleName() != null && sample.getSampleName().trim().length() > 0);
        } catch (Exception ex) {
            String message = "Exception: " + ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testGetLibraryByReadEntityId() {
        Library library = null;
        try {
            library = metadataDAO.getLibraryByReadEntityId(412084264L);
            assertNotNull(library);
            assertEquals(library.getLibraryId(), new Long(281621514L));
            assertEquals(library.getLibraryAcc(), "JGI_LIB_HF770_12-21-03");
        } catch (Exception ex) {
            String message = "Exception: " + ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testLibraryRetrieval() {
        try {
            Library library = metadataDAO.getLibraryByReadEntityId(403393930L);
            Session session = metadataDAO.getSessionFactory().getCurrentSession();
            // close session to test for lazy objects
            session.close();
            validateLibrary(library);

        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public void testLibraryProxyFreeRetrieval() {
        try {
            Library library = metadataDAO.getLibraryByReadEntityId(403393930L);
            Session session = metadataDAO.getSessionFactory().getCurrentSession();
            // close session to test for lazy objects
            GWTEntityCleaner.evictAndClean(library, session);
            session.close();
            validateLibrary(library);
            assertNull(library.getReads());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public void testSamplesByLibraryIdRetrieval() {
        try {
            List<Sample> sampleList = metadataDAO.getSamplesWithSitesByLibraryId(281621516L);
            Session session = metadataDAO.getSessionFactory().getCurrentSession();
            GWTEntityCleaner.evictAndClean(sampleList, session);
            // close session to test for lazy objects
            session.close();
            validateSamples(sampleList);
        } catch (Exception e) {
            fail(e.getMessage());
        }

    }

    private void validateLibrary(Library library) {
        assertEquals(Long.valueOf("281621516"), library.getLibraryId());
        assertEquals("SCUMS_LIB_Arctic", library.getLibraryAcc());
        assertEquals("pyrosequencing (454)", library.getSequencingTechnology());
    }

    private void validateSamples(List samples) {
        try {
            assertTrue(samples.size() > 0);
            Sample sample = (Sample) samples.iterator().next();
            validateSample(sample);

        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private void validateSample(Sample sample) {
        try {
            assertEquals(Long.valueOf(239426594), sample.getSampleId());
            assertEquals("SCUMS_SMPL_Arctic", sample.getSampleAcc());
            assertEquals("Arctic", sample.getSampleName());
//            assertEquals("SCUMS_SITE_ARCTIC", sample.getExperimentId());
//            if (sample instanceof FilterSample) {
//                FilterSample filterSample = (FilterSample) sample;
//                assertEquals(Double.valueOf("0.0"), filterSample.getFilterMin());
//                assertEquals(Double.valueOf("0.22"), filterSample.getFilterMax());
//            }
            validateSites(sample);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private void validateSites(Sample sample) {
        try {
            // Site
            Set biomaterials=sample.getBioMaterials();
            assertTrue(biomaterials.size() == 2);
            for (Object b : biomaterials) {
                BioMaterial biomat=(BioMaterial)b;
                CollectionSite site = biomat.getCollectionSite();
                GeoPoint point=(GeoPoint)site;
                if (site.getSiteId()==281621480L) {
                    assertEquals("CAM_PROJ_MarineVirome", biomat.getProject());
                    assertEquals("Canadian Arctic", site.getLocation());
                    assertEquals("Arctic", site.getRegion());
                    assertNull(site.getSiteDescription());
                    assertEquals("Canada", point.getCountry());
                    assertEquals("136d 25.6' w", point.getLongitude());
                    assertEquals("70d 41.6' n", point.getLatitude());
                    assertEquals("2-968 m", point.getDepth());
                    assertEquals("", biomat.getObservationAsString("water depth"));
                    assertEquals("", biomat.getObservationAsString("chlorophyll density"));
                    assertEquals("", biomat.getObservationAsString("fluorescence"));
                    assertEquals("", biomat.getObservationAsString("transmission"));
                    assertEquals("", biomat.getObservationAsString("dissolved oxygen"));
                    assertEquals("-1.5-1.5 C", biomat.getObservationAsString("temperature"));
                    assertEquals("20.3-34.9 psu", biomat.getObservationAsString("salinity"));
                    assertEquals("10-200 L", biomat.getObservationAsString("volume filtered"));
                    assertEquals("",biomat.getObservationAsString("biomass"));
                    assertEquals("",biomat.getObservationAsString("dissolved inorganic carbon"));
                    assertEquals("",biomat.getObservationAsString("dissolved inorganic phosphate"));
                    assertEquals("",biomat.getObservationAsString("dissolved organic carbon"));
                    assertEquals("",biomat.getObservationAsString("nitrate+nitrite"));
                    assertEquals("42", biomat.getObservationAsString("number of samples pooled"));
                    assertEquals("23", biomat.getObservationAsString("number of stations sampled"));
                    assertEquals(new SimpleDateFormat("yyyy-MM-dd").parse("2002-09-01"), biomat.getCollectionStartTime());
                    assertEquals(new SimpleDateFormat("yyyy-MM-dd").parse("2002-10-01"), biomat.getCollectionStopTime());
                } else if (site.getSiteId()==281621474L) {
                    assertEquals("CAM_PROJ_MarineVirome", biomat.getProject());
                    assertEquals("Arctic, Chukchi Sea", site.getLocation());
                    assertEquals("USA", point.getCountry());
                    assertEquals("Arctic", site.getRegion());
                    assertEquals("159d 26.8' w", point.getLongitude());
                    assertEquals("73d 9.7' n", point.getLatitude());
                    assertEquals("10-3246 m", point.getDepth());
                    assertEquals("", biomat.getObservationAsString("water depth"));
                    assertEquals("", biomat.getObservationAsString("chlorophyll density"));
                    assertEquals("", biomat.getObservationAsString("fluorescence"));
                    assertEquals("", biomat.getObservationAsString("transmission"));
                    assertEquals("", biomat.getObservationAsString("dissolved oxygen"));
                    assertEquals("-1.4-5.4 C", biomat.getObservationAsString("temperature"));
                    assertEquals("26.8-35 psu", biomat.getObservationAsString("salinity"));
                    assertEquals("10-200 L", biomat.getObservationAsString("volume filtered"));
                    assertEquals("",biomat.getObservationAsString("biomass"));
                    assertEquals("",biomat.getObservationAsString("dissolved inorganic carbon"));
                    assertEquals("",biomat.getObservationAsString("dissolved inorganic phosphate"));
                    assertEquals("",biomat.getObservationAsString("dissolved organic carbon"));
                    assertEquals("",biomat.getObservationAsString("nitrate+nitrite"));
                    assertEquals("14", biomat.getObservationAsString("number of samples pooled"));
                    assertEquals("7", biomat.getObservationAsString("number of stations sampled"));
                    assertEquals(new SimpleDateFormat("yyyy-MM-dd").parse("2002-09-01"), biomat.getCollectionStartTime());
                    assertEquals(new SimpleDateFormat("yyyy-MM-dd").parse("2002-09-01"), biomat.getCollectionStopTime());
                } else {
                    fail("Unknown site id " + site.getSiteId());
                }
            }

        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public static Test suite() {
        return new TestSuite(MetadataDAOImplTest.class);
    }

}
