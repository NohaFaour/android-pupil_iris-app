from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse
import numpy as np
import cv2
import io
import iris
import base64
from io import BytesIO
import matplotlib.pyplot as plt
from matplotlib.patches import Circle

app = FastAPI()

def analyze_iris_image(img_pixels):
    # --- Inpainting Step: Remove Reflections from Pupil Area ---
    iris_pipeline_temp = iris.IRISPipeline(env=iris.IRISPipeline.DEBUGGING_ENVIRONMENT)
    _ = iris_pipeline_temp(img_data=img_pixels, eye_side="right")

    segmap = iris_pipeline_temp.call_trace["segmentation"].predictions
    pupil_softmax = segmap[:, :, 2]
    pupil_mask = (pupil_softmax > 0.5).astype(np.uint8) * 255

    reflection_threshold = 200
    bright_spots = (img_pixels > reflection_threshold).astype(np.uint8) * 255
    reflection_mask = cv2.bitwise_and(bright_spots, pupil_mask)

    kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (6, 6))
    reflection_mask = cv2.dilate(reflection_mask, kernel, iterations=2)

    img_cleaned_telea = cv2.inpaint(img_pixels, reflection_mask, inpaintRadius=6, flags=cv2.INPAINT_TELEA)
    img_pixels = img_cleaned_telea

    # ─── Run IRIS Pipeline ─────────────────────────────────────────────────────────
    iris_pipeline = iris.IRISPipeline(env=iris.IRISPipeline.DEBUGGING_ENVIRONMENT)
    output = iris_pipeline(img_data=img_pixels, eye_side="right")

    geometry = iris_pipeline.call_trace['geometry_estimation']
    if geometry is None:
        return {"error": "Geometry estimation failed."}

    pupil_array = geometry.pupil_array
    pupil_contour = pupil_array.reshape(-1, 1, 2).astype(np.float32)
    (pupil_x, pupil_y), pupil_radius = cv2.minEnclosingCircle(pupil_contour)
    pupil_center = (pupil_x, pupil_y)

    iris_center = output["metadata"]["eye_centers"]["iris_center"]
    iris_radius = (output["metadata"]["iris_bbox"]["x_max"] - output["metadata"]["iris_bbox"]["x_min"]) / 2
    ipr = pupil_radius / iris_radius if iris_radius > 0 else 0

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
    plt.legend(loc='lower center', bbox_to_anchor=(0.5, -0.15), ncol=2)
    plt.title("Full Geometry + Fitted Pupil Circle")
    plt.axis("off")

    buf = BytesIO()
    plt.savefig(buf, format='png', bbox_inches='tight')
    plt.close()
    buf.seek(0)
    img_base64 = base64.b64encode(buf.read()).decode('utf-8')

    return {"ipr": float(ipr), "visualization": img_base64}

@app.post("/analyze_iris/")
async def analyze_iris(file: UploadFile = File(...)):
    contents = await file.read()
    print(f"Received file size: {len(contents)} bytes")
    nparr = np.frombuffer(contents, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_GRAYSCALE)
    if img is None:
        return JSONResponse({"error": "Invalid image file."}, status_code=400)
    result = analyze_iris_image(img)
    return JSONResponse(result)