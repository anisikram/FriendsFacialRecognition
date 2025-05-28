package fr.anisikram.faces;

import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

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
    private final Size maxFaceSize = new Size(); // Represents no maximum size limit

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
            File tempFile = File.createTempFile("javacv_", fileName); // Changed prefix to avoid conflict
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
        List<Rect> facesList = new ArrayList<>();
        try (Mat grayImage = new Mat(); RectVector faceDetections = new RectVector()) {
            if (image.channels() > 1) {
                opencv_imgproc.cvtColor(image, grayImage, opencv_imgproc.COLOR_BGR2GRAY);
            } else {
                image.copyTo(grayImage); // Use copyTo for cloning in JavaCV for Mats
            }
            opencv_imgproc.equalizeHist(grayImage, grayImage);
            faceDetector.detectMultiScale(
                    grayImage,
                    faceDetections,
                    scaleFactor,
                    minNeighbors,
                    0, // flags
                    minFaceSize,
                    maxFaceSize
            );

            for (long i = 0; i < faceDetections.size(); i++) {
                facesList.add(faceDetections.get(i)); // Rect is a value type, no need to copy/retain
            }
        } // grayImage and faceDetections are automatically closed here
        return facesList;
    }

    /**
     * Extracts a face from an image, optionally normalizing it.
     *
     * @param image The source image.
     * @param faceRect The rectangle defining the face in the source image.
     * @param normalize If true, the extracted face will be converted to grayscale and histogram equalized.
     * @return A new Mat object containing the extracted (and optionally normalized) face.
     *         The caller is responsible for calling .close() on this Mat to release native memory.
     */
    public Mat extractFace(Mat image, Rect faceRect, boolean normalize) {
        // Ensure margin calculation doesn't cause issues with int casting or negative values
        int marginWidth = (int) (faceRect.width() * 0.2);
        int marginHeight = (int) (faceRect.height() * 0.2);

        // Create enlargedRect carefully, ensuring bounds are within the image
        Rect enlargedRect = new Rect(
                Math.max(0, faceRect.x() - marginWidth / 2),
                Math.max(0, faceRect.y() - marginHeight / 2),
                Math.min(faceRect.width() + marginWidth, image.cols() - (faceRect.x() - marginWidth / 2)),
                Math.min(faceRect.height() + marginHeight, image.rows() - (faceRect.y() - marginHeight / 2))
        );
        // Ensure width and height are positive
        if (enlargedRect.width() <= 0 || enlargedRect.height() <= 0) {
            // Fallback to original faceRect if enlargedRect is invalid
            enlargedRect = faceRect;
        }


        Mat returnedMat;

        try (Mat face = new Mat(image, enlargedRect)) { // 'face' is temporary and created from ROI
            Mat resizedFace = new Mat(); // Will be returned or become temporary
            Size standardSize = new Size(224, 224);
            opencv_imgproc.resize(face, resizedFace, standardSize);

            if (normalize) {
                Mat grayFace = new Mat(); // Will be returned
                try {
                    if (resizedFace.channels() > 1) {
                        opencv_imgproc.cvtColor(resizedFace, grayFace, opencv_imgproc.COLOR_BGR2GRAY);
                    } else {
                        resizedFace.copyTo(grayFace); // Use copyTo for cloning
                    }
                    opencv_imgproc.equalizeHist(grayFace, grayFace);
                    returnedMat = grayFace; // grayFace is now the Mat to be returned
                } finally {
                    resizedFace.close(); // resizedFace is temporary in this block, close it
                }
            } else {
                returnedMat = resizedFace; // resizedFace is the Mat to be returned
            }
        } // 'face' (ROI) is automatically closed here

        return returnedMat; // Caller is responsible for this Mat
    }

    public void drawFaceRectangles(Mat image, List<Rect> faces) {
        for (Rect face : faces) {
            opencv_imgproc.rectangle(
                    image,
                    new Point(face.x(), face.y()), // Use accessors x() and y()
                    new Point(face.x() + face.width(), face.y() + face.height()),
                    new Scalar(0, 255, 0, 0), // Scalar for color (Blue, Green, Red, Alpha)
                    2,
                    opencv_imgproc.LINE_8, // lineType, e.g., LINE_8
                    0 // shift
            );
        }
    }
}
