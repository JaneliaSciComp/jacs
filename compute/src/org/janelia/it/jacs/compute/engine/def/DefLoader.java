
package org.janelia.it.jacs.compute.engine.def;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.shared.utils.FileUtil;

/**
 * This class is responsible for loading and initilaizing the process definition based on the .process xml
 * process definition file provided in the classpath.  At the moment it shamelessly performs validations manually.
 * In the future, we should do it through an XML schema
 *
 * @author Tareq Nabeel
 */
public class DefLoader implements Serializable {

    private static final String PROCESS_ELE = "process";
    private static final String INCLUDE_ELE = "include";
    private static final String SEQUENCE_ELE = "sequence";
    private static final String OPERATION_ELE = "operation";
    private static final String EXCEPTION_HANDLER_ELE = "exceptionHandler";
    private static final String NAME_ATTR = "name";
    private static final String PROCESSOR_ATTR = "processor";
    private static final String PROCESSOR_TYPE_ATTR = "processorType";
    private static final String WAIT_FOR_ASYNC_ATTR = "waitForAsync";
    private static final String FOR_EACH_ATTR = "forEach";
    private static final String LOOP_UNTIL_ATTR = "loopUntil";
    private static final String IF_ATTR = "if";
    private static final String ASYNC_ATTR = "async";
    private static final String QUEUE_TO_LINK_FROM = "queueToLinkFrom";
    private static final String QUEUE_TO_LINK_TO = "queueToLinkTo";
    private static final String INPUT_ELE = "input";
    private static final String OUTPUT_ELE = "output";
    private static final String DATA_TYPE_ATTR = "dataType";
    private static final String VALUE_ATTR = "value";
    private static final String ENTRY_ELE = "entry";
    private static final String KEY_ATTR = "key";
    private static final String MANDATORY_ATTR = "mandatory";
    private static final String UPDATE_PROCESS_STATUS = "updateProcessStatus";
    private static final String MAX_JMS_WAIT_TIME = "maxJMSWaitTime";
    private static final String HALT_ON_ERROR = "haltOnError";
    private static final String START_EVENT = "startEvent";
    private static final String PROCESS_NAME_ATTR = "process";

