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

package performance;

import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;
import org.janelia.it.jacs.model.metadata.BioMaterial;

import java.io.File;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Apr 21, 2006
 * Time: 1:01:04 PM
 * @version $Id: HQLBioMaterialDAOTest.java 1 2011-02-16 21:07:19Z tprindle $
 */
public class HQLBioMaterialDAOTest extends TestCase {

    //private static String HIBERNATE_CONFIGURATION_FILE = "C:\\development\\CAMERA\\camera\\build\\container_descriptor\\hibernate.cfg.xml";
    // Tom's location
    private static String HIBERNATE_CONFIGURATION_FILE = "C:\\home\\tomCVS\\CAMERA\\camera\\build\\container_descriptor\\hibernate.cfg.xml";
    private SessionFactory factory;
    private static Logger logger = Logger.getLogger(HQLBioMaterialDAOTest.class);

    private Session session;
    private Transaction transaction;


    public HQLBioMaterialDAOTest() {
        Configuration cfg = new Configuration();
        cfg = cfg.configure(new File(HIBERNATE_CONFIGURATION_FILE));
        factory = cfg.buildSessionFactory();
        session = factory.getCurrentSession();
        transaction = session.beginTransaction();
    }

    public void testFindAll() {
        try {
            Query query = session.createQuery("from BioMaterial");
            List bioMaterialObjects = query.list();
            if (null==bioMaterialObjects || 0==bioMaterialObjects.size()) {
                logger.debug("There are no BioMaterial objects coming back from the database.");
                return;
            }
            logger.debug("Found "+bioMaterialObjects.size()+" BioMaterial objects in the database.  They are:");
            for (Object o : bioMaterialObjects) {
                logger.debug(o.toString());
            }

        } catch (HibernateException e) {
            String message = "Exception: " + e.getMessage();
            logger.warn(message,e);
            fail(message);
        }
    }

    public void testFindById(int id) {
        try {
            Query query = session.createQuery("from BioMaterial exp where exp.id = :id");
            query.setInteger("id", id);
            BioMaterial sampleCollectionObject = (BioMaterial) query.uniqueResult();
            if (null== sampleCollectionObject) {
                logger.debug("There is no BioMaterial object id "+id+" coming back from the database.");
                return;
            }
            logger.debug("Found BioMaterial object id="+ sampleCollectionObject.getMaterialId()+". It's information is:");
            logger.debug(sampleCollectionObject.toString());
        } catch (HibernateException e) {
            String message = "Exception: " + e.getMessage();
            logger.warn(message,e);
            fail(message);
        }
    }

    public void testFindByType() {
        try {
            Query query = session.createQuery("select mat from BioMaterial mat");
            List bioMaterialObjects = query.list();
            if (null==bioMaterialObjects || 0==bioMaterialObjects.size()) {
                logger.debug("There are no BioMaterial objects coming back from the database.");
                return;
            }
            logger.debug("Found "+bioMaterialObjects.size()+" BioMaterial objects in the database.  They are:");
            for (Object o : bioMaterialObjects) {
                logger.debug(o.toString());
            }
        }
        catch (HibernateException e) {
            String message = "Exception: " + e.getMessage();
            logger.warn(message,e);
            fail(message);
        }
    }

    public static void main(String[] args) {
        HQLBioMaterialDAOTest test = new HQLBioMaterialDAOTest();
        logger.debug("\n**TESTING**********************\n");
        logger.debug("\n**TEST FIND ALL****************\n");
        test.testFindAll();
        logger.debug("\n**TEST FIND ALL COMPLETE*******\n");
        logger.debug("\n**TEST FIND BY ID**************\n");
        test.testFindById(1);
        logger.debug("\n**TEST FIND BY ID COMPLETE*****\n");
        logger.debug("\n**TEST FIND BY TYPE************\n");
        test.testFindByType();
        logger.debug("\n**TEST FIND BY TYPE COMPLETE***\n");
        logger.debug("\n**TESTING COMPLETE*************\n");
    }
}
