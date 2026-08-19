//****************************************************************************/
//  Copyright (c) 2022-2026 The CORESENSE Consortium.                        //
//  Licensed under the Apache License, Version 2.0                           //
//****************************************************************************/

package de.fraunhofer.ipa.ros.sysml.tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.fraunhofer.ipa.ros.sysml2rostooling.parser.SysMLParser;
import de.fraunhofer.ipa.ros.sysml2rostooling.parser.model.SysMLModel;
import de.fraunhofer.ipa.ros.sysml2rostooling.transform.SysML2RosSystemTransformer;
import de.fraunhofer.ipa.ros.sysml2rostooling.transform.SysML2RosSystemTransformer.RosSystemResult;

/**
 * Standard JUnit / OSGi Plugin Test for Forward Transformation: SysML v2 -> RosTooling (.rossystem).
 */
public class SysML2RosToolingTest {

    public static Path findResource(String relativePath) {
        // 1. Try OSGi bundle entry if running inside Eclipse OSGi runtime
        try {
            org.osgi.framework.Bundle bundle = org.eclipse.core.runtime.Platform.getBundle("de.fraunhofer.ipa.ros.sysml.transformer.tests");
            if (bundle != null) {
                java.net.URL url = bundle.getEntry("resources/" + relativePath);
                if (url != null) {
                    java.net.URL fileUrl = org.eclipse.core.runtime.FileLocator.toFileURL(url);
                    return Paths.get(fileUrl.toURI());
                }
            }
        } catch (Throwable ignored) {
            // Not running in OSGi runtime or bundle not found, fallback to filesystem
        }

        // 2. Try standard filesystem locations
        Path[] candidates = new Path[] {
            Paths.get("resources").resolve(relativePath),
            Paths.get("test").resolve(relativePath),
            Paths.get("de.fraunhofer.ipa.ros.sysml.transformer.tests/resources").resolve(relativePath),
            Paths.get("src/sysml-ros-transformer/test").resolve(relativePath),
            Paths.get("src/sysml-ros-transformer/de.fraunhofer.ipa.ros.sysml.transformer.tests/resources").resolve(relativePath)
        };
        for (Path p : candidates) {
            if (Files.exists(p)) return p;
        }
        return Paths.get(relativePath);
    }

    @Test
    public void testSysMLToRosSystemTransformation() throws IOException {
        Path inputPath = findResource("test_model/test_annotated.sysml");
        Path expectedPath = findResource("expected/test_system.rossystem");

        Assert.assertTrue("Input SysML file must exist at " + inputPath, Files.exists(inputPath));
        Assert.assertTrue("Expected rossystem file must exist at " + expectedPath, Files.exists(expectedPath));

        SysMLParser parser = new SysMLParser();
        SysMLModel model = parser.parse(Arrays.asList(inputPath.toString()));
        Assert.assertFalse("Model packages should not be empty", model.getPackages().isEmpty());

        SysML2RosSystemTransformer transformer = new SysML2RosSystemTransformer();
        List<RosSystemResult> results = transformer.transform(model);
        Assert.assertEquals("Should have generated 1 system", 1, results.size());

        RosSystemResult result = results.get(0);
        String actualOutput = transformer.generateText(result).trim().replace("\r\n", "\n");
        String expectedOutput = Files.readString(expectedPath).trim().replace("\r\n", "\n");

        Assert.assertEquals("Generated .rossystem should match expected output", expectedOutput, actualOutput);
    }
}
