package org.janelia.it.jacs.model.domain.sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A set of LSMs in a Sample with a common objective. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ObjectiveSample {

    private List<SampleTile> tiles;
    private List<SamplePipelineRun> pipelineRuns;
    private transient String objective;
    private transient Sample parent;

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
    
    @JsonIgnore
    public List<SamplePipelineRun> getPipelineRuns() {
        for(SamplePipelineRun pipelineRun : pipelineRuns) {
            pipelineRun.setParent(this);
        }
        return pipelineRuns==null?null:Collections.unmodifiableList(pipelineRuns);
    }

    @JsonProperty
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
    public List<SampleTile> getTiles() {
        for(SampleTile tile : tiles) {
            tile.setParent(this);
        }
        return Collections.unmodifiableList(tiles);
    }

    @JsonProperty
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

}
