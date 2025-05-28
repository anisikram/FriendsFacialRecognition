package fr.anisikram.faces;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_face.FaceRecognizerSF;
import org.bytedeco.javacpp.indexer.FloatIndexer;


import java.util.ArrayList;
import java.util.List;

/**
 * Classe permettant de reconnaître des visages à l'aide de FaceRecognizerSF d'OpenCV.
 * La classe peut être utilisée pour enregistrer des visages connus et les reconnaître ultérieurement.
 */
public class FaceRecognizer {

    // Le modèle de reconnaissance faciale FaceRecognizerSF
    private org.bytedeco.opencv.opencv_face.FaceRecognizerSF faceRecognizer;

    // Seuil de confiance pour la reconnaissance (entre 0 et 1)
    private float confidenceThreshold;

    // Base de données des caractéristiques faciales connues
    private final List<org.bytedeco.opencv.opencv_core.Mat> faceFeatures;

    // Noms associés aux visages connus (même index que faceFeatures)
    private List<String> faceNames;

    /**
     * Constructeur initialisant le modèle de reconnaissance faciale.
     *
     * @param modelPath Chemin vers le fichier du modèle FaceRecognizerSF préentraîné
     * @param configPath Chemin vers le fichier de configuration (peut être vide)
     * @param threshold Seuil de confiance pour la reconnaissance (0.4 par défaut)
     */
    public FaceRecognizer(String modelPath, String configPath, float threshold) {
        // Initialisation du modèle FaceRecognizerSF
        this.faceRecognizer = org.bytedeco.opencv.opencv_face.FaceRecognizerSF.create(modelPath, configPath);
        this.confidenceThreshold = threshold;

        // Initialisation des listes pour stocker les visages connus
        this.faceFeatures = new ArrayList<>();
        this.faceNames = new ArrayList<>();

        System.out.println("FaceRecognizer initialisé avec succès.");
    }

    /**
     * Constructeur avec valeurs par défaut.
     *
     * @param modelPath Chemin vers le fichier du modèle
     */
    public FaceRecognizer(String modelPath) {
        this(modelPath, "", 0.6f);
    }

