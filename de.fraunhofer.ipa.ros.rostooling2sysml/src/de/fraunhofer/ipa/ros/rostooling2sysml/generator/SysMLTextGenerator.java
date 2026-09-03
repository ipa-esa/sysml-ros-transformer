//****************************************************************************/
//  Copyright (c) 2022-2026 The CORESENSE Consortium.                        //
//  Licensed under the Apache License, Version 2.0                           //
//****************************************************************************/

package de.fraunhofer.ipa.ros.rostooling2sysml.generator;

import de.fraunhofer.ipa.ros.rostooling2sysml.transform.RosSystem2SysMLTransformer;
import de.fraunhofer.ipa.ros.rostooling2sysml.transform.RosSystem2SysMLTransformer.SysMLResult;

/**
 * Text generator for converting a SysMLResult into SysML v2 syntax.
 * Pure Java implementation without external Xtend/StringConcatenation dependencies.
 */
public class SysMLTextGenerator {

    public CharSequence generate(SysMLResult result) {
        RosSystem2SysMLTransformer transformer = new RosSystem2SysMLTransformer();
        return transformer.generateSysMLText(result);
    }
}
