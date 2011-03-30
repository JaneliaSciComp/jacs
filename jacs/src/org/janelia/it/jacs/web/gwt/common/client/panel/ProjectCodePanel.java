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

package org.janelia.it.jacs.web.gwt.common.client.panel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.prefs.UserPreference;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.service.prefs.Preferences;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ExternalLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.suggest.BaseSuggestOracle;
import org.janelia.it.jacs.web.gwt.common.client.ui.suggest.MatchesAnywhereSuggestOracle;
import org.janelia.it.jacs.web.gwt.common.client.util.BlastData;
import org.janelia.it.jacs.web.gwt.common.client.util.StringUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.SystemProps;

import java.util.HashSet;

/**
 * User: aresnick
 * Date: Jun 30, 2009
 * Time: 3:04:02 PM
 * <p/>
 * <p/>
 * Description: NOTE!!!! Any jsp which includes this widget will not populate the user pref unless this is in the jsp
 *     <jsp:include page="/WEB-INF/jsp/common/Preferences.jsp">
        <jsp:param name="prefCategoryNames" value="tasks,BasePaginatorRowsPerPage"/> (the latter category can be whatever)
    </jsp:include>
 */
public class ProjectCodePanel extends VerticalPanel {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.panel.ProjectCodePanel");

    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);

    private SuggestBox _projectCodeSuggestBox;
    private String _userCodePref="";
    private BaseSuggestOracle _suggestOracle;

    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

    public String getProjectCode() {
        // if someone is asking, then save the value as a preference
        Preferences.setUserPreference(new UserPreference("grantCode", "tasks", _projectCodeSuggestBox.getText().trim()));
        return _projectCodeSuggestBox.getText().trim();
    }

    public void setProjectCode(String projectCodeText) {
        _projectCodeSuggestBox.setText(projectCodeText);
    }

    public ProjectCodePanel() {
        super();
        UserPreference prefProjectCode = Preferences.getUserPreference("grantCode", "tasks", null);
        _userCodePref = prefProjectCode.getValue();
        init(prefProjectCode.getValue());
    }

    public ProjectCodePanel(BlastData _blastData) {
        super();
        UserPreference prefProjectCode = Preferences.getUserPreference("grantCode", "tasks", null);
        _userCodePref = prefProjectCode.getValue();
        String tmpCode = getProjectCodeWidgetText(_blastData);
        if (null==tmpCode || "".equals(tmpCode)){
            tmpCode = _userCodePref;
        }
        init(tmpCode);
    }

    private String getProjectCodeWidgetText(BlastData _blastData) {
        String projectCodeText = null;
        if (_blastData != null
                && StringUtils.hasValue(_blastData.getTaskIdFromParam())
                && _blastData.getBlastTask() != null) {
            projectCodeText = _blastData.getBlastTask().getParameter(Task.PARAM_project);
        }
        return projectCodeText;
    }

    private void init(String projectCodeText) {
        // avoid potential null pointer issues by re-assigning null strings a default
        String suggestText = (projectCodeText != null) ? projectCodeText : "";

        _suggestOracle = new MatchesAnywhereSuggestOracle();
        _dataservice.getProjectCodes(new AsyncCallback() {
            public void onFailure(Throwable caught) {
                _logger.error("Error retrieving project codes: " + ((caught == null) ? "" : caught.getMessage()));
            }

            public void onSuccess(Object result) {
                HashSet<String> codes = (HashSet<String>) result;
                _suggestOracle.addAll(codes);
                String suggestText = (_userCodePref != null) ? _userCodePref : "";
                if (_suggestOracle.hasItem(suggestText)) {
                    _projectCodeSuggestBox.setText(suggestText);
                }
            }
        });
        _projectCodeSuggestBox = new SuggestBox(_suggestOracle);
        if (_suggestOracle.hasItem(suggestText)) {
            _projectCodeSuggestBox.setText(suggestText);
        }
        add(_projectCodeSuggestBox);
        add(new ExternalLink("Find Great Plains project codes (Column C)", SystemProps.getString("Grid.ProjectCodeLookupURL", "")));
    }

    public boolean isCurrentProjectCodeValid() {
        return _suggestOracle.hasItem(getProjectCode());
    }
}
