package fr.anisikram.video;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.bytedeco.opencv.global.opencv_videoio;

import java.util.Objects;

public class VideoCapturer {

    // L'objet VideoCapture est l'interface principale pour accéder à la caméra
    private VideoCapture camera;
    private boolean opened = false;

    public VideoCapturer(int deviceId) {
        // Initialiser la caméra avec l'ID fourni
        camera = new VideoCapture(deviceId);

        // Attendre un court instant pour que la caméra s'initialise
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Vérifier si la caméra est accessible
        if (!camera.isOpened()) {
            System.err.println("ERREUR : Impossible d'accéder à la caméra avec l'ID " + deviceId);
            return;
        }

        // Configurer la résolution de capture (640x480 est une résolution standard bien supportée)
        camera.set(opencv_videoio.CAP_PROP_FRAME_WIDTH, 1280);
        camera.set(opencv_videoio.CAP_PROP_FRAME_HEIGHT, 720);

        opened = true;
        System.out.println("Caméra initialisée avec succès (ID: " + deviceId + ")");
    }

    /**
     * Captures a frame from the camera.
     *
     * @return A new Mat object containing the frame. The caller is responsible for
     *         calling .close() on this Mat to release native memory.
     *         Returns null if the camera is not opened or if the frame is empty.
     */
    public Mat getFrame() {
        if (!opened) {
            System.err.println("ERREUR : La caméra n'est pas ouverte.");
            return null;
        }

        // Créer un objet Mat pour stocker l'image capturée
        Mat frame = new Mat();

        // Capturer une image de la caméra
        camera.read(frame);

        // Vérifier si l'image a été capturée avec succès
        if (frame.empty()) {
            System.err.println("ERREUR : Impossible de capturer l'image.");
            return null;
        }

        return frame;
    }

    public boolean isOpened() {
        return opened && camera.isOpened();
    }

    public void close(){
        if(Objects.nonNull(camera)){
            camera.release();
            opened = false;
            System.out.println("Caméra fermée avec succès.");
        } else {
            System.err.println("ERREUR : La caméra n'est pas initialisée.");
        }
    }
}
