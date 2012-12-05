package org.janelia.it.jacs.compute.service.common.grid.submit.sge;

/**
 * Calculate grid resource native specifications, based on the grid type and memory/slot requirements. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GridResourceUtils {
	
	/**
	 * Returns the SGE spec necessary to claim the given memory and slots. 
	 * 
	 * @param useR620Nodes 
	 * @param requiredMemoryInGB
	 * @param requiredSlots
	 * @return
	 */
	public static String getSpec(boolean useR620Nodes, int requiredMemoryInGB, int requiredSlots) {

		StringBuffer spec = new StringBuffer();
		
        int slotsPerNode;
        int memPerNode;
        int minMem = requiredMemoryInGB;
    	
    	if (useR620Nodes) {
    		spec.append("-l r620=true ");
    		slotsPerNode = 16;
    		memPerNode = 128;
    	}
    	else {
    		if (minMem>24) {
    			// Use a high-mem node
    			spec.append("-l mem96=true -now n ");
        		slotsPerNode = 8;
        		memPerNode = 96;
    		}
    		else {
        		slotsPerNode = 8;
        		memPerNode = 24;
    		}
    	}
    	
    	int memPerSlot = memPerNode/slotsPerNode;
    	int minSlotsByMem = (int)Math.ceil((double)minMem / (double)memPerSlot);
    	int slots = Math.max(minSlotsByMem, requiredSlots);
    	
    	if (slots>slotsPerNode) {
    		// Can't grab more slots than exist. This is the best we can do.
    		slots = slotsPerNode;
    	}
    	
    	spec.append("-pe batch "+slots);
    	return spec.toString();
	}
    
    public static final void main(String[] args) {

    	System.out.println("24 GB needed");
    	System.out.println("  Legacy Grid: "+getSpec(false, 24, 1));
    	System.out.println("  R620 Grid: "+getSpec(true, 24, 1));

    	System.out.println("12 GB needed");
    	System.out.println("  Legacy Grid: "+getSpec(false, 12, 1));
    	System.out.println("  R620 Grid: "+getSpec(true, 12, 1));

    	System.out.println("96 GB needed");
    	System.out.println("  Legacy Grid: "+getSpec(false, 96, 1));
    	System.out.println("  R620 Grid: "+getSpec(true, 96, 1));

    	System.out.println("128 GB needed");
    	System.out.println("  Legacy Grid: "+getSpec(false, 128, 1));
    	System.out.println("  R620 Grid: "+getSpec(true, 128, 1));

    	System.out.println("2 GB needed, and at least 4 slots");
    	System.out.println("  Legacy Grid: "+getSpec(false, 2, 4));
    	System.out.println("  R620 Grid: "+getSpec(true, 2, 4));
    	
    }
}
