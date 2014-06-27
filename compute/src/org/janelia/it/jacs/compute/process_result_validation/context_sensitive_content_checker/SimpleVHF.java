package org.janelia.it.jacs.compute.process_result_validation.context_sensitive_content_checker;

import org.janelia.it.jacs.model.entity.EntityConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * This factory implementation uses simple, hardcoded creation.
 * Created by fosterl on 6/24/14.
 */
public class SimpleVHF implements ValidatableCollectionFactory {
    private int id;
    private Map<String,PrototypeValidatable> prototypeValidatables;

    @Override
    public Map<String,PrototypeValidatable> getValidatables() {
        if ( prototypeValidatables == null ) {
            createCollection();
        }
        return prototypeValidatables;
    }

    private void createCollection() {
        prototypeValidatables = new HashMap<>();
        PrototypeValidatable sampleValidatable = new PrototypeValidatable();
        sampleValidatable.setValidationType(ValidationType.Entity);
        sampleValidatable.setValidationTypeCategory(EntityConstants.TYPE_SAMPLE);

        id = 1;

        PrototypeValidatable neuronCollectionValidation = establishNeuronCollectionValidation();

        // Major sub tree: pipeline run....
        PrototypeValidatable pipelineRun =
                new Builder()
                        .valType(ValidationType.Entity)
                        .category(EntityConstants.TYPE_PIPELINE_RUN)
                .build();
        pipelineRun.addChild( new PrototypeValidatable.Relationship(EntityConstants.ATTRIBUTE_MASK_ENTITY_COLLECTION, id++), neuronCollectionValidation.shallowClone() );

        //...runs have files under them.
        PrototypeValidatable v3dpbd =
                new Builder()
                        .valType(ValidationType.File)
                        .category("v3dpbd")
                        .maxCount(1)
                .build();
        PrototypeValidatable png =
                new Builder()
                        .valType(ValidationType.File)
                        .category("png")
                        .maxCount(1)
                .build();

        pipelineRun.addChild( new PrototypeValidatable.Relationship( EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE, id++), v3dpbd );
        pipelineRun.addChild( new PrototypeValidatable.Relationship( EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, id++), png );
        pipelineRun.addChild( new PrototypeValidatable.Relationship( EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE, id++), png );
        pipelineRun.addChild( new PrototypeValidatable.Relationship( EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE, id++), png );

        prototypeValidatables.put( pipelineRun.getValidationTypeCategory(), pipelineRun );

    }

//        // Pipeline's sample proc....
//        PrototypeValidatable sampleProcResult =
//                new Builder()
//                        .valType(ValidationType.Entity)
//                        .category(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)
//                .build();
//        pipelineRun.addChild(PrototypeValidatable.Relationship.createNonNamedRelationship(id++), sampleProcResult);
//
//        // Sample proc's neuron sep...
//        PrototypeValidatable spNeuronSep =
//                new Builder()
//                        .valType(ValidationType.Entity)
//                        .category(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)
//                .build();
//        sampleProcResult.addChild(PrototypeValidatable.Relationship.createNonNamedRelationship(id++), spNeuronSep);
//
//
//        // Neuron sep's supporting data...
//        PrototypeValidatable snNsSupportingData =
//                new Builder()
//                        .valType(ValidationType.Entity)
//                        .category(EntityConstants.TYPE_SUPPORTING_DATA)
//                .build();
//        spNeuronSep.addChild(PrototypeValidatable.Relationship.createNonNamedRelationship(id++), snNsSupportingData);
//
//
//
//        // Pipeline run's alignment result.
//        PrototypeValidatable alignmentResult =
//                new Builder()
//                        .valType(ValidationType.Entity)
//                        .category(EntityConstants.TYPE_ALIGNMENT_RESULT)
//                .build();
//        pipelineRun.addChild(PrototypeValidatable.Relationship.createNonNamedRelationship(id++), alignmentResult);

//        // Major sub tree: supporting data....
//        PrototypeValidatable supportingData =
//                new Builder()
//                        .valType(ValidationType.Entity)
//                        .maxCount(1)
//                        .category(EntityConstants.TYPE_SUPPORTING_DATA)
//                .build();
//        prototypeValidatables.put( supportingData.getValidationTypeCategory(), supportingData );

//Q: Will all Image File entities have the same stuff under them?
    // For the default 3d image, signal mip, default 2d image and ref mip, there will also be entities bearing
    // more files.
    //               LATER.
//        PrototypeValidatable default3dImage =
//                new Builder()
//                        .valType(ValidationType.Entity)
//                        .category(EntityConstants.TYPE_IMAGE_3D)
//                        .maxCount(1)
//                        .build();




