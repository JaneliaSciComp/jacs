
package org.janelia.it.jacs.compute.service.metageno.orf;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.metageno.MetaGenoPerlConfig;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.user_data.metageno.MetaGenoOrfCallerResultNode;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 19, 2009
 * Time: 4:34:34 PM
 */
public class MetaGenoOrfMetageneMapService implements IService {

    /*
     *  camera_orf_metagene_mapping
     *
     *    --m_input_file <metagene to btab>
     *    --c_input_file <camera orf to btab>
     *    --output_file <.mapping_orf>
     */

    private static String metageneMapCmd = SystemConfigurationProperties.getString("MgPipeline.MetageneMapCmd");
    private static String scratchDirPath = SystemConfigurationProperties.getString("SystemCall.ScratchDir");

    /* SCRIPT DEPENDENCIES

       MetageneMapCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/camera_orf_metagene_mapping
           <none>

      MODULE SUMMARY
           <none>

    */

    MetaGenoOrfCallerResultNode resultNode;
    File resultNodeDir;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            resultNode = (MetaGenoOrfCallerResultNode) processData.getItem("META_GENO_ORF_CALLER_RESULT_NODE");
            logger.info(this.getClass().getName() + " execute() start");
            logger.info("Using result node directory=" + resultNode.getDirectoryPath());
            resultNodeDir = new File(resultNode.getDirectoryPath());

            File orfBtabFile = (File) processData.getItem("ORF_BTAB_FILE");
            if (orfBtabFile == null) {
                logger.error("Received null orfBtabFile");
            }
            else {
                logger.info("Received orfBtabFile=" + orfBtabFile.getAbsolutePath());
            }
            File metageneBtabFile = (File) processData.getItem("METAGENE_BTAB_FILE");
            if (metageneBtabFile == null) {
                logger.error("Received null metageneBtabFile");
            }
            else {
                logger.info("Received metageneBtabFile=" + metageneBtabFile.getAbsolutePath());
            }
            File mapFile = new File(orfBtabFile.getParentFile(), orfBtabFile.getName() + ".mapping_orf");

            File scratchDir = new File(scratchDirPath);
            logger.info("Using scratchDir=" + scratchDir.getAbsolutePath());
            SystemCall sc = new SystemCall(null, scratchDir, logger);

            String mapCmd = MetaGenoPerlConfig.getCmdPrefix() + metageneMapCmd +
                    " --m_input_file " + metageneBtabFile.getAbsolutePath() +
                    " --c_input_file " + orfBtabFile.getAbsolutePath() +
                    " --output_file " + mapFile.getAbsolutePath() +
                    "\n";
            int ev = sc.execute(mapCmd, false);
            if (ev != 0) {
                throw new Exception("SystemCall produced non-zero exit value=" + mapCmd);
            }

            sc.cleanup();

            logger.info(this.getClass().getName() + " execute() finish");
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

}
