package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;
import java.util.List;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.compute.service.vaa3d.MergedLsmPair;
import org.janelia.it.jacs.compute.util.FileUtils;

/**
 * Cleans up the merged tiles to save on disk space.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CleanUpMergedTilesService extends AbstractEntityService {

    public void execute() throws Exception {
        List<AnatomicalArea> sampleAreas = (List<AnatomicalArea>) processData.getItem("SAMPLE_AREAS");
        for(AnatomicalArea sampleArea : sampleAreas) {
            cleanup(sampleArea.getMergedLsmPairs(), sampleArea.getStitchedFilename());
        }
    }
    
    public void cleanup(List<MergedLsmPair> mergedLsmPairs, String stitchedFile) throws Exception {

        for(MergedLsmPair mergedLsmPair : mergedLsmPairs) {

            File file = new File(mergedLsmPair.getMergedFilepath());
            if (file.getAbsolutePath().equals(stitchedFile)) continue; // never delete the "stitched" file
            
            File symlink = new File(mergedLsmPair.getMergedFilepath().replace("merge", "group"));
            if (symlink.exists()) {
                contextLogger.info("Cleaning up symlink to merged tile: "+symlink.getAbsolutePath());
                FileUtils.forceDelete(symlink);
            }
            
            contextLogger.info("Cleaning up merged tile: "+file.getAbsolutePath());
            FileUtils.forceDelete(file);
        }
    }
}
