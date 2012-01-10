
package org.janelia.it.jacs.model.common;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;
import org.janelia.it.jacs.model.vo.*;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Dec 4, 2006
 * Time: 2:28:47 PM
 */

public class ParameterVOMapUserType extends HashMap<String, ParameterVO> implements UserType {
    private static final String LSEP = "<PVOMUT.L>"; // top-level list of ParameterVO separator
    private static final String DSEP = "<PVOMUT.D>"; // separator for data
    private static final String VSEP = "<PVOMUT.V>"; // separator for arguments for VO constructors
    private static final String CSEP = "<PVOMUT.C>"; // separator for collections

    public ParameterVOMapUserType() {
    }

    private static final int[] SQL_TYPES = {Types.LONGVARCHAR};

    public int[] sqlTypes() {
        return SQL_TYPES;
    }

    public Class returnedClass() {
        return Map.class;
    }

    public boolean equals(Object x, Object y) {
        return x == y || !(x == null || y == null) && x.equals(y);
    }

    public Object deepCopy(Object value) {
        return value;
    }

    public boolean isMutable() {
        return false;
    }

    public Object nullSafeGet(ResultSet resultSet,
                              String[] names,
                              Object owner) throws HibernateException, SQLException {
        String value = resultSet.getString(names[0]);
        if (resultSet.wasNull()) {
            return null;
        }
        return new ParameterVOMapUserType(value);
    }

    public void nullSafeSet(PreparedStatement statement,
                            Object value,
                            int index)
            throws HibernateException, SQLException {
        if (value == null) {
            statement.setNull(index, Types.LONGVARCHAR);
        }
        else {
            String parameterString = getStringValue((Map) value);
            statement.setString(index, parameterString);
        }
    }

    private ParameterVOMapUserType(String value) throws HibernateException {
        try {
            populateMapWithPVOs(this, value);
        }
        catch (Throwable t) {
            t.printStackTrace();
            throw new HibernateException(t.getMessage(), t);
        }
    }

