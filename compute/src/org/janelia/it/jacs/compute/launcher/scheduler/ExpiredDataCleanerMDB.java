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
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

@MessageDriven(activationConfig = {
        // crontTrigger starts with seconds.  Below should run at the stroke of 2AM EST, every day
        @ActivationConfigProperty(propertyName = "cronTrigger", propertyValue = "0 0 4 * * ?")
})
@ResourceAdapter("quartz-ra.rar")
/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: May 29, 2009
 * Time: 4:30:39 PM
 */
public class ExpiredDataCleanerMDB implements Job {
    private static final Logger log = Logger.getLogger(ExpiredDataCleanerMDB.class);

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (!SystemConfigurationProperties.getBoolean("ComputeServer.UseScheduler")) {
            return;
        }
        log.info("Waking to clean out the expired tasks and nodes.");
//        try {
////            String filestoreDir = SystemConfigurationProperties.getString("FileStore.CentralDir");
//            String systemEmail = SystemConfigurationProperties.getString("ComputeServer.SystemEmail");
////            int warningDayAge = SystemConfigurationProperties.getInt("ExpiredDataCleaner.DaysToTriggerWarning");
////            int deletionDayAge = SystemConfigurationProperties.getInt("ExpiredDataCleaner.DaysToTriggerDeletion");
//
//            ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();
//            List<User> users = computeBean.getAllUsers();
//            // Loop through all the users and get a list of items which are older than 7 days
//            for (User user : users) {
//                MailHelper helper = new MailHelper();
//                helper.sendEmail(systemEmail, user.getEmail(), "VICS Data Expiration Notice", getFormattedEmail());
//            }
//            // Email the user for things exactly warningDayAge days old Warning, deletionDayAge days old scheduled for deletion
//            // Send email
//            // Clean up the items
//        }
//        catch (Exception e) {
//            log.error("There was a problem cleaning the expired data.", e);
//        }
        log.info("Expired data cleaning complete.");
    }

    public String getFormattedEmail() {
        return "Boo!";
    }
}