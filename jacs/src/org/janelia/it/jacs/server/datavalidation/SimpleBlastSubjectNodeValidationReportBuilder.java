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

package org.janelia.it.jacs.server.datavalidation;

import org.janelia.it.jacs.model.user_data.Node;

import java.util.List;

/**
 * Test program for validating blastable subject nodes
 */
public class SimpleBlastSubjectNodeValidationReportBuilder extends ValidationReportBuilder {

    public SimpleBlastSubjectNodeValidationReportBuilder() {
    }

    public ValidationReport createaValidationReport() {
        return new SimpleBlastSubjectNodeValidationReport();
    }

    public void printReport(ValidationReport report) {
        SimpleBlastSubjectNodeValidationReport simpleBlastSubjectNodeReport = (SimpleBlastSubjectNodeValidationReport) report;
        List<String> reportContent = simpleBlastSubjectNodeReport.getContent();
        System.out.println("================= Print Validation Report For Blastable Subject Nodes =================");
        if (reportContent.size() > 0) {
            for (String msg : reportContent) {
                System.out.println(msg);
            }
        }
        else {
            System.out.println("No problem found");
        }
        System.out.println("================ Finished Validation Report For Blastable Subject Nodes =================");
    }

    public void startObjectValidation(Object obj, ValidationReport report) {
        SimpleBlastSubjectNodeValidationReport simpleBlastSubjectNodeReport = (SimpleBlastSubjectNodeValidationReport) report;
    }

    public void endObjectValidation(Object obj,
                                    ValidationReport report,
                                    int validationCode,
                                    String validationMessage) {
        SimpleBlastSubjectNodeValidationReport simpleBlastSubjectNodeReport = (SimpleBlastSubjectNodeValidationReport) report;
        if (validationMessage != null) {
            Node n = (Node) obj;
            simpleBlastSubjectNodeReport.addMessage("Node " +
                    n.getObjectId().toString() + " - " +
                    n.getDescription() + ": " + validationMessage);
        }
    }

}
