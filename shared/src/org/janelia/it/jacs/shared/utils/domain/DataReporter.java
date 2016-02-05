package org.janelia.it.jacs.shared.utils.domain;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.shared.utils.MailHelper;

/**
 * Report problems with certain data via email.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DataReporter {

    private String fromEmail;
    private String toEmail;
    
    public DataReporter(String fromEmail, String toEmail) {
        this.fromEmail = fromEmail;
        this.toEmail = toEmail;
    }

    private String createEntityReport(DomainObject domainObject, String annotation) {
        StringBuilder sBuf = new StringBuilder();
        sBuf.append("Name: ").append(domainObject.getName()).append("\n");
        sBuf.append("Type: ").append(domainObject.getType()).append("\n");
        sBuf.append("Owner: ").append(domainObject.getOwnerKey()).append("\n");
        sBuf.append("ID: ").append(domainObject.getId().toString()).append("\n");
        if (annotation!=null) {
            sBuf.append("Annotation: ").append(annotation).append("\n\n");
        }
        return sBuf.toString();
    }
 
    public void reportData(DomainObject domainObject, String annotation) {
        String subject = "Reported Data: " + domainObject.getName();
        String report = createEntityReport(domainObject, annotation);
        MailHelper helper = new MailHelper();
        helper.sendEmail(fromEmail, toEmail, subject, report);
    }
    
    public void reportData(DomainObject domainObject) {
        reportData(domainObject, null);
    }
    
}