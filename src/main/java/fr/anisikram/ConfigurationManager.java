package fr.anisikram;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * A lightweight configuration manager using the singleton pattern.
 * This class provides centralized access to application configuration settings.
 */
public class ConfigurationManager {
    // Singleton instance
    private static ConfigurationManager instance;
    
    // Configuration storage
    private final Map<String, Object> configValues;
    
    // Default configuration values
    private static final Map<String, Object> DEFAULT_CONFIG = new HashMap<>();
    
    static {
        // Voice synthesis defaults
        DEFAULT_CONFIG.put("voice.default", "fr-FR-Wavenet-C");
        DEFAULT_CONFIG.put("voice.alternative", "fr-FR-Wavenet-B");
        DEFAULT_CONFIG.put("voice.pitch", 0.0);
        DEFAULT_CONFIG.put("voice.rate", 1.0);
        DEFAULT_CONFIG.put("voice.volume", 0.0);
        DEFAULT_CONFIG.put("voice.cooldown", 10000L);
        DEFAULT_CONFIG.put("voice.enabled", true);
        
        // Google Cloud API settings
        DEFAULT_CONFIG.put("google.api.key", "");
        
        // Face recognition settings
        DEFAULT_CONFIG.put("face.model.path", "models/face_recognition_sface_2021dec.onnx");
        
        // Video settings
        DEFAULT_CONFIG.put("video.camera.index", 0);
    }
    
    /**
     * Private constructor to prevent instantiation outside of this class.
     */
    private ConfigurationManager() {
        configValues = new HashMap<>(DEFAULT_CONFIG);
    }
    
    /**
     * Get the singleton instance of the ConfigurationManager.
     * 
     * @return The ConfigurationManager instance
     */
    public static synchronized ConfigurationManager getInstance() {
        if (instance == null) {
            instance = new ConfigurationManager();
        }
        return instance;
    }
    
    /**
     * Get a configuration value.
     * 
     * @param key The configuration key
     * @param <T> The expected type of the configuration value
     * @return The configuration value, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) configValues.get(key);
    }
    
    /**
     * Get a configuration value with a default fallback.
     * 
     * @param key The configuration key
     * @param defaultValue The default value to return if the key is not found
     * @param <T> The expected type of the configuration value
     * @return The configuration value, or the default value if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        return (T) configValues.getOrDefault(key, defaultValue);
    }
    
    /**
     * Set a configuration value.
     * 
     * @param key The configuration key
     * @param value The configuration value
     */
    public void set(String key, Object value) {
        configValues.put(key, value);
    }
    
    /**
     * Reset all configuration values to their defaults.
     */
    public void resetToDefaults() {
        configValues.clear();
        configValues.putAll(DEFAULT_CONFIG);
    }
    
    /**
     * Load configuration from a properties file.
     * 
     * @param filePath The path to the properties file
     * @return true if the configuration was loaded successfully, false otherwise
     */
    public boolean loadFromFile(String filePath) {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(filePath)) {
            properties.load(fis);
            
            // Convert properties to configuration values
            for (String key : properties.stringPropertyNames()) {
                String value = properties.getProperty(key);
                
                // Try to convert the value to the appropriate type based on the default value
                if (DEFAULT_CONFIG.containsKey(key)) {
                    Object defaultValue = DEFAULT_CONFIG.get(key);
                    switch (defaultValue) {
                        case Boolean b -> configValues.put(key, b);
                        case Integer i -> configValues.put(key, i);
                        case Long l -> configValues.put(key, Long.parseLong(value));
                        case Double v -> configValues.put(key, Double.parseDouble(value));
                        case Float v -> configValues.put(key, Float.parseFloat(value));
                        case null, default -> configValues.put(key, value);
                    }
                } else {
                    // If the key is not in the default config, store it as a string
                    configValues.put(key, value);
                }
            }
            return true;
        } catch (IOException e) {
            System.err.println("Error loading configuration from file: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Save configuration to a properties file.
     * 
     * @param filePath The path to the properties file
     * @return true if the configuration was saved successfully, false otherwise
     */
    public boolean saveToFile(String filePath) {
        Properties properties = new Properties();
        
        // Convert configuration values to properties
        for (Map.Entry<String, Object> entry : configValues.entrySet()) {
            properties.setProperty(entry.getKey(), entry.getValue().toString());
        }
        
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            properties.store(fos, "Friends Facial Recognition Configuration");
            return true;
        } catch (IOException e) {
            System.err.println("Error saving configuration to file: " + e.getMessage());
            return false;
        }
    }
}