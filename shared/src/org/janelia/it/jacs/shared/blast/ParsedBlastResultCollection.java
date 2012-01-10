
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
