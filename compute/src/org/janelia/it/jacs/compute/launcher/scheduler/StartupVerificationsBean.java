package org.janelia.it.jacs.compute.launcher.scheduler;

import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityType;
import org.jboss.annotation.ejb.Management;
import org.jboss.annotation.ejb.Service;

import java.lang.reflect.Field;
import java.util.List;
import java.util.TreeMap;

/**
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 5/2/13
 * Time: 2:35 PM
 */
@Service
@Management(StartupVerificationService.class)
public class StartupVerificationsBean implements StartupVerificationService {

    @Override
    public void start() throws Exception {
        List<EntityType> dbTypes = EJBFactory.getLocalEntityBean().getEntityTypes();
        List<EntityAttribute> dbAttributes = EJBFactory.getLocalEntityBean().getEntityAttributes();
        System.out.println("\n\n\nTesting the startup service:");
        TreeMap<String, EntityType> dbTypeMap  = new TreeMap<String, EntityType>();
        TreeMap<String, EntityAttribute> dbAttributeMap  = new TreeMap<String, EntityAttribute>();
        for (EntityType type : dbTypes) {
            dbTypeMap.put(type.getName(), type);
        }
        for (EntityAttribute att : dbAttributes) {
            dbAttributeMap.put(att.getName(), att);
        }

        // Debug and print out the types and attributes
//        for (EntityType dbType : dbTypes) {
//            System.out.println("Type: "+dbType.getName());
//        }
//        for (EntityAttribute dbAttribute : dbAttributes) {
//            System.out.println("Attribute: "+dbAttribute.getName());
//        }
//        System.out.println("\n\n\n");

        // Validate the fields from the class are actually in the database
        boolean missingFields = false;
        Field[] constantFields = EntityConstants.class.getFields();
        for (Field constantField : constantFields) {
            if (constantField.getName().toUpperCase().startsWith("TYPE") && constantField.getType().getName().equals("java.lang.String")){
                if (!dbTypeMap.keySet().contains((String)constantField.get(new String()))) {
                    System.out.println("WARNING!!!!!:  The database does not know about TYPE constant: "+
                            constantField.getName()+" "+(String)constantField.get(new String()));
                    missingFields = true;
                }
            }
            else if (constantField.getName().toUpperCase().startsWith("ATTRIBUTE") && constantField.getType().getName().equals("java.lang.String")) {
                if (!dbAttributeMap.keySet().contains((String)constantField.get(new String()))) {
                    System.out.println("WARNING!!!!!:  The database does not know about ATTRIBUTE constant: "+
                            constantField.getName()+" "+(String)constantField.get(new String()));
                    missingFields = true;
                }
            }
//            else {
//                System.out.println("\n\nWARNING!!!!! EntityConstants defines something not in the database: "+constantField.toString()+"\n\n\n");
//                throw new Exception("EntityConstants do not match the values in the database");
//            }
        }
        if (missingFields) {
            throw new Exception("\n\n\nEntityConstants do not match the values in the database.  Check the Warning messages in the log above.\n\n\n");
        }
    }
}
