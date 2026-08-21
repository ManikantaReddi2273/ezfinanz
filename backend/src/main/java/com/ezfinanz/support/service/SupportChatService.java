package com.ezfinanz.support.service;

import com.ezfinanz.ai.OpenAiClient;
import com.ezfinanz.ai.PineconeClient;
import com.ezfinanz.auth.domain.Role;
import com.ezfinanz.auth.domain.User;
import com.ezfinanz.auth.repo.UserRepository;
import com.ezfinanz.common.ApiException;
import com.ezfinanz.support.dto.SupportChatResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * RAG support chatbot: embeds the customer question, retrieves Pinecone knowledge chunks, and answers via OpenAI.
 * Separate from human support tickets ({@link SupportService}).
 */
@Service
public class SupportChatService {

    private static final int TOP_K = 5;
    private static final int MAX_PER_HOUR = 30;
    private static final double MIN_SCORE = 0.35;
    private static final Pattern CONVERSATIONAL = Pattern.compile(
            "^(hi|hello|hey|hiya|howdy|good\\s*(morning|afternoon|evening)|thanks|thank\\s*you|ty|ok|okay|bye|goodbye|"
                    + "how\\s+are\\s+you|who\\s+are\\s+you|what\\s+can\\s+you\\s+(do|help)|help\\s*me\\??|"
                    + "what\\s+is\\s+this|are\\s+you\\s+(a\\s+)?bot)\\b.*",
            Pattern.CASE_INSENSITIVE
    );

    private final OpenAiClient openAiClient;
    private final PineconeClient pineconeClient;
    private final UserRepository userRepository;
    private final ConcurrentHashMap<Long, List<Long>> rateWindow = new ConcurrentHashMap<>();

    public SupportChatService(
            OpenAiClient openAiClient,
            PineconeClient pineconeClient,
            UserRepository userRepository
    ) {
        this.openAiClient = openAiClient;
        this.pineconeClient = pineconeClient;
        this.userRepository = userRepository;
    }

    /**
     * Answers a customer message using RAG when relevant docs match; otherwise a polite no-context or conversational reply.
     */
    public SupportChatResponse chat(Long userId, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Not authenticated"));
        if (user.getRole() != Role.CUSTOMER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CUSTOMERS_ONLY", "Only customers can use the support chatbot.");
        }
        openAiClient.requireConfigured();
        pineconeClient.requireConfigured();

        String cleaned = message == null ? "" : message.trim();
        if (cleaned.length() < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MESSAGE_REQUIRED", "Enter a question for the chatbot.");
        }
        enforceRateLimit(userId);

        String firstName = firstName(user);
        if (isConversational(cleaned)) {
            String reply = openAiClient.chat(conversationalSystemPrompt(firstName), cleaned);
            return new SupportChatResponse(reply, List.of());
        }

        float[] queryVector = openAiClient.embedOne(cleaned);
        List<PineconeClient.Match> matches = pineconeClient.query(queryVector, TOP_K);
        List<String> contextBlocks = new ArrayList<>();
        List<String> sources = new ArrayList<>();
        for (PineconeClient.Match match : matches) {
            if (match.score() < MIN_SCORE) {
                continue;
            }
            Map<String, Object> metadata = match.metadata();
            Object text = metadata.get("text");
            Object title = metadata.get("title");
            if (text == null || text.toString().isBlank()) {
                continue;
            }
            String titleLabel = title == null || title.toString().isBlank() ? "EZFINANZ docs" : title.toString();
            contextBlocks.add("Source: " + titleLabel + "\n" + text);
            if (!sources.contains(titleLabel)) {
                sources.add(titleLabel);
            }
        }

        if (contextBlocks.isEmpty()) {
            String reply = openAiClient.chat(noContextSystemPrompt(firstName), cleaned);
            return new SupportChatResponse(reply, List.of());
        }

        String systemPrompt = """
                You are EZFINANZ Assistant, a friendly customer support chatbot for a personal loan application platform.
                Be warm and natural. You may greet briefly when appropriate.
                For product/process questions, ground your answer in the provided EZFINANZ help-document context.
                If the context does not cover the question, say so politely and suggest using Contact Support in Help & Support.
                Never invent loan approvals, custom interest rates, disbursement dates, or account-specific decisions.
                Keep answers concise and practical (short paragraphs or bullets when helpful).
                """;
        String userPrompt = "Applicant first name: " + firstName
                + "\n\nContext from EZFINANZ docs:\n\n"
                + String.join("\n\n---\n\n", contextBlocks)
                + "\n\nCustomer message:\n" + cleaned;
        String reply = openAiClient.chat(systemPrompt, userPrompt);
        return new SupportChatResponse(reply, sources);
    }

    private static boolean isConversational(String message) {
        String normalized = message.toLowerCase(Locale.ROOT).trim();
        if (normalized.length() <= 24 && CONVERSATIONAL.matcher(normalized).matches()) {
            return true;
        }
        return CONVERSATIONAL.matcher(normalized).matches() && normalized.split("\\s+").length <= 8;
    }

    private static String conversationalSystemPrompt(String firstName) {
        return """
                You are EZFINANZ Assistant, a friendly support chatbot for the EZFINANZ personal loan app.
                Respond naturally to greetings and small talk.
                Briefly introduce what you can help with: application steps, eligibility, EMI, KYC, bank details, declaration, selfie review, and resubmitting after rejection.
                Invite the user to ask a specific question.
                Address the user as %s when it feels natural.
                Do not invent loan decisions or rates.
                Keep the reply short (2-4 sentences).
                """.formatted(firstName);
    }

    private static String noContextSystemPrompt(String firstName) {
        return """
                You are EZFINANZ Assistant for the EZFINANZ personal loan app.
                You did not receive matching help-document context for this message.
                Reply politely. If it is a product question, say you are not sure from the docs and suggest Contact Support in Help & Support.
                If it is casual chat, respond briefly and invite a product question.
                Address the user as %s when natural.
                Do not invent loan approvals, rates, or timelines.
                Keep the reply short.
                """.formatted(firstName);
    }

    private static String firstName(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName().trim().split("\\s+")[0];
        }
        return "there";
    }

    private void enforceRateLimit(Long userId) {
        long now = Instant.now().toEpochMilli();
        long cutoff = now - 3_600_000L;
        List<Long> stamps = rateWindow.computeIfAbsent(userId, id -> new ArrayList<>());
        synchronized (stamps) {
            Iterator<Long> it = stamps.iterator();
            while (it.hasNext()) {
                if (it.next() < cutoff) {
                    it.remove();
                }
            }
            if (stamps.size() >= MAX_PER_HOUR) {
                throw new ApiException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "RATE_LIMITED",
                        "You have reached the chatbot limit for this hour. Please try again later or use Contact Support."
                );
            }
            stamps.add(now);
        }
    }
}
