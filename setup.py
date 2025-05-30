from setuptools import setup, find_packages

setup(
    name="iris",
    version="0.1.0",
    packages=find_packages(where="app/src/main/assets"),
    package_dir={"": "app/src/main/assets"},
    install_requires=[
        "numpy==1.24.3",
        "opencv-python==4.8.1.78",
        "matplotlib==3.7.1",
        "pillow==10.0.0",
        "torch==2.0.1",
        "torchvision==0.15.2"
    ],
    python_requires=">=3.8",
) 