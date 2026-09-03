// Copyright (c) 2022-2026 The CORESENSE Consortium. Apache License 2.0.

package de.fraunhofer.ipa.ros.sysml2rostooling.generator;

import de.fraunhofer.ipa.ros.sysml2rostooling.transform.SysML2RosSystemTransformer;
import de.fraunhofer.ipa.ros.sysml2rostooling.transform.SysML2RosSystemTransformer.RosSystemResult;

/**
 * Text generator for converting a RosSystemResult into .rossystem syntax.
 * Pure Java implementation without external Xtend/StringConcatenation dependencies.
 */
public class RosSystemTextGenerator {

    public CharSequence generate(RosSystemResult system) {
        SysML2RosSystemTransformer transformer = new SysML2RosSystemTransformer();
        return transformer.generateText(system);
    }
}