    /**
     * Ajoute un visage à la base de données des visages connus.
     *
     * @param faceImage Image Mat contenant un visage aligné
     * @param personName Nom de la personne
     * @return true si l'ajout a réussi, false sinon
     */
    public boolean addFace(Mat faceImage, String personName) {
        if (faceImage.empty()) {
            System.err.println("L'image du visage est vide.");
            return false;
        }

package fr.anisikram.faces;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_face.FaceRecognizerSF;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.PointerScope; // Import for PointerScope


import java.util.ArrayList;
import java.util.List;

/**
 * Classe permettant de reconnaître des visages à l'aide de FaceRecognizerSF d'OpenCV.
 * La classe peut être utilisée pour enregistrer des visages connus et les reconnaître ultérieurement.
 */
public class FaceRecognizer {

    // Le modèle de reconnaissance faciale FaceRecognizerSF
    private org.bytedeco.opencv.opencv_face.FaceRecognizerSF faceRecognizer;

    // Seuil de confiance pour la reconnaissance (entre 0 et 1)
    private float confidenceThreshold;

    // Base de données des caractéristiques faciales connues
    private final List<org.bytedeco.opencv.opencv_core.Mat> faceFeatures;

    // Noms associés aux visages connus (même index que faceFeatures)
    private List<String> faceNames;

    /**
     * Constructeur initialisant le modèle de reconnaissance faciale.
     *
     * @param modelPath Chemin vers le fichier du modèle FaceRecognizerSF préentraîné
     * @param configPath Chemin vers le fichier de configuration (peut être vide)
     * @param threshold Seuil de confiance pour la reconnaissance (0.4 par défaut)
     */
    public FaceRecognizer(String modelPath, String configPath, float threshold) {
        // Initialisation du modèle FaceRecognizerSF
        this.faceRecognizer = org.bytedeco.opencv.opencv_face.FaceRecognizerSF.create(modelPath, configPath);
        this.confidenceThreshold = threshold;

        // Initialisation des listes pour stocker les visages connus
        this.faceFeatures = new ArrayList<>();
        this.faceNames = new ArrayList<>();

        System.out.println("FaceRecognizer initialisé avec succès.");
    }

    /**
     * Constructeur avec valeurs par défaut.
     *
     * @param modelPath Chemin vers le fichier du modèle
     */
    public FaceRecognizer(String modelPath) {
        this(modelPath, "", 0.6f);
    }

    /**
     * Ajoute un visage à la base de données des visages connus.
     *
     * @param faceImage Image Mat contenant un visage aligné
     * @param personName Nom de la personne
     * @return true si l'ajout a réussi, false sinon
     */
    public boolean addFace(Mat faceImage, String personName) {
        if (faceImage.empty()) {
            System.err.println("L'image du visage est vide.");
            return false;
        }

        try (Mat processedFace = preprocessFace(faceImage)) {
            // Extraction des caractéristiques faciales
            // faceFeature is added to a list and managed by the class, so it should not be in try-with-resources here.
            Mat faceFeature = new Mat();
            faceRecognizer.feature(processedFace, faceFeature);

            // Ajout des caractéristiques et du nom à la base de données
            faceFeatures.add(faceFeature); // faceFeature is now managed by the list
            faceNames.add(personName);

            System.out.println("Visage de '" + personName + "' ajouté à la base de données.");
            return true;
        } catch (Exception e) {
            System.err.println("Erreur lors de l'ajout du visage: " + e.getMessage());
            // If an exception occurs, faceFeature might have been created but not added to the list.
            // However, if faceRecognizer.feature() or preprocessFace() throws,
            // faceFeature might be empty or not initialized.
            // It's generally safer not to try to close faceFeature here as it's intended to be stored.
            // The original code didn't attempt to close faceFeature in case of an error,
            // and since it's added to a list for later management, this is consistent.
            return false;
        }
    }

    /**
     * Reconnaît un visage à partir d'une image.
     *
     * @param faceImage Image Mat contenant un visage aligné
     * @return Le nom de la personne reconnue ou "Inconnu" si le visage n'est pas reconnu
     */
    public String recognize(Mat faceImage) {
        if (faceImage.empty()) {
            System.err.println("L'image du visage est vide.");
            return "Inconnu";
        }

        if (faceFeatures.isEmpty()) {
            System.err.println("Aucun visage dans la base de données.");
            return "Inconnu";
        }

        try (Mat processedFace = preprocessFace(faceImage); Mat queryFeature = new Mat()) {
            // Extraction des caractéristiques faciales
            faceRecognizer.feature(processedFace, queryFeature);

            // Recherche du visage le plus similaire
            double bestMatch = -1;
            int bestMatchIndex = -1;

            // Comparaison avec tous les visages connus
            for (int i = 0; i < faceFeatures.size(); i++) {
                // Calcul de la similarité cosinus entre les caractéristiques
                double similarity = faceRecognizer.match(queryFeature, faceFeatures.get(i), org.bytedeco.opencv.opencv_face.FaceRecognizerSF.FR_COSINE);

                // Si la similarité est supérieure au meilleur match actuel, on met à jour
                if (similarity > bestMatch) {
                    bestMatch = similarity;
                    bestMatchIndex = i;
                }
            }

            // Si le meilleur match dépasse le seuil de confiance, on retourne le nom associé
            if (bestMatch > confidenceThreshold && bestMatchIndex != -1) {
                System.out.println("Visage reconnu: '" + faceNames.get(bestMatchIndex) +
                        "' avec une confiance de " + bestMatch);
                return faceNames.get(bestMatchIndex);
            } else {
                System.out.println("Visage non reconnu. Meilleure correspondance: " + bestMatch);
                return "Inconnu";
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la reconnaissance du visage: " + e.getMessage());
            return "Erreur";
        }
    }

    /**
     * Prétraite une image de visage pour la reconnaissance. Cette méthode gère
     * la fermeture de tous les objets Mat intermédiaires créés pendant le traitement.
     *
     * @param faceImage Image Mat contenant un visage.
     * @return Une nouvelle image Mat prétraitée. L'appelant est responsable de la fermeture
     *         de cette image Mat retournée en appelant sa méthode .close().
     */
    private Mat preprocessFace(Mat faceImage) {
        Mat normalized = new Mat(); // This Mat will be returned, created outside PointerScope

        try (PointerScope scope = new PointerScope()) {
            Mat currentProcessedMat = new Mat(); // Temporary Mat for intermediate steps

            Mat enhanced = new Mat();
            opencv_core.normalize(faceImage, enhanced, 0, 255, opencv_core.NORM_MINMAX, -1, null);

            Mat denoised = new Mat();
            opencv_imgproc.GaussianBlur(enhanced, denoised, new Size(3, 3), 0, 0, opencv_core.BORDER_DEFAULT);
            // enhanced is no longer needed, will be closed by scope

            // Redimensionnement si nécessaire
            if (denoised.rows() != 224 || denoised.cols() != 224) {
                opencv_imgproc.resize(denoised, currentProcessedMat, new Size(224, 224));
            } else {
                denoised.copyTo(currentProcessedMat);
            }
            // denoised is no longer needed, will be closed by scope

            // Conversion en BGR si l'image est en niveaux de gris
            if (currentProcessedMat.channels() == 1) {
                Mat tempBgr = new Mat();
                opencv_imgproc.cvtColor(currentProcessedMat, tempBgr, opencv_imgproc.COLOR_GRAY2BGR);
                currentProcessedMat = tempBgr; // currentProcessedMat (old) will be closed by scope, tempBgr is now tracked
            }

            // Normalisation d'histogramme pour améliorer le contraste
            if (currentProcessedMat.channels() == 1) { // Should generally not happen if previous step converted to BGR
                Mat equalizedGray = new Mat();
                opencv_imgproc.equalizeHist(currentProcessedMat, equalizedGray);
                Mat tempBgr = new Mat();
                opencv_imgproc.cvtColor(equalizedGray, tempBgr, opencv_imgproc.COLOR_GRAY2BGR);
                currentProcessedMat = tempBgr; // equalizedGray and old currentProcessedMat closed by scope
            } else { // Assuming BGR image
                Mat yuv = new Mat();
                opencv_imgproc.cvtColor(currentProcessedMat, yuv, opencv_imgproc.COLOR_BGR2YUV);

                MatVector channels = new MatVector();
                opencv_core.split(yuv, channels);
                // yuv is no longer needed, will be closed by scope

                Mat channelToEqualize = channels.get(0); // This is a view, not a new Mat
                Mat equalizedChannel = new Mat();
                opencv_imgproc.equalizeHist(channelToEqualize, equalizedChannel);
                channels.put(0, equalizedChannel); // equalizedChannel replaces the view in channels
                                                  // original channelToEqualize (view) is fine, equalizedChannel is tracked by scope

                Mat mergedYuv = new Mat();
                opencv_core.merge(channels, mergedYuv);
                // channels and its Mats (including equalizedChannel) will be closed by scope

                Mat finalBgr = new Mat();
                opencv_imgproc.cvtColor(mergedYuv, finalBgr, opencv_imgproc.COLOR_YUV2BGR);
                currentProcessedMat = finalBgr; // mergedYuv and old currentProcessedMat closed by scope
            }

            // Normalisation finale
            currentProcessedMat.convertTo(normalized, opencv_core.CV_32F, 1.0 / 255.0, 0);
            // currentProcessedMat is no longer needed, will be closed by scope
            // 'normalized' is populated with the final result.
        } // PointerScope closes all Mats created within it that were not retained.

        return normalized; // Caller is responsible for closing this Mat
    }

    /**
     * Enregistre la base de données des visages connus dans un fichier.
     *
     * @param filePath Chemin du fichier où enregistrer la base de données
     * @return true si l'enregistrement a réussi, false sinon
     */
    public boolean saveDatabase(String filePath) {
        try {
            if (faceFeatures.isEmpty()) {
                System.err.println("Aucun visage à sauvegarder.");
                return false;
            }

            java.nio.file.Path path = java.nio.file.Paths.get(filePath);
            if (path.getParent() != null) {
                 java.nio.file.Files.createDirectories(path.getParent());
            }

            List<float[]> allFeatures = new ArrayList<>();
            for (Mat feature : faceFeatures) {
                if (feature.empty() || feature.channels() != 1 || feature.depth() != opencv_core.CV_32F) {
                     System.err.println("Skipping invalid feature Mat during save.");
                     continue;
                }
                int totalElements = (int) feature.total() * feature.channels(); // Should be CV_32FC1
                float[] featureArray = new float[totalElements];
                FloatIndexer indexer = feature.createIndexer();
                for (int i = 0; i < totalElements; i++) {
                    featureArray[i] = indexer.get(0, i);
                }
                indexer.close();
                allFeatures.add(featureArray);
            }

            try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(
                    new java.io.FileOutputStream(filePath + ".features"))) {
                oos.writeObject(allFeatures);
            }

            java.nio.file.Files.write(java.nio.file.Paths.get(filePath + ".names"),
                    String.join("\n", faceNames).getBytes());

            System.out.println("Base de données sauvegardée avec succès dans : " + filePath);
            return true;
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde de la base de données: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Charge la base de données des visages connus depuis un fichier.
     *
     * @param filePath Chemin du fichier contenant la base de données
     * @return true si le chargement a réussi, false sinon
     */
    public boolean loadDatabase(String filePath) {
        try {
            java.io.File featuresFile = new java.io.File(filePath + ".features");
            java.io.File namesFile = new java.io.File(filePath + ".names");

            if (!featuresFile.exists() || !namesFile.exists()) {
                System.err.println("Fichiers de base de données introuvables: " + filePath);
                return false;
            }

            String namesContent = new String(java.nio.file.Files.readAllBytes(
                    java.nio.file.Paths.get(filePath + ".names")));
            String[] names = namesContent.split("\n");

            List<float[]> loadedFeatures;
            try (java.io.ObjectInputStream ois = new java.io.ObjectInputStream(
                    new java.io.FileInputStream(filePath + ".features"))) {
                loadedFeatures = (List<float[]>) ois.readObject();
            }

            if (loadedFeatures.size() != names.length) {
                System.err.println("Les données chargées sont incohérentes: " +
                        loadedFeatures.size() + " caractéristiques vs " +
                        names.length + " noms");
                return false;
            }

            // Release existing features before loading new ones
            release(); // Clear current faceFeatures and faceNames

            for (int i = 0; i < loadedFeatures.size(); i++) {
                float[] featureArray = loadedFeatures.get(i);
                // Assuming features are single row, CV_32F
                Mat feature = new Mat(1, featureArray.length, opencv_core.CV_32F);
                FloatIndexer indexer = feature.createIndexer();
                for (int j = 0; j < featureArray.length; j++) {
                    indexer.put(0, j, featureArray[j]);
                }
                indexer.close();
                faceFeatures.add(feature);
                faceNames.add(names[i]);
            }

            System.out.println(loadedFeatures.size() + " visages chargés dans la base de données.");
            return true;
        } catch (ClassNotFoundException e) {
            System.err.println("Erreur: Format de fichier non compatible: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de la base de données: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Libère les ressources utilisées par la classe.
     */
    public void release() {
        for (Mat feature : faceFeatures) {
            if (feature != null && !feature.isNull()) {
                feature.close();
            }
        }
        faceFeatures.clear();
        faceNames.clear();
        // faceRecognizer itself might need a close/release if its JavaCV binding has one
        // For now, assuming it's managed by GC or doesn't have an explicit native resource release method exposed
        System.out.println("Ressources libérées.");
    }
}
