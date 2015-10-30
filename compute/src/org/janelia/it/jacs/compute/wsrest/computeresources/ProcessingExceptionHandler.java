package org.janelia.it.jacs.compute.wsrest.computeresources;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Created by goinac on 9/3/15.
 */
@Provider
public class ProcessingExceptionHandler implements ExceptionMapper<ProcessingException> {

   @Override
   public Response toResponse(ProcessingException e) {
      return Response.status(e.getHttpStatus()).entity(new ServiceMessage(e.getMessage())).build();
   }

}
