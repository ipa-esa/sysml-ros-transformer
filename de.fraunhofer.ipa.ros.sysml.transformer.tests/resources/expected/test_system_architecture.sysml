//****************************************************************************/
//  Copyright (c) 2022-2026 The CORESENSE Consortium.                        //
//  Licensed under the Apache License, Version 2.0                           //
//****************************************************************************/

package test_system_architecture {
    private import CSCore::*;
    private import CSRosBridge::*;

    // ─── Modelet Types ───
    @RosTypeMapping { rosType = "sensor_msgs/msg/Image"; }
    part def Image specializes Modelet;

    @RosTypeMapping { rosType = "vision_msgs/msg/Detection2DArray"; }
    part def Detection2DArray specializes Modelet;

    // ─── Engine Definitions ───
    @RosArtifactMapping { rosPackage = "test_drivers"; rosArtifact = "camera_driver"; }
    part def CameraEngine specializes Engine;

    @RosArtifactMapping { rosPackage = "test_perception"; rosArtifact = "object_detector"; }
    part def DetectorEngine specializes Engine;

    // ─── Exert Definitions ───
    action def CameraExert specializes Exert {
        in ref engine : CameraEngine redefines Exert::engine;
        out camera_image : Image subsets Exert::output;
    }

    action def DetectorExert specializes Exert {
        in detector_image : Image subsets Exert::modelets;
        in ref engine : DetectorEngine redefines Exert::engine;
        out detector_detections : Detection2DArray subsets Exert::output;
    }

    // ─── System Composition ───
    @RosSystemMapping { systemName = "test_system"; fromFile = "test_pkg/launch/test.launch.py"; }
    part def Test_system {
        part camera : CameraEngine;
        part detector : DetectorEngine;

        perform action cameraExert : CameraExert {
            in ref engine = camera;
        }
        perform action detectorExert : DetectorExert {
            in ref engine = detector;
        }

        flow from cameraExert.camera_image to detectorExert.detector_image;
    }
}
