package com.juahaki.juahaki.service.ai.quiz;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@Slf4j
public class PromptTemplateService {

    @Value("classpath:templates/quiz-prompt-template.txt")
    private Resource promptTemplatResource;

    private String prompTemplate;

    @PostConstruct
    public void loadPromptTemplate() {
        try {
            this.prompTemplate = StreamUtils.copyToString(
                    promptTemplatResource.getInputStream(),
                    StandardCharsets.UTF_8
            );
            log.info("Successfuly loaded quiz prompt template");
        } catch (IOException e) {
            log.error("Failed to load quiz prompt template", e);
        }
    }

    public String getQuizPromptTemplate() {
        return prompTemplate;
    }

    public String buildPrompt(Map<String, Object> variables) {
        String prompt = prompTemplate;

        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            prompt = prompt.replace(placeholder, value);
        }

        return prompt;
    }
}
