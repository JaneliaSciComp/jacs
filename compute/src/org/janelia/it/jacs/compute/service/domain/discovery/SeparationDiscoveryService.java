package org.janelia.it.jacs.compute.service.domain.discovery;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.compute.service.domain.util.FileDiscoveryHelperNG;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.compute.service.neuronSeparator.NeuronSeparationPipelineGridService;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.user_data.FileNode;

import com.google.common.collect.Ordering;

/**
 * A neuron separation results discovery service which can be re-run multiple times on the same separation and
 * discover additional files each time.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SeparationDiscoveryService extends AbstractDomainService {
    
    private static final String NEURON_MIP_PREFIX = NeuronSeparationPipelineGridService.NAME+".PR.neuron";
    
    private SampleHelperNG sampleHelper;
    private Sample sample;
    private ObjectiveSample objectiveSample;

    private Map<Integer,NeuronFiles> neuronMap = new HashMap<>();
    
    public void execute() throws Exception {

        this.sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger, contextLogger);
        this.sample = sampleHelper.getRequiredSample(data);
        this.objectiveSample = sampleHelper.getRequiredObjectiveSample(sample, data);

        List<PipelineResult> results = null;
        String rootPath = null;
    
        String resultName = data.getRequiredItemAsString("RESULT_ENTITY_NAME");
        FileNode resultFileNode = (FileNode)data.getRequiredItem("ROOT_FILE_NODE");
        rootPath = resultFileNode.getDirectoryPath();
        Long rootId = data.getRequiredItemAsLong("ROOT_ENTITY_ID");
        results = objectiveSample.getResultsById(null, rootId);
        NeuronSeparation separation = null;
        for(PipelineResult result : results) {
            if (separation == null) {
                separation = sampleHelper.addNewNeuronSeparation(result, resultName);   
                separation.setFilepath(rootPath);
            }
            else {
                result.addResult(separation);
            }
            logger.info("Created new separation result: "+separation.getId());
        }
        
        FileDiscoveryHelperNG helper = new FileDiscoveryHelperNG(computeBean, ownerKey, logger);
        List<String> filepaths = helper.getFilepaths(rootPath);
        contextLogger.info("Collected "+filepaths.size()+" files in "+rootPath);

        for(String filepath : filepaths) {
            File file = new File(filepath);
            String filename = file.getName();
            if (filename.startsWith(NEURON_MIP_PREFIX) && filename.endsWith(".png")) {
                NeuronFiles files = getFiles(file);
                if (files!=null) {
                    files.mip = filepath;
                }
            }
            else if (filepath.matches(".*?maskChan/neuron_(\\d++).mask")) {
                NeuronFiles files = getFiles(file);
                if (files!=null) {
                    files.mask = filepath;
                }
            }
            else if (filepath.matches(".*?maskChan/neuron_(\\d++).chan")) {
                NeuronFiles files = getFiles(file);
                if (files!=null) {
                    files.chan = filepath;
                }
            }
            else if ("SeparationResultUnmapped.nsp".equals(filename) || "SeparationResult.nsp".equals(filename)) {
                DomainUtils.setFilepath(separation, FileType.NeuronSeparatorResult, filepath);
            }
            else if ("ConsolidatedSignal2_25.mp4".equals(filename)) {
                for(PipelineResult result : results) {
                    contextLogger.info("Setting fast 3d image on the separation's parent result: "+result.getId());
                    DomainUtils.setFilepath(result, FileType.FastStack, filepath);
                }
            }
        }
        
        List<NeuronFragment> neuronFragments = new ArrayList<>();
        for(Integer index : Ordering.natural().sortedCopy(neuronMap.keySet())) {
            NeuronFiles files = neuronMap.get(index);
            logger.debug("Processing neuron #"+index+" with MIP "+files.mip);
            NeuronFragment neuron = sampleHelper.addNewNeuronFragment(separation, index);
            DomainUtils.setFilepath(neuron, FileType.SignalMip, files.mip);
            DomainUtils.setFilepath(neuron, FileType.MaskFile, files.mask);
            DomainUtils.setFilepath(neuron, FileType.ChanFile, files.chan);
            sampleHelper.saveNeuron(neuron);
            neuronFragments.add(neuron);
        }
        
        separation.getFragmentsReference().setCount(new Long(neuronFragments.size()));
        
        logger.info("Saving sample "+sample.getId()+" with neuron separation "+separation.getId());
        sampleHelper.saveSample(sample);
        
        contextLogger.info("Putting "+separation.getId()+" in RESULT_ENTITY_ID");
        data.putItem("RESULT_ENTITY_ID", separation.getId());
    }
    
    private NeuronFiles getFiles(File file) {
        int index = getNeuronIndex(file.getAbsolutePath());
        NeuronFiles files = neuronMap.get(index);
        if (files==null) {
            files = new NeuronFiles();
            neuronMap.put(index, files);    
        }
        return files;
    }
    
    private class NeuronFiles {
        String mip;
        String mask;
        String chan;
    }

    private Integer getNeuronIndex(String filename) {
        Pattern p = Pattern.compile(".*?_(\\d+)\\.(\\w+)");
        Matcher m = p.matcher(filename);
        if (m.matches()) {
            String mipNum = m.group(1);
            try {
                return Integer.parseInt(mipNum);
            }
            catch (NumberFormatException e) {
                logger.warn("Error parsing neuron index from filename: "+mipNum);
            }
        }
        return null;
    }
}
