// Copyright (c) 2022-2026 The CORESENSE Consortium. Apache License 2.0.

package de.fraunhofer.ipa.ros.sysml2rostooling;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
    private static BundleContext context;
    
    public static BundleContext getContext() { return context; }
    
    @Override
    public void start(BundleContext bundleContext) throws Exception {
        Activator.context = bundleContext;
    }
    
    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        Activator.context = null;
    }
}
