package fr.anisikram;

import com.google.auth.Credentials;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoogleCloudApiKeyCredentials {

    public static Credentials createCredentialsFromApiKey(String apiKey) {
        // Créer une instance de ApiKeyCredentials
        return new ApiKeyCredentials(apiKey);
    }

    // Classe interne pour gérer les credentials basées sur une API Key
    private static class ApiKeyCredentials extends Credentials {
        private final String apiKey;

        public ApiKeyCredentials(String apiKey) {
            this.apiKey = apiKey;
        }

        @Override
        public String getAuthenticationType() {
            return "API_KEY";
        }

        @Override
        public void refresh() throws IOException {
            // Pas besoin de refresh pour les API keys
        }

        @Override
        public Map<String, List<String>> getRequestMetadata(URI uri) {
            Map<String, List<String>> metadata = new HashMap<>();
            metadata.put("x-goog-api-key", Collections.singletonList(apiKey));
            return metadata;
        }

        @Override
        public boolean hasRequestMetadata() {
            return true;
        }

        @Override
        public boolean hasRequestMetadataOnly() {
            return true;
        }


    }
}
