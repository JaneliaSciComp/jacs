/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.compute.access;

import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.genomics.Read;
import org.janelia.it.jacs.model.genomics.SequenceType;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.blast.BlastDatabaseFileNode;
import org.janelia.it.jacs.model.user_data.blast.BlastDatasetNode;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 12, 2007
 * Time: 1:01:36 PM
 *
 */
public class ComputeDAOTest extends AbstractTransactionalDataSourceSpringContextTests {
    private ComputeDAO computeDAO;

    public ComputeDAO getComputeDAO() {
        return computeDAO;
    }

    public void setComputeDAO(ComputeDAO computeDAO) {
        this.computeDAO = computeDAO;
    }

    public ComputeDAOTest(String name) {
        super(name);
//        configureLog4jConsole();
    }

    protected String[] getConfigLocations() {
        return new String[] {
                "classpath*:/applicationContext-common.xml",
                "classpath*:/applicationContext-test.xml"};
    }

//    protected static void configureLog4jConsole() {
//        Properties log4jprops = new Properties();
//        log4jprops.setProperty("log4j.category.org.janelia.it.jacs.compute.access.ComputeDAOTestt","DEBUG");
//        log4jprops.setProperty("log4j.category.org.springframework=DEBUG
//        log4jprops.setProperty("log4j.category.org.hibernate=INFO
//        log4jprops.setProperty("log4j.rootCategory","ERROR, stdout");
//        log4jprops.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
//        log4jprops.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
//        log4jprops.setProperty("log4j.appender.stdout.layout.ConversionPattern","%-5r[%24F:%-3L:%-5p]%x %m%n");
//        PropertyConfigurator.configure(log4jprops);
//    }

    public void testComputeDAO() {
        if (computeDAO==null)
            fail("computeDAO is null");
    }

