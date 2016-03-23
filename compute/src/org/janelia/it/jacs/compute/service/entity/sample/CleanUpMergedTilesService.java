package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;
import java.util.List;

import org.janelia.it.jacs.compute.service.domain.model.AnatomicalArea;
import org.janelia.it.jacs.compute.service.entity.AbstractDomainService;
import org.janelia.it.jacs.compute.service.vaa3d.MergedLsmPair;
import org.janelia.it.jacs.compute.util.FileUtils;

/**
 * Cleans up the merged tiles to save on disk space.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CleanUpMergedTilesService extends AbstractDomainService {

    public void execute() throws Exception {
        List<AnatomicalArea> sampleAreas = (List<AnatomicalArea>) processData.getItem("SAMPLE_AREAS");
        if (sampleAreas != null) {
            for(AnatomicalArea sampleArea : sampleAreas) {
                cleanup(sampleArea.getMergedLsmPairs(), sampleArea.getStitchedFilepath());
            }
        }
    }
    
    public void cleanup(List<MergedLsmPair> mergedLsmPairs, String stitchedFile) throws Exception {

        if (mergedLsmPairs==null) return;
        
        for(MergedLsmPair mergedLsmPair : mergedLsmPairs) {

            if (mergedLsmPair==null) continue;
            
            File file = new File(mergedLsmPair.getMergedFilepath());
            if (file.getAbsolutePath().equals(stitchedFile)) continue; // never delete the "stitched" file
            
            File symlink = new File(mergedLsmPair.getMergedFilepath().replace("merge", "group"));
            if (symlink.exists()) {
                contextLogger.info("Cleaning up symlink to merged tile: "+symlink.getAbsolutePath());
                FileUtils.forceDelete(symlink);
            }
            
            contextLogger.info("Cleaning up merged tile: "+file.getAbsolutePath());
            FileUtils.forceDelete(file);
            
            // TODO: do we need to clean up the merged files from the database like we did back in the entity days?
//            for(Entity imageEntity : entityBean.getEntitiesWithAttributeValue(ownerKey, EntityConstants.ATTRIBUTE_FILE_PATH, file.getAbsolutePath())) {
//                entityBean.deleteEntityTreeById(ownerKey, imageEntity.getId());
//            }
        }
    }
}
