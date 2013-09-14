
package org.janelia.it.jacs.model.user_data;

import com.google.gwt.user.client.rpc.IsSerializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.io.Serializable;
import java.util.Date;

@XmlAccessorType(XmlAccessType.NONE)
public class UserToolEvent implements Serializable, IsSerializable {

    public static final String TOOL_EVENT_LOGIN =   "Login";
    public static final String TOOL_EVENT_LOGOUT=   "Logout";

    public static final String TOOL_CATEGORY_SESSION="Session";

    private Long id;
    private Long sessionId;
    private String userLogin;
    private String toolName;
    private String category;
    private String action;
    private Date timestamp;

    // No arg constructor for Hibernate
    public UserToolEvent() {}

    public UserToolEvent(Long sessionId, String userLogin, String toolName, String category, String action, Date timestamp) {
        this.action = action;
        this.category = category;
        this.sessionId = sessionId;
        this.timestamp = timestamp;
        this.toolName = toolName;
        this.userLogin = userLogin;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "UserToolEvent{" +
                "id=" + id +
                ", sessionId=" + sessionId +
                ", userLogin='" + userLogin + '\'' +
                ", toolName='" + toolName + '\'' +
                ", category='" + category + '\'' +
                ", action='" + action + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}