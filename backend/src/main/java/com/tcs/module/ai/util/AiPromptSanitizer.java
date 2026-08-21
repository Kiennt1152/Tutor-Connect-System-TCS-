package com.tcs.module.ai.util;

/**
 * Unified Sanitizer and Boundary Encoder for AI Prompts (Classification & Generation).
 * Prevents prompt injection, tag breakout, and markdown fence structure manipulation.
 */
public final class AiPromptSanitizer {

    private AiPromptSanitizer() {}

    /**
     * Sanitizes and escapes user inputs before injecting into system or user prompt templates.
     *
     * @param input Raw user input string
     * @param maxLength Maximum allowed characters (overly long inputs are cleanly truncated)
     * @return Escaped, bounded and safe string
     */
    public static String sanitizeForPrompt(String input, int maxLength) {
        if (input == null) {
            return "";
        }
        String trimmed = input.trim();
        if (trimmed.length() > maxLength) {
            trimmed = trimmed.substring(0, maxLength);
        }

        // Clean null/control characters except standard newlines and tabs
        StringBuilder clean = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c >= 32 || c == '\n' || c == '\r' || c == '\t') {
                clean.append(c);
            }
        }

        return clean.toString()
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("```", "'''")
            .replace("`", "'");
    }
}
