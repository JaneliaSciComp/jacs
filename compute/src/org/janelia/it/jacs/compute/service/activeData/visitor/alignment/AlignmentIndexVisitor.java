package org.janelia.it.jacs.compute.service.activeData.visitor.alignment;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.service.activeData.scanner.AlignmentSampleScanner;
import org.janelia.it.jacs.compute.service.activeData.visitor.ActiveVisitor;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;

/**
 * Created by murphys on 1/7/15.
 */
public class AlignmentIndexVisitor extends ActiveVisitor {

    Logger logger = Logger.getLogger(AlignmentIndexVisitor.class);

    public final String VAA3D_PATH = SystemConfigurationProperties.getString("AlignmentResource.Vaa3dPath");
    public final String VAA3D_LIBRARY_PATH = SystemConfigurationProperties.getString("AlignmentResource.Vaa3dLibraryPath");
    public final String ALIGNMENT_RESOURCE_DIR = SystemConfigurationProperties.getString("AlignmentResource.Dir");

    @Override
    public Boolean call() throws Exception {
        AlignmentSampleScanner.SampleInfo sampleInfo=(AlignmentSampleScanner.SampleInfo)contextMap.get(AlignmentSampleScanner.SAMPLE_INFO);
        if (sampleInfo!=null) {
            Boolean validAlignmentSpace = (Boolean) contextMap.get(AlignmentIndexValidationVisitor.SAMPLE_INDEX_VALIDATION);
            if (validAlignmentSpace != null && validAlignmentSpace) {

//                String cmd = "ln -s "+mergedFile.getAbsolutePath()+" "+mergedFileLink.getAbsolutePath();
//                String[] args = cmd.split("\\s+");
//                StringBuffer stdout = new StringBuffer();
//                StringBuffer stderr = new StringBuffer();
//                SystemCall call = new SystemCall(stdout, stderr);
//                int exitCode = call.emulateCommandLine(args, null, null, 3600);
//                if (exitCode!=0) throw new Exception("Could not create symlink to merged file");


                File indexSpecificationFile = getIndexSpecificationFile();

                if (!indexSpecificationFile.exists()) {
                        logger.error("Could not find sample specification file="+indexSpecificationFile.getAbsolutePath());
                        return false;
                }

                File alignmentResourceDir=new File(AlignmentSampleScanner.ALIGNMENT_RESOURCE_DIR);
                String sampleInfoFilepath=sampleInfo.getResourcePath(alignmentResourceDir.getAbsolutePath());
                File sampleFile = new File(sampleInfoFilepath);
                File sampleDir=sampleFile.getParentFile();

                SystemCall sc = new SystemCall(null /*props*/, sampleDir, logger);


                String commandString = VAA3D_PATH + " -cmd volume-index -mode sample -indexSpecificationFile " +
                        indexSpecificationFile.getAbsolutePath() + " -sampleSpecificationFile " + sampleInfoFilepath +
                        " -sampleIndexFile " + sampleInfoFilepath+".index";

                if (VAA3D_LIBRARY_PATH!=null && VAA3D_LIBRARY_PATH.length()>0) {
                    commandString = "export LD_LIBRARY_PATH=" + VAA3D_LIBRARY_PATH + ":$LD_LIBRARY_PATH;" + commandString;
                }

                /*
                *   THE LONG-TERM PLAN IS TO ADD COMPARTMENT AND SCREEN AS ALTERNATIVE META-DATA FILE TYPES,
                *   WHICH IMPLIES ADDING THE SOURCING OF THESE DATA TYPES TO EARLIER-STAGE, AND PROBABLY
                *   CHANGING THE NAME TO "NEURON" FROM SAMPLE FOR TYPE, AND ADDING THESE TOP-LEVEL DIRS, ETC.
                *
                *  However, each type (mcfo sample, screen sample, compartment, etc.) will be handled by a
                *  different scanner sub-tree, headed by a different scanner per entity type.
                *
                * */


                logger.info("Executing command with system call: " + commandString);

                 int exitValue = sc.execute(commandString, true);

                if (exitValue!=0) {
                    logger.error("exit value non-zero for generation of sample index file");
                    return false;
                }

                sc.cleanup();

            }
        }
        return true;
    }

    File getIndexSpecificationFile() {
        File isf = new File(ALIGNMENT_RESOURCE_DIR, "neuron-index.spec");
        return isf;
    }


}

