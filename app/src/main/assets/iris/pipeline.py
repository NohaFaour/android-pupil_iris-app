import cv2
import numpy as np
from typing import Dict, Any, Optional

class IRISPipeline:
    DEBUGGING_ENVIRONMENT = "debug"

    def __init__(self, env: str = "production"):
        self.env = env
        self.call_trace = {}

    def __call__(self, img_data: np.ndarray, eye_side: str) -> Dict[str, Any]:
        # Create segmentation predictions (simplified version)
        h, w = img_data.shape
        segmap = np.zeros((h, w, 3), dtype=np.float32)
        
        # Simple threshold-based segmentation
        pupil_mask = (img_data < 100).astype(np.float32)
        iris_mask = (img_data < 200).astype(np.float32)
        
        segmap[:, :, 0] = 1 - iris_mask  # Background
        segmap[:, :, 1] = iris_mask - pupil_mask  # Iris
        segmap[:, :, 2] = pupil_mask  # Pupil
        
        self.call_trace["segmentation"] = type('obj', (object,), {
            'predictions': segmap
        })
        
        # Estimate geometry
        pupil_contour = self._find_contour(pupil_mask)
        iris_contour = self._find_contour(iris_mask)
        
        if pupil_contour is None or iris_contour is None:
            self.call_trace['geometry_estimation'] = None
            return {"error": "Failed to detect pupil or iris"}
        
        # Calculate centers and radii
        pupil_center, pupil_radius = cv2.minEnclosingCircle(pupil_contour)
        iris_center, iris_radius = cv2.minEnclosingCircle(iris_contour)
        
        # Store geometry
        self.call_trace['geometry_estimation'] = type('obj', (object,), {
            'pupil_array': pupil_contour,
            'iris_array': iris_contour
        })
        
        # Store metadata
        self.call_trace['eye_orientation'] = "right"
        self.call_trace['eye_center_estimation'] = {
            'iris_center': iris_center,
            'pupil_center': pupil_center
        }
        
        return {
            "metadata": {
                "eye_centers": {
                    "iris_center": iris_center,
                    "pupil_center": pupil_center
                },
                "iris_bbox": {
                    "x_min": iris_center[0] - iris_radius,
                    "x_max": iris_center[0] + iris_radius,
                    "y_min": iris_center[1] - iris_radius,
                    "y_max": iris_center[1] + iris_radius
                }
            }
        }

    def _find_contour(self, mask: np.ndarray) -> Optional[np.ndarray]:
        contours, _ = cv2.findContours(
            mask.astype(np.uint8),
            cv2.RETR_EXTERNAL,
            cv2.CHAIN_APPROX_SIMPLE
        )
        if not contours:
            return None
        return max(contours, key=cv2.contourArea) 