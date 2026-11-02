package org.wildfly.examples.competence;

import java.time.temporal.ChronoUnit;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import dev.langchain4j.cdi.spi.RegisterAIService;
import dev.langchain4j.service.SystemMessage;

@RegisterAIService(
        chatModelName = "chat-model",
        chatMemoryName = "chat-memory"
        //tools = CompetenceEndpoint.class
)
public interface CompetenceAIService {

    @SystemMessage("""
            You are fellow student keen to make new friends. Chat with users about your
            takes on college lifestyle, your favourite subjects and what you would like
            your future to be.

            Today is {{current_date}}.
            """)
    @Timeout(unit = ChronoUnit.MINUTES, value = 5)
    @Retry(maxRetries = 2)
    @Fallback(fallbackMethod = "fallback")
    String chat(String question);

    default String fallback(String question) {
        return "Sorry, I'm having trouble processing your request at the moment. Please try again later.";
    }
}