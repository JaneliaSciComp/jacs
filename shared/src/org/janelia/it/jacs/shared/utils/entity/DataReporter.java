package org.janelia.it.jacs.shared.utils.entity;

import org.janelia.it.jacs.model.entity.Entity;
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

    private String createEntityReport(Entity entity, String annotation) {
        StringBuilder sBuf = new StringBuilder();
        sBuf.append("Name: ").append(entity.getName()).append("\n");
        sBuf.append("Type: ").append(entity.getEntityTypeName()).append("\n");
        sBuf.append("Owner: ").append(entity.getOwnerKey()).append("\n");
        sBuf.append("ID: ").append(entity.getId().toString()).append("\n");
        if (annotation!=null) {
            sBuf.append("Annotation: ").append(annotation).append("\n\n");
        }
        return sBuf.toString();
    }
 
    public void reportData(Entity entity, String annotation) {
        String subject = "Reported Data: " + entity.getName();
        String report = createEntityReport(entity, annotation);
        MailHelper helper = new MailHelper();
        helper.sendEmail(fromEmail, toEmail, subject, report);
    }
    
    public void reportData(Entity entity) {
        reportData(entity, null);
    }
    
}