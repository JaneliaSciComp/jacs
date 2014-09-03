package org.janelia.it.jacs.compute.process_result_validation.content_checker.component;


import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import java.io.File;
import java.util.Collection;
import java.util.Map;

/**
 * Accepts messages launching the validation of content in the JACS database.
 *
 * Created by fosterl on 6/24/14.
 */
/*
@MessageDriven(
        name="ContentValidationMDB",
        activationConfig={
                @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
                @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
                @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "2"),
                @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/contentValidation"),
                @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000"),
                @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
        }
)
*/
public class ContentValidationMDB {

    @EJB
    private EntityBeanLocal entityBean;

    private static Logger logger = Logger.getLogger(ContentValidationMDB.class);

    public void onMessage(Message message) {
        // Only process the message if it is the right type.
        try {
            if ( message instanceof ObjectMessage) {
                // So far, object content is unused.  May as well be null.


/*
                // Get the prototype hierarchy for the testing.
                SimpleVHF validationHierarchyFactory = new SimpleVHF();
                Map<String,PrototypeValidatable> validatables = validationHierarchyFactory.getValidatables();
                if ( validatables == null || validatables.size() == 0 ) {
                    logger.error("Prototype collection is empty; nothing to check.");
                    return;
                }

                // Establish the finders to use.
                FileFinder fileFinder = new FilesystemFileFinder();
                EntityFinder entityFinder = new ConcreteEntityFinder();
                AttributeFinder attributeFinder = new ConcreteAttributeFinder();

                // Get the user list.

                Collection<String> subjectKeys = null;//???;

                for ( String subjectKey: subjectKeys ) {
                    // For each user get all samples.
                    entityBean.getEntitiesByTypeName(subjectKey, EntityConstants.TYPE_SAMPLE );

                    // Go through the hierarchy, checking that everything in the prototype is satisfied.
                }
                */
            }
            else {
                logger.error("Wrong message type " + message.getClass());
            }
        } catch ( Exception cex ) {
            logger.error("Failed to complete validation.");
            throw new RuntimeException(cex);
        }
    }

}
