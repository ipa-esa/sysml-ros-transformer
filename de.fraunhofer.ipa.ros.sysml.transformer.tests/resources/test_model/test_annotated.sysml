//****************************************************************************/
//  Copyright (c) 2022-2026 The CORESENSE Consortium.                        //
//  Licensed under the Apache License, Version 2.0                           //
//****************************************************************************/

// Test fixture: A minimal annotated SysML v2 model for validating
// the SysML-to-.rossystem M2M transformer.
//
// This model defines:
//   - 2 Modelet types (Image, Detection) with @RosTypeMapping
//   - 2 Engines (CameraEngine, DetectorEngine) with @RosArtifactMapping
//   - 2 Exert specializations (CameraCapture, DetectObjects)
//   - 1 System composition (TestSystem) with @RosSystemMapping
//   - 1 Flow connection (camera image -> detector image)

package test_system_model {
    private import CSCore::*;
    private import CSRosBridge::*;

    // ─── Modelet Types (ROS message types) ───

    @RosTypeMapping { rosType = "sensor_msgs/msg/Image"; }
    part def Image specializes Modelet;

    @RosTypeMapping { rosType = "vision_msgs/msg/Detection2DArray"; }
    part def Detection2DArray specializes Modelet;

    // ─── Engine Definitions (ROS nodes) ───

    @RosArtifactMapping { rosPackage = "test_drivers"; rosArtifact = "camera_driver"; }
    part def CameraEngine specializes Engine;

    @RosArtifactMapping { rosPackage = "test_perception"; rosArtifact = "object_detector"; }
    part def DetectorEngine specializes Engine;

    // ─── Exert Definitions (Interface contracts) ───

    action def CameraCapture specializes Exert {
        in ref modelets : Modelet[*] redefines Exert::modelets;
        in ref engine : CameraEngine redefines Exert::engine;
        out image : Image subsets Exert::output;
    }

    action def DetectObjects specializes Exert {
        in image : Image subsets Exert::modelets;
        in ref engine : DetectorEngine redefines Exert::engine;
        out detections : Detection2DArray subsets Exert::output;
    }

    // ─── System Composition ───

    @RosSystemMapping { systemName = "test_system"; fromFile = "test_pkg/launch/test.launch.py"; }
    part def TestSystem {
        part camera : CameraEngine;
        part detector : DetectorEngine;

        perform action cameraCapture : CameraCapture {
            in ref engine = camera;
        }

        perform action detectObjects : DetectObjects {
            in ref engine = detector;
        }

        flow from cameraCapture.image to detectObjects.image;
    }
}
