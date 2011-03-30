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

package org.janelia.it.jacs.compute.app.ejb;

import org.janelia.it.jacs.compute.ComputeTestCase;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.mbean.RecruitmentNodeManager;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 13, 2008
 * Time: 3:36:08 PM
 *
 */
public class BlastFrvGenbankFileTest extends ComputeTestCase {
    RecruitmentNodeManager recruitmentNodeManager;

    public BlastFrvGenbankFileTest() {
        super(BlastFrvGenbankFileTest.class.getName());
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testBlastFrvGenbankFile() {
        String filePath="S:\\filestore\\system\\genomeProject\\1167236322503426404\\NC_002253.gbk";
        String ownerLogin="testuser";
        long taskId=0L;
        // Launch test - this does not block but uses a queue
        try {
            recruitmentNodeManager=new RecruitmentNodeManager();
            taskId=recruitmentNodeManager.blastFrvASingleGenbankFileReturnId(filePath, ownerLogin);
        } catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.toString());
        }
        // Next, poll and wait for result, within a limit
        try {
            org.janelia.it.jacs.compute.api.ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            String[] statusTypeAndValue = computeBean.getTaskStatus(taskId);
            int sanityCheck=300; // five-minutes
            while (!isTaskComplete(statusTypeAndValue[0])) {
                sanityCheck--;
                if (sanityCheck<=0)
                    throw new Exception("Test exceeded maximum permitted time");
                Thread.sleep(1000);
                statusTypeAndValue = computeBean.getTaskStatus(taskId);
            }
            if (!statusTypeAndValue[0].equals("completed"))
                throw new Exception("Task finished with status other than complete="+statusTypeAndValue[0]);
        } catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.toString());
        }
    }

    private boolean isTaskComplete(String status) {
        return status.equals("completed") || status.equals("error");
    }

}
