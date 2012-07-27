package org.janelia.it.jacs.model.user_data;

import com.google.gwt.user.client.rpc.IsSerializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlValue;
import java.io.Serializable;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: kimmelr
 * Date: 7/11/12
 * Time: 2:35 PM
 */
@XmlAccessorType(XmlAccessType.NONE)
public class DataGroup implements Serializable, IsSerializable {

    private Long id;
    @XmlValue
    private String name;
    private String description;
    private User owner;
    private Set<User> dataGroupMembers;

    public DataGroup(Long id, String name, String description, HashSet<User> dataGroupMembers) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.dataGroupMembers = dataGroupMembers;
    }

    public User getOwner(){
        return owner;
    }

    public void setOwner(User owner){
        this.owner = owner;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<User> getDataGroupMembers() {
        return dataGroupMembers;
    }

    public void setDataGroupMembers(Set<User> dataCircleUsers) {
        this.dataGroupMembers = dataCircleUsers;
    }

    public List<User> getOrderedMembers() {
        List<User> users = new ArrayList<User>(getDataGroupMembers());
        Collections.sort(users, new Comparator<User>() {
            @Override
            public int compare(User o1, User o2) {
                return o1.getUserLogin().compareTo(o2.getUserLogin());
            }
        });
        return users;
    }
}
