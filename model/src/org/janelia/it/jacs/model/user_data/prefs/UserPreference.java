
package org.janelia.it.jacs.model.user_data.prefs;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * @author Michael Press
 */
public class UserPreference implements Serializable, IsSerializable {

    // Preferences
    public transient static final String PREF_EMAIL_ON_JOB_COMPLETION = "emailUponJobCompletion";

    // Categories
    public transient static final String CAT_NOTIFICATION = "notification";

    private String _name;
    private String _value;
    private String _category;

    public UserPreference() {
    }

    public UserPreference(String name, String category, String value) {
        _name = name;
        _category = category;
        _value = value;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public String getValue() {
        return _value;
    }

    public void setValue(String value) {
        _value = value;
    }

    public String getCategory() {
        return _category;
    }

    public void setCategory(String category) {
        _category = category;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserPreference that = (UserPreference) o;

        return _category.equals(that._category) &&
               _name.equals(that._name) &&
                !(_value != null ? !_value.equals(that._value) : that._value != null);

    }

    @Override
    public int hashCode() {
        int result = _name.hashCode();
        result = 31 * result + (_value != null ? _value.hashCode() : 0);
        result = 31 * result + _category.hashCode();
        return result;
    }
}
