// Copyright (c) 2022-2026 The CORESENSE Consortium. Apache License 2.0.

package de.fraunhofer.ipa.ros.sysml2rostooling.generator

import de.fraunhofer.ipa.ros.sysml2rostooling.transform.SysML2RosSystemTransformer.RosSystemResult
import de.fraunhofer.ipa.ros.sysml2rostooling.transform.SysML2RosSystemTransformer.RosNodeResult
import de.fraunhofer.ipa.ros.sysml2rostooling.transform.SysML2RosSystemTransformer.RosInterfaceResult
import de.fraunhofer.ipa.ros.sysml2rostooling.transform.SysML2RosSystemTransformer.RosConnectionResult

class RosSystemTextGenerator {
    def CharSequence generate(RosSystemResult system) '''
    «system.systemName»:
      «IF system.fromFile !== null»
      fromFile: "«system.fromFile»"
      «ENDIF»
      nodes:
        «FOR node : system.nodes»
        «generateNode(node)»
        «ENDFOR»
      «IF !system.connections.empty»
      connections:
        «FOR conn : system.connections»
        - ["«conn.fromInterface»", "«conn.toInterface»"]
        «ENDFOR»
      «ENDIF»
    '''
    
    private def CharSequence generateNode(RosNodeResult node) '''
    "«node.name»":
      from: "«node.fromRef»"
      «IF node.namespace !== null»
      namespace: "«node.namespace»"
      «ENDIF»
      «IF !node.interfaces.empty»
      interfaces:
        «FOR iface : node.interfaces»
        - "«iface.name»": «iface.direction» "«iface.fromRef»"
        «ENDFOR»
      «ENDIF»
    '''
}
