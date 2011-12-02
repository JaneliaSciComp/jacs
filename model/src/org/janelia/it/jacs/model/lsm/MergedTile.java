package org.janelia.it.jacs.model.lsm;

import java.io.File;

/**
 * A pair of LSMs that have been merged.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MergedTile {

	private String name;
	private File mergedFile;
	
	public MergedTile(String name, File mergedFile) {
		this.name = name;
		this.mergedFile = mergedFile;
	}

	public String getName() {
		return name;
	}

	public File getMergedFile() {
		return mergedFile;
	}

	public String getMergedFilepath() {
		return mergedFile.getAbsolutePath();
	}
	
	public String getSeparationResultName() {
		return name+" Neuron Separation";
	}
	
}