    /**
     * This method loads the process defintion specified by <code>processName</code>
     *
     * @param processName the name of the process file on the classpath
     * @return a fully initialized ProcessDef instance
     */
    public ProcessDef loadProcessDef(String processName) {
        try {
            InputStream contentStream = FileUtil.getResourceAsStream(processName + ".process");
            Document defDocument = new SAXReader().read(contentStream);
            Element processElement = defDocument.getRootElement();
            if (!PROCESS_ELE.equals(processElement.getName())) {
                throw new UnexpectedElementException(processElement.getName() + "\n" + processElement.asXML());
            }
            return createProcessDef(processElement, null);
        }
        catch (DocumentException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Initializes the process definition using supplied "process" element
     *
     * @param processElement   process piece from the XML process file
     * @param parentProcessDef parent process defintion, if one exists
     * @return returns the process definition which comes from the element passed in
     */
    private ProcessDef createProcessDef(Element processElement, ProcessDef parentProcessDef) {
        ProcessDef processDef = new ProcessDef(parentProcessDef);
        initSeriesDef(processDef, processElement);
        // todo We don't do anything with attributes?
//        List attributes = processElement.attributes();
//        if (null != attributes) {
//            for (Object attribute : attributes) {
//                Attribute att = (Attribute) attribute;
//            }
//        }
        List children = processElement.elements();
        if (children != null) {
            for (Object o : children) {
                Element child = (Element) o;
                if (SEQUENCE_ELE.equals(child.getName())) {
                    processDef.addChildDef(createSequenceDef(child, processDef));
                }
                else if (PROCESS_ELE.equals(child.getName())) {
                    processDef.addChildDef(createProcessDef(child, processDef));
                }
                else {
                    throw new UnexpectedElementException(child.getName());
                }
            }
        }
        return processDef;
    }

    /**
     * Initializes the sequence definition using the "sequence" element
     *
     * @param sequenceElement sequence defined in the process file
     * @param parentSeriesDef parent series definition from the process file
     * @return returns the sequence definition
     */
    private SequenceDef createSequenceDef(Element sequenceElement, SeriesDef parentSeriesDef) {
        SequenceDef sequenceDef = new SequenceDef(parentSeriesDef);
        initSeriesDef(sequenceDef, sequenceElement);

        if (sequenceDef.getProcessIfCondition()!=null) {
        	String name = sequenceDef.getProcessIfCondition().getName();
            sequenceDef.getLocalInputParameters().add(new Parameter(name, null, null, false));
        }

        if (sequenceDef.getForEachParam()!=null) {
        	String name = sequenceDef.getForEachParam();
            sequenceDef.getLocalInputParameters().add(new Parameter(name, null, null, false));
        }
        
        List<Element> inputs = new ArrayList<Element>();
        List<Element> outputs = new ArrayList<Element>();
        
        List children = sequenceElement.elements();
        if (children != null) {
            for (Object o : children) {
                Element child = (Element) o;
                if (OPERATION_ELE.equals(child.getName())) {
                    sequenceDef.addChildDef(createOperationDef(child, sequenceDef));
                }
                else if (SEQUENCE_ELE.equals(child.getName())) {
                    sequenceDef.addChildDef(createSequenceDef(child, sequenceDef));
                }
                else if (INCLUDE_ELE.equals(child.getName())) {
                	sequenceDef.addChildDef(createSequenceDefViaInclude(child, sequenceDef));
                }
                else if (EXCEPTION_HANDLER_ELE.equals(child.getName())) {
                	sequenceDef.setExceptionHandlerDef(createSequenceDef(child, sequenceDef));
                }
                else if (INPUT_ELE.equals(child.getName())) {
            		inputs.add(child);
                }
                else if (INPUT_ELE.equals(child.getName()) ){
                	outputs.add(child);
                }
                else {
                    throw new UnexpectedElementException(child.getName());
                }
            }
        }
        
        addParameters(sequenceDef.getLocalInputParameters(), inputs, ParameterType.INPUT);
        addParameters(sequenceDef.getLocalOutputParameters(), outputs, ParameterType.OUTPUT);
        
        return sequenceDef;
    }

    /**
     * Initializes the sequence definition using the "include" element
     *
     * @param operationElement  operation defined in the process file
     * @param parentSequenceDef parent sequence definition from the process file
     * @return returns the operation definition
     */
    private SequenceDef createSequenceDefViaInclude(Element includeElement, SeriesDef parentSeriesDef) {
    	
    	String processName = includeElement.attributeValue(PROCESS_NAME_ATTR);
    	ProcessDef processDef = loadProcessDef(processName);
    	
        SequenceDef sequenceDef = new SequenceDef(parentSeriesDef);
        initSeriesDef(sequenceDef, includeElement);
        sequenceDef.setIncluded(true);
        
    	for(ActionDef actionDef : processDef.getChildActionDefs()) {
            sequenceDef.addChildDef(actionDef);
    	}

        addParameters(sequenceDef.getLocalInputParameters(), includeElement.elements(INPUT_ELE), ParameterType.INPUT);
        addParameters(sequenceDef.getLocalOutputParameters(), includeElement.elements(OUTPUT_ELE), ParameterType.OUTPUT);
        
        return sequenceDef;
    }
    
    /**
     * Initializs the operation definition using the "operation" element
     *
     * @param operationElement  operation defined in the process file
     * @param parentSequenceDef parent sequence definition from the process file
     * @return returns the operation definition
     */
    private OperationDef createOperationDef(Element operationElement, SequenceDef parentSequenceDef) {
        OperationDef operationDef = new OperationDef(parentSequenceDef);
        initActionDef(operationDef, operationElement);
        if (!VALID_OPERATION_ATTRIBUTES.containsAll(extractAttributeNames(operationElement.attributes()))) {
            throw new UnexpectedAttributeException("\n" + operationElement.asXML());
        }
        if (operationDef.getProcessorName() == null) {
            if (operationDef.getQueueToLinkFrom() == null) {
                throw new MandatoryAttributeMissingException("processor missing in operation: " + operationDef.getName() + "\n" + operationElement.asXML());
            }
        }
        else if ("true".equalsIgnoreCase(operationElement.attributeValue(ASYNC_ATTR))) {
            operationDef.setProcessorType(ProcessorType.LOCAL_MDB);
        }
        else {
            setProcessorTypeBasedOnProcessorName(operationDef);
        }
        addParameters(operationDef.getInputParameters(), operationElement.elements(INPUT_ELE), ParameterType.INPUT);
        addParameters(operationDef.getOutputParameters(), operationElement.elements(OUTPUT_ELE), ParameterType.OUTPUT);
        return operationDef;
    }

    /**
     * Adds input and output parameters specified within the "operation" element to the corresponding
     * parameters collection in the Operation definition
     *
     * @param targetSet     set of input and output parameters (from the process file) relating to an element
     * @param elements      list of elements to get parameters from
     * @param parameterType type of parameter (input/output)
     */
    private void addParameters(Set<Parameter> targetSet, List elements, ParameterType parameterType) {
        if (elements != null) {
            for (Object o : elements) {
                Element element = (Element) o;
                String name = element.attributeValue(NAME_ATTR);
                String strValue = element.attributeValue(VALUE_ATTR);
                String datatype = element.attributeValue(DATA_TYPE_ATTR);
                Boolean mandatory = Boolean.valueOf(element.attributeValue(MANDATORY_ATTR));
                
                Object value = null;
                if (strValue!=null) {
                    value = createParameterValue(datatype, strValue);
                    addReferencedParameters(targetSet, strValue, parameterType);
                }
                else {
                    // There is no string value, so check for other data types
                    Map<String,String> map = new HashMap<>();
                    for (Object o2 : element.elements(ENTRY_ELE)) {
                        Element entryEle = (Element) o2;
                        String entryKey = entryEle.attributeValue(KEY_ATTR);
                        String entryValue = entryEle.attributeValue(VALUE_ATTR);
                        if (entryKey==null) {
                            throw new MandatoryAttributeMissingException("Missing 'key' attribute on map entry for parameter '"+name+"'");
                        }
                        if (entryValue==null) {
                            throw new MandatoryAttributeMissingException("Missing 'value' attribute on map entry for parameter '"+name+"'");
                        }
                        if (entryValue.startsWith("$V{")) {
                            throw new UnexpectedAttributeException("Variable interpolation ($V{}) is not yet supported for map entry values");
                        }
                        // TODO: uncomment this once we add support for interpolation
                        //addReferencedParameters(targetSet, entryValue, parameterType);
                        map.put(entryKey, entryValue);
                    }
                    if (!map.isEmpty()) {
                        value = map;
                    }
                }
                
                Parameter parameter = new Parameter(name, value, parameterType, mandatory);
                setParameter(targetSet, parameter);
            }
        }
    }
    
    private void addReferencedParameters(Set<Parameter> targetSet, String value, ParameterType parameterType) {
        if (value!=null && value.startsWith("$V{")) {
            // Also add the referenced variable
            String name = value.substring(value.indexOf("$V{") + 3, value.length() - 1);
            Parameter parameter = new Parameter(name, null, parameterType, false);
            setParameter(targetSet, parameter);
        }                
    }
    
    /**
     * Set a parameter in the given set, overriding any current parameters with that name.
     * @param parameterSet
     * @param parameter
     */
    private void setParameter(Set<Parameter> parameterSet, Parameter parameter) {

    	Set<Parameter> existingSet = new HashSet<Parameter>();
    	for(Parameter p : parameterSet) {
    		if (p.getName().equals(parameter.getName())) {
    			existingSet.add(p);
    		}
    	}             
    	
    	for(Parameter existing : existingSet) {
    		parameterSet.remove(existing);
    	}
    	
    	parameterSet.add(parameter);
    }

    private Object createParameterValue(String dataType, String value) {
        if (value == null || dataType == null) {
            return value;
        }
        if (dataType.equals("long")) {
            return Long.valueOf(value);
        }
        else if (dataType.equals("int")) {
            return Integer.valueOf(value);
        }
        else if (dataType.equals("float")) {
            return Float.valueOf(value);
        }
        else if (dataType.equals("double")) {
            return Double.valueOf(value);
        }
        else if (dataType.equals("boolean")) {
            return Boolean.valueOf(value);
        }
        else if (dataType.equals("file")) {
            return new File(value);
        }
        else {
            return value;
        }
    }


    /**
     * Initializes the base attributes of an Action definition
     *
     * @param actionDef     action definition to initialize
     * @param actionElement elements which describe the action
     */
    private void initActionDef(ActionDef actionDef, Element actionElement) {
        actionDef.setName(actionElement.attributeValue(NAME_ATTR));
        actionDef.setProcessorName(actionElement.attributeValue(PROCESSOR_ATTR));
        actionDef.setQueueToLinkFrom(actionElement.attributeValue(QUEUE_TO_LINK_FROM));
        actionDef.setQueueToLinkTo(actionElement.attributeValue(QUEUE_TO_LINK_TO));
        actionDef.setProcessIfCondition(Condition.create(actionElement.attributeValue(IF_ATTR)));
        actionDef.setForEachParam(actionElement.attributeValue(FOR_EACH_ATTR));
        actionDef.setLoopUntilCondition(Condition.create(actionElement.attributeValue(LOOP_UNTIL_ATTR)));
        setStartEvent(actionDef, actionElement);
        setStatusUpdate(actionDef, actionElement);
        if (actionElement.attributeValue(HALT_ON_ERROR) != null)
            actionDef.setHaltProcessOnError(Boolean.valueOf(actionElement.attributeValue(HALT_ON_ERROR)));
    }

    private void setStartEvent(ActionDef actionDef, Element actionElement) {
        String startEvent = actionElement.attributeValue(START_EVENT);
        if (startEvent != null) {
            if (Event.isValid(startEvent)) {
                actionDef.setStartEvent(startEvent);
            }
            else {
                throw new IllegalArgumentException("Invalid status: " + startEvent + ". Valid status types include:" + Event.getValidStatuses());
            }
        }
    }

    private void setStatusUpdate(ActionDef actionDef, Element actionElement) {
        String statusUpdateType = actionElement.attributeValue(UPDATE_PROCESS_STATUS);
        if (statusUpdateType != null) {
            if (StatusUpdate.NEVER.toString().equalsIgnoreCase(statusUpdateType)) {
                actionDef.setStatusUpdateType(StatusUpdate.NEVER);
            }
            else if (StatusUpdate.ON_FAILURE.toString().equalsIgnoreCase(statusUpdateType)) {
                actionDef.setStatusUpdateType(StatusUpdate.ON_FAILURE);
            }
            else if (StatusUpdate.ON_SUCCESS.toString().equalsIgnoreCase(statusUpdateType)) {
                actionDef.setStatusUpdateType(StatusUpdate.ON_SUCCESS);
            }
            else {
                throw new IllegalArgumentException("Invalid updateProcessStatus value:" + statusUpdateType + " valid values include: " + StatusUpdate.ON_SUCCESS + ", " + StatusUpdate.ON_FAILURE + ", and " + StatusUpdate.NEVER);
            }
        }
        else {
            actionDef.setStatusUpdateType(StatusUpdate.ON_FAILURE);
        }
    }

    /**
     * Initialzes attributes common to process and sequence defintions
     *
     * @param seriesDef     series definition
     * @param seriesElement series element to get values from
     */
    private void initSeriesDef(SeriesDef seriesDef, Element seriesElement) {
        initActionDef(seriesDef, seriesElement);
        if (!VALID_SERIES_ATTRIBUTES.containsAll(extractAttributeNames(seriesElement.attributes()))) {
            throw new UnexpectedAttributeException("\n" + seriesElement.asXML());
        }
        if ("true".equalsIgnoreCase(seriesElement.attributeValue(ASYNC_ATTR))) {
            seriesDef.setProcessorType(ProcessorType.LOCAL_MDB);
        }
        seriesDef.setWaitOnAsyncActions(Boolean.valueOf(seriesElement.attributeValue(WAIT_FOR_ASYNC_ATTR)));
        seriesDef.setMaxJMSWaitTime(getSeriesMaxWaitTime(seriesElement));
        setDefaultSeriesProcessor(seriesDef);
    }

    /**
     * Initializes the maximum wait time that a ProcessLauncher or SequenceLauncher would wait for the asynchronous
     * actions within it to complete.  Priority is given to value specified in process definition and then
     * to one specified in jacs.properties.  If no value is specified in jacs.properties, then 999999999 is used
     *
     * @param seriesElement series element which may contain the wait time value
     * @return returns the maximum wait time for this series
     */
    private long getSeriesMaxWaitTime(Element seriesElement) {
        String waitTime = seriesElement.attributeValue(MAX_JMS_WAIT_TIME);
        if (waitTime != null) {
            return new Long(waitTime);//override all other settings
        }
        else {
            return SystemConfigurationProperties.getLong("maxJMSWaitTime", 999999999);
        }
    }

    /**
     * Sets the default processor name and processor type to be used for a Series Launcher
     *
     * @param seriesDef the series definition being used
     */
    private void setDefaultSeriesProcessor(SeriesDef seriesDef) {
        if (seriesDef.getProcessorType() == null && seriesDef.getProcessorName() != null) {
            setProcessorTypeBasedOnProcessorName(seriesDef);
        }
        else if (seriesDef.getProcessorType() != null && seriesDef.getProcessorName() == null) {
            setProcessorNameBasedOnProcessorType(seriesDef);
        }
        else if (seriesDef.getProcessorType() == null && seriesDef.getProcessorName() == null) {
            seriesDef.setProcessorType(seriesDef.getDefaultProcessorType());
            setProcessorNameBasedOnProcessorType(seriesDef);
        }
        //neither are null, don't set any defaults
    }

    /**
     * Sets the processor type based on the supplied processor name
     *
     * @param actionDef the action definition being processed
     */
    private void setProcessorTypeBasedOnProcessorName(ActionDef actionDef) {
        if (actionDef.getProcessorName().endsWith("/local")) {
            actionDef.setProcessorType(ProcessorType.LOCAL_SLSB);
        }
        else if (actionDef.getProcessorName().startsWith("queue/")) {
            actionDef.setProcessorType(ProcessorType.LOCAL_MDB);
        }
        else {
            actionDef.setProcessorType(actionDef.getDefaultProcessorType());
        }
    }

    /**
     * Sets the processor name if processor type is supplied
     *
     * @param seriesDef the series to obtain the processor from
     */
    private void setProcessorNameBasedOnProcessorType(SeriesDef seriesDef) {
        switch (seriesDef.getProcessorType()) {
            case LOCAL_MDB:
                seriesDef.setProcessorName(seriesDef.getDefaultMdbProcessor());
                break;
            case POJO:
                seriesDef.setProcessorName(seriesDef.getDefaultPojoProcessor());
                break;
            case LOCAL_SLSB:
                seriesDef.setProcessorName(seriesDef.getDefaultLocalSlsbProcessor());
                break;
            default:
                throw new IllegalArgumentException("Unsupported processor type " + seriesDef.getProcessorType());
        }
    }

    //todo remove the code below after xml schema is complete and validation is done using xml schema !!!!!!!!!!!!!
    private class UnexpectedElementException extends RuntimeException {
        private UnexpectedElementException(String msg) {
            super(msg);
        }
    }

    private class UnexpectedAttributeException extends RuntimeException {
        private UnexpectedAttributeException(String msg) {
            super(msg);
        }
    }

    private class MandatoryAttributeMissingException extends RuntimeException {
        private MandatoryAttributeMissingException(String msg) {
            super(msg);
        }
    }

    private Set extractAttributeNames(List attributes) {
        Set<String> attributeNames = new HashSet<String>();
        for (Object attribute : attributes) {
            attributeNames.add(((Attribute) attribute).getName());
        }
        return attributeNames;
    }

    private static Set<String> VALID_SERIES_ATTRIBUTES = new HashSet<String>();
    private static Set<String> VALID_ACTION_ATTRIBUTES = new HashSet<String>();
    private static Set<String> VALID_OPERATION_ATTRIBUTES = new HashSet<String>();

    static {
        VALID_ACTION_ATTRIBUTES.add(NAME_ATTR);
        VALID_ACTION_ATTRIBUTES.add(PROCESSOR_ATTR);
        VALID_ACTION_ATTRIBUTES.add(PROCESSOR_TYPE_ATTR);
        VALID_ACTION_ATTRIBUTES.add(IF_ATTR);
        VALID_ACTION_ATTRIBUTES.add(QUEUE_TO_LINK_FROM);
        VALID_ACTION_ATTRIBUTES.add(QUEUE_TO_LINK_TO);
        VALID_ACTION_ATTRIBUTES.add(FOR_EACH_ATTR);
        VALID_ACTION_ATTRIBUTES.add(LOOP_UNTIL_ATTR);
        VALID_ACTION_ATTRIBUTES.add(ASYNC_ATTR);
        VALID_ACTION_ATTRIBUTES.add(HALT_ON_ERROR);
        VALID_ACTION_ATTRIBUTES.add(UPDATE_PROCESS_STATUS);
        VALID_ACTION_ATTRIBUTES.add(START_EVENT);
    }

    static {
        VALID_SERIES_ATTRIBUTES.addAll(VALID_ACTION_ATTRIBUTES);
        VALID_SERIES_ATTRIBUTES.add(WAIT_FOR_ASYNC_ATTR);
        VALID_SERIES_ATTRIBUTES.add(ASYNC_ATTR);
        VALID_SERIES_ATTRIBUTES.add(PROCESS_NAME_ATTR);
    }

    static {
        VALID_OPERATION_ATTRIBUTES.addAll(VALID_ACTION_ATTRIBUTES);
    }

}
