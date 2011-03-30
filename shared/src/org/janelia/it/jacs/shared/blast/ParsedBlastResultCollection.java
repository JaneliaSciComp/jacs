/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.shared.blast;

import java.io.Serializable;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: kli
 * Date: Jan 30, 2007
 * Time: 3:48:41 PM
 */
public class ParsedBlastResultCollection implements Serializable {

    private ArrayList<ParsedBlastResult> pbrList = new ArrayList<ParsedBlastResult>();
    private Map<String, String> deflineMap = new HashMap<String, String>();

    public void addParsedBlastResult(ParsedBlastResult pbr) {
        if (!pbrList.contains(pbr)) {
            pbrList.add(pbr);
        }
    }

    public void addDefline(String id, String defline) {
        deflineMap.put(id, defline);
    }

    public String getDefline(String id) {
        return deflineMap.get(id);
    }

    public Map<String, String> getDeflineMap() {
        return deflineMap;
    }

    public void setDeflineMap(Map<String, String> deflineMap) {
        this.deflineMap = deflineMap;
    }

    public void addDeflineMap(Map<String, String> deflineMap) {
        for (Map.Entry<String, String> deflineEntry : deflineMap.entrySet()) {
            addDefline(deflineEntry.getKey(), deflineEntry.getValue());
        }
    }

    public Iterator<ParsedBlastResult> iterator() {
        return pbrList.iterator();
    }

    public int size() {
        return pbrList.size();
    }

    public ArrayList<ParsedBlastResult> getParsedBlastResults() {
        return this.pbrList;
    }

    public void setPbrList(ArrayList<ParsedBlastResult> pbrList) {
        this.pbrList = pbrList;
    }

    public void addAll(ParsedBlastResultCollection parsedBlastResultCollection) {
        if (parsedBlastResultCollection == null) {
            throw new IllegalArgumentException("Cannot perform addAll with null");
        }
        this.pbrList.addAll(parsedBlastResultCollection.pbrList);
        this.deflineMap.putAll(parsedBlastResultCollection.deflineMap);

    }

    public void addPbrList(Collection<ParsedBlastResult> pbrs) {
        for (ParsedBlastResult pbr : pbrs) {
            addParsedBlastResult(pbr);
        }
    }

    public void sort() {
        Collections.sort(this.pbrList);
    }

}
