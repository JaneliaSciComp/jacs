package org.janelia.it.jacs.compute.process_result_validation.context_sensitive_content_checker.engine;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.process_result_validation.context_sensitive_content_checker.*;
import org.janelia.it.jacs.compute.process_result_validation.context_sensitive_content_checker.finder.ConcreteAttributeFinder;
import org.janelia.it.jacs.compute.process_result_validation.context_sensitive_content_checker.finder.ConcreteEntityFinder;
import org.janelia.it.jacs.compute.process_result_validation.context_sensitive_content_checker.finder.FilesystemFileFinder;
import org.janelia.it.jacs.compute.process_result_validation.context_sensitive_content_checker.validatable.*;
import org.janelia.it.jacs.model.entity.Entity;

import java.io.File;
import java.util.Map;

/**
 * This class can validate entities, given their types and samples. It takes sub-trees that are necessary to
 * work out complex relationships, prior to a given validation being attempted.  For instance: there are many
 * attributes with the attribute name of "File Path", but their contents can only be validated through applying
 * knowledge of the name of the entity having the "File Path" attribute.
 *
 * Ex:
 *    An entity of type "Sample" will have a child entity called "Default 3D Image".
 *    This Default 3D Image will then have a "File Path" attribute whose value actually points to filesystem.
 *
 * To validate this scenario means knowning you are dealing with a sample, tracking down to its Default 3D Image,
 * and then knowing that the file will be under a File Path attribute.
 *
 * Were we simply to say "every File Path must have a valid file associated with it", we would then not be able
 * to verify the file type, or apply any specifics to the file size, etc.
 *
 * Created by fosterl on 6/26/14.
 */
public class ValidationEngine {

    private static Logger logger = Logger.getLogger(ValidationEngine.class);
    private ValidatableCollectionFactory validatableCollectionFactory;
    private FileFinder fileFinder;
    private EntityFinder entityFinder;
    private AttributeFinder attributeFinder;

    public ValidationEngine() {
        this( new SimpleVHF() );
    }

    public ValidationEngine( ValidatableCollectionFactory validatableCollectionFactory ) {
        this.validatableCollectionFactory = validatableCollectionFactory;
        this.fileFinder = new FilesystemFileFinder();
        this.entityFinder = new ConcreteEntityFinder();
        this.attributeFinder = new ConcreteAttributeFinder();
    }

    public void validateByEntityType( Entity entity, Long sampleId ) throws Exception {
        Map<String,PrototypeValidatable> map = validatableCollectionFactory.getValidatables();
        String entityTypeName = entity.getEntityTypeName();
        PrototypeValidatable validatable = map.get( entityTypeName );
        if ( validatable != null ) {
            applySubEntityValidation( validatable, entity, sampleId );
        }
    }

    /*
    Problems:
    1- may fail because the value of the attribute was not pre-cached in the entity.
    2- FileValidatable may not be correct as yet.
     */
    private void applyFileValidation( PrototypeValidatable parent, Entity entity, Long sampleId, PrototypeValidatable.Relationship relationship ) throws Exception {
        String relationNameAttribute = relationship.getName();
        String fileLoc = entity.getValueByAttributeName( relationNameAttribute );
        FileValidatable fileValidatable = createFileValidatable(sampleId, parent, fileLoc);
        standardDescent(parent, entity, sampleId, fileValidatable);
    }

    private void applySubEntityValidation( PrototypeValidatable validatable, Entity entity, Long sampleId ) throws Exception {
        EntityValidatable entityValidatable = new EntityValidatable( validatable, entity, sampleId, entityFinder );
        standardDescent(validatable, entity, sampleId, entityValidatable);
    }

    private void applyAttributeValidation( PrototypeValidatable validatable, Entity entity, Long sampleId ) throws Exception {
        AttributeValidatable attributeValidatable = new AttributeValidatable( validatable, entity, attributeFinder, sampleId );
        standardDescent( validatable, entity, sampleId, attributeValidatable );
    }

    private void standardDescent(PrototypeValidatable validatable, Entity entity, Long sampleId, Validatable entityValidatable) throws Exception {
        String validationResult = entityValidatable.getValidityReason();
        if ( validationResult.equals( Validatable.VALIDITY_REASON_MISSING ) ) {
            logger.error( entity.getName() + "/" + entity.getId()+", "+ validatable.getValidationTypeCategory() + ": " + Validatable.VALIDITY_REASON_MISSING );
            return;
        }
        else if ( ! validationResult.equals( Validatable.VALIDITY_REASON_MISSING ) ) {
            logger.error( entity.getName() + "/" + entity.getId() + ","  + validatable.getValidationTypeCategory() + ": " + validationResult );
        }

        validateChildren( validatable, entity, sampleId );
    }

    private void validateChildren(PrototypeValidatable parent, Entity entity, Long sampleId) throws Exception {
        Map<PrototypeValidatable.Relationship,PrototypeValidatable> validatables = parent.getChildren();
        if ( validatables != null ) {
            for (PrototypeValidatable.Relationship relationship: validatables.keySet() ) {
                PrototypeValidatable childValidatable = validatables.get( relationship );
                switch (childValidatable.getValidationType()) {
                    case Attribute:
                        applyAttributeValidation( childValidatable, entity, sampleId );
                        break;
                    case File:
                        applyFileValidation( childValidatable, entity, sampleId, relationship );
                        break;
                    case Entity:
                        applySubEntityValidation(childValidatable, entity, sampleId );
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown validation type.");
                }
            }
        }
    }

    /**
     * Applies the rules for file validation.  Whichever kind of file validatable is relevant to the file
     * type should be used.   Factory Method
     *
     * @param sampleId this owns the file.
     * @param proto provides guidelines for validating stuff.
     * @param fileLoc where is the filesystem file.
     * @return something that can be used to validate this file.
     * @throws FinderException thrown by called methods.
     */
    private FileValidatable createFileValidatable(Long sampleId, PrototypeValidatable proto, String fileLoc) throws FinderException {
        return new FileValidatable( sampleId, proto, fileFinder, new File(fileLoc),  1 );

    }

}
