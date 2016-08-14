package org.janelia.it.jacs.shared.units;

public interface PhysicalQuantity<D extends PhysicalDimension>
{
	PhysicalUnit<D> getUnit();
	double getValue();
}
