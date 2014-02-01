package org.janelia.it.jacs.model.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.graph.entity.support.EntityGraphObjectFactory;
import org.reflections.ReflectionUtils;

/**
 * Utility methods to help with common reflection tasks.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ReflectionHelper {

    private static final Logger log = Logger.getLogger(EntityGraphObjectFactory.class);

    private static final Class[] EMPTY_ARGS_TYPES = {};
    private static final Object[] EMPTY_ARGS_VALUES = {};
    
    public static Method getMethod(Object obj, String methodName) {
        Method matchedMethod = null;
        for(Method method : ReflectionUtils.getAllMethods(obj.getClass(), ReflectionUtils.withName(methodName))) {
            if (matchedMethod!=null) {
                throw new IllegalStateException("Found multiple methods with name "+methodName+" on "+obj.getClass().getName());
            }
            matchedMethod = method;
        }
        return matchedMethod;
    }

    public static Collection<Method> getMethods(Object obj, String methodName) {
        return ReflectionUtils.getAllMethods(obj.getClass(), ReflectionUtils.withName(methodName));
    }

    public static List<Field> getFields(Object obj) {
        return new ArrayList<Field>(ReflectionUtils.getAllFields(obj.getClass()));
    }
    
    public static Field getField(Object obj, Class<? extends Annotation> annotationClass) throws NoSuchFieldException {
        Field matchedField = null;
        for(Field field : ReflectionUtils.getAllFields(obj.getClass(), ReflectionUtils.withAnnotation(annotationClass))) {
            if (matchedField!=null) {
                throw new IllegalStateException("Found multiple fields with annotation "+annotationClass.getName()+" on "+obj.getClass().getName());
            }
            matchedField = field;
        }
        if (matchedField==null) {
            throw new NoSuchFieldException("Field with annotation "+annotationClass.getName()+" does not exist on "+obj.getClass().getName());
        }
        return matchedField;
    }

    public static Collection<Field> getFields(Object obj, Class<? extends Annotation> annotationClass) {
        List<Field> matchedFields = new ArrayList<Field>();
        for(Field field : ReflectionUtils.getAllFields(obj.getClass(), ReflectionUtils.withAnnotation(annotationClass))) {
            matchedFields.add(field);
        }
        return matchedFields;
    }
    
    public static Object getFieldValue(Object obj, String fieldName) throws NoSuchFieldException {
        return getFieldValue(obj, getField(obj, fieldName)); 
    }
    
    public static void setFieldValue(Object obj, String fieldName, Object value) throws NoSuchFieldException {
        setFieldValue(obj, getField(obj, fieldName), value); 
    }
    
    public static Field getField(Object obj, String fieldName) throws NoSuchFieldException {
        Field matchedField = null;
        final Set<Field> fields = ReflectionUtils.getAllFields(obj.getClass(), ReflectionUtils.withName(fieldName));
        for (Field field : fields) {
            if (matchedField!=null) {
                throw new IllegalStateException("Found multiple fields with name "+fieldName+" on "+obj.getClass().getName());
            }
            matchedField = field;
        }
        if (matchedField==null) {
            throw new NoSuchFieldException("Field with name '"+fieldName+"' does not exist on "+obj.getClass().getName());
        }
        return matchedField;
    }

    public static void setFieldValue(Object obj, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(obj, value);
        }
        catch (IllegalAccessException e) {
            // shouldn't happen since getField sets the field accessible
            throw new RuntimeException("Field cannot be accessed",e);
        }
    }

    public static Object getFieldValue(Object obj, Field field) {
        try {
            field.setAccessible(true);
            return field.get(obj);
        }
        catch (IllegalAccessException e) {
            // shouldn't happen since getField sets the field accessible
            throw new RuntimeException("Field cannot be accessed",e);
        }
    }
    
    /**
     * Get the given attribute from the specified object, using the public getter method.
     */
    public static Object getUsingGetter(Object obj, String fieldName) 
            throws Exception {
        String methodName = getAccessor("get", fieldName);
        return obj.getClass().getMethod(methodName, EMPTY_ARGS_TYPES).invoke(
            obj, EMPTY_ARGS_VALUES);
    }

    /**
     * Set the given attribute on the specified object, using the public setter method.
     */
    public static void setUsingSetter(Object obj, String fieldName, Object value) 
            throws Exception {
        Class[] argTypes = {value.getClass()};
        Object[] argValues = {value};
        String methodName = getAccessor("set", fieldName);
        obj.getClass().getMethod(methodName, argTypes).invoke(obj, argValues);
    }
    
    private static String getAccessor(String prefix, String fieldName) throws NoSuchMethodException {
        String firstChar = fieldName.substring(0, 1).toUpperCase();
        return prefix+firstChar+fieldName.substring(1);
    }
}
