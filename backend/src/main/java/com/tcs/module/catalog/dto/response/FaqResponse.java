package com.tcs.module.catalog.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaqResponse {

    private Long faqId;
    private String question;
    private String answer;
    private String category;
    private Integer sortOrder;
    private Boolean published;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getFaqId() { return faqId; }
    public void setFaqId(Long faqId) { this.faqId = faqId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getPublished() { return published; }
    public void setPublished(Boolean published) { this.published = published; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static FaqResponseBuilder builder() {
        return new FaqResponseBuilder();
    }

    public static class FaqResponseBuilder {
        private Long faqId;
        private String question;
        private String answer;
        private String category;
        private Integer sortOrder;
        private Boolean published;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public FaqResponseBuilder faqId(Long faqId) { this.faqId = faqId; return this; }
        public FaqResponseBuilder question(String question) { this.question = question; return this; }
        public FaqResponseBuilder answer(String answer) { this.answer = answer; return this; }
        public FaqResponseBuilder category(String category) { this.category = category; return this; }
        public FaqResponseBuilder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public FaqResponseBuilder published(Boolean published) { this.published = published; return this; }
        public FaqResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public FaqResponseBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public FaqResponse build() { return new FaqResponse(faqId, question, answer, category, sortOrder, published, createdAt, updatedAt); }
    }
}
