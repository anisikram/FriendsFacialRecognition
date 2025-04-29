package fr.anisikram.faces;

import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.FaceRecognizerSF;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_core.CV_32F;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_GRAY2BGR;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;

public class FaceRecognizerProcessor {

    private FaceRecognizerSF faceRecognizer;

    private float confidenceThreshold;

    private final List<Mat> faceFeatures;

    private List<String> faceNames;

    public FaceRecognizerProcessor(String modelPath, String configPath, float threshold) {
        // Initialisation du modèle FaceRecognizerSF
        this.faceRecognizer = FaceRecognizerSF.create(modelPath, configPath);
        this.confidenceThreshold = threshold;

        // Initialisation des listes pour stocker les visages connus
        this.faceFeatures = new ArrayList<>();
        this.faceNames = new ArrayList<>();

        System.out.println("FaceRecognizer initialisé avec succès.");
    }


    public FaceRecognizerProcessor(String modelPath) {
        this(modelPath, "", 0.6f);
    }

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
                double similarity = faceRecognizer.match(queryFeature, faceFeatures.get(i), 
                                                        FaceRecognizerSF.FR_COSINE);

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

    private Mat preprocessFace(Mat faceImage) {
        Mat processedFace = new Mat();
        resize(faceImage, processedFace, new Size(224, 224));

        if (processedFace.channels() == 1) {
            cvtColor(processedFace, processedFace, COLOR_GRAY2BGR);
        }

        return processedFace;
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
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());

            // Structure pour stocker toutes les caractéristiques
            List<float[]> allFeatures = new ArrayList<>();

            // Extraction des caractéristiques de chaque visage
            for (Mat feature : faceFeatures) {
                int size = (int) feature.total();
                float[] featureArray = new float[size];
                try (FloatPointer floatPointer = new FloatPointer(size)) {
                    feature.data().put(floatPointer);
                    floatPointer.get(featureArray);
                    allFeatures.add(featureArray);
                }
            }

            // Sauvegarde des caractéristiques en binaire
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(filePath + ".features"))) {
                oos.writeObject(allFeatures);
            }

            // Sauvegarde des noms
            Files.write(Paths.get(filePath + ".names"), String.join("\n", faceNames).getBytes());

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
            File featuresFile = new File(filePath + ".features");
            File namesFile = new File(filePath + ".names");

            if (!featuresFile.exists() || !namesFile.exists()) {
                System.err.println("Fichiers de base de données introuvables: " + filePath);
                return false;
            }
            // Chargement des noms
            String namesContent = new String(Files.readAllBytes(
                    Paths.get(filePath + ".names")));
            String[] names = namesContent.split("\n");

            // Chargement des caractéristiques
            List<float[]> loadedFeatures;
            try (ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(filePath + ".features"))) {
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
            for (Mat feature : faceFeatures) {
                if (feature != null && !feature.isNull()) {
                    feature.close();
                }
            }
            faceFeatures.clear();
            faceNames.clear();

            // Reconstruction des caractéristiques individuelles et des noms
            for (int i = 0; i < loadedFeatures.size(); i++) {
                float[] featureArray = loadedFeatures.get(i);
                Mat feature = new Mat(1, featureArray.length, CV_32F);
                try(FloatPointer floatPointer = new FloatPointer(featureArray)) {
                    feature.data().put(floatPointer);
                    faceFeatures.add(feature);
                    faceNames.add(names[i]);
                }
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

    public void release() {
        faceRecognizer.close();
        for (Mat feature : faceFeatures) {
            if (feature != null && !feature.isNull()) {
                feature.close();
            }
        }
        faceFeatures.clear();
        faceNames.clear();
        System.out.println("Ressources libérées.");
    }
}
