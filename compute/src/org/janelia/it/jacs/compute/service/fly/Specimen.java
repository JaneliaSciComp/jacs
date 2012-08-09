package org.janelia.it.jacs.compute.service.fly;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A fly specimen with a name that follows the FlyLight project's general file naming scheme. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Specimen {
	
	private String fullName;
	private String normalizedName;
	private String lab;
	private String fragmentId;
	private String plate;
	private String well;
	private String vector;
	private String insertionSite;
	private String specimen;
	private String descriptor;
	private String gender;
	private String age;
	private String anatomicalArea;
	private String scanningDate;
	private String guid;
	private String suffix;
	
	
	private Specimen(String fullName) throws Exception {

		this.fullName = fullName;
		
        String[] mainParts = fullName.split("-");
        String[] parts = mainParts[0].split("_");
        
        this.lab = parts[0];
        if (parts.length>1) {
        	this.fragmentId = parts[1];	
        	Pattern p = Pattern.compile("^(\\d+)([A-Z]\\d+)$");
        	Matcher m = p.matcher(fragmentId);
        	if (m.matches()) {
	        	this.plate = m.group(1);
	        	this.well = m.group(2);
        	}
        }
        if (parts.length>2) {
        	this.vector = parts[2];	
        }
        if (parts.length>3) {
        	this.insertionSite = parts[3];	
        }
        if (parts.length>4) {
        	this.specimen = parts[4];	
        }
        
		if (mainParts.length>1) {
			String[] parts2 = mainParts[1].split("_");
			this.descriptor = parts2[0];
			if (parts2.length>1) {
				this.scanningDate = parts2[1];
			}
			if (parts2.length>2) {
				this.guid = parts2[2];
			}
			if (parts2.length>3) {
				this.suffix = parts2[3];
			}
		}

		if (mainParts.length>2) {
			for(int i=2; i<mainParts.length; i++) {
				this.suffix = suffix!=null?suffix+"-"+mainParts[i]:mainParts[i];
			}
		}

		if (descriptor!=null) {
	    	Pattern p = Pattern.compile("^([mfx])([EPLA]\\d{2})([a-z])$");
	    	Matcher m = p.matcher(descriptor);
	    	if (m.matches()) {
	        	this.gender = m.group(1);
	        	this.age = m.group(2);
	        	this.anatomicalArea = m.group(3);
	    	}
		}
		
		this.normalizedName = lab+"_"+fragmentId+"_"+vector+"_"+insertionSite+"_"+specimen+"-"+descriptor+"_"+scanningDate+"_"+guid;
	}
	
	public boolean isValidLine() {
		return (lab!=null && fragmentId!=null && vector!=null && insertionSite != null);
	}
	
	public static Specimen createSpecimenFromFullName(String fullName) throws Exception {
		return new Specimen(fullName);
	}
	
	public String getFlylineName() {
		return lab+"_"+fragmentId+"_"+vector+"_"+insertionSite;
	}
	
	public String getSpecimenName() {
		return getFlylineName()+"_"+specimen;
	}
	
	public String getFragmentName() {
		return lab+"_"+fragmentId;
	}
	
	public String getFullName() {
		return fullName;
	}

	public String getNormalizedName() {
		return normalizedName;
	}

	public String getLab() {
		return lab;
	}

	public String getFragmentId() {
		return fragmentId;
	}

	public String getVector() {
		return vector;
	}

	public String getInsertionSite() {
		return insertionSite;
	}

	public String getSpecimen() {
		return specimen;
	}

	public String getDescriptor() {
		return descriptor;
	}

	public String getGender() {
		return gender;
	}

	public String getAge() {
		return age;
	}

	public String getAnatomicalArea() {
		return anatomicalArea;
	}

	public String getScanningDate() {
		return scanningDate;
	}

	public String getGuid() {
		return guid;
	}

	public String getSuffix() {
		return suffix;
	}

	public String getPlate() {
		return plate;
	}

	public String getWell() {
		return well;
	}
	
	@Override
	public String toString() {
		return "Specimen [fullName=" + fullName + ", normalizedName=" + normalizedName + ", lab=" + lab
				+ ", fragmentId=" + fragmentId + ", plate=" + plate + ", well=" + well + ", vector=" + vector
				+ ", insertionSite=" + insertionSite + ", specimen=" + specimen + ", descriptor=" + descriptor
				+ ", gender=" + gender + ", age=" + age + ", anatomicalArea=" + anatomicalArea + ", scanningDate="
				+ scanningDate + ", guid=" + guid + ", suffix=" + suffix + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((normalizedName == null) ? 0 : normalizedName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Specimen other = (Specimen) obj;
		if (normalizedName == null) {
			if (other.normalizedName != null)
				return false;
		} else if (!normalizedName.equals(other.normalizedName))
			return false;
		return true;
	}

	/**
	 * Test harness for naming scheme parser.
	 * @param args
	 * @throws Exception
	 */
	public static final void main(String[] args) throws Exception {
		
		String[] examples = {
				"GMR_50C09_AE_01_00-fA01b_C090123_20090123121057531_ch2_total",
				"GMR_50H06_AE_01_05-fA01v_C110104_20110104100003640",
				"GMR_11G09_BB_21",
				"GMR_10C09_AE_01-brain-mixup",
				"INTERESTING:sc1,"
		};
		
		for(String example : examples) {
			Specimen specimen = createSpecimenFromFullName(example);
			System.out.println("Input: "+specimen.getFullName());
			System.out.println("    Flyline: "+specimen.getFlylineName());
			System.out.println("    Fragment Name: "+specimen.getFragmentName());
			System.out.println("    toString(): "+specimen);
		}
		
	}
}