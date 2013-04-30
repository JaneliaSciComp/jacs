package org.janelia.it.jacs.compute.access;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeException;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 4/30/13
 * Time: 12:57 PM
 * To change this template use File | Settings | File Templates.
 */

public class TiledMicroscopeDAO extends ComputeBaseDAO {

    public TiledMicroscopeDAO(Logger logger) {
        super(logger);
    }

    public void createTiledMicroscopeEntityTypes() throws Exception {
        _logger.debug("createTiledMicroscopeEntityTypes() - TiledMicroscopeDAO layer");



        // FROM EARLY ANNOTATIONDAO

//        public void setupEntityTypes() throws DaoException {
//            try {
//
//                //========== Status ============
//                createEntityStatus(EntityConstants.STATUS_DEPRECATED);
//
//                //========== Attribute ============
//                createEntityAttribute(EntityConstants.ATTRIBUTE_FILE_PATH);
//                createEntityAttribute(EntityConstants.ATTRIBUTE_ONTOLOGY_ELEMENT);
//                createEntityAttribute(EntityConstants.ATTRIBUTE_COMMON_ROOT);
//                createEntityAttribute(EntityConstants.ATTRIBUTE_ENTITY);
//
//                //========== Type ============
//                Set<String> lsmAttributeNameSet = new HashSet<String>();
//                lsmAttributeNameSet.add(EntityConstants.ATTRIBUTE_FILE_PATH);
//                createEntityType(EntityConstants.TYPE_LSM_STACK, lsmAttributeNameSet);
//
//                Set<String> ontologyElementAttributeNameSet = new HashSet<String>();
//                ontologyElementAttributeNameSet.add(EntityConstants.ATTRIBUTE_ONTOLOGY_ELEMENT);
//                createEntityType(EntityConstants.TYPE_ONTOLOGY_ELEMENT, ontologyElementAttributeNameSet);
//
//                Set<String> ontologyRootAttributeNameSet = new HashSet<String>();
//                ontologyRootAttributeNameSet.add(EntityConstants.ATTRIBUTE_ONTOLOGY_ELEMENT);
//                createEntityType(EntityConstants.TYPE_ONTOLOGY_ROOT, ontologyRootAttributeNameSet);
//
//                Set<String> folderAttributeNameSet = new HashSet<String>();
//                folderAttributeNameSet.add(EntityConstants.ATTRIBUTE_FILE_PATH);
//                folderAttributeNameSet.add(EntityConstants.ATTRIBUTE_COMMON_ROOT);
//                folderAttributeNameSet.add(EntityConstants.ATTRIBUTE_ENTITY);
//                createEntityType(EntityConstants.TYPE_FOLDER, folderAttributeNameSet);
//
//                Set<String> neuronSeparationAttributeNameSet = new HashSet<String>();
//                neuronSeparationAttributeNameSet.add(EntityConstants.ATTRIBUTE_FILE_PATH);
//                neuronSeparationAttributeNameSet.add(EntityConstants.ATTRIBUTE_INPUT);
//                neuronSeparationAttributeNameSet.add(EntityConstants.ATTRIBUTE_ENTITY);
//                createEntityType(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT, neuronSeparationAttributeNameSet);
//
//                Set<String> tif2DImageAttributeSet = new HashSet<String>();
//                tif2DImageAttributeSet.add(EntityConstants.ATTRIBUTE_FILE_PATH);
//                createEntityType(EntityConstants.TYPE_TIF_2D, tif2DImageAttributeSet);
//
//                Set<String> tif3DImageAttributeSet = new HashSet<String>();
//                tif3DImageAttributeSet.add(EntityConstants.ATTRIBUTE_FILE_PATH);
//                createEntityType(EntityConstants.TYPE_TIF_3D, tif3DImageAttributeSet);
//
//                Set<String> tif3DLabelMaskAttributeSet = new HashSet<String>();
//                tif3DLabelMaskAttributeSet.add(EntityConstants.ATTRIBUTE_FILE_PATH);
//                createEntityType(EntityConstants.TYPE_TIF_3D_LABEL_MASK, tif3DLabelMaskAttributeSet);
//
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//                throw new DaoException(e);
//            }
//        }


    }

}
