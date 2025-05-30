import cv2
import numpy as np
from typing import Dict, Any
from .image import IRImage

class IRISVisualizer:
    def plot_all_geometry(
        self,
        ir_image: IRImage,
        geometry_polygons: Any,
        eye_orientation: str,
        eye_center: Dict[str, Any]
    ) -> np.ndarray:
        # Create a simple visualization
        canvas = cv2.cvtColor(ir_image.img_data, cv2.COLOR_GRAY2BGR)
        
        # Draw pupil contour
        if hasattr(geometry_polygons, 'pupil_array'):
            cv2.drawContours(canvas, [geometry_polygons.pupil_array], -1, (0, 255, 0), 2)
        
        # Draw iris contour
        if hasattr(geometry_polygons, 'iris_array'):
            cv2.drawContours(canvas, [geometry_polygons.iris_array], -1, (255, 0, 0), 2)
        
        return canvas 