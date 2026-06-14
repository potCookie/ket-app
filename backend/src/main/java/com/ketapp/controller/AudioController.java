package com.ketapp.controller;

import com.ketapp.service.AudioService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/audio")
@RequiredArgsConstructor
public class AudioController {

    private final AudioService audioService;

    /**
     * Serve audio file by filename.
     * GET /api/audio/{filename}
     */
    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getAudio(@PathVariable String filename) {
        if (!audioService.exists(filename)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(audioService.getAudioFile(filename));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/wav"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    /**
     * Generate and serve audio for text. Caches result.
     * GET /api/audio/generate?text=...
     * Returns JSON with audio URL.
     */
    @GetMapping("/generate")
    public ResponseEntity<?> generateAudio(@RequestParam String text) {
        try {
            String filename = audioService.generateAudio(text);
            String url = "/api/audio/" + filename;
            return ResponseEntity.ok(java.util.Map.of("url", url, "filename", filename));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