    public static void populateMapWithPVOs(Map<String, ParameterVO> map, String value) throws Exception {
        if (null == value || "".equals(value)) {
            return;
        }
        String[] voListArr = value.split(LSEP);
        for (String aVoListArr : voListArr) {
            String[] voArr = aVoListArr.split(DSEP);
            String className = voArr[0];
            String key = voArr[1];
            String[] dArr = voArr[2].split(VSEP);
            ParameterVO pvo = null;
            if (className.equals(BooleanParameterVO.class.getName())) {
                pvo = new BooleanParameterVO(Boolean.parseBoolean(dArr[0]));
            }
            else if (className.equals(DoubleParameterVO.class.getName())) {
                if (dArr.length == 1 && dArr[0].trim().length() == 0) {
                    pvo = new DoubleParameterVO();
                }
                else if (dArr.length == 1 && dArr[0].trim().length() > 0) {
                    pvo = new DoubleParameterVO(new Double(dArr[0]));
                }
                else if (dArr.length == 3 && dArr[0].trim().length() > 0 && dArr[1].trim().length() > 0
                        && dArr[2].trim().length() > 0) {
                    pvo = new DoubleParameterVO(new Double(dArr[0]), new Double(dArr[1]), new Double(dArr[2]));
                }
                else {
                    StringBuffer sb = new StringBuffer();
                    for (int d = 0; d < dArr.length; d++) {
                        sb.append("dArr ").append(d).append(":").append(dArr[d]).append("\n");
                    }
                    throw new Exception("Could not parse dArr for " + className + ":\n" + sb.toString());
                }
            }
            else if (className.equals(LongParameterVO.class.getName())) {
                if (dArr.length == 1 && dArr[0].trim().length() == 0) {
                    pvo = new LongParameterVO();
                }
                else if (dArr.length == 3 && dArr[0].trim().length() > 0 && dArr[1].trim().length() > 0
                        && dArr[2].trim().length() > 0) {
                    pvo = new LongParameterVO(new Long(dArr[0]), new Long(dArr[1]), new Long(dArr[2]));
                }
                else {
                    StringBuffer sb = new StringBuffer();
                    for (int d = 0; d < dArr.length; d++) {
                        sb.append("dArr ").append(d).append(":").append(dArr[d]).append("\n");
                    }
                    throw new Exception("Could not parse dArr for " + className + ":\n" + sb.toString());
                }
            }
            else if (className.equals(MultiSelectVO.class.getName())) {
                if (dArr.length == 0 || dArr.length == 1 && dArr[0].trim().length() == 0) {
                    pvo = new MultiSelectVO();
                }
                else if (dArr.length == 1) {
                    ArrayList<String> choicesList = new ArrayList<String>();
                    if (dArr[0].trim().length() > 0) {
                        String[] choicesArr = dArr[0].split(CSEP);
                        choicesList = new ArrayList<String>(choicesArr.length);
                        for (int j = 0; j < choicesArr.length; j++) choicesList.add(j, choicesArr[j]);
                    }
                    pvo = new MultiSelectVO(new ArrayList<String>(), choicesList);
                }
                else if (dArr.length == 2) {
                    ArrayList<String> valuesList = new ArrayList<String>();
                    if (dArr[0].trim().length() > 0) {
                        String[] valuesArr = dArr[0].split(CSEP);
                        valuesList = new ArrayList<String>(valuesArr.length);
                        for (int j = 0; j < valuesArr.length; j++) valuesList.add(j, valuesArr[j]);
                    }
                    ArrayList<String> choicesList = new ArrayList<String>();
                    if (dArr[1].trim().length() > 0) {
                        String[] choicesArr = dArr[1].split(CSEP);
                        choicesList = new ArrayList<String>(choicesArr.length);
                        for (int j = 0; j < choicesArr.length; j++) choicesList.add(j, choicesArr[j]);
                    }
                    pvo = new MultiSelectVO(valuesList, choicesList);
                }
                else {
                    StringBuffer sb = new StringBuffer();
                    for (int d = 0; d < dArr.length; d++) {
                        sb.append("dArr ").append(d).append(":").append(dArr[d]).append("\n");
                    }
                    throw new Exception("Could not parse dArr for " + className + ":\n" + sb.toString());
                }
            }
            else if (className.equals(SingleSelectVO.class.getName())) {
                if (dArr.length == 0 || dArr.length == 1 && dArr[0].trim().length() == 0) {
                    pvo = new SingleSelectVO();
                }
                else if (dArr.length == 1) {
                    pvo = new SingleSelectVO(new ArrayList<String>(), dArr[0]);
                }
                else if (dArr.length == 2) {
                    ArrayList<String> valuesList = new ArrayList<String>();
                    if (dArr[0].trim().length() > 0) {
                        String[] valuesArr = dArr[0].split(CSEP);
                        valuesList = new ArrayList<String>(valuesArr.length);
                        for (int j = 0; j < valuesArr.length; j++) valuesList.add(j, valuesArr[j]);
                    }
                    pvo = new SingleSelectVO(valuesList, dArr[1]);
                }
                else {
                    StringBuffer sb = new StringBuffer();
                    for (int d = 0; d < dArr.length; d++) {
                        sb.append("dArr ").append(d).append(":").append(dArr[d]).append("\n");
                    }
                    throw new Exception("Could not parse dArr for " + className + ":\n" + sb.toString());
                }
            }
            else if (className.equals(TextParameterVO.class.getName())) {
                if (dArr.length == 1 && dArr[0].trim().length() == 0) {
                    pvo = new TextParameterVO();
                }
                else if (dArr.length == 2 && dArr[1].trim().length() > 0) {
                    pvo = new TextParameterVO(dArr[0], Integer.parseInt(dArr[1]));
                }
                else {
                    StringBuffer sb = new StringBuffer();
                    for (int d = 0; d < dArr.length; d++) {
                        sb.append("dArr ").append(d).append(":").append(dArr[d]).append("\n");
                    }
                    throw new Exception("Could not parse dArr for " + className + ":\n" + sb.toString());
                }
            }

            if (pvo == null) throw new HibernateException("Did not recognize class " + className +
                    " or could not find constructor match for value string:" + value);
            map.put(key, pvo);
        }
    }

