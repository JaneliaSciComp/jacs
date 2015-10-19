
package org.janelia.it.jacs.compute.service.recruitment;

import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.shared.processors.recruitment.RecruitmentDataHelper;
import org.janelia.it.jacs.shared.tasks.GenbankFileInfo;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.genbank.GenbankFile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 10, 2009
 * Time: 11:38:40 AM
 */
public class BuildFrvSamplingFastaService implements IService {

    public void execute(IProcessData processData) throws ServiceException {
        // Get the values from the task
        FileWriter writer = null;
        try {
            // Prep for execution - Wipe out the old data
            FastaFileNode fastaNode = (FastaFileNode) EJBFactory.getLocalComputeBean().
                    getNodeById(SystemConfigurationProperties.getLong("Recruitment.GenomeProjectFastaFileNode"));
            FileUtil.cleanDirectory(fastaNode.getDirectoryPath());
            writer = new FileWriter(new File(fastaNode.getDirectoryPath() + File.separator + "nucleotide.fasta"));
            List<GenbankFileInfo> genbankFiles = RecruitmentDataHelper.getGenbankFileList();
            System.out.println("There are " + genbankFiles.size() + " genbank files.");
            int count = 0;
            int failedParses = 0;
            for (GenbankFileInfo genbankFile : genbankFiles) {
                try {
                    GenbankFile tmpFile = new GenbankFile(genbankFile.getGenbankFile().getAbsolutePath());
                    String tmpSequence = tmpFile.getFastaFormattedSequence().toUpperCase();
                    if (null != tmpSequence && !"".equals(tmpSequence)) {
                        writer.append(">").append(genbankFile.getGenbankFile().getName()).append("\n");
                        // Formatted sequence does not need a newline after
                        writer.append(tmpSequence);
                        count++;
                    }
                }
                catch (Exception e) {
                    System.out.println("Error adding sequence info for genbank project: " + genbankFile.getGenomeProjectNodeId() + ", file: " + genbankFile.getGenbankFile().getName());
                    System.out.println("Continuing...");
                    failedParses++;
                    e.printStackTrace();
                }
            }
            System.out.println("Sequence from " + count + " files were added. Failed GBK parses: " + failedParses);

        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (null != writer) {
                try {
                    writer.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}