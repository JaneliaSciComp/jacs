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

package org.janelia.it.jacs.compute.mbean;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 7, 2008
 * Time: 11:21:05 AM
 */
public interface HmmerTestMBean {

    public void start();

    public void stop();

    public void createFastaNode_seqpath_usr_name_desc_type(
            String seqPath, String username, String name, String description, String sequenceType);

    public void submitHmmpfamSmallSingleTest();

    public void submitHmmpfamTIGRFAMTestByQueryNodeId(long queryNodeId);

    public void createHmmpfamDatabaseNode(String databaseFilePath, String name, String description, int numberOfHmms);

}
