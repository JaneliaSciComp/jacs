package org.janelia.it.jacs.compute.service.vaa3d;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.domain.DomainDAL;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.sample.CuratedNeuron;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.neuron.NeuronMergeTask;
import org.janelia.it.jacs.model.user_data.neuron.NeuronMergeResultNode;

/**
 * File discovery service for neuron merge results.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Vaa3DNeuronMergeResultsDiscoveryService implements IService{

	protected Sample sample;
	protected DomainDAL domainDAL;
    protected Logger logger;
    protected String ownerKey;
    protected Date createDate;
    protected IProcessData processData;
    protected Task task;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, Vaa3DNeuronMergeResultsDiscoveryService.class);
            domainDAL = DomainDAL.getInstance();
            String ownerName = ProcessDataHelper.getTask(processData).getOwner();
            Subject subject = domainDAL.getSubjectByNameOrKey(ownerName);
            this.ownerKey = subject.getKey();
            createDate = new Date();
            task = ProcessDataHelper.getTask(processData);
            NeuronSeparation separation = domainDAL.getNeuronSeparation(null, Long.valueOf(task.getParameter(NeuronMergeTask.PARAM_separationEntityId)));
            // if the collection doesn't exist add it
            CuratedNeuron curatedNeuron = createCuratedNeuronCollection(separation);

            // set neuron fragments and weights
            List<Reference> fragmentList = new ArrayList<>();
            String commaSeparatedFragmentIdList=task.getParameter(NeuronMergeTask.PARAM_commaSeparatedNeuronFragmentList);
            int voxelWeight = 0;
            for (String tmpFragmentOid : Task.listOfStringsFromCsvString(commaSeparatedFragmentIdList)) {
                NeuronFragment fragment = (NeuronFragment) domainDAL.getDomainObject(null, Reference.createFor(NeuronFragment.class.getSimpleName(), Long.parseLong(tmpFragmentOid)));
                fragmentList.add(Reference.createFor(fragment));
                if (fragment.getVoxelWeight()!=null) {
                    voxelWeight += fragment.getVoxelWeight();
                }
            }
            curatedNeuron.setComponentFragments(fragmentList);
            curatedNeuron.setVoxelWeight(voxelWeight);

            // set image locations
            NeuronMergeResultNode tmpNode = (NeuronMergeResultNode)ProcessDataHelper.getResultFileNode(processData);
            curatedNeuron.setFilepath(tmpNode.getDirectoryPath());
            Map<FileType, String> images = new HashMap<>();
            images.put(FileType.SignalMip, tmpNode.getFilePathByTag(NeuronMergeResultNode.TAG_MIP));
            images.put(FileType.LosslessStack, tmpNode.getFilePathByTag(NeuronMergeResultNode.TAG_STACK));

            domainDAL.save(ownerKey, curatedNeuron);
        }
        catch (Exception e) {
            throw new ServiceException("Unable to process the entities from the neuron merge step.",e);
        }
    }

    protected CuratedNeuron createCuratedNeuronCollection(NeuronSeparation separation) throws Exception {
        if (null==separation) {
            throw new ServiceException("Cannot add Curated Neurons to a null Separation Result");
        }
        CuratedNeuron curatedNeuron = new CuratedNeuron();
        Sample sample = separation.getParentRun().getParent().getParent();
        curatedNeuron.setSample(Reference.createFor(Sample.class.getSimpleName(), sample.getId()));

        // find the previous curated neurons to get number
        List<NeuronFragment> existingFragments = domainDAL.getNeuronFragmentsBySeparationId(null, separation.getId());
        int numCurated = 0;
        for (NeuronFragment fragment: existingFragments) {
            if (fragment instanceof CuratedNeuron) {
                numCurated++;
            }
        }
        curatedNeuron.setName("Curated Neuron " + numCurated);
        curatedNeuron = domainDAL.save(sample.getOwnerKey(), curatedNeuron);
        logger.info("Saved curated neuron as " + curatedNeuron.getId());
        return curatedNeuron;
    }
}
