package org.janelia.it.jacs.shared.utils;

import org.janelia.it.jacs.model.user_data.Group;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import java.util.TreeMap;

/**
 * Created by IntelliJ IDEA.
 * Used this for 2 years in a row.  DO NOT remove.  :-)
 * User: saffordt
 * Date: 1/5/12
 * Time: 8:14 PM
 */
public class LoginParser {

    public static void main(String[] args) {
        try {
            File rootFile = new File("/groups/jacs/jacsHosts/servers/jacs/jboss-4.2.3.GA/server/default/log/usersFY15.txt");
            Scanner scanner = new Scanner(rootFile);
            TreeMap<String, ArrayList<String>> userMap = new TreeMap<String, ArrayList<String>>();
            TreeMap<String, ArrayList<String>> usersByMonthMap = new TreeMap<String, ArrayList<String>>();

            ArrayList<String> excludedLogins = new ArrayList<String>();
            excludedLogins.add("saffordt");
            excludedLogins.add("brunsc");
            excludedLogins.add("olbrisd");
            excludedLogins.add("trautmane");
            excludedLogins.add("yuy");
            excludedLogins.add("rokickik");
            excludedLogins.add("murphys");
            excludedLogins.add("svirskasr");
            excludedLogins.add("parvathama");
            excludedLogins.add("fosterl");
            excludedLogins.add("carlilek");
            excludedLogins.add("bukowinskip");
            excludedLogins.add("zhaot");
            excludedLogins.add("taylora");

            while (scanner.hasNextLine()){
                String tmpLine = scanner.nextLine();
                String[] pieces = tmpLine.substring(tmpLine.indexOf(":")+1).split(" ");
                if (!excludedLogins.contains(pieces[8])&&!pieces[8].toLowerCase().startsWith(Group.ADMIN_GROUP_NAME)) {
                    // Manage the unique users
                    if (userMap.containsKey(pieces[8])) {
                        userMap.get(pieces[8]).add(pieces[0]+" "+pieces[1]);
                    }
                    else {
                        ArrayList<String> tmpList = new ArrayList<String>();
                        tmpList.add(pieces[0]+" "+pieces[1]);
                        userMap.put(pieces[8], tmpList);
                    }

                    //Manage the unique users by month data
                    String tmpMonth = pieces[0].substring(0, pieces[0].lastIndexOf("-"));
                    if (usersByMonthMap.containsKey(tmpMonth)) {
                        ArrayList<String> tmpUserList = usersByMonthMap.get(tmpMonth);
                        if (!tmpUserList.contains(pieces[8])) {
                            tmpUserList.add(pieces[8]);
                        }
                    }
                    else {
                        ArrayList<String> newList = new ArrayList<String>();
                        newList.add(pieces[8]);
                        usersByMonthMap.put(tmpMonth,newList);
                    }
                }
            }
            System.out.println("There are over "+userMap.size()+" distinct users of the Workstation.");
            System.out.println("Unique users are currently hidden by \"Lab Accounts\".");
            // Print out the digested user data
            System.out.println("They are:");
            for (String key : userMap.keySet()) {
                ArrayList tmpList = userMap.get(key);
                StringBuilder builder = new StringBuilder();
                builder.append(key).append("\t{logins:").append(tmpList.size()).append(", last login ").
                        append(tmpList.get(tmpList.size()-1)).append("}");
                System.out.println(builder.toString());
            }

            // Print out the unique users per month data
            System.out.println("");
            System.out.println("Here is the number of distinct users by Year-Month:");
            System.out.println("Date\t\tUsers\tUser List");
            for (String month : usersByMonthMap.keySet()) {
                ArrayList<String> tmpUsers = usersByMonthMap.get(month);
                Collections.sort(tmpUsers);
                String prefix = month+"\t\t"+tmpUsers.size()+"\t\t";
                System.out.println(prefix+tmpUsers.toString());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
