package org.janelia.it.jacs.model.domain;

import java.util.List;

public class ObjectiveSample {
    
    private List<SampleTile> tiles;
    private List<SamplePipelineRun> pipelineRuns;
    
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
