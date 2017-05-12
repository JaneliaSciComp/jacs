package org.janelia.jacs2.asyncservice.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.cdi.ObjectMapperFactory;
import org.janelia.jacs2.dao.mongo.utils.MongoModule;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceDataUtils {

    private static ObjectMapper MAPPER = ObjectMapperFactory.instance().newObjectMapper().registerModule(new MongoModule());

    public static List<File> serializableObjectToFileList(Object o) {
        if (o != null) {
            List<String> filePathsList = MAPPER.convertValue(o, new TypeReference<List<String>>() {});
            return filePathsList.stream()
                    .map(File::new)
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    public static Object fileListToSerializableObject(List<File> fileList) {
        if (CollectionUtils.isNotEmpty(fileList)) {
            return fileList.stream()
                    .filter(r -> r != null)
                    .map(File::getAbsolutePath)
                    .collect(Collectors.toList());
        } else {
            return null;
        }
    }

    public static File serializableObjectToFile(Object o) {
        if (o instanceof String && StringUtils.isNotBlank((String) o)) {
            return new File((String) o);
        } else {
            return null;
        }
    }

    public static Object fileToSerializableObject(File result) {
        if (result != null) {
            return result.getAbsolutePath();
        } else {
            return null;
        }
    }

    public  static <T> T serializableObjectToAny(Object o, TypeReference typeRef) {
        if (o == null)
            return null;
        else
            return MAPPER.convertValue(o, typeRef);
    }

    public static <T> Object anyToSerializableObject(T any) {
        return any == null ? null : MAPPER.valueToTree(any);
    }
}
