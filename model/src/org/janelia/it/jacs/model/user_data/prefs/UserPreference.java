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
