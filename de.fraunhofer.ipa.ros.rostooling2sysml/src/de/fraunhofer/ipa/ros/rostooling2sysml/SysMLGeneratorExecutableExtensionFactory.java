//****************************************************************************/
//  Copyright (c) 2022-2026 The CORESENSE Consortium.                        //
//  Licensed under the Apache License, Version 2.0                           //
//****************************************************************************/

package de.fraunhofer.ipa.ros.rostooling2sysml;

import org.eclipse.xtext.ui.guice.AbstractGuiceAwareExecutableExtensionFactory;
import org.osgi.framework.Bundle;
import com.google.inject.Injector;
import de.fraunhofer.ipa.rossystem.xtext.ui.internal.XtextActivator;

public class SysMLGeneratorExecutableExtensionFactory extends AbstractGuiceAwareExecutableExtensionFactory {
    @Override
    protected Bundle getBundle() {
        return Activator.getContext().getBundle();
    }

    @Override
    protected Injector getInjector() {
        return XtextActivator.getInstance()
            .getInjector(XtextActivator.DE_FRAUNHOFER_IPA_ROSSYSTEM_ROSSYSTEM);
    }
}
