//****************************************************************************/
//  Copyright (c) 2022-2026 The CORESENSE Consortium.                        //
//  Licensed under the Apache License, Version 2.0                           //
//****************************************************************************/

package de.fraunhofer.ipa.ros.rostooling2sysml;

import java.util.Set;
import org.eclipse.xtext.generator.IFileSystemAccess;
import org.eclipse.xtext.generator.IOutputConfigurationProvider;
import org.eclipse.xtext.generator.OutputConfiguration;
import com.google.common.collect.Sets;

public class CustomOutputProvider implements IOutputConfigurationProvider {
    public final static String DEFAULT_OUTPUT = "DEFAULT_OUTPUT";

    @Override
    public Set<OutputConfiguration> getOutputConfigurations() {
        OutputConfiguration defaultOutput = new OutputConfiguration(IFileSystemAccess.DEFAULT_OUTPUT);
        defaultOutput.setDescription("Output Folder");
        defaultOutput.setOutputDirectory("./src-gen/");
        defaultOutput.setOverrideExistingResources(true);
        defaultOutput.setCreateOutputDirectory(true);
        defaultOutput.setCleanUpDerivedResources(true);
        defaultOutput.setSetDerivedProperty(true);
        defaultOutput.setKeepLocalHistory(true);
        return Sets.newHashSet(defaultOutput);
    }
}
