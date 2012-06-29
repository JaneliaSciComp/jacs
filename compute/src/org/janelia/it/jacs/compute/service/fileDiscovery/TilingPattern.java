package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.util.List;

/**
 * Known tiling patterns for FlyLight imaging.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public enum TilingPattern {
	
	WHOLE_BRAIN("Whole Brain", true, true),
	OPTIC_SPAN("Optic Span", true, true),
	CENTRAL_BRAIN("Central Brain", true, true),
	OPTIC_CENTRAL_BORDER("Optic/Central Border", true, false),
	OPTIC_TILE("Optic Lobe Tile", false, true),
	CENTRAL_TILE("Central Tile", false, false),
	OTHER("Other", false, false), // For other unsupported tiling configurations
	UNKNOWN("Unknown", true, true); // If we don't know, might as well try to run everything
	
	private String name;
	private boolean isStitchable;
	private boolean isAlignable;
	
	private TilingPattern(String name, boolean isStitchable, boolean isAlignable) {
		this.name = name;
		this.isStitchable = isStitchable;
		this.isAlignable = isAlignable;
	}
	
	public String getName() {
		return name;
	}

	public boolean isStitchable() {
		return isStitchable;
	}	

    public boolean isAlignable() {
		return isAlignable;
	}

	/**
     * Known tiling patterns as of 11/2011, enumerated by Chris Z.
     * Added two-tile whole brains for Lee Lab on 6/2012.  
     * 
     * @param filePairs
     * @return
     */
    public static TilingPattern getTilingPattern(List<String> tags) {

        boolean hasLeftOptic = false;
        boolean hasRightOptic = false;
        boolean hasLeftCentralBrain = false;
        boolean hasRightCentralBrain = false;
        boolean hasLeftDorsalBrain = false;
        boolean hasRightDorsalBrain = false;
        boolean hasVentralBrain = false;
        boolean hasCentralBrain = false;
        boolean hasLeftBrain = false;
        boolean hasRightBrain = false;
        
        for(String tag : tags) {
        	if ("Left Optic Lobe".equals(tag)) hasLeftOptic = true;
        	if ("Right Optic Lobe".equals(tag)) hasRightOptic = true;
        	if ("Left Central Brain".equals(tag)) hasLeftCentralBrain = true;
        	if ("Right Central Brain".equals(tag)) hasRightCentralBrain = true;
        	if ("Left Dorsal Brain".equals(tag)) hasLeftDorsalBrain = true;
        	if ("Right Dorsal Brain".equals(tag)) hasRightDorsalBrain = true;
        	if ("Ventral Brain".equals(tag)) hasVentralBrain = true;
        	if ("Central Brain".equals(tag)) hasCentralBrain = true;
        	if ("Left Brain".equals(tag)) hasLeftBrain = true;
        	if ("Right Brain".equals(tag)) hasRightBrain = true;
        }
        
        if (hasLeftOptic && hasLeftDorsalBrain && hasVentralBrain && hasRightDorsalBrain && hasRightOptic) return TilingPattern.WHOLE_BRAIN;
        if (hasLeftOptic && hasLeftCentralBrain && hasRightCentralBrain && hasRightOptic) return TilingPattern.OPTIC_SPAN;
        if (hasLeftOptic && hasCentralBrain && hasRightOptic) return TilingPattern.OPTIC_SPAN;
        if (hasLeftDorsalBrain && hasVentralBrain && hasRightDorsalBrain) return TilingPattern.CENTRAL_BRAIN;
        if ((hasLeftOptic && hasLeftCentralBrain) || (hasRightOptic && hasRightCentralBrain)) return TilingPattern.OPTIC_CENTRAL_BORDER;
        if (hasLeftBrain && hasRightBrain) return TilingPattern.WHOLE_BRAIN;
        
        		
        if (tags.size()==1) {
        	if (hasLeftOptic || hasRightOptic) return TilingPattern.OPTIC_TILE;
        	if (hasCentralBrain) return TilingPattern.CENTRAL_TILE;
        }
        
        if (tags.size()==2) {
        	if (hasLeftOptic && hasRightOptic) return TilingPattern.OPTIC_TILE;
        }
        
        return TilingPattern.OTHER;	
    }
    
}