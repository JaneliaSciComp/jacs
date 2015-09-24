package org.janelia.it.jacs.compute.service.mip;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.align.ImageStack;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.neuronSeparator.NeuronSeparatorHelper;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.compute.util.ChanSpecUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generate map tiles at different magnification levels from an original set.
 *
 * @author cgoina
 */
public class MIPMapTilesService extends SubmitDrmaaJobService {

    private final Map<String, Object> scriptParams = new LinkedHashMap<>();
    
    @Override
    protected String getGridServicePrefixName() {
        return "mipmaptiles";
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected void init(IProcessData processData) throws Exception {
        super.init(processData);
        for (Map.Entry<String, Object> paramEntry : processData.entrySet()) {
            if (paramEntry.getValue() != null) {
                scriptParams.put(paramEntry.getKey(), paramEntry.getValue());
            }
        }
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        writeShellScript(writer);
        // TODO
    }

    @Override
    protected int getRequiredMemoryInGB() {
        return 6;
    }

    /**
     * Write the shell script used for all instances in the job array.
     */
    private void writeShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        for (String paramName : scriptParams.keySet()) {
            script.append("read ").append(paramName).append('\n');
        }
        script.append(MIPMapTilesHelper.getMipMapTilesCommands()).append('\n');
        writer.write(script.toString());
    }

}