    private PrototypeValidatable establishNeuronCollectionValidation() {
        // Neuron sep's Frag Collec(tion)
        PrototypeValidatable snNsFragCollec =
                new Builder()
                        .valType(ValidationType.Entity)
                        .category(EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION)
                .build();

        // ...frags IN the Neuron Sep's NF collec.
        PrototypeValidatable snNsFragInCollec =
                new Builder()
                        .valType(ValidationType.Entity)
                        .category(EntityConstants.TYPE_NEURON_FRAGMENT)
                .build();


        // Each fragment will have certain files in it.
        // ...and each of those file entities will be associated with a file path.
        PrototypeValidatable filePathPV = null;
        PrototypeValidatable snNsFragMask =
                new Builder()
                        .valType(ValidationType.Entity)
                        .category(EntityConstants.TYPE_IMAGE_3D)
                        .maxCount(1)
                .build();
        snNsFragInCollec.addChild(new PrototypeValidatable.Relationship(EntityConstants.ATTRIBUTE_MASK_IMAGE, id++), snNsFragMask);
        filePathPV = new Builder()
                .valType(ValidationType.File)
                .category("mask")
                .build();
        snNsFragMask.addChild(new PrototypeValidatable.Relationship(EntityConstants.ATTRIBUTE_FILE_PATH, id++), filePathPV);


        PrototypeValidatable snNsFragChan =
                new Builder()
                        .valType(ValidationType.Entity)
                        .category(EntityConstants.TYPE_IMAGE_3D)
                .build();
        snNsFragInCollec.addChild(new PrototypeValidatable.Relationship(EntityConstants.ATTRIBUTE_CHAN_IMAGE, id++), snNsFragChan);
        filePathPV = new Builder()
                        .valType(ValidationType.File)
                        .category("chan")
                        .maxCount(1)
                .build();
        snNsFragChan.addChild(new PrototypeValidatable.Relationship(EntityConstants.ATTRIBUTE_FILE_PATH, id++), filePathPV);


        PrototypeValidatable snsFrag2D =
                new Builder()
                        .valType(ValidationType.Entity)
                        .category(EntityConstants.TYPE_IMAGE_2D)
                .build();
        snNsFragInCollec.addChild(new PrototypeValidatable.Relationship(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, id++), snsFrag2D);
        filePathPV = new Builder()
                        .valType(ValidationType.File)
                        .category("png")
                .build();
        snsFrag2D.addChild(new PrototypeValidatable.Relationship(EntityConstants.ATTRIBUTE_FILE_PATH, id++), filePathPV);

        // Ensure these types are registered for validation.
        prototypeValidatables.put( EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION, snNsFragCollec );
        prototypeValidatables.put( EntityConstants.TYPE_NEURON_FRAGMENT, snNsFragInCollec );
        return snNsFragCollec;
    }

    class Builder {
        private PrototypeValidatable val;
        public Builder() {
            val = new PrototypeValidatable();
        }
        public Builder category(String validationTypeCategory) {
            val.setValidationTypeCategory( validationTypeCategory );
            return this;
        }
        public Builder valType( ValidationType type ) {
            val.setValidationType(type);
            return this;
        }
        public Builder maxCount( int count ) {
            val.setMaxCount( count );
            return this;
        }
        public PrototypeValidatable build() { return val; }
    }
}
