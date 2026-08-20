package com.tcs.module.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSourceResponse {
    private String sourceId;
    private String sourceType;
    private String title;
    private String snippet;
    private Double similarity;
    private Double finalScore;
    private String visibility;

    public static AiSourceResponseBuilder builder() {
        return new AiSourceResponseBuilder();
    }

    public static class AiSourceResponseBuilder {
        private String sourceId;
        private String sourceType;
        private String title;
        private String snippet;
        private Double similarity;
        private Double finalScore;
        private String visibility;

        public AiSourceResponseBuilder sourceId(String sourceId) {
            this.sourceId = sourceId;
            return this;
        }

        public AiSourceResponseBuilder sourceType(String sourceType) {
            this.sourceType = sourceType;
            return this;
        }

        public AiSourceResponseBuilder title(String title) {
            this.title = title;
            return this;
        }

        public AiSourceResponseBuilder snippet(String snippet) {
            this.snippet = snippet;
            return this;
        }

        public AiSourceResponseBuilder similarity(Double similarity) {
            this.similarity = similarity;
            return this;
        }

        public AiSourceResponseBuilder finalScore(Double finalScore) {
            this.finalScore = finalScore;
            return this;
        }

        public AiSourceResponseBuilder visibility(String visibility) {
            this.visibility = visibility;
            return this;
        }

        public AiSourceResponse build() {
            return new AiSourceResponse(sourceId, sourceType, title, snippet, similarity, finalScore, visibility);
        }
    }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }

    public Double getSimilarity() { return similarity; }
    public void setSimilarity(Double similarity) { this.similarity = similarity; }

    public Double getFinalScore() { return finalScore; }
    public void setFinalScore(Double finalScore) { this.finalScore = finalScore; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
}