    public void testGetTaskById() {
        Task task=null;
        try {
            task=computeDAO.getTaskById(1044410836048675132L);
            assertNotNull(task);
            BlastTask blastTask=(BlastTask)task;
            Event startEvent=blastTask.getFirstEvent();
            Event endEvent=blastTask.getLastEvent();
            assertTrue(endEvent.getTimestamp().getTime()-startEvent.getTimestamp().getTime() > 0);
            assertEquals(task.getTaskName(),"blastx");
            assertEquals(task.getOwner(),"kli");
            Set parameterSet=task.getParameterKeySet();
            assertNotNull(parameterSet);
            assertTrue(parameterSet.size() > 0);
            Iterator iter=parameterSet.iterator();
            while(iter.hasNext()) {
                String key=(String)iter.next();
                if (key!=null) {
                    if (task.getParameter(key)==null) {
                        fail("Parameter map for task has null value for key="+key);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testUpdateTaskStatus() {
        try {
            computeDAO.updateTaskStatus(1044410836048675132L, Event.COMPLETED_EVENT, "JUnit test for ComputeDAO");
        } catch (Exception ex) {
            ex.printStackTrace();
            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testGetNodeById() {
        Node node=null;
        try {
            node=computeDAO.getNodeById(1045153068942885180L);
            assertNotNull(node);
            assertNotNull(node.getDataType());
            assertTrue(node.getDataType().trim().length()>0);
        } catch (Exception ex) {
            ex.printStackTrace();
            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testGetBlastDatasetNodeById() {
        BlastDatabaseFileNode refDataFileNode=null;
        BlastDatasetNode refDatasetNode=null;
        BlastDatabaseFileNode retrievedDataFileNode=null;
        BlastDatasetNode retrievedDatasetNode=null;
        try {
            User user=computeDAO.getUserByName("unitTest");
            if(user == null) {
                user=new User("unitTest","unitTest",null,null);
                computeDAO.saveOrUpdate(user);
            }
            // create a dataFile Node
            refDataFileNode=new BlastDatabaseFileNode(
                    user.getUserLogin(),
                    null,
                    "test data file node",
                    "test data file node description",
                    Node.VISIBILITY_PRIVATE,
                    SequenceType.NUCLEOTIDE, null);
            refDataFileNode.setPartitionCount(100);
            computeDAO.saveOrUpdate(refDataFileNode);
            // create a dataset node
            refDatasetNode=new BlastDatasetNode(
                    user.getUserLogin(),
                    null,
                    "test dataset node",
                    "test dataset node description",
                    Node.VISIBILITY_PRIVATE,
                    SequenceType.NUCLEOTIDE);
            refDatasetNode.addBlastDatabaseFileNode(refDataFileNode);
            computeDAO.saveOrUpdate(refDatasetNode);
            retrievedDataFileNode=computeDAO.getBlastDatabaseFileNodeById(refDataFileNode.getObjectId());
            assertNotNull(retrievedDataFileNode);
            assertEquals(retrievedDataFileNode.getObjectId(),refDataFileNode.getObjectId());
            retrievedDatasetNode=computeDAO.getBlastDatasetNodeById(refDatasetNode.getObjectId(), true);
            assertEquals(retrievedDatasetNode.getBlastDatabaseFileNodes().size(),1);
            assertNotNull(retrievedDatasetNode);
            assertEquals(retrievedDatasetNode.getObjectId(),refDatasetNode.getObjectId());
            retrievedDatasetNode=computeDAO.getBlastDatasetNodeById(refDataFileNode.getObjectId(), true);
            assertNotNull(retrievedDatasetNode);
            assertEquals(retrievedDatasetNode.getObjectId(),refDataFileNode.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testCreateEvent() {
        try {
            Task task=computeDAO.getTaskById(1044410836048675132L);
            computeDAO.createEvent(task.getObjectId(), Event.COMPLETED_EVENT, "JUnit test", new Date());
        } catch (Exception ex) {
            ex.printStackTrace();
            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testGetUserByName() {
        User user=null;
        try {
            user=computeDAO.getUserByName("smurphy");
            assertNotNull(user);
            assertEquals(user.getUserLogin(), "smurphy");
        } catch (Exception ex) {
            ex.printStackTrace();
            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testGenericLoad() {
        Task task=null;
        try {
            task=(Task)computeDAO.genericGet(Task.class,1044410836048675132L);
            assertNotNull(task);
        } catch (Exception ex) {
            ex.printStackTrace();
            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testGetReadByBseEntityId() {
        Read read=null;
        try {
            read=computeDAO.getReadByBseEntityId(15860450L);
            assertNotNull(read);
            assertEquals(read.getCameraAcc(),"JCVI_READ_1092963132211");
            assertEquals(read.getClearRangeBegin(),new Integer(294));
            assertEquals(read.getClearRangeEnd(),new Integer(513));
            assertEquals(read.getBioSequence().getSequence(),"GACGTTGCGTATGATGTCTTGATTGGATGTATGAAACCCACAATTACGATTTCAGGAATTCAAAACATCCAAATATTTAGTTGACTATATTCAAAAAAATTCAAAATCTGTAAATTATCCCTCTCATGGTCTTGAATAGCTTTTGATATTACATTTGGTGAAGAGGGCCCTCTTACAGTTATATGTGTTGAATATGATGCCTTGCCTGAAATTGGCCATGCATGTGGACATAACATTATTGCAACAGCGTCTATCCGTGCACGATTAGGTTTAAAGGACATAGCTTCCAAATTACGGATTATAGTAAAGCTCCTACGAACTCCTGCTGAAGAAGGTGGGGGTGGAAAAATTATTCTTATTAATGAACGAGCATTTGACGACCCTTCATGTTCAATGATGATTCATCCTGGGTATGAAGATGTGGTGAATCCTACATTTACCACTATTGAACAATATACAGTGGAGTATTTTGGTAAAGACGCACATGCTGCAGGTGCGCCTGATCAAGGCATCAATGCCCTAGATGCACAAATACAACTGTTCGTTAATGCATCTACCTATCGACAGCAAATGGTACAAAGCAACAGAATGCACGGTGTGATAAGAGATGGGGGTTTTAAACCAAATATAATTCCATCATATACAAAATCACAATGGTATTTAAGATCACTAAATGAATAACGATTAAACCAGTTGGAGCAAGACTTTTATAATTTTGTCAATGCTGCTGCATTATCAACAAAGTGTGAAGTAAAAATTACATCACCTGATTACAGATATGAAGAAATCAATAACAATGAAACAATGTATAAGCTCTTCATGGAAAATGCACAAGACGTCGTAGAGAAATGATATTACAAACAGATGCCACGAGACCAGGTTTGGGCTCTACTGATATGGGAAATGTATCCCAAATCTTTCCATCAGTACACCCAATGCTTGGCCATTGCAGAAAAAGAAGCTGTTAATCATCAACCTGAATATGCTGCAGCTACATTAACTGACGTGGTCATAAAGCCATATGATGATGGTGCATATGCAATGGGGTGCTTCCATCATTGTTTTAGCTGAAAAAAATCCTCTGGG");
        } catch (Exception ex) {
            ex.printStackTrace();
            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testGetEntityIdsByCameraAccSet() {
        Map<String, BaseSequenceEntity> map=null;
        try {
            Set accSet=getCameraAccSet();
            logger.info("starting testGetEntityIdsByCameraAccSet");
            map = computeDAO.getEntityIdsByCameraAccSet(accSet);
            assertNotNull(map);
            Set keySet=map.keySet();
            Iterator iter=keySet.iterator();
            while(iter.hasNext()) {
                String key=(String)iter.next();
                assertNotNull(key);
                BaseSequenceEntity bse=(BaseSequenceEntity)map.get(key);
                assertNotNull(bse);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
        logger.info("done with  testGetEntityIdsByCameraAccSet");
        Set values = new HashSet(map.values());
        values.remove(null);
        logger.info("got " + values.size() + "values ");
    }

    protected Set<String> getCameraAccSet() {

        Set<String> hashSet=new HashSet<String>(computeDAO.getFirstAccesions(50000));
        logger.info("getCameraAccSet: got " +hashSet.size() + " accesions");
        return hashSet;
    }

}
