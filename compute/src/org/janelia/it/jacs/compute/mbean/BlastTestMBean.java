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
 * Date: Nov 7, 2006
 * Time: 11:22:09 AM
 *
 * @version $Id: BlastTestMBean.java 1 2011-02-16 21:07:19Z tprindle $
 */
public interface BlastTestMBean {

    public void start();

    public void stop();

    public void submitFileNodeQueryBlastNTest();

    public void submitDataNodeQueryBlastNTest();

    public String submitMultipleFileNodeQueryBlastNTest(int numOfJobs, int numOfAlinments);

    public void submitFrvBlastNTest();

    public void saveTaskParameterTest();

    public void updateTasksToParameterStringMap();

    public void deleteBlastResultsOrOrphanDirsForUser(String username, boolean isDebug);

    public void deleteUserBlastNodesOrOrphanDirsBeforeDate(String username, int month, int day, int year, boolean isDebug);

    public void submitSimpleBlastGridTest(String queryPath, String dbPrefixPath, String outputPrefixPath, int numJobs, String queue);

    public void removeFastaAndResultFileNodesForSystem();

    public long getCumulativeCpuTime(long taskId);
//    public void convertBlastXmlToAnotherFormat(String pathToSourceXmlResultFile, String outputFormat, String newFileDestinationDir);
}

