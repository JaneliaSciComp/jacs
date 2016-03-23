package org.janelia.it.jacs.model.domain.sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;

/**
 * A set of LSMs in a Sample with a common objective. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ObjectiveSample {

    private String chanSpec;
    private List<SampleTile> tiles;
    private List<SamplePipelineRun> pipelineRuns;
    
    private transient String objective;
    private transient Sample parent;

    public String getChanSpec() {
        return chanSpec;
    }

    public void setChanSpec(String chanSpec) {
        this.chanSpec = chanSpec;
    }
    
    @JsonIgnore
    public String getObjective() {
        return objective;
    }

    @JsonIgnore
    void setObjective(String objective) {
        this.objective = objective;
    }

    @JsonIgnore
    public Sample getParent() {
        return parent;
    }

    @JsonIgnore
    void setParent(Sample parent) {
        this.parent = parent;
    }

    @JsonIgnore
    public boolean hasPipelineRuns() {
        return pipelineRuns!=null && !pipelineRuns.isEmpty();
    }

    public List<SamplePipelineRun> getPipelineRuns() {
        for(SamplePipelineRun pipelineRun : pipelineRuns) {
            pipelineRun.setParent(this);
        }
        return pipelineRuns==null?null:Collections.unmodifiableList(pipelineRuns);
    }

    public void setPipelineRuns(List<SamplePipelineRun> pipelineRuns) {
        this.pipelineRuns = pipelineRuns;
    }
    
    @JsonIgnore
    public void addRun(SamplePipelineRun pipelineRun) {
        if (pipelineRuns==null) {
            this.pipelineRuns = new ArrayList<>();
        }
        pipelineRun.setParent(this);
        pipelineRuns.add(pipelineRun);
    }

    @JsonIgnore
    public void removeRun(SamplePipelineRun pipelineRun) {
        if (pipelineRuns==null) {
            return;
        }
        pipelineRun.setParent(null);
        pipelineRuns.remove(pipelineRun);
    }

    @JsonIgnore
    public SamplePipelineRun getLatestRun() {
        if (pipelineRuns == null) {
            return null;
        }
        if (pipelineRuns.isEmpty()) {
            return null;
        }
        return getPipelineRuns().get(pipelineRuns.size() - 1);
    }

    @JsonIgnore
    public SamplePipelineRun getLatestSuccessfulRun() {
        if (pipelineRuns == null) {
            return null;
        }
        if (pipelineRuns.isEmpty()) {
            return null;
        }
        for(SamplePipelineRun run : Lists.reverse(getPipelineRuns())) {
            if (!run.hasError()) return run;
        }
        return null;
    }

    @JsonIgnore
    public <T extends PipelineResult> T getLatestResultOfType(Class<T> resultClass) {
        return getLatestResultOfType(resultClass, null);
    }

    @JsonIgnore
    public <T extends PipelineResult> T getLatestResultOfType(Class<T> resultClass, String resultName) {
        for(SamplePipelineRun run : Lists.reverse(getPipelineRuns())) {
            for(T result : Lists.reverse(run.getResultsOfType(resultClass))) {
                if (resultName==null || result.getName().equals(resultName)) {
                    return result;
                }
            }
        }
        return null;
    }
    
    @JsonIgnore
    public SamplePipelineRun getRunById(Long pipelineRunId) {
        if (pipelineRuns == null) {
            return null;
        }
        for(SamplePipelineRun run : pipelineRuns) {
            if (run.getId().equals(pipelineRunId)) {
                return run;
            }
        }
        return null;
    }

    public <T extends PipelineResult> List<T> getResultsById(Class<T> resultClass, Long resultEntityId) {
        List<T> results = new ArrayList<>();
        for(SamplePipelineRun run : getPipelineRuns()) {
            for(T result : run.getResultsById(resultClass, resultEntityId)) {
                results.add(result);
            }
        }
        return results;
    }

    public List<SampleTile> getTiles() {
        for(SampleTile tile : tiles) {
            tile.setParent(this);
        }
        return Collections.unmodifiableList(tiles);
    }

    public void setTiles(List<SampleTile> tiles) {
        this.tiles = tiles;
    }
    
    @JsonIgnore
    public void addTile(SampleTile tile) {
        if (tiles==null) {
            this.tiles = new ArrayList<>();
        }
        tile.setParent(this);
        tiles.add(tile);
    }

    @JsonIgnore
    public void removeTile(SampleTile tile) {
        if (tiles==null) {
            return;
        }
        tile.setParent(null);
        tiles.remove(tile);
    }

    @JsonIgnore
    public SampleTile getTileByName(String name) {
        for(SampleTile tile : tiles) {
            if (tile.getName().equals(name)) {
                return tile;
            }
        }
        return null;
    }

    @JsonIgnore
    public String getName() {
        return getParent().getName()+"~"+getObjective();
    }
}
