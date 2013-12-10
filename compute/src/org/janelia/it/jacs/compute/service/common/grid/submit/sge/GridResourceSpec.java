package org.janelia.it.jacs.compute.service.common.grid.submit.sge;

/**
 * Calculate grid resource native specifications, based on the grid type and memory/slot requirements. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GridResourceSpec {
	
    private String spec;
    private int slots;
    
    /**
	 * Creates the SGE spec necessary to claim the given memory and slots. 
	 * 
	 * @param useR620Nodes 
	 * @param requiredMemoryInGB
	 * @param requiredSlots
	 * @return
	 */
	public GridResourceSpec(boolean useR620Nodes, int requiredMemoryInGB, int requiredSlots, boolean isExclusive) {

		StringBuffer spec = new StringBuffer();
		
        int slotsPerNode;
        float memPerNode;
        float minMem = requiredMemoryInGB;
    	
    	if (useR620Nodes) {
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
    	
    	if (isExclusive) {
    	    this.slots = slotsPerNode;
    	}
    	else {
            float memPerSlot = memPerNode/(float)slotsPerNode;
            int minSlotsByMem = (int)Math.ceil((double)minMem / (double)memPerSlot);
            this.slots = Math.max(minSlotsByMem, requiredSlots);
            if (this.slots>slotsPerNode) {
                // Can't grab more slots than exist. This is the best we can do.
                this.slots = slotsPerNode;
            }   
    	}

        spec.append("-pe batch "+slots); 
    	this.spec = spec.toString();
	}
	
    public String getNativeSpec() {
        return spec;
    }

    public int getSlots() {
        return slots;
    }
    
    private static String getSpec(boolean useR620Nodes, int requiredMemoryInGB, int requiredSlots, boolean isExclusive) {
        GridResourceSpec spec = new GridResourceSpec(useR620Nodes, requiredMemoryInGB, requiredSlots, isExclusive);
        return spec.getNativeSpec();
    }
    
    public static final void main(String[] args) {

    	System.out.println("24 GB needed");
    	System.out.println("  Legacy Grid: "+getSpec(false, 24, 1, false));
    	System.out.println("  R620 Grid: "+getSpec(true, 24, 1, false));

    	System.out.println("12 GB needed");
    	System.out.println("  Legacy Grid: "+getSpec(false, 12, 1, false));
    	System.out.println("  R620 Grid: "+getSpec(true, 12, 1, false));

    	System.out.println("96 GB needed");
    	System.out.println("  Legacy Grid: "+getSpec(false, 96, 1, false));
    	System.out.println("  R620 Grid: "+getSpec(true, 96, 1, false));

    	System.out.println("128 GB needed");
    	System.out.println("  Legacy Grid: "+getSpec(false, 128, 1, false));
    	System.out.println("  R620 Grid: "+getSpec(true, 128, 1, false));

    	System.out.println("2 GB needed, and at least 4 slots");
    	System.out.println("  Legacy Grid: "+getSpec(false, 2, 4, false));
    	System.out.println("  R620 Grid: "+getSpec(true, 2, 4, false));
    	
        System.out.println("Exclusive");
        System.out.println("  Legacy Grid: "+getSpec(false, 2, 4, true));
        System.out.println("  R620 Grid: "+getSpec(true, 2, 4, true));
    	
    }
}
