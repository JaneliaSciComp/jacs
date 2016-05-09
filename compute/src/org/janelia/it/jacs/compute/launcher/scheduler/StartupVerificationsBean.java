package org.janelia.it.jacs.compute.launcher.scheduler;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.SolrBeanImpl;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityType;
import org.jboss.annotation.ejb.Management;
import org.jboss.annotation.ejb.Service;

/**
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 5/2/13
 * Time: 2:35 PM
 */
@Service
@Management(StartupVerificationService.class)
public class StartupVerificationsBean implements StartupVerificationService {

    private static final Logger log = Logger.getLogger(SolrBeanImpl.class);
    
    @Override
    public void start() throws Exception {
        
        log.info("Verifying EntityConstants...");
        
        List<EntityType> dbTypes = EJBFactory.getLocalEntityBean().getEntityTypes();
        Set<String> dbTypeSet  = new HashSet<String>();
        Set<String> dbAttributeSet  = new HashSet<String>();
        for (EntityType type : dbTypes) {
            dbTypeSet.add(type.getName());
            for(EntityAttribute attr : type.getAttributes()) {
                dbAttributeSet.add(attr.getName());
            }
        }

        // Validate the fields from the class are actually in the database
        boolean missingFields = false;
        Field[] constantFields = EntityConstants.class.getFields();
        for (Field constantField : constantFields) {
            if (constantField.getName().toUpperCase().startsWith("TYPE") && constantField.getType().getName().equals("java.lang.String")){
                if (!dbTypeSet.contains(constantField.get(""))) {
                    log.warn("The database does not know about TYPE constant: "+
                            constantField.getName()+" "+constantField.get(""));
                    missingFields = true;
                }
            }
            else if (constantField.getName().toUpperCase().startsWith("ATTRIBUTE") && constantField.getType().getName().equals("java.lang.String")) {
                if (!dbAttributeSet.contains(constantField.get(""))) {
                    log.warn("The database does not know about ATTRIBUTE constant: "+
                            constantField.getName()+" "+constantField.get(""));
                    missingFields = true;
                }
            }
        }
        if (missingFields) {
            log.warn("EntityConstants do not match the values in the database.  Check the warning messages in the log above.");
        }
        else {
            log.info("Verified all EntityConstants: "+dbTypeSet.size()+" types, "+dbAttributeSet.size()+" attributes");
        }
    }
}
