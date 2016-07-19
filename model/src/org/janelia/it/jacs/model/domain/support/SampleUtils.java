package org.janelia.it.jacs.model.domain.support;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.janelia.it.jacs.model.domain.enums.AlignmentScoreType;
import org.janelia.it.jacs.model.domain.interfaces.HasFileGroups;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for dealing with Samples, Neuron Fragments, and other related objects.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleUtils {

    private static final Logger log = LoggerFactory.getLogger(SampleUtils.class);

    public static String getLabel(PipelineResult result) {
        return result.getParentRun().getParent().getObjective() + " " + result.getName();
    }

    public static boolean equals(PipelineResult o1, PipelineResult o2) {
        if (o1==null || o2==null) return false;
        if (o1.getId()==null || o2.getId()==null) return false;
        return o1.getId().equals(o2.getId());
    }

    public static HasFiles getResult(Sample sample, ResultDescriptor result) {

        log.debug("Getting result '{}' from {}",result,sample.getName());
        log.trace("  Objective: {}",result.getObjective());
        log.trace("  Result name: {}",result.getResultName());
        log.trace("  Group name: {}",result.getGroupName());
        log.trace("  Is aligned: {}",result.isAligned());

        HasFiles chosenResult = null;
        if (result.getObjective()==null) {
            List<ObjectiveSample> objectiveSamples = sample.getObjectiveSamples();
            for(int i=objectiveSamples.size()-1; chosenResult==null && i>=0; i--) {
                ObjectiveSample objectiveSample = objectiveSamples.get(i);
                log.debug("Testing objective: "+objectiveSample.getObjective());
                chosenResult = getResult(objectiveSample, result);
                if (chosenResult!=null) break;
            }
        }
        else {
            ObjectiveSample objectiveSample = sample.getObjectiveSample(result.getObjective());
            if (objectiveSample!=null) {
                log.debug("Testing objective: "+objectiveSample.getObjective());
                chosenResult = getResult(objectiveSample, result);
            }
        }
        
        if (chosenResult!=null && chosenResult.getFiles().isEmpty() && chosenResult instanceof HasFileGroups) {
            // The chosen result doesn't have files itself, but it does have file groups
            HasFileGroups hasGroups = (HasFileGroups)chosenResult;
            // We pick the first group, since there is no way to tell which is latest
            for(String groupKey : new TreeSet<>(hasGroups.getGroupKeys())) {
                log.debug("Picking first group: "+groupKey);
                chosenResult = hasGroups.getGroup(groupKey);
                break;
            }
        }

        log.debug("Got result: "+chosenResult);
        return chosenResult;
    }

    public static HasFiles getResult(ObjectiveSample objectiveSample, ResultDescriptor result) {

        List<SamplePipelineRun> runs = objectiveSample.getPipelineRuns();

        // TODO: test latest successful run first?

        for(int i=runs.size()-1; i>=0; i--) {
            SamplePipelineRun run = runs.get(i);
            log.debug("  Testing run: " + run.getId());

            List<PipelineResult> results = run.getResults();

            for (int j = results.size() - 1; j >= 0; j--) {
                PipelineResult pipelineResult = results.get(j);
                log.debug("  Testing result: " + pipelineResult.getId());

                if (result.isAligned() == null
                        || (result.isAligned() && pipelineResult instanceof SampleAlignmentResult)
                        || (!result.isAligned() && !(pipelineResult instanceof SampleAlignmentResult))) {

                    if (result.getResultName() == null || pipelineResult.getName().equals(result.getResultName())) {
                        log.debug("    Found matching result");
                        if (result.getGroupName() == null) {
                            return pipelineResult;
                        }
                        else if (pipelineResult instanceof HasFileGroups) {
                            HasFileGroups hasGroups = (HasFileGroups) pipelineResult;
                            HasFiles hasFiles = hasGroups.getGroup(result.getGroupName());
                            if (hasFiles != null) {
                                log.debug("    Found group: " + result.getGroupName());
                                return hasFiles;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public static ResultDescriptor getLatestResultDescriptor(Sample sample) {

        List<ObjectiveSample> objectiveSamples = sample.getObjectiveSamples();
        if (objectiveSamples==null || objectiveSamples.isEmpty()) return null;

        ObjectiveSample objSample = objectiveSamples.get(objectiveSamples.size() - 1);
        if (objSample==null) return null;
        SamplePipelineRun run = objSample.getLatestSuccessfulRun();
        if (run==null) return null;
        PipelineResult chosenResult = run.getLatestResult();

        if (chosenResult instanceof HasFileGroups) {
            HasFileGroups hasGroups = (HasFileGroups)chosenResult;
            // Pick the first group, since there is no way to tell which is latest
            for(String groupKey : hasGroups.getGroupKeys()) {
                return new ResultDescriptor(objSample.getObjective(), chosenResult.getName(), groupKey);
            }
        }

        String name = (chosenResult==null)?null:chosenResult.getName();
        return new ResultDescriptor(objSample.getObjective(), name, null);
    }

    public static PipelineResult getResultContainingNeuronSeparation(Sample sample, NeuronFragment neuronFragment) {
        return getNeuronSeparation(sample, neuronFragment, PipelineResult.class);
    }

    public static NeuronSeparation getNeuronSeparation(Sample sample, NeuronFragment neuronFragment) {
        return getNeuronSeparation(sample, neuronFragment, NeuronSeparation.class);
    }

    public static <T extends PipelineResult> T getNeuronSeparation(Sample sample, NeuronFragment neuronFragment, Class<T> returnClazz) {

        if (neuronFragment==null) return null;

        for(ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {
            for(SamplePipelineRun run : objectiveSample.getPipelineRuns()) {
                if (run!=null && run.getResults()!=null) {
                    for(PipelineResult result : run.getResults()) {
                        if (result!=null && result.getResults()!=null) {
                            for(PipelineResult secondaryResult : result.getResults()) {
                                if (secondaryResult!=null && secondaryResult instanceof NeuronSeparation) {
                                    NeuronSeparation separation = (NeuronSeparation)secondaryResult;
                                    if (separation.getFragmentsReference().getReferenceId().equals(neuronFragment.getSeparationId())) {
                                        return returnClazz.equals(NeuronSeparation.class) ? (T)separation : (T)result;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    public static Map<AlignmentScoreType, String> getLatestAlignmentScores(Sample sample) {

        Map<AlignmentScoreType, String> scores = new HashMap<>();

        for(ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {
            for (SamplePipelineRun run : objectiveSample.getPipelineRuns()) {
                if (run != null && run.getResults() != null) {
                    for (SampleAlignmentResult alignment : run.getResultsOfType(SampleAlignmentResult.class)) {
                        scores.putAll(alignment.getScores());
                    }
                }
            }
        }

        return scores;
    }
}
