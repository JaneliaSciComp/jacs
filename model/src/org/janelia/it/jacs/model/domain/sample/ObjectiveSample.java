package org.janelia.it.jacs.model.domain.sample;

import java.util.List;

public class ObjectiveSample {
    
    private List<SampleTile> tiles;
    private List<SamplePipelineRun> pipelineRuns;

    public SamplePipelineRun getLatestRun() {
        if (pipelineRuns==null) return null;
        if (pipelineRuns.isEmpty()) return null;
        return pipelineRuns.get(pipelineRuns.size()-1);
    }

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    
    public List<SampleTile> getTiles() {
        return tiles;
    }
    public void setTiles(List<SampleTile> tiles) {
        this.tiles = tiles;
    }
    public List<SamplePipelineRun> getPipelineRuns() {
        return pipelineRuns;
    }
    public void setPipelineRuns(List<SamplePipelineRun> pipelineRuns) {
        this.pipelineRuns = pipelineRuns;
    }
    
}
