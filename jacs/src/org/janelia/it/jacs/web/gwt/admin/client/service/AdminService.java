
package org.janelia.it.jacs.web.gwt.admin.client.service;

import com.google.gwt.user.client.rpc.RemoteService;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 2, 2006
 * Time: 3:51:05 PM
 */
public interface AdminService extends RemoteService {
    public String createUser(String login, String name);

    public ArrayList<String> getDiskUsageReport();
}
