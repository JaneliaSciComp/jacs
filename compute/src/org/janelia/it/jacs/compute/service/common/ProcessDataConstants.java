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

package org.janelia.it.jacs.compute.service.common;

import org.janelia.it.jacs.compute.engine.data.IProcessData;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 14, 2007
 * Time: 11:41:49 AM
 */
public interface ProcessDataConstants extends IProcessData {

    public static final String RECRUITMENT_FILE_NODE_ID = "RECRUITMENT_FILE_NODE_ID";
    public static final String RECRUITMENT_FILE_NODE = "RECRUITMENT_FILE_NODE";

    public static final String RESULT_FILE_NODE_ID = "RESULT_FILE_NODE_ID";
    public static final String RESULT_FILE_NODE = "RESULT_FILE_NODE";
    public static final String RESULT_FILE_NODE_DIR = "RESULT_FILE_NODE_DIR";
    public static final String JOB_SET = "JOB_SET";

    public static final String INPUT_FILE_NODE_ID = "INPUT_FILE_NODE_ID";
}
