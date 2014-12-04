package org.janelia.it.jacs.model.domain.enums;

public enum Operator {
	StartsWith("Starts With"),
	Equals("Is Equal To"),
	NotEquals("Is Not Equal To"),
	Empty("Is Empty"),
	NotEmpty("Is Not Empty");
	private String label;
	private Operator(String label) {
		this.label = label;
	}
	public String getLabel() {
		return label;
	}
}
