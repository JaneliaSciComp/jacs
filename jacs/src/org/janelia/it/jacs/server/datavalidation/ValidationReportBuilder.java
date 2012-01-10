
package org.janelia.it.jacs.server.datavalidation;

/**
 * Test program for validating blastable subject nodes
 */
abstract public class ValidationReportBuilder {

    protected ValidationReportBuilder() {
    }

    abstract public ValidationReport createaValidationReport();

    abstract public void printReport(ValidationReport report);

    abstract public void startObjectValidation(Object obj,
                                               ValidationReport report);

    abstract public void endObjectValidation(Object obj,
                                             ValidationReport report,
                                             int validationCode,
                                             String validationMessage);

}
