package org.janelia.it.jacs.compute.process_result_validation.context_sensitive_content_checker.validatable;

import org.janelia.it.jacs.compute.process_result_validation.context_sensitive_content_checker.FinderException;
import org.janelia.it.jacs.compute.process_result_validation.context_sensitive_content_checker.PrototypeValidatable;
import org.janelia.it.jacs.compute.process_result_validation.context_sensitive_content_checker.Validatable;
import org.janelia.it.jacs.model.entity.Entity;

import java.util.List;

/**
 * Carries out validation of the entity described through parent and prototype.
 * Created by fosterl on 6/18/14.
 */
public class EntityValidatable implements Validatable {

    public static final int UNLIMITED_INSTANCES = Integer.MAX_VALUE;

    private PrototypeValidatable prototypeValidatable;
    private Entity parentEntity;
    private Long sampleId;
    private String id;
    private String name;
    private boolean valid;
    private String validityReason;

    /**
     * Given the parent entity, look for a child that matches the prototype validatable.  Fill that contract,
     * or indicate it could not be filled in the validity getters.
     *
     * @param prototypeValidatable the child found must conform to this.
     * @param parentEntity the child will be among this one's children.
     * @param sampleId this is for reporting.
     * @param maxSubEntities can be set to the UNLIMITED_INSTANCES if this is not needed. Typical value is 1.
     */
    public EntityValidatable( PrototypeValidatable prototypeValidatable, Entity parentEntity, Long sampleId, EntityFinder finder ) throws FinderException {
        this.prototypeValidatable = prototypeValidatable;
        this.parentEntity = parentEntity;
        this.sampleId = sampleId;

        validate( finder, prototypeValidatable.getMaxCount() );
    }

    @Override
    public PrototypeValidatable getProtypeValidatable() {
        return prototypeValidatable;
    }

    @Override
    public long getSampleId() {
        return sampleId;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public String getValidityReason() {
        return validityReason.trim();
    }

    private void validate( EntityFinder finder, int maxSubEntities ) throws FinderException {
        StringBuilder validationReasonBuilder = new StringBuilder();
        ValidatableHelper helper = new ValidatableHelper();
        // Want to walk the direct descendents of the parent entity, looking for entities.  Need to find
        // one (or more?) of the type given, by the prototype.
        List<Entity> targetEntities = finder.getChildrenOfType(parentEntity, prototypeValidatable.getValidationTypeCategory());
        if ( targetEntities == null  ||  targetEntities.size() == 0 ) {
            helper.addInstance(validationReasonBuilder, Validatable.VALIDITY_REASON_MISSING);
        }
        else if ( targetEntities.size() > maxSubEntities ) {
            helper.addInstance(validationReasonBuilder, "Too Many Sub-Entities of Type " + targetEntities.size());
        }
        else {
            helper.addInstance(validationReasonBuilder, Validatable.VALIDITY_REASON_OK);
            valid = true;
        }

        StringBuilder id = new StringBuilder();
        StringBuilder name = new StringBuilder();
        if ( targetEntities != null ) {
            for ( Entity subEntity: targetEntities ) {
                helper.addInstance( id, subEntity.getId().toString() );
                helper.addInstance( name, subEntity.getName() );
            }
        }
        this.id = id.toString();
        this.name = id.toString();

        validityReason = validationReasonBuilder.toString();
    }
}
