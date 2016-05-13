package org.janelia.it.jacs.shared.units;

public class Length extends QuantityImpl<BaseDimension.Length>
{
	public Length(double value, PhysicalUnit<BaseDimension.Length> unit) {
		super(value, unit);
	}
}
