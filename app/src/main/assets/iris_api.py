from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse
import numpy as np
import cv2
import io
import iris 
import base64
from io import BytesIO
import matplotlib.pyplot as plt


app = FastAPI()

def analyze_iris_image(img_pixels):
    # Step 1: First run to extract segmentation from original image
    iris_pipeline_temp = iris_pipeline = iris.IRISPipeline(env=iris.IRISPipeline.DEBUGGING_ENVIRONMENT)

    _ = iris_pipeline_temp(img_data=img_pixels, eye_side="right")

    # Step 2: Extract pupil segmentation mask
    segmap = iris_pipeline_temp.call_trace["segmentation"].predictions
    pupil_softmax = segmap[:, :, 2]
    pupil_mask = (pupil_softmax > 0.5).astype(np.uint8) * 255

    # Step 3: Find bright reflections only inside pupil area
    reflection_threshold = 200
    bright_spots = (img_pixels > reflection_threshold).astype(np.uint8) * 255
    reflection_mask = cv2.bitwise_and(bright_spots, pupil_mask)

    # Optional: Refine reflection mask with larger kernel
    kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (9, 9))
    reflection_mask = cv2.dilate(reflection_mask, kernel, iterations=2)

    # Step 4: Inpaint reflections with larger radius
    img_cleaned = cv2.inpaint(img_pixels, reflection_mask, inpaintRadius=9, flags=cv2.INPAINT_NS)

    # Step 5: Run IRIS pipeline with cleaned image
    iris_pipeline = iris.IRISPipeline(env=iris.IRISPipeline.DEBUGGING_ENVIRONMENT)
    output = iris_pipeline(img_data=img_cleaned, eye_side="right")

    geometry = iris_pipeline.call_trace['geometry_estimation']
    if geometry is None:
        return {"error": "Geometry estimation failed."}

    # Calculate measurements
    pupil_array = geometry.pupil_array
    pupil_contour = pupil_array.reshape(-1, 1, 2).astype(np.float32)
    (pupil_x, pupil_y), pupil_radius = cv2.minEnclosingCircle(pupil_contour)
    iris_center = output["metadata"]["eye_centers"]["iris_center"]
    iris_radius = (output["metadata"]["iris_bbox"]["x_max"] - output["metadata"]["iris_bbox"]["x_min"]) / 2
    ipr = iris_radius / pupil_radius if iris_radius > 0 else 0

    # Create custom overlay visualization based on segmentation
    plt.figure(figsize=(10, 10))
    plt.imshow(img_cleaned, cmap='gray')
    
    # Draw pupil as a circle
    pupil_circle = plt.Circle((pupil_x, pupil_y), pupil_radius, 
                            fill=False, color='green', linewidth=2, 
                            label='Pupil Circle')
    plt.gca().add_patch(pupil_circle)
    
    # Draw iris contour (assuming iris_array is available in geometry)
    iris_array = geometry.iris_array
    iris_contour = iris_array.reshape(-1, 2)
    plt.plot(iris_contour[:, 0], iris_contour[:, 1], 'b-', linewidth=2, label='Iris Boundary')
    
    # Draw centers
    plt.plot(iris_center[0], iris_center[1], 'bo', markersize=5, label='Iris Center')
    plt.plot(pupil_x, pupil_y, 'ro', markersize=5, label='Pupil Center')
    
    plt.title("Custom Overlay: Pupil Circle and Iris Boundary")
    plt.legend()
    plt.axis("off")
    
    # Save the visualization
    buf = BytesIO()
    plt.savefig(buf, format='png', bbox_inches='tight', dpi=300)
    plt.close()
    buf.seek(0)
    img_base64 = base64.b64encode(buf.read()).decode('utf-8')

    return {
        "ipr": float(ipr),
        "visualization": img_base64,
        "pupil_center": [float(pupil_x), float(pupil_y)],
        "pupil_radius": float(pupil_radius),
        "iris_center": [float(iris_center[0]), float(iris_center[1])],
        "iris_radius": float(iris_radius)
    }

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