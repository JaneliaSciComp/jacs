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
	OPTIC_CENTRAL_BORDER_AND_OPTIC_TILE("Optic/Central Border and other Optic Lobe", false, false),
	BOTH_OPTIC_TILES("Both Optic Tiles", false, false),
	OPTIC_TILE("Optic Lobe Tile", false, true),
	CENTRAL_TILE("Central Tile", false, false),
	OTHER("Other", true, false), // For other unsupported tiling configurations
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
     * Added two-tile whole brains for Lee Lab as of 6/2012.  
     * Added "unknown" stitched brains for Lee Lab as of 8/2012.
     * 
     * @param filePairs
     * @return
     */
    public static TilingPattern getTilingPattern(List<String> tags) {

    	boolean allUnnamed = true;
    	for(String tag : tags) {
    		if (!tag.startsWith("Tile ")) {
    			allUnnamed = false;
    			break;
    		}
    	}
    	
    	if (allUnnamed) {
    		return UNKNOWN;
    	}
    	
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
        
        if (tags.size()==6) {
            if (hasLeftOptic && hasLeftDorsalBrain && hasVentralBrain && hasRightDorsalBrain && hasRightOptic && hasCentralBrain) return WHOLE_BRAIN;
        }
        else if (tags.size()==5) {
            if (hasLeftOptic && hasLeftDorsalBrain && hasVentralBrain && hasRightDorsalBrain && hasRightOptic) return WHOLE_BRAIN;
        }
        else if (tags.size()==4) {
            if (hasLeftOptic && hasLeftCentralBrain && hasRightCentralBrain && hasRightOptic) return OPTIC_SPAN;
        }
        else if (tags.size()==3) {
	        if (hasLeftOptic && hasCentralBrain && hasRightOptic) return OPTIC_SPAN;
	        if (hasLeftDorsalBrain && hasVentralBrain && hasRightDorsalBrain) return CENTRAL_BRAIN;
            if (hasLeftCentralBrain && hasRightCentralBrain && (hasLeftOptic || hasRightOptic)) return OPTIC_CENTRAL_BORDER;
            if (hasVentralBrain && (hasLeftDorsalBrain && hasLeftOptic) || (hasRightDorsalBrain || hasRightOptic)) return OPTIC_CENTRAL_BORDER;
            if (hasLeftDorsalBrain && hasRightDorsalBrain && (hasLeftOptic || hasRightOptic)) return OPTIC_CENTRAL_BORDER;
            if ((hasLeftCentralBrain || hasRightCentralBrain) && hasLeftOptic && hasRightOptic) return OPTIC_CENTRAL_BORDER_AND_OPTIC_TILE;
        }
        else if (tags.size()==2) {
        	if ((hasLeftOptic && hasLeftCentralBrain) || (hasRightOptic && hasRightCentralBrain)) return OPTIC_CENTRAL_BORDER;
            if (hasLeftOptic && hasRightOptic) return BOTH_OPTIC_TILES;
            if (hasLeftBrain && hasRightBrain) return WHOLE_BRAIN;
        }
        else if (tags.size()==1) {
        	if (hasLeftOptic || hasRightOptic) return OPTIC_TILE;
        	if (hasCentralBrain) return CENTRAL_TILE;
        }
        
        return OTHER;	
    }
    
}