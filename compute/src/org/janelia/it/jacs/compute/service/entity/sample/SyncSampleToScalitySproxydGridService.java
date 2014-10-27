package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import org.janelia.it.jacs.compute.access.scality.ScalityDAO;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.FileUtil;

/**
 * Moves the given Sample's files to the Scality object store via the Sproxyd REST API and updates the object model to add a Scality Id attribute to each file entity.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SyncSampleToScalitySproxydGridService extends SyncSampleToScalityFuseGridService {
    
    @Override
    protected void writeInstanceFile(Entity entity, int configIndex) throws Exception {
		String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
		if (filepath==null) {
			logger.warn("Entity should be moved to Scality but has no filepath: "+entity.getId());
			return;
		}
		String scalityUrl = ScalityDAO.getUrl(""+entity.getId());
		
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+configIndex);
        FileWriter fw = new FileWriter(configFile);
        try {
        	fw.write(filepath + "\n");
        	fw.write(scalityUrl + "\n");
        }
        catch (IOException e) {
        	throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath(),e); 
        }
        finally {
            fw.close();
        }
    	configIndex++;
    }

    @Override
    protected void createShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append("read FILE_PATH\n");
        script.append("read SCALITY_URL\n");
        script.append("curl -D - -X PUT \"$SCALITY_URL\" --data-binary @\"$FILE_PATH\"\n");
        writer.write(script.toString());
    }
    
    @Override
	public void postProcess() throws MissingDataException {


        File outputDir = new File(resultFileNode.getDirectoryPath(), "sge_output");
    	File[] outputFiles = FileUtil.getFilesWithPrefixes(outputDir, getGridServicePrefixName());
    	
    	if (outputFiles.length != entitiesToMove.size()) {
    		throw new MissingDataException("Number of entities to move ("+entitiesToMove.size()+") does not match number of output files ("+outputFiles.length+")");
    	}
    	
    	boolean[] hasError = new boolean[entitiesToMove.size()];

    	for(File outputFile : outputFiles) {
    		String name = outputFile.getName();
    		String ext = name.substring(name.lastIndexOf('.'));
    		int index = Integer.parseInt(ext);
    		try {
    			Scanner in = new Scanner(new FileReader(outputFile));
    			while (in.hasNext()) {
    				String line = in.nextLine();
    				if ("HTTP/1.1 100 Continue".equals(line)) {
    					continue;
    				}
    				if (line.startsWith("HTTP/1.1 ")) {
    					String responseCodeStr = line.substring(9).trim();
    					if (!"200".equals(responseCodeStr)) {
    						hasError[index] = true;
    						logger.error("Error uploading "+entitiesToMove.get(index).getId()+" to Scality. Details can be found at "+outputFile.getAbsolutePath());
    					}
    				}
    			}
    		}
    		catch (FileNotFoundException e) {
    			throw new MissingDataException("Missing file "+outputFile.getName(),e);
    		}
    	}
    	
    	int i=0;
    	for(Entity entity : entitiesToMove) {
    		if (!hasError[i++]) {
    			String scalityUrl = ScalityDAO.getUrl(""+entity.getId());
    			entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_SCALITY_URL, scalityUrl);
    		}	
    	}
	}
}
