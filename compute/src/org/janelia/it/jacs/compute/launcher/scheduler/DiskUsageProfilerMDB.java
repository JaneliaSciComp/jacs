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

package org.janelia.it.jacs.compute.launcher.scheduler;

import org.apache.log4j.Logger;
import org.jboss.annotation.ejb.ResourceAdapter;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.utils.SystemCall;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import java.io.File;

@MessageDriven(activationConfig = {
        // crontTrigger starts with seconds.  Below should run at the stroke of 1 AM EST, every day
        @ActivationConfigProperty(propertyName = "cronTrigger", propertyValue = "0 0 1 * * ?")
})
@ResourceAdapter("quartz-ra.rar")
/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: May 29, 2009
 * Time: 4:30:39 PM
 */
public class DiskUsageProfilerMDB implements Job {
    private static final Logger log = Logger.getLogger(DiskUsageProfilerMDB.class);

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (!SystemConfigurationProperties.getBoolean("ComputeServer.UseScheduler")) {
            return;
        }
        log.info("Waking to profile the filestore disk usage.");
        try {
            String filestoreDir = SystemConfigurationProperties.getString("FileStore.CentralDir");
            String reportsDir = SystemConfigurationProperties.getString("Reports.Dir");
            String diskUsageFilename = SystemConfigurationProperties.getString("DiskUsageFilename");
            SystemCall call = new SystemCall(log);
            call.emulateCommandLine("du --max-depth=1 " + filestoreDir + File.separator + "* > " + reportsDir + File.separator + diskUsageFilename, true);
        }
        catch (Exception e) {
            log.error("There was a problem profiling the disk usage.", e);
        }
        log.info("Filestore disk usage profiling complete.");
    }
}
