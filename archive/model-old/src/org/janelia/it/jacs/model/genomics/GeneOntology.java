
package org.janelia.it.jacs.model.genomics;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.lang.String;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: jhoover
 * Date: Sep 11, 2007
 * Time: 3:36:56 PM
 */
public class GeneOntology implements Serializable, IsSerializable {

    private transient List<String> synonymList;
    private transient List<String> alternateGOIdList;
    private transient Map<String, String> relatedGOIdMap;
    private transient List<String> xrefAnalogList;
    private transient List<String> subsetList;

    private String accession;
    private String name;
    private String category;
    private String definition;
    private String synonyms;
    private String alternateGOIds;
    private String relatedGOIds;
    private String xrefAnalogs;
    private String subsets;
    private String comment;
    private Boolean isObsolete;
    private String goVersion;

    public GeneOntology() {
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public String getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(String synonyms) {
        this.synonyms = synonyms;
    }

    public String getAlternateGOIds() {
        return alternateGOIds;
    }

    public void setAlternateGOIds(String alternateGOIds) {
        this.alternateGOIds = alternateGOIds;
    }

    public String getRelatedGOIds() {
        return relatedGOIds;
    }

    public void setRelatedGOIds(String relatedGOIds) {
        this.relatedGOIds = relatedGOIds;
    }

    public String getXrefAnalogs() {
        return xrefAnalogs;
    }

    public void setXrefAnalogs(String xrefAnalogs) {
        this.xrefAnalogs = xrefAnalogs;
    }

    public String getSubsets() {
        return subsets;
    }

    public void setSubsets(String subsets) {
        this.subsets = subsets;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Boolean getObsolete() {
        return isObsolete;
    }

    public void setObsolete(Boolean obsolete) {
        isObsolete = obsolete;
    }

    public String getGoVersion() {
        return goVersion;
    }

    public void setGoVersion(String goVersion) {
        this.goVersion = goVersion;
    }

    public List<String> getSynonymList() {
        if (synonymList == null)
            synonymList = Arrays.asList(synonyms.split(", "));
        return synonymList;
    }

    public List<String> getAlternateGOIdList() {
        if (alternateGOIdList == null)
            alternateGOIdList = Arrays.asList(alternateGOIds.split(", "));
        return alternateGOIdList;
    }

    public List<String> getXrefAnalogList() {
        if (xrefAnalogList == null)
            xrefAnalogList = Arrays.asList(xrefAnalogs.split(", "));
        return xrefAnalogList;
    }

    public List<String> getSubsetList() {
        if (subsetList == null)
            subsetList = Arrays.asList(subsets.split(", "));
        return subsetList;
    }

    public Map<String, String> getRelatedGOIdMap() {
        if (relatedGOIdMap == null) {
            relatedGOIdMap = new HashMap<String, String>();
            for (String relString : Arrays.asList(relatedGOIds, "\n")) {
                int blankPos = relString.indexOf(' ');
                relatedGOIdMap.put(relString.substring(0, blankPos).trim(),
                        relString.substring(blankPos).trim());
            }
        }
        return relatedGOIdMap;
    }
}
