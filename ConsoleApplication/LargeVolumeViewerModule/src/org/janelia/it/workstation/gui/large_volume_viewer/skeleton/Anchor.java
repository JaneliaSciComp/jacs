package org.janelia.it.workstation.gui.large_volume_viewer.skeleton;

import java.util.LinkedHashSet;
import java.util.Set;
import org.janelia.it.workstation.geom.CoordinateAxis;

import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.it.workstation.signal.Signal1;

public class Anchor {
	public enum Type {
		UNDEFINED,
		SOMA,
		AXON,
		DENDRITE,
		APICAL_DENDRITE,
		FORK_POINT,
		END_POINT,
		CUSTOM
	};
	
	private Long annotationID;
    private Long neuronID;
	private Vec3 location;
	private Type anchorType = Type.UNDEFINED;
	private double radius = 1.0;
	// No explicit edge objects, just symmetric neighbor references
	private Set<Anchor> neighbors = new LinkedHashSet<Anchor>();

    // the difference between these signals: one is triggered by
    //  user mouse actions, will trigger things happening due
    //  to the anchor's having been moved (eg, merges)
    // the "silent" version is triggered
    //  programmatically and won't cause anything other than
    //  the positioning and drawing of the anchor
	public Signal1<Anchor> anchorMovedSignal = new Signal1<Anchor>();
	public Signal1<Anchor> anchorMovedSilentSignal = new Signal1<Anchor>();

    /**
     * Construct a valid anchor, based on information provided from external
     * source, such as database.  Translates from external to view-like
     * location.
     * 
     * @param locationInVoxel coords for anchor.
     * @param parent (possibly null) parent, or previous node in tree.
     * @param tileFormat for translations.
     */
	public Anchor(Vec3 locationInVoxel, Anchor parent, Long neuronID, TileFormat tileFormat) {
        TileFormat.MicrometerXyz micron = tileFormat.micrometerXyzForVoxelXyz(
                new TileFormat.VoxelXyz(
                        (int)locationInVoxel.getX(),
                        (int)locationInVoxel.getY(),
                        (int)locationInVoxel.getZ()
                ), 
                CoordinateAxis.Z
        );
        // Need to bias the position towards the center of the voxel.
		this.location = tileFormat.centerJustifyMicrometerCoordsAsVec3(micron);
        this.neuronID = neuronID;
		addNeighbor(parent);
	}
	
	public boolean addNeighbor(Anchor neighbor) {
		if (neighbor == null)
			return false;
		if (neighbor == this)
			return false;
		if (neighbors.contains(neighbor))
			return false;
		neighbors.add(neighbor);
		neighbor.addNeighbor(this); // ensure reciprocity
		return true;
	}

	public Vec3 getLocation() {
		return location;
	}

	public Type getAnchorType() {
		return anchorType;
	}

	public Long getGuid() {
		return this.annotationID;
	}
	
	public void setGuid(Long id) {
		this.annotationID = id;
	}

	public double getRadius() {
		return radius;
	}

	public Set<Anchor> getNeighbors() {
		return neighbors;
	}

	public void setLocation(Vec3 location) {
		if (location.equals(this.location))
			return;
		this.location = location;
		anchorMovedSignal.emit(this);
	}

    /**
     * update the anchor location, but send the signal
     * on the "silent" channel that will not initiate
     * further actions; meant to be used when setting
     * an anchor location programmatically rather
     * than by the user
     */
	public void setLocationSilent(Vec3 location) {
		if (location.equals(this.location))
			return;
		this.location = location;
		anchorMovedSilentSignal.emit(this);
	}

	public void setAnchorType(Type anchorType) {
		this.anchorType = anchorType;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

    public Long getNeuronID() {
        return neuronID;
    }

    public void setNeuronID(Long neuronID) {
        this.neuronID = neuronID;
    }

}
