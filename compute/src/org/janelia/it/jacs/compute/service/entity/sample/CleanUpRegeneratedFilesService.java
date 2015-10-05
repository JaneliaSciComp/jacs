package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;

import org.janelia.it.jacs.compute.service.align.AlignmentInputFile;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;

/**
 * Cleans up the temporary regenerated files. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CleanUpRegeneratedFilesService extends AbstractEntityService {

    public void execute() throws Exception {

        AlignmentInputFile alignmentInputFile = (AlignmentInputFile)data.getRequiredItem("ALIGNMENT_INPUT");
        String filepath = alignmentInputFile.getFilepath();
        File file = new File(filepath);
        File sampleDir = file.getParentFile().getParentFile().getAbsoluteFile();
        contextLogger.info("Deleting regenerated temporary files at: "+sampleDir.getAbsolutePath());
        //FileUtils.deleteDirectory(sampleDir);
    }
}
