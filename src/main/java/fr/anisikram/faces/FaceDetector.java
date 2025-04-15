package fr.anisikram.faces;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FaceDetector {
    private CascadeClassifier faceDetector;
    private final double scaleFactor = 1.1;
    private final int minNeighbors = 3;
    private final Size minFaceSize = new Size(80, 80);
    private final Size maxFaceSize = new Size();

    public FaceDetector(String classifierPath) {
        faceDetector = new CascadeClassifier();
        if (!faceDetector.load(classifierPath)) {
            System.err.println("Error: Could not load classifier file from: " + classifierPath);
            throw new RuntimeException("Failed to load cascade classifier");
        }
        System.out.println("Face detector initialized successfully");
    }

    public FaceDetector() {
        try {
            File cascadeFile = extractResource("/haarcascades/haarcascade_frontalface_default.xml");
            if (cascadeFile != null) {
                faceDetector = new CascadeClassifier();
                if (!faceDetector.load(cascadeFile.getAbsolutePath())) {
                    System.err.println("Error: Could not load default classifier file.");
                    throw new RuntimeException("Failed to load cascade classifier");
                }
                System.out.println("Face detector initialized with default classifier");
            } else {
                throw new RuntimeException("Failed to extract default cascade classifier");
            }
        } catch (Exception e) {
            System.err.println("Error initializing face detector: " + e.getMessage());
            throw new RuntimeException("Failed to initialize face detector", e);
        }
    }

    private File extractResource(String resourcePath) {
        try {
            InputStream is = getClass().getResourceAsStream(resourcePath);
            if (is == null) {
                System.err.println("Could not find resource: " + resourcePath);
                return null;
            }
            String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
            File tempFile = File.createTempFile("opencv_", fileName);
            tempFile.deleteOnExit();
            try (FileOutputStream os = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
            is.close();
            return tempFile;
        } catch (IOException e) {
            System.err.println("Failed to extract resource: " + e.getMessage());
            return null;
        }
    }

    public List<Rect> detectFaces(Mat image) {
        if (faceDetector.empty()) {
            System.err.println("Error: Face detector not properly initialized");
            return new ArrayList<>();
        }
        Mat grayImage = new Mat();
        if (image.channels() > 1) {
            Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
        } else {
            grayImage = image.clone();
        }
        Imgproc.equalizeHist(grayImage, grayImage);
        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(
                grayImage,
                faceDetections,
                scaleFactor,
                minNeighbors,
                0,
                minFaceSize,
                maxFaceSize
        );
        grayImage.release();
        return faceDetections.toList();
    }

    public Mat extractFace(Mat image, Rect faceRect, boolean normalize) {
        int margin = (int) (Math.min(faceRect.width, faceRect.height) * 0.2);
        Rect enlargedRect = new Rect(
                Math.max(0, faceRect.x - margin / 2),
                Math.max(0, faceRect.y - margin / 2),
                Math.min(faceRect.width + margin, image.width() - faceRect.x),
                Math.min(faceRect.height + margin, image.height() - faceRect.y)
        );
        Mat face = new Mat(image, enlargedRect);
        Mat resizedFace = new Mat();
        Size standardSize = new Size(224, 224);
        Imgproc.resize(face, resizedFace, standardSize);
        if (normalize) {
            Mat grayFace = new Mat();
            if (resizedFace.channels() > 1) {
                Imgproc.cvtColor(resizedFace, grayFace, Imgproc.COLOR_BGR2GRAY);
            } else {
                grayFace = resizedFace.clone();
                resizedFace.release();
            }
            Imgproc.equalizeHist(grayFace, grayFace);
            return grayFace;
        }
        return resizedFace;
    }

    public void drawFaceRectangles(Mat image, List<Rect> faces) {
        for (Rect face : faces) {
            Imgproc.rectangle(
                    image,
                    new Point(face.x, face.y),
                    new Point(face.x + face.width, face.y + face.height),
                    new Scalar(0, 255, 0),
                    2
            );
        }
    }
}
