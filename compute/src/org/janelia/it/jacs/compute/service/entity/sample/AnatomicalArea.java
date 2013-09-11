package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * An anatomical area within a sample.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnatomicalArea implements Serializable {

    private String name;
    private List<Entity> tiles = new ArrayList<Entity>();
    private boolean hasLamina;
	private Long sampleProcessingResultId;
	private String sampleProcessingResultFilename;

    public AnatomicalArea(String name) {
        this.name = name;
        this.hasLamina = false;
    }

    public void addTile(Entity imageTile) {
        tiles.add(imageTile);
        final String tileName = imageTile.getName();
        final Matcher m = LAMINA_PATTERN.matcher(tileName);
        if (m.matches()) {
            hasLamina = true;
        }
    }

    public String getName() {
        return name;
    }

    public List<Entity> getTiles() {
        return tiles;
    }

    public boolean hasLamina() {
        return hasLamina;
    }

    public Long getSampleProcessingResultId() {
        return sampleProcessingResultId;
    }

    public void setSampleProcessingResultId(Long sampleProcessingResultId) {
        this.sampleProcessingResultId = sampleProcessingResultId;
    }

    public String getSampleProcessingResultFilename() {
        return sampleProcessingResultFilename;
    }

    public void setSampleProcessingResultFilename(String sampleProcessingResultFilename) {
        this.sampleProcessingResultFilename = sampleProcessingResultFilename;
    }

    @Override
    public String toString() {
        return "AnatomicalArea{" +
                "name='" + name + '\'' +
                ", tiles=" + tiles +
                ", hasLamina=" + hasLamina +
                ", sampleProcessingResultId=" + sampleProcessingResultId +
                ", sampleProcessingResultFilename='" + sampleProcessingResultFilename + '\'' +
                '}';
    }

    private static final Pattern LAMINA_PATTERN = Pattern.compile("(?i).*lamina.*");
}
