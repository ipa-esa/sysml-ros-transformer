package de.fraunhofer.ipa.ros.sysml2rostooling;

import org.eclipse.xtext.ui.guice.AbstractGuiceAwareExecutableExtensionFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.google.inject.Injector;

import de.fraunhofer.ipa.rossystem.xtext.ui.internal.XtextActivator;

public class SysMLGeneratorExecutableExtensionFactory extends AbstractGuiceAwareExecutableExtensionFactory {

    @Override
    protected Bundle getBundle() {
        return FrameworkUtil.getBundle(getClass());
    }

    @Override
    protected Injector getInjector() {
        XtextActivator activator = XtextActivator.getInstance();
        return activator != null ? activator.getInjector(XtextActivator.DE_FRAUNHOFER_IPA_ROSSYSTEM_ROSSYSTEM) : null;
    }
}
