package com.tcs.module.catalog.service;

import java.util.Optional;

/** Goi Gemini API de tra loi cau hoi khi khong tim duoc FAQ khop (fallback). */
public interface GeminiService {

    /** Tra ve Optional.empty() neu chua cau hinh API key hoac goi API loi (rate limit, timeout...). */
    Optional<String> askQuestion(String question);
}
