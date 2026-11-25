package com.cesar.validation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Storage storage = new Storage();
    private final Ocr ocr = new Ocr();
    private final Ai ai = new Ai();
    private final Validation validation = new Validation();

    public Storage getStorage() {
        return storage;
    }

    public Ocr getOcr() {
        return ocr;
    }

    public Ai getAi() {
        return ai;
    }

    public Validation getValidation() {
        return validation;
    }

    public static class Storage {
        private String uploadDir = "/tmp/residence-validation-uploads";
        private long maxFileSizeBytes = 10_485_760L;

        public String getUploadDir() {
            return uploadDir;
        }

        public void setUploadDir(String uploadDir) {
            this.uploadDir = uploadDir;
        }

        public long getMaxFileSizeBytes() {
            return maxFileSizeBytes;
        }

        public void setMaxFileSizeBytes(long maxFileSizeBytes) {
            this.maxFileSizeBytes = maxFileSizeBytes;
        }
    }

    public static class Ocr {
        private String datapath = "/usr/share/tessdata";
        private String language = "por+eng";

        public String getDatapath() {
            return datapath;
        }

        public void setDatapath(String datapath) {
            this.datapath = datapath;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }
    }

    public static class Ai {
        private final Gemini gemini = new Gemini();

        public Gemini getGemini() {
            return gemini;
        }

        public static class Gemini {
            private String apiKey = "";
            private String model = "gemini-4-hydrogen";
            private String apiUrl = "https://generativelanguage.googleapis.com/v1/interactions";
            private int timeoutSeconds = 45;
            private boolean fallbackHeuristicEnabled = true;

            public String getApiKey() {
                return apiKey;
            }

            public void setApiKey(String apiKey) {
                this.apiKey = apiKey;
            }

            public String getModel() {
                return model;
            }

            public void setModel(String model) {
                this.model = model;
            }

            public String getApiUrl() {
                return apiUrl;
            }

            public void setApiUrl(String apiUrl) {
                this.apiUrl = apiUrl;
            }

            public int getTimeoutSeconds() {
                return timeoutSeconds;
            }

            public void setTimeoutSeconds(int timeoutSeconds) {
                this.timeoutSeconds = timeoutSeconds;
            }

            public boolean isFallbackHeuristicEnabled() {
                return fallbackHeuristicEnabled;
            }

            public void setFallbackHeuristicEnabled(boolean fallbackHeuristicEnabled) {
                this.fallbackHeuristicEnabled = fallbackHeuristicEnabled;
            }
        }
    }

    public static class Validation {
        private int maxDocumentAgeDays = 90;
        private int minOcrChars = 25;
        private double nameSimilarityThreshold = 0.60;

        public int getMaxDocumentAgeDays() {
            return maxDocumentAgeDays;
        }

        public void setMaxDocumentAgeDays(int maxDocumentAgeDays) {
            this.maxDocumentAgeDays = maxDocumentAgeDays;
        }

        public int getMinOcrChars() {
            return minOcrChars;
        }

        public void setMinOcrChars(int minOcrChars) {
            this.minOcrChars = minOcrChars;
        }

        public double getNameSimilarityThreshold() {
            return nameSimilarityThreshold;
        }

        public void setNameSimilarityThreshold(double nameSimilarityThreshold) {
            this.nameSimilarityThreshold = nameSimilarityThreshold;
        }
    }
}
