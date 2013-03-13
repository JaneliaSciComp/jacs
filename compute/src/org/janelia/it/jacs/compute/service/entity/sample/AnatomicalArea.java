package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * An anatomical area within a sample.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnatomicalArea implements Serializable {

    private String name;
    private List<Entity> tiles = new ArrayList<Entity>();
	
	public AnatomicalArea(String name) {
		this.name = name;
	}

	public void addTile(Entity imageTile) {
	    tiles.add(imageTile);
	}

    public String getName() {
        return name;
    }

    public List<Entity> getTiles() {
        return tiles;
    }
}
