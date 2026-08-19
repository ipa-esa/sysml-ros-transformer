//****************************************************************************/
//  Copyright (c) 2022-2026 The CORESENSE Consortium.                        //
//  Licensed under the Apache License, Version 2.0                           //
//****************************************************************************/

package de.fraunhofer.ipa.ros.rostooling2sysml.generator

import de.fraunhofer.ipa.ros.rostooling2sysml.transform.RosSystem2SysMLTransformer.SysMLResult
import de.fraunhofer.ipa.ros.rostooling2sysml.transform.RosSystem2SysMLTransformer.ModeletTypeResult
import de.fraunhofer.ipa.ros.rostooling2sysml.transform.RosSystem2SysMLTransformer.EngineResult
import de.fraunhofer.ipa.ros.rostooling2sysml.transform.RosSystem2SysMLTransformer.ExertResult
import de.fraunhofer.ipa.ros.rostooling2sysml.transform.RosSystem2SysMLTransformer.ExertParamResult
import de.fraunhofer.ipa.ros.rostooling2sysml.transform.RosSystem2SysMLTransformer.FlowResult

/**
 * Generates SysML v2 text from SysMLResult model.
 */
class SysMLTextGenerator {

    def CharSequence generate(SysMLResult result) '''
        package «result.packageName» {
            private import CSCore::*;
            private import CSRosBridge::*;
            
            // ─── Modelet Types ───
            «FOR type : result.modeletTypes»
            @RosTypeMapping { rosType = "«type.rosType»"; }
            part def «type.name» specializes Modelet;
            
            «ENDFOR»
            // ─── Engine Definitions ───
            «FOR engine : result.engines»
            @RosArtifactMapping { rosPackage = "«engine.rosPackage»"; rosArtifact = "«engine.rosArtifact»"«IF engine.rosNamespace !== null»; rosNamespace = "«engine.rosNamespace»"«ENDIF»; }
            part def «engine.defName» specializes Engine;
            
            «ENDFOR»
            // ─── Exert Definitions ───
            «FOR exert : result.exerts»
            action def «exert.name» specializes Exert {
                «FOR param : exert.inParams»
                in «param.name» : «param.modeletTypeName»«IF param === exert.inParams.head» subsets Exert::modelets«ENDIF»;
                «ENDFOR»
                in ref engine : «exert.engineDefName» redefines Exert::engine;
                «FOR param : exert.outParams»
                out «param.name» : «param.modeletTypeName»«IF param === exert.outParams.head» subsets Exert::output«ENDIF»;
                «ENDFOR»
            }
            
            «ENDFOR»
            // ─── System Composition ───
            @RosSystemMapping { systemName = "«result.systemName»"«IF result.fromFile !== null»; fromFile = "«result.fromFile»"«ENDIF»; }
            part def «result.systemName.toFirstUpper» {
                «FOR engine : result.engines»
                part «engine.instanceName» : «engine.defName»;
                «ENDFOR»
                
                «FOR exert : result.exerts»
                perform action «exert.engineInstanceName»Exert : «exert.name» {
                    in ref engine = «exert.engineInstanceName»;
                }
                «ENDFOR»
                
                «FOR flow : result.flows»
                flow from «flow.sourceExertInstance».«flow.sourceParam» to «flow.targetExertInstance».«flow.targetParam»;
                «ENDFOR»
            }
        }
    '''
}
