package com.tcs.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Detects the real MIME type of an uploaded file by inspecting its leading bytes
 * (magic numbers) instead of trusting the client-supplied Content-Type header.
 *
 * Supported signatures:
 *   JPEG  – FF D8 FF
 *   PNG   – 89 50 4E 47 0D 0A 1A 0A
 *   WEBP  – 52 49 46 46 ?? ?? ?? ?? 57 45 42 50
 *   GIF   – 47 49 46 38
 *   PDF   – 25 50 44 46
 */
public final class FileMagicDetector {

    public static final String MIME_JPEG = "image/jpeg";
    public static final String MIME_PNG  = "image/png";
    public static final String MIME_WEBP = "image/webp";
    public static final String MIME_GIF  = "image/gif";
    public static final String MIME_PDF  = "application/pdf";

    private FileMagicDetector() {}

    /**
     * Reads up to 12 bytes from the stream (without consuming it) and returns
     * the detected MIME type, or {@code null} if none of the known signatures match.
     *
     * <p>The stream must support {@code mark/reset}; {@link java.io.BufferedInputStream}
     * wrapping satisfies this requirement.</p>
     */
    public static String detect(InputStream in) throws IOException {
        in.mark(12);
        byte[] header = new byte[12];
        int read = in.read(header, 0, 12);
        in.reset();

        if (read < 3) return null;

        // JPEG: FF D8 FF
        if ((header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF) {
            return MIME_JPEG;
        }

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (read >= 8
                && (header[0] & 0xFF) == 0x89
                && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47
                && header[4] == 0x0D && header[5] == 0x0A
                && (header[6] & 0xFF) == 0x1A && header[7] == 0x0A) {
            return MIME_PNG;
        }

        // WEBP: "RIFF????WEBP"
        if (read == 12
                && header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) {
            return MIME_WEBP;
        }

        // GIF: "GIF8"
        if (read >= 4
                && header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x38) {
            return MIME_GIF;
        }

        // PDF: "%PDF"
        if (read >= 4
                && header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46) {
            return MIME_PDF;
        }

        return null;
    }

    /** Returns the canonical file extension (including dot) for a known MIME type. */
    public static String extensionFor(String mimeType) {
        return switch (mimeType) {
            case MIME_JPEG -> ".jpg";
            case MIME_PNG  -> ".png";
            case MIME_WEBP -> ".webp";
            case MIME_GIF  -> ".gif";
            case MIME_PDF  -> ".pdf";
            default        -> throw new IllegalArgumentException("Unknown MIME type: " + mimeType);
        };
    }
}
