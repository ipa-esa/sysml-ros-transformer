//****************************************************************************/
//  Copyright (c) 2022-2026 The CORESENSE Consortium.                        //
//  Licensed under the Apache License, Version 2.0                           //
//****************************************************************************/

package de.fraunhofer.ipa.ros.sysml.tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import de.fraunhofer.ipa.ros.rostooling2sysml.transform.RosSystem2SysMLTransformer;
import de.fraunhofer.ipa.ros.rostooling2sysml.transform.RosSystem2SysMLTransformer.SysMLResult;

/**
 * Standard JUnit / OSGi Plugin Test for Reverse Transformation: RosTooling (.rossystem) -> SysML v2.
 */
public class RosTooling2SysMLTest {

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
    public void testRosSystemToSysMLTransformation() throws IOException {
        Path inputRosSystem = findResource("expected/test_system.rossystem");
        Path inputRos2 = findResource("test_ros_models/test_nodes.ros2");
        Path expectedSysML = findResource("expected/test_system_architecture.sysml");

        Assert.assertTrue("Input rossystem file must exist at " + inputRosSystem, Files.exists(inputRosSystem));
        Assert.assertTrue("Expected SysML file must exist at " + expectedSysML, Files.exists(expectedSysML));

        String rossystemContent = Files.readString(inputRosSystem);
        String ros2Content = Files.exists(inputRos2) ? Files.readString(inputRos2) : null;

        RosSystem2SysMLTransformer transformer = new RosSystem2SysMLTransformer();
        SysMLResult result = transformer.transformText(rossystemContent, ros2Content);

        Assert.assertFalse("Should have extracted engines", result.engines.isEmpty());

        String actualOutput = transformer.generateSysMLText(result).trim().replace("\r\n", "\n");
        String expectedOutput = Files.readString(expectedSysML).trim().replace("\r\n", "\n");

        Assert.assertEquals("Generated SysML v2 model should match expected output", expectedOutput, actualOutput);
    }
}
