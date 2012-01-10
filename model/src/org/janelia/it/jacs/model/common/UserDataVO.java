
package org.janelia.it.jacs.model.common;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 4, 2008
 * Time: 8:57:32 AM
 */
public class UserDataVO implements IsSerializable, Serializable {
    public static final String SORT_BY_USER_LOGIN = "userLogin";
    public static final String SORT_BY_USER_ID = "userId";
    public static final String SORT_BY_FULLNAME = "fullName";
    public static final String SORT_BY_EMAIL = "email";

    private String userLogin;
    private Long userId;
    private String fullname;
    private String email;

    public UserDataVO() {
    }

    public UserDataVO(String userLogin, Long userId, String fullname, String email) {
        this.userLogin = userLogin;
        this.userId = userId;
        this.fullname = fullname;
        this.email = email;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String toString() {
        return "UserDataVO{" +
                "userLogin='" + userLogin + '\'' +
                ", userId=" + userId +
                ", fullname='" + fullname + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
