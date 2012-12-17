/*
 * Copyright (c) 2012 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 */
package com.jpl.embedded;

import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * JAX-RS web service & JavaDB
 * Registered REST class EmbeddedREST first time REST services are requested
 */

@ApplicationPath("/")
public class JAXRXConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(EmbeddedREST.class);
        return classes;
    }
}
