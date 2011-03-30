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

package org.janelia.it.jacs.compute.service.recruitment;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.service.blast.submit.BlastSubmitJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;

import java.io.File;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jan 29, 2010
 * Time: 9:28:27 AM
 */
public class FrvBlastSubmitJobService extends BlastSubmitJobService {
    @Override
    /**
     * If the FRV query file is too large, run on the default queue; otherwise, hit the medium queue.
     */
    protected void setQueue(SerializableJobTemplate jt) throws DrmaaException {
        String queue = NORMAL_QUEUE;
        try {
            List<File> queryFiles = getBlastQueryFiles(processData);
            if (null != queryFiles && queryFiles.size() >= 1) {
                File tmpFile = queryFiles.get(0);
                if (tmpFile.length() <= SystemConfigurationProperties.getLong("Recruitment.MediumQueueMaxByteSize")) {
                    queue = "-l medium";
                }
            }
        }
        catch (Throwable e) {
            logger.error("There was a problem determining the FRVBlast queue.  Defaulting to normal queue.");
        }
        logger.info("Drmaa job=" + jt.getJobName() + " assigned nativeSpec=" + queue);
        jt.setNativeSpecification(queue);
    }
}
