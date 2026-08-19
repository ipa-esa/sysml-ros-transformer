// Copyright (c) 2022-2026 The CORESENSE Consortium. Apache License 2.0.

package de.fraunhofer.ipa.ros.sysml2rostooling;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.xtext.generator.IOutputConfigurationProvider;
import org.eclipse.xtext.generator.OutputConfiguration;

public class CustomOutputProvider implements IOutputConfigurationProvider {
    public static final String DEFAULT_OUTPUT = "DEFAULT_OUTPUT";
    
    @Override
    public Set<OutputConfiguration> getOutputConfigurations() {
        Set<OutputConfiguration> outputs = new HashSet<>();
        OutputConfiguration defaultOutput = new OutputConfiguration(DEFAULT_OUTPUT);
        defaultOutput.setOutputDirectory("./src-gen");
        outputs.add(defaultOutput);
        return outputs;
    }
}
