package org.janelia.it.jacs.compute.wsrest.mouselight;

/**
 * Created by schauderd on 6/28/16.
 */
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.ws.rs.NameBinding;

//@Compress annotation is the name binding annotation
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
public @interface Compress {}