package fr.anisikram;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.TextToSpeechSettings;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.protobuf.ByteString;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class VoiceSynthesizer {
    private TextToSpeechClient textToSpeechClient;
    private Map<String, Long> lastGreetingTime;
    private long greetingCooldown;
    private boolean initialized;
    private boolean enabled;

    // Voix françaises disponibles sur Google TTS
    private static final String DEFAULT_VOICE = "fr-FR-Wavenet-C"; // Voix française masculine
    private static final String ALTERNATIVE_VOICE = "fr-FR-Wavenet-B"; // Voix française féminine alternative
    
    // Paramètres de la voix
    private double pitch = 0.0; // Valeur par défaut (entre -20.0 et 20.0)
    private double speakingRate = 1.0; // Valeur par défaut (entre 0.25 et 4.0)
    private double volume = 0.0; // Valeur par défaut (entre -96.0 et 16.0)
    private String currentVoice = DEFAULT_VOICE;

    public VoiceSynthesizer() {
        this(10000, true); // 10 secondes par défaut, activé par défaut
    }

    public VoiceSynthesizer(long cooldownMs) {
        this(cooldownMs, true); // Activé par défaut
    }

    public VoiceSynthesizer(long cooldownMs, boolean enabled) {
        this.lastGreetingTime = new HashMap<>();
        this.greetingCooldown = cooldownMs;
        this.initialized = false;
        this.enabled = enabled;
    }

    public boolean initialize() {
        if (!enabled) {
            return false;
        }

        try {
            var textToSpeechClientSettings = TextToSpeechSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(GoogleCloudApiKeyCredentials.createCredentialsFromApiKey("")))
                    .build();

            this.textToSpeechClient = TextToSpeechClient.create(textToSpeechClientSettings);
            System.out.println("Google Text-to-Speech API initialisée avec succès");
            System.out.println("Voix française sélectionnée: " + currentVoice);
            
            this.initialized = true;
            return true;
        } catch (IOException e) {
            System.err.println("Erreur lors de l'initialisation du synthétiseur vocal Google: " + e.getMessage());
            return false;
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled && initialized && textToSpeechClient != null) {
            release();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void greet(String name) {
        if (!initialized || !enabled || name == null || name.isEmpty() || name.equals("Inconnu") || name.equals("Erreur")) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (lastGreetingTime.containsKey(name)) {
            long lastTime = lastGreetingTime.get(name);
            if (currentTime - lastTime < greetingCooldown) {
                return;
            }
        }

        lastGreetingTime.put(name, currentTime);
        
        String greeting = "Bonjour " + name;
        speak(greeting);
    }

    public void setPitch(float pitch) {
        if (initialized) {
            this.pitch = pitch;
        }
    }

    public void setRate(float rate) {
        if (initialized) {
            // Google TTS utilise des valeurs entre 0.25 et 4.0
            // Nous pouvons utiliser la valeur directement
            this.speakingRate = rate;
        }
    }

    public void setVolume(float volume) {
        if (initialized) {
            // Google TTS utilise des valeurs entre -96.0 et 16.0
            // Convertir de l'échelle 0.0-2.0 à -16.0-16.0
            this.volume = (volume - 1.0) * 16.0;
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void release() {
        if (initialized && textToSpeechClient != null) {
            try {
                textToSpeechClient.close();
                this.initialized = false;
            } catch (Exception e) {
                System.err.println("Erreur lors de la fermeture du client TTS: " + e.getMessage());
            }
        }
    }
    
    // Méthode utilitaire pour parler un texte quelconque
    public void speak(String text) {
        if (!initialized || text == null || text.isEmpty() || textToSpeechClient == null) {
            return;
        }

        try {
            // Configurer la requête TTS
            SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();
            
            // Configurer la voix
            VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                .setLanguageCode("fr-FR")
                .setName(currentVoice)
                .build();
            
            // Configurer les paramètres audio
            AudioConfig audioConfig = AudioConfig.newBuilder()
                .setAudioEncoding(AudioEncoding.LINEAR16)
                .setPitch(pitch)
                .setSpeakingRate(speakingRate)
                .setVolumeGainDb((float)volume)
                .build();
            
            // Effectuer la requête de synthèse
            SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);
            ByteString audioContent = response.getAudioContent();
            
            // Jouer l'audio
            playAudio(audioContent.toByteArray());
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la synthèse vocale: " + e.getMessage());
        }
    }
    
    // Méthode pour changer de voix
    public void setVoice(String voiceName) {
        if (voiceName != null && !voiceName.isEmpty()) {
            this.currentVoice = voiceName;
        }
    }
    
    // Utiliser la voix masculine par défaut
    public void useDefaultVoice() {
        this.currentVoice = DEFAULT_VOICE;
    }
    
    // Utiliser la voix féminine alternative
    public void useAlternativeVoice() {
        this.currentVoice = ALTERNATIVE_VOICE;
    }
    
    // Méthode privée pour jouer l'audio reçu
    private void playAudio(byte[] audioData) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        // Convertir les bytes en InputStream
        ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
        
        // Obtenir l'AudioInputStream
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bais);
        
        // Obtenir le format audio
        AudioFormat audioFormat = audioInputStream.getFormat();
        
        // Obtenir une ligne audio
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine audioLine = (SourceDataLine) AudioSystem.getLine(info);
        
        // Ouvrir et démarrer la ligne audio
        audioLine.open(audioFormat);
        audioLine.start();
        
        // Lire les données audio
        byte[] buffer = new byte[4096];
        int bytesRead = 0;
        while ((bytesRead = audioInputStream.read(buffer)) != -1) {
            audioLine.write(buffer, 0, bytesRead);
        }
        
        // Fermer les ressources
        audioLine.drain();
        audioLine.close();
        audioInputStream.close();
    }
}
