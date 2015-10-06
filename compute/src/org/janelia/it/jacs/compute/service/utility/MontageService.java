package org.janelia.it.jacs.compute.service.utility;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.exceptions.MissingGridResultException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Create a square montage from all the PNGs in a given directory.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MontageService extends SubmitDrmaaJobService {

    protected static final String MONTAGE_CMD = SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("ImageMagick.Bin.Path")+"/montage";

    protected static final String CONVERT_LIB_DIR = SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("ImageMagick.Lib.Path");

    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
    private static final String CONFIG_PREFIX = "montageConfiguration.";

    private Multimap<String,String> groups = ArrayListMultimap.<String,String>create();
    private FileNode inputFileNode;
    private int side;
    
    @Override
    protected String getGridServicePrefixName() {
        return "montage";
    }

    @Override
    protected void init(IProcessData processData) throws Exception {
        
        super.init(processData);
        
        this.inputFileNode = (FileNode)processData.getItem("INPUT_FILE_NODE");
        if (inputFileNode==null) {
            throw new ServiceException("Input parameter INPUT_FILE_NODE may not be null");
        }

        File[] images = FileUtil.getFilesWithSuffixes(new File(inputFileNode.getDirectoryPath()), "png");
        for(File imageFile : images) {

            Pattern p = Pattern.compile("^(.*)(_.*)\\.(\\w+)$");
            Matcher m = p.matcher(imageFile.getAbsolutePath());
            
            if (m.matches()) {
                String prefix = m.group(1);
                String type = m.group(2);
                String ext = m.group(3);
                groups.put(type, imageFile.getName());
            }
        }
        
        if (groups.isEmpty()) {
            cancel();
            return;
        }

        int max = 0;
        for(String group : groups.keySet()) {
            int size = groups.get(group).size();
            if (size>max) max = size;
        }
        
        this.side = (int)Math.ceil(Math.sqrt(max));
    }
    
    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

        int configIndex = 1;
    	for(String group : groups.keySet()) {
            writeInstanceFiles(group, groups.get(group), configIndex++);
    	}

    	createShellScript(writer, side, inputFileNode.getDirectoryPath());
        setJobIncrementStop(configIndex-1);
    }

    private void writeInstanceFiles(String type, Collection<String> filenames, int configIndex) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+configIndex);
        FileWriter fw = new FileWriter(configFile);
        try {
            fw.write(type + "\n");
        	for(String filename : filenames) {
                fw.write(filename + "  ");
        	}
            fw.write("\n");
        }
        catch (IOException e) {
        	throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath(),e); 
        }
        finally {
            fw.close();
        }
    }

    /**
     * Write the shell script that runs the stitcher on the merged files.
     * @param writer
     * @param
     * @param
     * @throws Exception
     */
    private void createShellScript(FileWriter writer, int side, String dir) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append("read TYPE\n");
        script.append("read INPUT_FILES\n");
        script.append("export LD_LIBRARY_PATH=").append(CONVERT_LIB_DIR).append("\n");
        script.append("cd ").append(dir).append("\n");
        script.append(MONTAGE_CMD).append(" -background '#000000' -geometry '300x300>' -tile ");
        script.append(side).append("x").append(side);
        script.append(" $INPUT_FILES montage$TYPE.png");
        script.append("\n");
        writer.write(script.toString());
    }

    @Override
    protected int getRequiredMemoryInGB() {
    	return 1;
    }

    @Override
    protected boolean isShortPipelineJob() {
    	return true;
    }

    @Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }

    @Override
	public void postProcess() throws MissingDataException {

    	FileNode parentNode = ProcessDataHelper.getResultFileNode(processData);
    	File file = new File(parentNode.getDirectoryPath());
    	
    	File[] coreFiles = FileUtil.getFilesWithPrefixes(file, "core");
    	if (coreFiles.length > 0) {
    		throw new MissingGridResultException(file.getAbsolutePath(), "Grouping core dumped for "+resultFileNode.getDirectoryPath());
    	}
    }
}
