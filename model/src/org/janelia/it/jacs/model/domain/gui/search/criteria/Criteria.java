package org.janelia.it.jacs.model.domain.gui.search.criteria;

import java.util.HashSet;

import org.janelia.it.jacs.model.domain.Reference;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class")
public abstract class Criteria {

}
