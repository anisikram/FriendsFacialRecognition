package fr.anisikram.faces;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.FaceRecognizerSF;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe permettant de reconnaître des visages à l'aide de FaceRecognizerSF d'OpenCV.
 * La classe peut être utilisée pour enregistrer des visages connus et les reconnaître ultérieurement.
 */
public class FaceRecognizer {

    // Le modèle de reconnaissance faciale FaceRecognizerSF
    private FaceRecognizerSF faceRecognizer;

    // Seuil de confiance pour la reconnaissance (entre 0 et 1)
    private float confidenceThreshold;

    // Base de données des caractéristiques faciales connues
    private final List<Mat> faceFeatures;

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
        this.faceRecognizer = FaceRecognizerSF.create(modelPath, configPath);
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

        try {
            // Prétraitement de l'image si nécessaire
            Mat processedFace = preprocessFace(faceImage);

            // Extraction des caractéristiques faciales
            Mat faceFeature = new Mat();
            faceRecognizer.feature(processedFace, faceFeature);

            // Ajout des caractéristiques et du nom à la base de données
            faceFeatures.add(faceFeature);
            faceNames.add(personName);

            System.out.println("Visage de '" + personName + "' ajouté à la base de données.");
            return true;
        } catch (Exception e) {
            System.err.println("Erreur lors de l'ajout du visage: " + e.getMessage());
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

        try {
            // Prétraitement de l'image
            Mat processedFace = preprocessFace(faceImage);

            // Extraction des caractéristiques faciales
            Mat queryFeature = new Mat();
            faceRecognizer.feature(processedFace, queryFeature);

            // Recherche du visage le plus similaire
            double bestMatch = -1;
            int bestMatchIndex = -1;

            // Comparaison avec tous les visages connus
            for (int i = 0; i < faceFeatures.size(); i++) {
                // Calcul de la similarité cosinus entre les caractéristiques
                double similarity = faceRecognizer.match(queryFeature, faceFeatures.get(i), FaceRecognizerSF.FR_COSINE);

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
     * Prétraite une image de visage pour la reconnaissance.
     *
     * @param faceImage Image Mat contenant un visage
     * @return Image Mat prétraitée
     */
    private Mat preprocessFace(Mat faceImage) {
        Mat processedFace = new Mat();

        // Amélioration du contraste avant traitement
        Mat enhanced = new Mat();
        Core.normalize(faceImage, enhanced, 0, 255, Core.NORM_MINMAX);

        // Réduction du bruit
        Mat denoised = new Mat();
        Imgproc.GaussianBlur(enhanced, denoised, new Size(3, 3), 0);

        // Redimensionnement si nécessaire (certains modèles nécessitent une taille spécifique)
        if (faceImage.rows() != 224 || faceImage.cols() != 224) {
            Imgproc.resize(faceImage, processedFace, new Size(224, 224));
        } else {
            faceImage.copyTo(processedFace);
        }

        // Conversion en BGR si l'image est en niveaux de gris
        if (processedFace.channels() == 1) {
            Imgproc.cvtColor(processedFace, processedFace, Imgproc.COLOR_GRAY2BGR);
        }

        // Normalisation d'histogramme pour améliorer le contraste
        if (processedFace.channels() == 1) {
            Imgproc.equalizeHist(processedFace, processedFace);
            Imgproc.cvtColor(processedFace, processedFace, Imgproc.COLOR_GRAY2BGR);
        } else {
            // Conversion en YUV pour égaliser uniquement la luminance
            Mat yuv = new Mat();
            Imgproc.cvtColor(processedFace, yuv, Imgproc.COLOR_BGR2YUV);
            List<Mat> channels = new ArrayList<>();
            Core.split(yuv, channels);
            Imgproc.equalizeHist(channels.getFirst(), channels.getFirst());
            Core.merge(channels, yuv);
            Imgproc.cvtColor(yuv, processedFace, Imgproc.COLOR_YUV2BGR);
        }

        // Normalisation finale
        Mat normalized = new Mat();
        processedFace.convertTo(normalized, CvType.CV_32F, 1.0/255);

        return normalized;
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

            // Préparation du dossier de destination si nécessaire
            java.nio.file.Path path = java.nio.file.Paths.get(filePath);
            java.nio.file.Files.createDirectories(path.getParent());

            // Structure pour stocker toutes les caractéristiques
            List<float[]> allFeatures = new ArrayList<>();

            // Extraction des caractéristiques de chaque visage
            for (Mat feature : faceFeatures) {
                float[] featureArray = new float[(int) feature.total()];
                feature.get(0, 0, featureArray);
                allFeatures.add(featureArray);
            }

            // Sauvegarde des caractéristiques en binaire
            try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(
                    new java.io.FileOutputStream(filePath + ".features"))) {
                oos.writeObject(allFeatures);
            }

            // Sauvegarde des noms
            java.nio.file.Files.write(java.nio.file.Paths.get(filePath + ".names"),
                    String.join("\n", faceNames).getBytes());

            System.out.println("Base de données sauvegardée avec succès dans : " + filePath);
            return true;
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde de la base de données: " + e.getMessage());
            e.printStackTrace(); // Affichage de la trace complète pour débogage
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


            // Vérification de l'existence des fichiers
            java.io.File featuresFile = new java.io.File(filePath + ".features");
            java.io.File namesFile = new java.io.File(filePath + ".names");

            if (!featuresFile.exists() || !namesFile.exists()) {
                System.err.println("Fichiers de base de données introuvables: " + filePath);
                return false;
            }
            // Chargement des noms
            String namesContent = new String(java.nio.file.Files.readAllBytes(
                    java.nio.file.Paths.get(filePath + ".names")));
            String[] names = namesContent.split("\n");

            // Chargement des caractéristiques
            List<float[]> loadedFeatures;
            try (java.io.ObjectInputStream ois = new java.io.ObjectInputStream(
                    new java.io.FileInputStream(filePath + ".features"))) {
                loadedFeatures = (List<float[]>) ois.readObject();
            }

            // Vérification de la cohérence des données
            if (loadedFeatures.size() != names.length) {
                System.err.println("Les données chargées sont incohérentes: " +
                        loadedFeatures.size() + " caractéristiques vs " +
                        names.length + " noms");
                return false;
            }

            // Réinitialisation des listes
            faceFeatures.clear();
            faceNames.clear();

            // Reconstruction des caractéristiques individuelles et des noms
            for (int i = 0; i < loadedFeatures.size(); i++) {
                float[] featureArray = loadedFeatures.get(i);
                Mat feature = new Mat(1, featureArray.length, CvType.CV_32F);
                feature.put(0, 0, featureArray);

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
            e.printStackTrace(); // Affichage de la trace complète pour débogage
            return false;
        }
    }

    /**
     * Libère les ressources utilisées par la classe.
     */
    public void release() {
        // Libération des ressources OpenCV

        // Libération des caractéristiques faciales
        for (Mat feature : faceFeatures) {
            feature.release();
        }

        faceFeatures.clear();
        faceNames.clear();

        System.out.println("Ressources libérées.");
    }
}
