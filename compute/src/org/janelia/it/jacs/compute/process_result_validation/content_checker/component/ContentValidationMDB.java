package org.janelia.it.jacs.compute.process_result_validation.content_checker.component;


import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.PrototypeValidatable;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.SimpleVHF;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.Validatable;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.finder.ConcreteAttributeFinder;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.finder.ConcreteEntityFinder;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.finder.FilesystemFileFinder;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.validatable.AttributeFinder;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.validatable.EntityFinder;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.validatable.FileFinder;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.validatable.FileValidatable;
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
public class ContentValidationMDB {

    @EJB
    private EntityBeanLocal entityBean;

    private static Logger logger = Logger.getLogger(ContentValidationMDB.class);

    public void onMessage(Message message) {
        // Only process the message if it is the right type.
        try {
            if ( message instanceof ObjectMessage) {
                // So far, object content is unused.  May as well be null.

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
            }
            else {
                logger.error("Wrong message type " + message.getClass());
            }
        } catch ( Exception cex ) {
            logger.error("Failed to complete validation.");
            throw new RuntimeException(cex);
        }
    }

    private void applyFileValidation( PrototypeValidatable parent, Entity entity, FileFinder fileFinder, EntityFinder entityFinder, AttributeFinder attributeFinder ) throws Exception {
        //public FileValidatable( Long sampleId, PrototypeValidatable prototypeValidatable, FileFinder fileFinder, File parentPath, int maxFileCount )
        FileValidatable fileValidatable = new FileValidatable( entity.getId(), parent, fileFinder, new File(""),  1 );
        String validationResult = fileValidatable.getValidityReason();
        if ( validationResult.equals( Validatable.VALIDITY_REASON_MISSING ) ) {
            logger.error( entity.getName() + "/" + entity.getId()+", "+ parent.getValidationTypeCategory() + ": " + Validatable.VALIDITY_REASON_MISSING );
            return;
        }

        Map<PrototypeValidatable.Relationship,PrototypeValidatable> validatables = parent.getChildren();
        if ( validatables != null ) {
            for ( PrototypeValidatable childValidatable: validatables.values() ) {
                switch (childValidatable.getValidationType()) {
                    case Attribute:
                        applyAttributeValidation( childValidatable, entity, fileFinder, entityFinder, attributeFinder );
                        break;
                    case File:
                        applyFileValidation( childValidatable, entity, fileFinder, entityFinder, attributeFinder );
                        break;
                    case Entity:
                        applySubEntityValidation(childValidatable, entity, fileFinder, entityFinder, attributeFinder);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown validation type.");
                }
            }
        }
    }

    private void applySubEntityValidation( PrototypeValidatable validatable, Entity entity, FileFinder fileFinder, EntityFinder entityFinder, AttributeFinder attributeFinder ) throws Exception {

    }

    private void applyAttributeValidation( PrototypeValidatable validatable, Entity entity, FileFinder fileFinder, EntityFinder entityFinder, AttributeFinder attributeFinder ) throws Exception {

    }

}
