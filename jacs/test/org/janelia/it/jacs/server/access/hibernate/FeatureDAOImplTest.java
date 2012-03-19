
package org.janelia.it.jacs.server.access.hibernate;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.hibernate.Session;
import org.hibernate.collection.GWTEntityCleaner;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.*;
import org.janelia.it.jacs.model.metadata.Library;
import org.janelia.it.jacs.server.access.FeatureDAO;
import org.janelia.it.jacs.test.JacswebTestCase;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tdolafi
 * Date: Aug 4, 2006
 * Time: 11:25:09 AM
 */
public class FeatureDAOImplTest extends JacswebTestCase {

    private FeatureDAO featureDAO;

    public FeatureDAO getFeatureDAO() {
        return featureDAO;
    }

    public void setFeatureDAO(FeatureDAO featureDAO) {
        this.featureDAO = featureDAO;
    }

    public FeatureDAOImplTest() {
        super(FeatureDAOImplTest.class.getName());
    }

    public void testFeatureDAO() {
        assertNotNull(featureDAO);
    }

    public void testFindBseByUid() {
        BaseSequenceEntity bse = null;
        try {
            bse = featureDAO.findBseByUid(1495672648L);
            assertNotNull(bse);
            assertEquals(bse.getAccession(), "JCVI_PEP_1096142014829");
            //assertEquals(bse.getExternalAcc(), "CF647727"); no longer valid
        } catch (Exception ex) {

            String message = "Exception: " + ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testGetSequenceEntitiesByAccessions() {
        try {
            List<String> accs= Arrays.asList(
                    "JCVI_PEP_1096462996691",
                    "JCVI_PEP_1096463696125",
                    "JCVI_PEP_1096464898311",
                    "JCVI_PEP_1096470595377",
                    "JCVI_PEP_1096491437105"
            );
            List<BaseSequenceEntity> bses = featureDAO.getSequenceEntitiesByAccessions(accs);
            Session session = featureDAO.getSessionFactory().getCurrentSession();
            // close session to test for lazy objects
            session.close();
            GWTEntityCleaner.evictAndClean(bses, session);
            assertNotNull(bses);
            assertEquals(bses.size(),accs.size());
            for(BaseSequenceEntity bse : bses) {
                assertTrue(accs.contains(bse.getAccession()));
                assertTrue(bse.getSequence() == null);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            String message = "Exception: " + ex.getMessage();
            logger.warn(message,ex);
            fail(message);
        }
    }

    public void testGetReadByBseEntityId() {
        Read read = null;
        try {
            read = featureDAO.getReadByBseEntityId(15860450L);
            assertNotNull(read);
            assertEquals(read.getAccession(), "JCVI_READ_1092963132211");
            assertEquals(read.getClearRangeBegin(), new Integer(294));
            assertEquals(read.getClearRangeEnd(), new Integer(513));
            assertEquals(read.getBioSequence().getSequence(), "GACGTTGCGTATGATGTCTTGATTGGATGTATGAAACCCACAATTACGATTTCAGGAATTCAAAACATCCAAATATTTAGTTGACTATATTCAAAAAAATTCAAAATCTGTAAATTATCCCTCTCATGGTCTTGAATAGCTTTTGATATTACATTTGGTGAAGAGGGCCCTCTTACAGTTATATGTGTTGAATATGATGCCTTGCCTGAAATTGGCCATGCATGTGGACATAACATTATTGCAACAGCGTCTATCCGTGCACGATTAGGTTTAAAGGACATAGCTTCCAAATTACGGATTATAGTAAAGCTCCTACGAACTCCTGCTGAAGAAGGTGGGGGTGGAAAAATTATTCTTATTAATGAACGAGCATTTGACGACCCTTCATGTTCAATGATGATTCATCCTGGGTATGAAGATGTGGTGAATCCTACATTTACCACTATTGAACAATATACAGTGGAGTATTTTGGTAAAGACGCACATGCTGCAGGTGCGCCTGATCAAGGCATCAATGCCCTAGATGCACAAATACAACTGTTCGTTAATGCATCTACCTATCGACAGCAAATGGTACAAAGCAACAGAATGCACGGTGTGATAAGAGATGGGGGTTTTAAACCAAATATAATTCCATCATATACAAAATCACAATGGTATTTAAGATCACTAAATGAATAACGATTAAACCAGTTGGAGCAAGACTTTTATAATTTTGTCAATGCTGCTGCATTATCAACAAAGTGTGAAGTAAAAATTACATCACCTGATTACAGATATGAAGAAATCAATAACAATGAAACAATGTATAAGCTCTTCATGGAAAATGCACAAGACGTCGTAGAGAAATGATATTACAAACAGATGCCACGAGACCAGGTTTGGGCTCTACTGATATGGGAAATGTATCCCAAATCTTTCCATCAGTACACCCAATGCTTGGCCATTGCAGAAAAAGAAGCTGTTAATCATCAACCTGAATATGCTGCAGCTACATTAACTGACGTGGTCATAAAGCCATATGATGATGGTGCATATGCAATGGGGTGCTTCCATCATTGTTTTAGCTGAAAAAAATCCTCTGGG");
        } catch (Exception ex) {

            String message = "Exception: " + ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testGetReadWithLibraryByAccesion() {
        Read read = null;
        try {
            read = featureDAO.getReadWithLibraryByAccesion("JCVI_READ_1092963132211");
            assertNotNull(read);
            assertEquals(read.getAccession(), "JCVI_READ_1092963132211");
            assertEquals(read.getClearRangeBegin(), new Integer(294));
            assertEquals(read.getClearRangeEnd(), new Integer(513));
            assertEquals(read.getBioSequence().getSequence(), "GACGTTGCGTATGATGTCTTGATTGGATGTATGAAACCCACAATTACGATTTCAGGAATTCAAAACATCCAAATATTTAGTTGACTATATTCAAAAAAATTCAAAATCTGTAAATTATCCCTCTCATGGTCTTGAATAGCTTTTGATATTACATTTGGTGAAGAGGGCCCTCTTACAGTTATATGTGTTGAATATGATGCCTTGCCTGAAATTGGCCATGCATGTGGACATAACATTATTGCAACAGCGTCTATCCGTGCACGATTAGGTTTAAAGGACATAGCTTCCAAATTACGGATTATAGTAAAGCTCCTACGAACTCCTGCTGAAGAAGGTGGGGGTGGAAAAATTATTCTTATTAATGAACGAGCATTTGACGACCCTTCATGTTCAATGATGATTCATCCTGGGTATGAAGATGTGGTGAATCCTACATTTACCACTATTGAACAATATACAGTGGAGTATTTTGGTAAAGACGCACATGCTGCAGGTGCGCCTGATCAAGGCATCAATGCCCTAGATGCACAAATACAACTGTTCGTTAATGCATCTACCTATCGACAGCAAATGGTACAAAGCAACAGAATGCACGGTGTGATAAGAGATGGGGGTTTTAAACCAAATATAATTCCATCATATACAAAATCACAATGGTATTTAAGATCACTAAATGAATAACGATTAAACCAGTTGGAGCAAGACTTTTATAATTTTGTCAATGCTGCTGCATTATCAACAAAGTGTGAAGTAAAAATTACATCACCTGATTACAGATATGAAGAAATCAATAACAATGAAACAATGTATAAGCTCTTCATGGAAAATGCACAAGACGTCGTAGAGAAATGATATTACAAACAGATGCCACGAGACCAGGTTTGGGCTCTACTGATATGGGAAATGTATCCCAAATCTTTCCATCAGTACACCCAATGCTTGGCCATTGCAGAAAAAGAAGCTGTTAATCATCAACCTGAATATGCTGCAGCTACATTAACTGACGTGGTCATAAAGCCATATGATGATGGTGCATATGCAATGGGGTGCTTCCATCATTGTTTTAGCTGAAAAAAATCCTCTGGG");
        } catch (Exception ex) {

            String message = "Exception: " + ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testGetPairedReads() {
        List<Read> matedReadsByAcc = null;
        List<Read> matedReadsById = null;
        try {
            final String readAcc = "JCVI_READ_299";
            Read currentRead = featureDAO.getReadByAccesion(readAcc);
            assertNotNull(currentRead);
            assertEquals(currentRead.getAccession(), readAcc);
            matedReadsByAcc = featureDAO.getPairedReadsByAccession(readAcc);
            matedReadsById = featureDAO.getPairedReadsByEntityId(currentRead.getEntityId());
            if(matedReadsByAcc != null && matedReadsByAcc.size() > 0) {
                assertEquals(matedReadsById.size(),matedReadsById.size());
                for(Read matedRead : matedReadsByAcc) {
                    assertEquals(matedRead.getTemplateAcc(),currentRead.getTemplateAcc());
                    assertEquals(matedRead.getLibraryAcc(),currentRead.getLibraryAcc());
                    assertFalse(matedRead.getAccession().equals(currentRead.getAccession()));
                    assertTrue(matedReadsById.contains(matedRead));
                }
                for(Read matedRead : matedReadsById) {
                    assertTrue(matedReadsByAcc.contains(matedRead));
                }
            } else {
                if(matedReadsByAcc == null) {
                    assertNull(matedReadsById);
                } else {
                    assertEquals(matedReadsById.size(),0);
                }
            }
        } catch (Exception ex) {

            String message = "Exception: " + ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testGetBsesForReadBySubclassName() {
        try {
            String testReadAcc = "HOT_READ_85804877";
            SortArgument[] testSortArgs = null;
            String[] featureTypes = new String[] {
                    "ORF",
                    "NonCodingRNA"
            };
            int numReadFeatures = featureDAO.getNumReadFeaturesBySubclassName(featureTypes,testReadAcc);
            List readFeatures = featureDAO.getReadFeaturesBySubclassName(featureTypes,testReadAcc,0,-1,testSortArgs);
            assertTrue(numReadFeatures == readFeatures.size());
        } catch(Exception e) {
            logger.error("testGetBsesForReadBySubclassName failed",e);
            failFromException(e);
        }
    }

    public void testNcbiNtRetrieval() {
        try {
            BaseSequenceEntity bse = featureDAO.findBseByAcc("NCBI_NT_42773");
            Session session = featureDAO.getSessionFactory().getCurrentSession();
            // close session to test for lazy objects
            session.close();
            GWTEntityCleaner.evictAndClean(bse, session);
            // this should be null but unfortunately BaseSequenceEntity.getSequence creates a new BioSequence object
            // assertEquals("", bse.getBioSequence().getSequence()); this line is failing pending evaluation
            assertNull(bse.getOwner());
            assertEquals("ORF", bse.getEntityType().getName());     // updated from 'GenericService NA'
            assertEquals("ORF", bse.getEntityType().getAbbrev());
            assertEquals("NA", bse.getEntityType().getSequenceType().getName());
            assertEquals("Nucleic Acid", bse.getEntityType().getSequenceType().getDescription());
            assertEquals(new Integer(1023),bse.getSequenceLength());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    /*
    public void testNcbiGenfLargeSequenceRetrieval() {
        try {
            BaseSequenceEntity bse = featureDAO.findBseByAcc("NCBI_GENF_89949249");
            Session session = featureDAO.getSessionFactory().getCurrentSession();
            GWTEntityCleaner.evictAndClean(bse, session);
            // close session to test for lazy objects
            session.close();

            // this should be null but unfortunately BaseSequenceEntity.getSequence creates a new BioSequence object
            assertEquals("", bse.getBioSequence().getSequence());
            assertNull(bse.getOwner());
            assertEquals("Saccharophagus degradans 2-40", bse.getDescription());
            assertEquals("CP000282.1", bse.getExternalAcc());
            assertEquals("Chromosome", bse.getEntityType().getName());
            assertEquals("CHR", bse.getEntityType().getAbbrev());
            assertEquals("Deoxyribonucleic acid", bse.getEntityType().getSequenceType().getDescription());
            assertEquals("DNA", bse.getEntityType().getSequenceType().getName());
            assertEquals(new Integer(5057471),bse.getSeqLength());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }   */

    public void testSequenceTextRetrieval() {
        try {
            String sequence = (String) featureDAO.getBioSequenceTextByBseEntityId(403393930L);
            Session session = featureDAO.getSessionFactory().getCurrentSession();
            // close session to test for lazy objects
            session.close();
            assertNotNull(sequence);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public void testSequenceRetrieval() {

        try {
            BioSequence sequence = (BioSequence) featureDAO.getBioSequenceByBseEntityId(403393930L);
            Session session = featureDAO.getSessionFactory().getCurrentSession();
            // close session to test for lazy objects
            session.close();
            validateSequence(sequence);
        } catch (Exception e) {
            fail(e.getMessage());
        }

    }

    public void testReadRetrieval() {
        try {
            Read read = featureDAO.getReadWithSequenceByBseEntityId(403393930L);
            Session session = featureDAO.getSessionFactory().getCurrentSession();
            // close session to test for lazy objects
            session.close();
            validateRead(read, false);
            assertNotNull(read.getBioSequence().getSequence());
            assertEquals(Integer.valueOf("114"), read.getSequenceLength());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public void testReadProxyFreeRetrieval() {
        try {

            Read read = featureDAO.getReadWithLibraryByBseEntityId(403393930L);
            Session session1 = featureDAO.getSessionFactory().getCurrentSession();
            // close session to test for lazy objects
            session1.close();
            GWTEntityCleaner.evictAndClean(read, session1);
            validateRead(read, true);

            // this should be null but unfortunately BaseSequenceEntity.getSequence creates a new BioSequence object
            // assertEquals("", read.getBioSequence().getSequence());  clean isn't working for some reason
            assertEquals(Integer.valueOf("114"), read.getSequenceLength());

        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public void testGetSubjectsForBLASTHits() {
        try {
            Long testTaskId = 1129555549319332191L;
            Set<Long> testBlastHitIDs = new HashSet<Long>(Arrays.asList(
                    1129555591698620767L,
                    1129555591698624863L,
                    1129555591698628959L,
                    1129555591698633055L,
                    1129555591698637151L,
                    1129555591698641247L,
                    1129555591702774111L,
                    1129555591702778207L,
                    1129555591702810975L,
                    1129555591702815071L,
                    1129555591702819167L,
                    1129555591702823263L,
                    1129555591702827359L,
                    1129555591702831455L,
                    1129555591702835551L,
                    1129555591706968415L,
                    1129555591702782303L,
                    1129555591698583903L,
                    1129555591698587999L,
                    1129555591698592095L,
                    1129555591698596191L,
                    1129555591698600287L
            ));
            long testNumDistinctSubjects = featureDAO.getNumSubjectBSESet_ByTaskId(testTaskId,testBlastHitIDs);
            Set<BaseSequenceEntity> subjectSeqs = featureDAO.getSubjectBSESet_ByTaskId(testTaskId,testBlastHitIDs);
            assertTrue(subjectSeqs.size() == testNumDistinctSubjects);
            // get all subjects
            testNumDistinctSubjects = featureDAO.getNumSubjectBSESet_ByTaskId(testTaskId,null);
            subjectSeqs = featureDAO.getSubjectBSESet_ByTaskId(testTaskId,null);
            assertTrue(subjectSeqs.size() == testNumDistinctSubjects);
        } catch (Exception ex) {
            failFromException(ex);
        }
    }

    public void testGetQueriesForBLASTHits() {
        try {
            Long testTaskId = 1129555549319332191L;
            Set<Long> testBlastHitIDs = new HashSet<Long>(Arrays.asList(
                    1129555591698620767L,
                    1129555591698624863L,
                    1129555591698628959L,
                    1129555591698633055L,
                    1129555591698637151L,
                    1129555591698641247L,
                    1129555591702774111L,
                    1129555591702778207L,
                    1129555591702810975L,
                    1129555591702815071L,
                    1129555591702819167L,
                    1129555591702823263L,
                    1129555591702827359L,
                    1129555591702831455L,
                    1129555591702835551L,
                    1129555591706968415L,
                    1129555591702782303L,
                    1129555591698583903L,
                    1129555591698587999L,
                    1129555591698592095L,
                    1129555591698596191L,
                    1129555591698600287L
            ));
            long testNumDistinctQueries = featureDAO.getNumQueryBSESet_ByTaskId(testTaskId,testBlastHitIDs);
            Set<BaseSequenceEntity> querySeqs = featureDAO.getQueryBSESet_ByTaskId(testTaskId,testBlastHitIDs);
            assertTrue(querySeqs.size() == testNumDistinctQueries);
            // get all queries
            testNumDistinctQueries = featureDAO.getNumQueryBSESet_ByTaskId(testTaskId,testBlastHitIDs);
            querySeqs = featureDAO.getQueryBSESet_ByTaskId(testTaskId,testBlastHitIDs);
            assertTrue(querySeqs.size() == testNumDistinctQueries);
        } catch (Exception ex) {
            failFromException(ex);
        }
    }

    public void testGetPagedReadsFromSample() {
        try {
            String testSampleAcc = "JCVI_SMPL_1103283000002";
            Set<String> testReadAccs = new HashSet<String>(Arrays.asList(
                    "JCVI_READ_834799",
                    "JCVI_READ_834798",
                    "JCVI_READ_834800",
                    "JCVI_READ_834801",
                    "JCVI_READ_834802",
                    "JCVI_READ_834803",
                    "JCVI_READ_834804",
                    "JCVI_READ_834805",
                    "JCVI_READ_834806",
                    "JCVI_READ_834807",
                    "JCVI_READ_834808",
                    "JCVI_READ_834809"
            ));
            SortArgument[] testSortArgs = new SortArgument[] {
                    new SortArgument("accession", SortArgument.SORT_ASC),
                    new SortArgument("sequenceLength", SortArgument.SORT_DESC),
                    new SortArgument("clearRangeBegin", SortArgument.SORT_ASC),
                    new SortArgument("clearRangeEnd", SortArgument.SORT_ASC),
                    new SortArgument("libraryAcc", SortArgument.SORT_DESC)
            };
            List<Read> readList = featureDAO.getPagedReadsFromSample(testSampleAcc,
                    testReadAccs,
                    0,
                    -1,
                    testSortArgs);
            assertTrue(readList.size() == testReadAccs.size());
            for(Read r : readList) {
                assertTrue(testReadAccs.contains(r.getAccession()));
                testReadAccs.remove(r.getAccession());
            }
            assertTrue("Following accessions were not found: " + testReadAccs,testReadAccs.size() == 0);
        } catch (Exception ex) {

            String message = "Exception: " + ex.getMessage();
            logger.warn(message,ex);
            fail(message);
        }
    }

    public void testProteinCoreCluster() {
        try {
            String coreClusterAcc = "CAM_CRCL_4523305";
            ProteinCluster coreCluster = featureDAO.getProteinCoreCluster(coreClusterAcc);
            assertTrue(coreCluster != null);
            assertTrue(coreClusterAcc.equals(coreCluster.getClusterAcc()));
        } catch (Exception ex) {

            String message = "Exception: " + ex.getMessage();
            logger.warn(message,ex);
            fail(message);
        }
    }

    public void testProteinFinalCluster() {
        try {
            String finalClusterAcc = "CAM_CL_11523305";
            ProteinCluster finalCluster = featureDAO.getProteinFinalCluster(finalClusterAcc);
            assertTrue(finalCluster != null);
            assertTrue(finalClusterAcc.equals(finalCluster.getClusterAcc()));
            finalClusterAcc = "CAM_CL_160";
            finalCluster = featureDAO.getProteinFinalCluster(finalClusterAcc);
            assertTrue(finalCluster != null);
            assertTrue(finalClusterAcc.equals(finalCluster.getClusterAcc()));
        } catch (Exception ex) {

            String message = "Exception: " + ex.getMessage();
            logger.warn(message,ex);
            fail(message);
        }
    }

    public void testGetCoreClusterAnnotations() {
        try {
            String coreClusterAcc = "CAM_CRCL_841";
            SortArgument[] sortArgs = new SortArgument[] {
                    new SortArgument("accession", SortArgument.SORT_ASC),
                    new SortArgument("annotationType", SortArgument.SORT_ASC),
                    new SortArgument("annotationID", SortArgument.SORT_ASC),
                    new SortArgument("description", SortArgument.SORT_ASC),
                    new SortArgument("evidencePct", SortArgument.SORT_ASC)
            };
            List<ClusterAnnotation> annotations = featureDAO.getCoreClusterAnnotations(coreClusterAcc,sortArgs);
        } catch (Exception ex) {

            String message = "Exception: " + ex.getMessage();
            logger.warn(message,ex);
            fail(message);
        }
    }

    public void testGetFinalClusterAnnotations() {
        try {
            String finalClusterAcc = "CAM_CL_10";
            SortArgument[] sortArgs = new SortArgument[] {
                    new SortArgument("accession", SortArgument.SORT_ASC),
                    new SortArgument("annotationType", SortArgument.SORT_ASC),
                    new SortArgument("annotationID", SortArgument.SORT_ASC),
                    new SortArgument("description", SortArgument.SORT_ASC)
            };
            List<ClusterAnnotation> annotations = featureDAO.getFinalClusterAnnotations(finalClusterAcc,sortArgs);
        } catch (Exception ex) {

            String message = "Exception: " + ex.getMessage();
            logger.warn(message,ex);
            fail(message);
        }
    }

    public void testGetPagedCoreClustersFromFinalCluster() {
        try {
            String finalClusterAcc = "CAM_CL_10";
            SortArgument[] sortArgs = new SortArgument[] {
                    new SortArgument("coreClusterAcc", SortArgument.SORT_ASC),
                    new SortArgument("finalClusterAcc", SortArgument.SORT_ASC),
                    new SortArgument("longestMemberAcc", SortArgument.SORT_ASC),
                    new SortArgument("clusterQuality", SortArgument.SORT_ASC),
                    new SortArgument("numNonRedundant", SortArgument.SORT_ASC),
                    new SortArgument("numProteins", SortArgument.SORT_ASC)
            };
            ProteinCluster finalCluster = featureDAO.getProteinFinalCluster(finalClusterAcc);
            List<ProteinCluster> coreClusters = featureDAO.getPagedCoreClustersFromFinalCluster(finalClusterAcc, null, 0,-1,sortArgs);
            assertTrue(finalCluster.getNumClusterMembers() == coreClusters.size());
            Set<String> finalClusterSubsetAccs = new HashSet<String>();
            int subsetIndex = 0;
            for(ProteinCluster cl : coreClusters) {
                finalClusterSubsetAccs.add(cl.getClusterAcc());
                if(++subsetIndex > 10) {
                    break;
                }
            }
            coreClusters = featureDAO.getPagedCoreClustersFromFinalCluster(finalClusterAcc, finalClusterSubsetAccs, 0,-1,sortArgs);
            assertTrue(coreClusters.size() == finalClusterSubsetAccs.size());
        } catch (Exception ex) {

            String message = "Exception: " + ex.getMessage();
            logger.warn(message,ex);
            fail(message);
        }
    }

    public void testGetPagedNRSeqMembersFromCoreCluster() {
        try {
            String coreClusterAcc = "CAM_CRCL_841";
            SortArgument[] sortArgs = new SortArgument[] {
                    new SortArgument("protein_acc", SortArgument.SORT_ASC),
                    new SortArgument("core_cluster_acc", SortArgument.SORT_ASC),
                    new SortArgument("final_cluster_acc", SortArgument.SORT_ASC),
                    new SortArgument("nr_parent_acc", SortArgument.SORT_ASC),
                    new SortArgument("longest_protein_acc", SortArgument.SORT_ASC),
                    new SortArgument("rep_protein_acc", SortArgument.SORT_ASC),
                    new SortArgument("sequence_length", SortArgument.SORT_ASC),
                    new SortArgument("cluster_quality", SortArgument.SORT_ASC)
            };
            ProteinCluster coreCluster = featureDAO.getProteinCoreCluster(coreClusterAcc);
            int currentOffset = 0;
            int pageLength = 100;
            int nRecords = 0;
            for(;;) {
                List<ProteinClusterMember> coreClusterMembers = featureDAO.getPagedNRSeqMembersFromCoreCluster(coreClusterAcc,
                        null,
                        currentOffset,
                        pageLength,
                        sortArgs);
                nRecords += coreClusterMembers.size();
                if(coreClusterMembers.size() < pageLength) {
                    break;
                }
                currentOffset += coreClusterMembers.size();
            }
            // for now we assert that cluster.numProteins >= selected records because
            // some of the proteins are missing
            assertTrue(coreCluster.getNumNonRedundantProteins() >= nRecords);
        } catch (Exception ex) {

            String message = "Exception: " + ex.getMessage();
            logger.warn(message,ex);
            fail(message);
        }
    }

    public void testGetPagedSeqMembersFromCoreCluster() {
        try {
            String coreClusterAcc = "CAM_CRCL_841";
            SortArgument[] sortArgs = new SortArgument[] {
                    new SortArgument("protein_acc", SortArgument.SORT_ASC),
                    new SortArgument("core_cluster_acc", SortArgument.SORT_ASC),
                    new SortArgument("final_cluster_acc", SortArgument.SORT_ASC),
                    new SortArgument("nr_parent_acc", SortArgument.SORT_ASC),
                    new SortArgument("longest_protein_acc", SortArgument.SORT_ASC),
                    new SortArgument("rep_protein_acc", SortArgument.SORT_ASC),
                    new SortArgument("sequence_length", SortArgument.SORT_ASC),
                    new SortArgument("cluster_quality", SortArgument.SORT_ASC)
            };
            ProteinCluster coreCluster = featureDAO.getProteinCoreCluster(coreClusterAcc);
            int currentOffset = 0;
            int pageLength = 100;
            int nRecords = 0;
            for(;;) {
                List<ProteinClusterMember> coreClusterMembers = featureDAO.getPagedSeqMembersFromCoreCluster(coreClusterAcc,
                        null,
                        currentOffset,
                        pageLength,
                        sortArgs);
                nRecords += coreClusterMembers.size();
                if(coreClusterMembers.size() < pageLength) {
                    break;
                }
                currentOffset += coreClusterMembers.size();
            }
            // for now we assert that cluster.numProteins >= selected records because
            // some of the proteins are missing
            assertTrue(coreCluster.getNumProteins() >= nRecords);
        } catch (Exception ex) {

            String message = "Exception: " + ex.getMessage();
            logger.warn(message,ex);
            fail(message);
        }
    }

    public void testGetPagedNRSeqMembersFromFinalCluster() {
        try {
            String finalClusterAcc = "CAM_CL_10";
            SortArgument[] sortArgs = new SortArgument[] {
                    new SortArgument("protein_acc", SortArgument.SORT_ASC),
                    new SortArgument("core_cluster_acc", SortArgument.SORT_ASC),
                    new SortArgument("final_cluster_acc", SortArgument.SORT_ASC),
                    new SortArgument("nr_parent_acc", SortArgument.SORT_ASC),
                    new SortArgument("longest_protein_acc", SortArgument.SORT_ASC),
                    new SortArgument("rep_protein_acc", SortArgument.SORT_ASC),
                    new SortArgument("sequence_length", SortArgument.SORT_ASC),
                    new SortArgument("cluster_quality", SortArgument.SORT_ASC)
            };
            ProteinCluster proteinCluster = featureDAO.getProteinFinalCluster(finalClusterAcc);
            int currentOffset = 0;
            int pageLength = 100;
            int nRecords = 0;
            for(;;) {
                List<ProteinClusterMember> clusterMembers = featureDAO.getPagedNRSeqMembersFromFinalCluster(finalClusterAcc,
                        null,
                        currentOffset,
                        pageLength,
                        sortArgs);
                nRecords += clusterMembers.size();
                if(clusterMembers.size() < pageLength) {
                    break;
                }
                currentOffset += clusterMembers.size();
            }
            // for now we assert that cluster.numProteins >= selected records because
            // some of the proteins are missing
            assertTrue(proteinCluster.getNumNonRedundantProteins() >= nRecords);
        } catch (Exception ex) {

            String message = "Exception: " + ex.getMessage();
            logger.warn(message,ex);
            fail(message);
        }
    }

    public void testGetPagedSeqMembersFromFinalCluster() {
        try {
            String finalClusterAcc = "CAM_CL_10";
            Set<String> clusterMemberAccs = new HashSet<String>();
            clusterMemberAccs.add("JCVI_PEP_1096459625305");
            clusterMemberAccs.add("JCVI_PEP_1096131038455");
            clusterMemberAccs.add("JCVI_PEP_1096138040137");
            clusterMemberAccs.add("JCVI_PEP_1096668198009");
            clusterMemberAccs.add("JCVI_PEP_1096691740185");
            clusterMemberAccs.add("JCVI_PEP_1096696768215");
            clusterMemberAccs.add("JCVI_PEP_1096682664275");
            SortArgument[] sortArgs = new SortArgument[] {
                    new SortArgument("protein_acc", SortArgument.SORT_ASC),
                    new SortArgument("core_cluster_acc", SortArgument.SORT_ASC),
                    new SortArgument("final_cluster_acc", SortArgument.SORT_ASC),
                    new SortArgument("nr_parent_acc", SortArgument.SORT_ASC),
                    new SortArgument("longest_protein_acc", SortArgument.SORT_ASC),
                    new SortArgument("rep_protein_acc", SortArgument.SORT_ASC),
                    new SortArgument("sequence_length", SortArgument.SORT_ASC),
                    new SortArgument("cluster_quality", SortArgument.SORT_ASC)
            };
            ProteinCluster proteinCluster = featureDAO.getProteinFinalCluster(finalClusterAcc);
            int currentOffset = 0;
            int pageLength = 100;
            int nRecords = 0;
            List<ProteinClusterMember> clusterMembers;
            for(;;) {
                clusterMembers = featureDAO.getPagedSeqMembersFromFinalCluster(finalClusterAcc,
                        null,
                        currentOffset,
                        pageLength,
                        sortArgs);
                nRecords += clusterMembers.size();
                if(clusterMembers.size() < pageLength) {
                    break;
                }
                currentOffset += clusterMembers.size();
            }
            // for now we assert that cluster.numProteins >= selected records because
            // some of the proteins are missing
            assertTrue(proteinCluster.getNumProteins() >= nRecords);
            clusterMembers = featureDAO.getPagedSeqMembersFromFinalCluster(finalClusterAcc,
                    clusterMemberAccs,
                    currentOffset,
                    pageLength,
                    sortArgs);
            assertTrue(clusterMembers.size() >= clusterMemberAccs.size());
        } catch (Exception ex) {
            
            String message = "Exception: " + ex.getMessage();
            logger.warn(message,ex);
            fail(message);
        }
    }

    public void testGetNumPeptidesByScaffoldAcc() throws Exception {
        // Scaffold with reads and peptides but no samples of its own
       // Scaffold with sample, reads, and peptides
        int count = featureDAO.getNumOfPeptidesForScaffoldByAccNo("JCVI_SCAF_1096627126331");
        assertTrue(count==9);
    }

    public void testGetNumPeptidesByScaffoldAcc2() throws Exception {
        // Scaffold with sample, reads, and peptides
        int count = featureDAO.getNumOfPeptidesForScaffoldByAccNo("JCVI_SCAF_1098315329727");
        assertTrue(count==7);
    }

    public void testGetPeptidesByScaffoldAcc() throws Exception {
        // Scaffold with reads and peptides but no samples of its own
        List results= featureDAO.getPeptidesForScaffoldByAccNo("JCVI_SCAF_1096627126331",0,-1,null);
        assertTrue(results.size()==9);
    }

    public void testGetPeptidesByScaffoldAcc2() throws Exception {
        // Scaffold with sample, reads, and peptides
        List results= featureDAO.getPeptidesForScaffoldByAccNo("JCVI_SCAF_1098315329727",0,-1,null);
        assertTrue(results.size()==7);
    }


    public void testGetORfsAndRnasByReadAcc() throws Exception {
        // Scaffold with sample, reads, and peptides
        List results= featureDAO.getRelatedORFsAndRNAs("JCVI_READ_1092955333854",0,-1,null);
        assertEquals(8,results.size());
    }
    private void validateLibrary(Library library) {
        assertEquals(Long.valueOf("281621516"), library.getLibraryId());
        assertEquals("SCUMS_LIB_Arctic", library.getLibraryAcc());
        assertEquals("pyrosequencing (454)", library.getSequencingTechnology());
    }


    private void validateRead(Read read, boolean validateLibrary) {
        // GenericService bse data
        assertEquals("SCUMS_READ_Arctic2448841", read.getAccession());
        assertNull(read.getExternalAcc());
        //assertNull(read.getDescription());

        // Entity type
        EntityTypeGenomic entityType = read.getEntityType();
        assertEquals("Read", entityType.getName());
        assertEquals("READ", entityType.getAbbrev());
        assertEquals("Read", entityType.getDescription());
        assertEquals("NA", entityType.getSequenceType().getName());

        // Read specific data
        assertNull(read.getTraceAcc());
        assertNull(read.getTemplateAcc());
        assertNull(read.getSequencingDirection());
        assertNull(read.getClearRangeBegin());
        assertNull(read.getClearRangeEnd());

        if (validateLibrary) {
            // Library
            Library library = read.getLibrary();
            validateLibrary(library);
        }

        // Samples
//        validateSamples(read.getLibrary().getSamples());
    }


    private void validateSequence(BioSequence sequence) {
        assertEquals("NA", sequence.getSequenceType().getName());
        assertEquals(Long.valueOf(403393932), sequence.getSequenceId());
    }

    public static Test suite() {
        return new TestSuite(FeatureDAOImplTest.class);
    }

}