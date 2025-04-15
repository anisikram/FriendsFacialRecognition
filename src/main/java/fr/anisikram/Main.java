package fr.anisikram;

import fr.anisikram.faces.FaceDetector;
import fr.anisikram.faces.FaceRecognizer;
import fr.anisikram.video.VideoCapturer;
import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Scalar;
import org.opencv.core.Point;

import java.util.List;
import java.util.Scanner;

/**
 * Classe principale démontrant l'utilisation de FaceRecognizer avec le détecteur de visages
 * et la capture vidéo.
 */
public class Main {
    public static void main(String[] args) {
        // Option pour activer/désactiver la synthèse vocale
        boolean enableSpeech = true; // Activée par défaut

        // Traitement des arguments en ligne de commande
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--no-speech") || arg.equalsIgnoreCase("-ns")) {
                enableSpeech = false;
                System.out.println("Synthèse vocale désactivée par argument en ligne de commande.");
            } else if (arg.equalsIgnoreCase("--help") || arg.equalsIgnoreCase("-h")) {
                System.out.println("Options disponibles:");
                System.out.println("  --no-speech, -ns : Désactiver la synthèse vocale");
                System.out.println("  --speech, -s     : Activer la synthèse vocale (défaut)");
                System.out.println("  --help, -h       : Afficher cette aide");
                System.exit(0);
            }
        }

        try {
            System.out.println("Initialisation du système de reconnaissance faciale...");
            OpenCV.loadLocally();
            System.out.println("Bibliothèque OpenCV chargée avec succès : " + Core.VERSION);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Impossible de charger la bibliothèque native OpenCV :");
            System.err.println(e.getMessage());
            System.exit(1);
        }

        // Initialisation de la capture vidéo
        VideoCapturer videoCapturer = new VideoCapturer(0);

        if (!videoCapturer.isOpened()) {
            System.err.println("Impossible d'ouvrir la caméra. Vérifiez les connexions et les permissions.");
            System.exit(1);
        }

        // Initialisation du détecteur de visages
        FaceDetector faceDetector = new FaceDetector();

        // Initialisation du reconnaisseur de visages avec le modèle pré-entraîné
        // Note: Remplacez le chemin par l'emplacement de votre modèle
        FaceRecognizer faceRecognizer = new FaceRecognizer("models/face_recognition_sface_2021dec.onnx");

        // Initialisation avec l'option d'activation/désactivation
        VoiceSynthesizer voiceSynthesizer = new VoiceSynthesizer(10000, enableSpeech);
        boolean speechEnabled = enableSpeech && voiceSynthesizer.initialize();

        if (speechEnabled) {
            System.out.println("Synthèse vocale initialisée avec succès.");
            voiceSynthesizer.setPitch(1f);  // Ajustement du ton
            voiceSynthesizer.setRate(1f);   // Ajustement de la vitesse
            voiceSynthesizer.setVolume(1f);
        } else {
            System.out.println("La synthèse vocale n'est pas disponible ou est désactivée.");
        }

        // Scanner pour lire les entrées utilisateur
        Scanner scanner = new Scanner(System.in);

        System.out.println("Mode d'utilisation :");
        System.out.println("1. Appuyez sur 'A' pour ajouter un visage à la base de données");
        System.out.println("2. Appuyez sur 'L' pour charger une base de données existante");
        System.out.println("3. Appuyez sur 'S' pour sauvegarder la base de données actuelle");
        System.out.println("4. Appuyez sur 'V' pour activer/désactiver la synthèse vocale");
        System.out.println("5. Appuyez sur 'Échap' pour quitter le programme");

        boolean proceed = true;
        String currentName = ""; // Pour stocker temporairement le nom lors de l'ajout d'un visage
        boolean addingFace = false;

        // Boucle principale
        while (proceed) {
            // Capture d'une image depuis la caméra
            Mat frame = videoCapturer.getFrame();

            if (frame != null && !frame.empty()) {
                // Détection des visages dans l'image
                List<Rect> faces = faceDetector.detectFaces(frame);

                // Traitement des visages détectés
                for (Rect face : faces) {
                    // Extraction du visage depuis l'image
                    Mat faceMat = faceDetector.extractFace(frame, face, true);

                    if (!faceMat.empty()) {
                        if (addingFace) {
                            // Mode ajout de visage
                            // Affichage d'un message sur l'image
                            Imgproc.putText(frame, "Ajout de " + currentName + "...",
                                    new Point(face.x, face.y - 10),
                                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.8,
                                    new Scalar(0, 255, 0), 2);
                        } else {
                            // Mode reconnaissance
                            // Reconnaissance du visage
                            String personName = faceRecognizer.recognize(faceMat);

                            // Affichage du nom reconnu sur l'image
                            Scalar textColor = personName.equals("Inconnu") ?
                                    new Scalar(0, 0, 255) : // Rouge pour inconnu
                                    new Scalar(0, 255, 0);  // Vert pour reconnu

                            Imgproc.putText(frame, personName,
                                    new Point(face.x, face.y - 10),
                                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.8,
                                    textColor, 2);

                            if (speechEnabled && !personName.equals("Inconnu") && !personName.equals("Erreur")) {
                                voiceSynthesizer.greet(personName);
                            }
                        }

                        faceMat.release(); // Libération de la mémoire
                    }
                }

                // Dessin des rectangles autour des visages
                faceDetector.drawFaceRectangles(frame, faces);

                // Affichage de l'image
                HighGui.imshow("Reconnaissance Faciale", frame);

                // Gestion des touches clavier
                int key = HighGui.waitKey(30);

                // Touche Échap pour quitter
                if (key == 27) {
                    proceed = false;
                }
                // Touche 'a' ou 'A' pour ajouter un visage
                else if (key == 'a' || key == 'A') {
                    if (!faces.isEmpty() && !addingFace) {
                        System.out.print("Entrez le nom de la personne : ");
                        currentName = scanner.nextLine().trim();

                        if (!currentName.isEmpty()) {
                            addingFace = true;
                            System.out.println("Positionnez votre visage et appuyez sur ESPACE pour confirmer l'ajout...");
                        }
                    } else if (faces.isEmpty()) {
                        System.out.println("Aucun visage détecté. Veuillez vous positionner face à la caméra.");
                    }
                }
                // Touche ESPACE pour confirmer l'ajout d'un visage
                else if (key == 32 && addingFace) { // Code ASCII de l'espace
                    if (!faces.isEmpty()) {
                        Mat faceMat = faceDetector.extractFace(frame, faces.get(0), true);

                        if (!faceMat.empty()) {
                            boolean success = faceRecognizer.addFace(faceMat, currentName);

                            if (success) {
                                System.out.println("Visage de '" + currentName + "' ajouté avec succès !");
                            } else {
                                System.out.println("Échec de l'ajout du visage.");
                            }

                            faceMat.release();
                        }

                        addingFace = false;
                        currentName = "";
                    } else {
                        System.out.println("Aucun visage détecté. Veuillez vous positionner face à la caméra.");
                    }
                }
                // Touche 's' ou 'S' pour sauvegarder la base de données
                else if (key == 's' || key == 'S') {
                    System.out.print("Entrez le nom du fichier pour la sauvegarde : ");
                    String filename = scanner.nextLine().trim();

                    if (!filename.isEmpty()) {
                        boolean success = faceRecognizer.saveDatabase(filename);

                        if (success) {
                            System.out.println("Base de données sauvegardée avec succès dans '" + filename + "'.");
                        } else {
                            System.out.println("Échec de la sauvegarde de la base de données.");
                        }
                    }
                }
                // Touche 'l' ou 'L' pour charger une base de données
                else if (key == 'l' || key == 'L') {
                    System.out.print("Entrez le nom du fichier à charger : ");
                    String filename = scanner.nextLine().trim();

                    if (!filename.isEmpty()) {
                        boolean success = faceRecognizer.loadDatabase(filename);

                        if (success) {
                            System.out.println("Base de données chargée avec succès depuis '" + filename + "'.");
                        } else {
                            System.out.println("Échec du chargement de la base de données.");
                        }
                    }
                }
                else if (key == 'v' || key == 'V') {
                    enableSpeech = !enableSpeech;
                    voiceSynthesizer.setEnabled(enableSpeech);

                    if (enableSpeech) {
                        if (!voiceSynthesizer.isInitialized()) {
                            speechEnabled = voiceSynthesizer.initialize();
                        } else {
                            speechEnabled = true;
                        }
                        System.out.println("Synthèse vocale activée.");
                        if (speechEnabled) {
                            voiceSynthesizer.speak("Synthèse vocale activée");
                        }
                    } else {
                        System.out.println("Synthèse vocale désactivée.");
                        speechEnabled = false;
                    }
                }
            }
        }

        // Nettoyage et libération des ressources
        scanner.close();
        videoCapturer.close();
        faceRecognizer.release();
        if (voiceSynthesizer.isInitialized()) {
            voiceSynthesizer.release();
        }
        HighGui.destroyAllWindows();
        System.out.println("Programme terminé.");
    }
}
