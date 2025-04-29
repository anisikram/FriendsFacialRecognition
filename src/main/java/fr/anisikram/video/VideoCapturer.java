package fr.anisikram.video;

import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

import java.util.Objects;

public class VideoCapturer {

    // L'objet FrameGrabber est l'interface principale pour accéder à la caméra avec JavaCV
    private OpenCVFrameGrabber camera;
    private boolean opened = false;

    public VideoCapturer(int deviceId) {
        try {
            // Initialiser la caméra avec l'ID fourni
            camera = new OpenCVFrameGrabber(deviceId);
            camera.start();

            // Attendre un court instant pour que la caméra s'initialise
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Configurer la résolution de capture
            camera.setImageWidth(1280);
            camera.setImageHeight(720);

            opened = true;
            System.out.println("Caméra initialisée avec succès (ID: " + deviceId + ")");
        } catch (FrameGrabber.Exception e) {
            System.err.println("ERREUR : Impossible d'accéder à la caméra avec l'ID " + deviceId);
            System.err.println(e.getMessage());
        }
    }

    public Mat getFrame() {
        if (!opened) {
            System.err.println("ERREUR : La caméra n'est pas ouverte.");
            return null;
        }

        try {
            // Capturer une image de la caméra
            Frame frame = camera.grab();

            if (frame == null) {
                System.err.println("ERREUR : Impossible de capturer l'image.");
                return null;
            }

            // Convertir le Frame JavaCV en Mat OpenCV
            Java2DFrameConverter converter1 = new Java2DFrameConverter();
            OpenCVFrameConverter.ToMat converter2 = new OpenCVFrameConverter.ToMat();
            Mat mat = converter2.convert(frame);

            if (mat == null || mat.empty()) {
                System.err.println("ERREUR : Conversion de l'image échouée.");
                return null;
            }

            return mat;
        } catch (FrameGrabber.Exception e) {
            System.err.println("ERREUR : Problème lors de la capture de l'image: " + e.getMessage());
            return null;
        }
    }

    public boolean isOpened() {
        return opened && camera != null;
    }

    public void close() {
        if (Objects.nonNull(camera)) {
            try {
                camera.stop();
                camera.release();
                opened = false;
                System.out.println("Caméra fermée avec succès.");
            } catch (FrameGrabber.Exception e) {
                System.err.println("ERREUR : Problème lors de la fermeture de la caméra: " + e.getMessage());
            }
        } else {
            System.err.println("ERREUR : La caméra n'est pas initialisée.");
        }
    }
}
