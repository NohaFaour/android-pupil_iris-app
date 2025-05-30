import numpy as np

class IRImage:
    def __init__(self, img_data: np.ndarray, eye_side: str):
        self.img_data = img_data
        self.eye_side = eye_side 