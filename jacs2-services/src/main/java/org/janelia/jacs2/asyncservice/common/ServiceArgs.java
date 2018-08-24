package org.janelia.jacs2.asyncservice.common;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.janelia.jacs2.asyncservice.sample.ServiceInput;
import org.janelia.jacs2.asyncservice.sample.ServiceResult;
import org.janelia.model.service.ServiceArgDescriptor;
import org.janelia.model.service.ServiceMetaData;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServiceArgs {

    public static <A extends ServiceArgs> A parse(String[] argsList, A args) {
        new JCommander(args).parse(argsList);
        return args;
    }

    private static <A extends ServiceArgs> void populateArgumentDescriptors(A args, ServiceMetaData smd) {
        JCommander jc = new JCommander(args);
        List<ParameterDescription> parameterDescriptiontList = jc.getParameters();
        smd.setServiceArgsObject(args);
        smd.setDescription(args.getServiceDescription());
        smd.setServiceArgDescriptors(parameterDescriptiontList.stream()
                .filter(pd -> !pd.isHelp())
                .map(pd -> {
                    Parameter parameterAnnotation = pd.getParameterAnnotation();
                    return new ServiceArgDescriptor(
                            pd.getParameterized(),
                            parameterAnnotation.names(),
                            pd.getDefault(),
                            parameterAnnotation.arity(),
                            parameterAnnotation.required(),
                            pd.getDescription()
                    );
                })
                .collect(Collectors.toList())
        );
    }

    public static <P extends ServiceProcessor, A extends ServiceArgs> ServiceMetaData getMetadata(Class<P> processorClass) {
        return getMetadata(processorClass, null);
    }

    public static <P extends ServiceProcessor, A extends ServiceArgs> ServiceMetaData getMetadata(Class<P> processorClass, A args) {
        Named namedAnnotation = processorClass.getAnnotation(Named.class);
        Preconditions.checkArgument(namedAnnotation != null);
        String serviceName = namedAnnotation.value();

        ServiceMetaData smd = createMetadata(serviceName, args);

        ServiceResult serviceResult = processorClass.getAnnotation(ServiceResult.class);
        if (serviceResult != null) {
            smd.setServiceResult(serviceResult);
        }

        ServiceInput[] annotationsByType = processorClass.getAnnotationsByType(ServiceInput.class);
        smd.setServiceInputs(Arrays.asList(annotationsByType));

        return smd;
    }

    static <A extends ServiceArgs> ServiceMetaData createMetadata(String serviceName, A args) {
        ServiceMetaData smd = new ServiceMetaData();
        smd.setServiceName(serviceName);
        if (args != null) {
            populateArgumentDescriptors(args, smd);
        }
        return smd;
    }

    private final String serviceDescription;

    @Parameter(description = "Remaining positional container arguments")
    private List<String> remainingArgs = new ArrayList<>();

    public ServiceArgs() {
        this(null);
    }

    public ServiceArgs(String serviceDescription) {
        this.serviceDescription = serviceDescription;
    }

    String getServiceDescription() {
        return serviceDescription;
    }

    public List<String> getRemainingArgs() {
        return streamArgs(remainingArgs, ',').collect(Collectors.toList());
    }

    private Stream<String> streamArgs(List<String> args, char separator) {
        return args.stream()
                .flatMap(s -> {
                    if (s.indexOf(separator) >= 0)
                        return streamArgs(Splitter.on(separator).splitToList(s), separator);
                    else
                        return Stream.of(s);
                });
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
