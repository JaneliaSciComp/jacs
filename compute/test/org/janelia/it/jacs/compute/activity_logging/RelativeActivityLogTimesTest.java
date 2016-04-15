/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.activity_logging;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import static org.junit.Assert.*;

/**
 *
 * @author fosterl
 */
public class RelativeActivityLogTimesTest {
    public static final String TAB_SEP = "\\t";
    public static final String GINA_EXAMPLE_TXT = "c:\\Users\\fosterl\\Documents\\ActivityLogging\\tool_activity_blaker_xyasw_04012016.txt";
    public static final String LF_DEV_EXAMPLE_TXT = "c:\\Users\\fosterl\\Documents\\ActivityLogging\\tool_activity_lfosterDev_xyzsw_04012016.txt";

    public RelativeActivityLogTimesTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
    @Test
    public void examineRecords() throws Exception {
        // id      session_id      user_login      tool_name       category        action   event_time
        BufferedReader br = new BufferedReader(new FileReader( LF_DEV_EXAMPLE_TXT ));
        String inline = null;
        inline = br.readLine();
        String[] headers = inline.split(TAB_SEP);
        Record prevRecord = null;
        System.out.println("Category\tAction\tUser\tTool\tTimestamp\tDelta-t");
        while (null != (inline = br.readLine())) {
            Record record = parseRecord(inline, headers);            
            if (prevRecord != null  &&  record.inSequence(prevRecord)) {
                Date recDate = record.getEventTime();
                Date prevDate = prevRecord.getEventTime();
                long secDelta = recDate.getTime() - prevDate.getTime();
                System.out.println(
                        String.format("%s\t%s\t%s\t%s\t%s\t%d",
                            record.getCategory(), record.getAction(), record.getUserLogin(), record.getToolName(), record.getEventTimeStr(), secDelta / 1000
                        )
                );
            }
            
            prevRecord = record;
        }
        br.close();
    }

    private Record parseRecord(String inline, String[] headers) throws NumberFormatException, ParseException {
        String[] fields = inline.split(TAB_SEP);
        Record record = new Record();
        Calendar cal = Calendar.getInstance();
        //2016-04-04 09:44:11
        SimpleDateFormat dateFormat = new SimpleDateFormat();
        dateFormat.applyPattern("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < fields.length; i++) {
            fields[i] = cleanup(fields[i]);
            if (headers[i].equals("event_time")) {
                try {
                    record.setEventTimeStr(fields[i].trim());
                    record.setEventTime(dateFormat.parse(fields[i]));
                } catch (ParseException dfe) {
                    dfe.printStackTrace();
                    throw dfe;
                }
            }
            else if (headers[i].equals("id")) {
                record.setId(Long.parseLong(fields[i]));
            }
            else if (headers[i].equals("session_id")) {
                record.setSessionId(Long.parseLong(fields[i]));
            }
            else if (headers[i].equals("user_login")) {
                record.setUserLogin(fields[i]);
            }
            else if (headers[i].equals("tool_name")) {
                record.setToolName(fields[i]);
            }
            else if (headers[i].equals("category")) {
                record.setCategory(fields[i]);
            }
            else if (headers[i].equals("action")) {
                record.setAction(fields[i]);
            }
        }
        return record;
    }
    
    private String cleanup( String field ) {
        char[] fieldChars = field.toCharArray();
        int startPtr = 0;
        if (fieldChars[0] == '"') {
            startPtr ++;
        }
        int endPtr = fieldChars.length - 1;
        if (fieldChars[endPtr] == '"') {
            endPtr --;
        }
        return field.substring(startPtr, endPtr + 1);
    }
    
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
    
    private class Record {
        //id      session_id      user_login      tool_name       category        action    event_time
        private Date eventTime;
        private Long id;
        private Long sessionId;
        private String userLogin;
        private String category;
        private String action;
        private String toolName;
        private String eventTimeStr;

        /**
         * @return the eventTime
         */
        public Date getEventTime() {
            return eventTime;
        }

        /**
         * @param eventTime the eventTime to set
         */
        public void setEventTime(Date eventTime) {
            this.eventTime = eventTime;
        }

        /**
         * @return the id
         */
        public Long getId() {
            return id;
        }

        /**
         * @param id the id to set
         */
        public void setId(Long id) {
            this.id = id;
        }

        /**
         * @return the userLogin
         */
        public String getUserLogin() {
            return userLogin;
        }

        /**
         * @param userLogin the userLogin to set
         */
        public void setUserLogin(String userLogin) {
            this.userLogin = userLogin;
        }

        /**
         * @return the category
         */
        public String getCategory() {
            return category;
        }

        /**
         * @param category the category to set
         */
        public void setCategory(String category) {
            this.category = category;
        }

        /**
         * @return the action
         */
        public String getAction() {
            return action;
        }

        /**
         * @param action the action to set
         */
        public void setAction(String action) {
            this.action = action;
        }

        /**
         * @return the sessionId
         */
        public Long getSessionId() {
            return sessionId;
        }

        /**
         * @param sessionId the sessionId to set
         */
        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }

        /**
         * @return the toolName
         */
        public String getToolName() {
            return toolName;
        }

        /**
         * @param toolName the toolName to set
         */
        public void setToolName(String toolName) {
            this.toolName = toolName;
        }
        
        public boolean inSequence(Record other) {
            return other.userLogin.equals(userLogin)  &&
                   other.sessionId.equals(sessionId);
        }

        /**
         * @return the eventTimeStr
         */
        public String getEventTimeStr() {
            return eventTimeStr;
        }

        /**
         * @param eventTimeStr the eventTimeStr to set
         */
        public void setEventTimeStr(String eventTimeStr) {
            this.eventTimeStr = eventTimeStr;
        }
    }
}
