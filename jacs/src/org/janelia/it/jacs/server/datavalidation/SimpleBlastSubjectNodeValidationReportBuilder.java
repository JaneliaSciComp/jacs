
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
