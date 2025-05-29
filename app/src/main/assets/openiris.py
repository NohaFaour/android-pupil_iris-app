import cv2
import numpy as np
import matplotlib.pyplot as plt
import iris
from matplotlib.patches import Circle

# ─── Load IR Image ─────────────────────────────────────────────────────────────
img_path = r"C:\Users\nohaf\Downloads\sample4.jpg"
img_pixels = cv2.imread(img_path, cv2.IMREAD_GRAYSCALE)

# --- Inpainting Step: Remove Reflections from Pupil Area ---
iris_pipeline_temp = iris.IRISPipeline(env=iris.IRISPipeline.DEBUGGING_ENVIRONMENT)
_ = iris_pipeline_temp(img_data=img_pixels, eye_side="right")

segmap = iris_pipeline_temp.call_trace["segmentation"].predictions
pupil_softmax = segmap[:, :, 2]
pupil_mask = (pupil_softmax > 0.5).astype(np.uint8) * 255

reflection_threshold = 200
bright_spots = (img_pixels > reflection_threshold).astype(np.uint8) * 255
reflection_mask = cv2.bitwise_and(bright_spots, pupil_mask)

kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (7, 7))
reflection_mask = cv2.dilate(reflection_mask, kernel, iterations=2)

img_cleaned_telea = cv2.inpaint(img_pixels, reflection_mask, inpaintRadius=7, flags=cv2.INPAINT_TELEA)
img_pixels = img_cleaned_telea

# ─── Run IRIS Pipeline ─────────────────────────────────────────────────────────
iris_pipeline = iris.IRISPipeline(env=iris.IRISPipeline.DEBUGGING_ENVIRONMENT)
output = iris_pipeline(img_data=img_pixels, eye_side="right")

geometry = iris_pipeline.call_trace['geometry_estimation']
if geometry is None:
    print("❌ Geometry estimation failed.")
    exit(1)

pupil_array = geometry.pupil_array
pupil_contour = pupil_array.reshape(-1, 1, 2).astype(np.float32)
(pupil_x, pupil_y), pupil_radius = cv2.minEnclosingCircle(pupil_contour)
pupil_center = (pupil_x, pupil_y)

iris_center = output["metadata"]["eye_centers"]["iris_center"]
iris_radius = (output["metadata"]["iris_bbox"]["x_max"] - output["metadata"]["iris_bbox"]["x_min"]) / 2
ipr = pupil_radius / iris_radius if iris_radius > 0 else 0
print(f"📏 Computed IPR from geometry pupil_array: {ipr:.3f}")

# ─── Visualize Cleaned Image with Fitted Circle ────────────────────────────────
overlay = cv2.cvtColor(img_pixels, cv2.COLOR_GRAY2BGR)
cv2.circle(overlay, (int(pupil_x), int(pupil_y)), int(pupil_radius), (0, 255, 0), 2)  # Green pupil circle
cv2.circle(overlay, tuple(np.int32(iris_center)), 3, (255, 0, 0), -1)                # Blue iris center
cv2.circle(overlay, (int(pupil_x), int(pupil_y)), 3, (0, 0, 255), -1)                # Red pupil center

plt.imshow(cv2.cvtColor(overlay, cv2.COLOR_BGR2RGB))
plt.title(f"Overlay: Pupil Circle + Iris Center (IPR = {ipr:.3f})")
plt.axis("off")
plt.show()

# ─── Visualize with Full Geometry and Legend ────────────────────────────────────
iris_visualizer = iris.visualisation.IRISVisualizer()
canvas = iris_visualizer.plot_all_geometry(
    ir_image=iris.IRImage(img_data=img_pixels, eye_side="right"),
    geometry_polygons=geometry,
    eye_orientation=iris_pipeline.call_trace['eye_orientation'],
    eye_center=iris_pipeline.call_trace['eye_center_estimation'],
)

# Add fitted circle manually
ax = plt.gca()
circle_patch = Circle(
    (pupil_x, pupil_y),
    radius=pupil_radius,
    edgecolor='red',
    facecolor='none',
    linewidth=2,
    label='Fitted Pupil Circle'
)
ax.add_patch(circle_patch)

# Add legend and title
plt.legend()
plt.title("Full Geometry + Fitted Pupil Circle")
plt.axis("off")
plt.show()