    private String getStringValue(Map map) throws HibernateException {
        StringBuffer sb = new StringBuffer();
        Set keySet = map.keySet();
        Iterator iter = keySet.iterator();
        while (iter.hasNext()) {
            Object key = iter.next();
            Object o = map.get(key);
            if (o instanceof BooleanParameterVO) {
                sb.append(BooleanParameterVO.class.getName());
                sb.append(DSEP);
                BooleanParameterVO bPvo = (BooleanParameterVO) o;
                sb.append(key);
                sb.append(DSEP);
                sb.append(bPvo.getStringValue());
            }
            else if (o instanceof DoubleParameterVO) {
                sb.append(DoubleParameterVO.class.getName());
                sb.append(DSEP);
                DoubleParameterVO dPvo = (DoubleParameterVO) o;
                sb.append(key);
                sb.append(DSEP);
                sb.append(dPvo.getMinValue().toString());
                sb.append(VSEP);
                sb.append(dPvo.getMaxValue().toString());
                sb.append(VSEP);
                sb.append(dPvo.getActualValue().toString());
            }
            else if (o instanceof LongParameterVO) {
                sb.append(LongParameterVO.class.getName());
                sb.append(DSEP);
                LongParameterVO lPvo = (LongParameterVO) o;
                sb.append(key);
                sb.append(DSEP);
                sb.append(lPvo.getMinValue().toString());
                sb.append(VSEP);
                sb.append(lPvo.getMaxValue().toString());
                sb.append(VSEP);
                sb.append(lPvo.getActualValue().toString());
            }
            else if (o instanceof MultiSelectVO) {
                sb.append(MultiSelectVO.class.getName());
                sb.append(DSEP);
                MultiSelectVO mPvo = (MultiSelectVO) o;
                sb.append(key);
                sb.append(DSEP);
                List valueList = mPvo.getPotentialChoices();
                List choiceList = mPvo.getActualUserChoices();
                for (int i = 0; i < valueList.size(); i++) {
                    sb.append(valueList.get(i));
                    if (i != valueList.size() - 1) sb.append(CSEP);
                }
                sb.append(VSEP);
                for (int i = 0; i < choiceList.size(); i++) {
                    sb.append(choiceList.get(i));
                    if (i != choiceList.size() - 1) sb.append(CSEP);
                }
            }
            else if (o instanceof SingleSelectVO) {
                sb.append(SingleSelectVO.class.getName());
                sb.append(DSEP);
                SingleSelectVO sPvo = (SingleSelectVO) o;
                sb.append(key);
                sb.append(DSEP);
                List valueList = sPvo.getPotentialChoices();
                for (int i = 0; i < valueList.size(); i++) {
                    sb.append(valueList.get(i));
                    if (i != valueList.size() - 1) sb.append(CSEP);
                }
                sb.append(VSEP);
                sb.append(sPvo.getActualUserChoice());
            }
            else if (o instanceof TextParameterVO) {
                sb.append(TextParameterVO.class.getName());
                sb.append(DSEP);
                TextParameterVO tPvo = (TextParameterVO) o;
                sb.append(key);
                sb.append(DSEP);
                sb.append(tPvo.getTextValue());
                sb.append(VSEP);
                sb.append(tPvo.getMaxLength());
            }
            else {
                throw new HibernateException("Do not recognize object " + o.getClass().getName());
            }
            if (iter.hasNext()) sb.append(LSEP);
        }
        return sb.toString();
    }

    public int hashCode(Object x) {
        return x.hashCode();
    }

    public Serializable disassemble(Object object) throws org.hibernate.HibernateException {
        return (Serializable) object;
    }

    public Object assemble(Serializable serializable, Object object) throws org.hibernate.HibernateException {
        return serializable;
    }

    public Object replace(Object object, Object object1, Object object2) throws org.hibernate.HibernateException {
        return object;
    }

}
