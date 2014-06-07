package org.janelia.it.jacs.model.user_data;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.prefs.SubjectPreference;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlValue;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@XmlAccessorType(XmlAccessType.NONE)
public class Subject implements Serializable, IsSerializable {
	
    private Long id;
    @XmlValue
    private String key = "";
    private String name = "";
    private String fullName = "";
    private String email = "";

    private Set<Node> nodes = new HashSet<Node>(0);
    private Set<Task> tasks = new HashSet<Task>(0);
    private Map<String, SubjectPreference> preferenceMap = new HashMap<String, SubjectPreference>();
    private Map<String, Map<String, SubjectPreference>> categoryMap;
    
    public Subject() {
    }
    
	public Subject(String name, String fullName, String key) {
		this.name = name;
		this.fullName = fullName;
		this.key = key;
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<Node> getNodes() {
        return this.nodes;
    }

    public void setNodes(Set<Node> nodes) {
        this.nodes = nodes;
    }

    public Set<Task> getTasks() {
        return this.tasks;
    }

    public void setTasks(Set<Task> tasks) {
        this.tasks = tasks;
    }

    public void addNode(Node newNode) {
        if (null != newNode) {
            nodes.add(newNode);
            newNode.setOwner(this.getName());
        }
    }

    public synchronized void setPreferenceMap(Map<String, SubjectPreference> preferenceMap) {
        this.preferenceMap = preferenceMap;
    }

    public Map<String, SubjectPreference> getPreferenceMap() {
        return preferenceMap;
    }

    /**
     * Returns the preference with the supplied category or name, or null if no such preference exists
     *
     * @param category - preference category
     * @param name     - preference name looked for
     * @return - the matching SubjectPreference object
     */
    public synchronized SubjectPreference getPreference(String category, String name) {
        return getPreferenceMap().get(getPrefKey(category, name));
    }

    private String getPrefKey(String category, String name) {
        return category + ":" + name;
    }

    public String getPreferenceValue(String category, String name) {
        return getPreference(category, name).getValue();
    }

    /**
     * Updates the preference in the User cache and database.
     *
     * @param pref desired user preference to update
     */
    public synchronized void setPreference(SubjectPreference pref) {
        getPreferenceMap().put(getPrefKey(pref.getCategory(), pref.getName()), pref);  // Hibernate will update database
        addPreferenceToCategoryMap(pref); // Add/update the preference in the appropriate category
    }
    
    public synchronized Map<String, SubjectPreference> getCategoryPreferences(String category) {
        if (categoryMap==null) {
            buildCategoryMap();
        }
        if (categoryMap.containsKey(category))
            return categoryMap.get(category);
        else
            return new HashMap<String, SubjectPreference>();
    }

    private synchronized void buildCategoryMap() {
        this.categoryMap = new HashMap<String, Map<String, SubjectPreference>>();
        for (String s : getPreferenceMap().keySet()) {
            addPreferenceToCategoryMap(getPreferenceMap().get(s));
        }
    }

    private synchronized void addPreferenceToCategoryMap(SubjectPreference pref) {
        if (categoryMap==null) {
            buildCategoryMap();
        }
        Map<String, SubjectPreference> catPrefs = getCategoryPreferences(pref.getCategory()); // creates new Map if not found
        catPrefs.put(pref.getName(), pref);
        categoryMap.put(pref.getCategory(), catPrefs);
    }
}