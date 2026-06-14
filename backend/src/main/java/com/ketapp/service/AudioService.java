package com.ketapp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
public class AudioService {

    private final Path audioDir;

    public AudioService(@Value("${ket.audio.dir:./audio}") String audioDirPath) throws IOException {
        this.audioDir = Paths.get(audioDirPath).toAbsolutePath().normalize();
        Files.createDirectories(audioDir);
        log.info("Audio directory: {}", this.audioDir);
    }

    /**
     * Generate TTS audio for the given text, return the filename.
     * Uses Windows PowerShell to invoke System.Speech.Synthesis (local TTS, no network).
     */
    public String generateAudio(String text) throws Exception {
        String hash = sha256(text);
        String filename = hash + ".wav";
        Path filePath = audioDir.resolve(filename);

        if (Files.exists(filePath)) {
            log.debug("Audio cache hit: {}", filename);
            return filename;
        }

        log.info("Generating audio for: {}", text.substring(0, Math.min(text.length(), 50)));

        // PowerShell script: use .NET System.Speech.Synthesis to generate WAV
        String psScript = String.format(
            "Add-Type -AssemblyName System.Speech; " +
            "$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
            "$synth.SetOutputToWaveFile('%s'); " +
            "$synth.Speak('%s'); " +
            "$synth.Dispose()",
            filePath.toString().replace("'", "''"),
            text.replace("'", "''")
        );

        ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-Command", psScript);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("TTS failed with exit code {}, output: {}", exitCode, output);
            throw new RuntimeException("TTS generation failed: " + output);
        }

        if (!Files.exists(filePath) || Files.size(filePath) == 0) {
            throw new RuntimeException("TTS generated empty file");
        }

        log.info("Audio generated: {} ({} bytes)", filename, Files.size(filePath));
        return filename;
    }

    public Path getAudioFile(String filename) {
        return audioDir.resolve(filename);
    }

    public boolean exists(String filename) {
        return Files.exists(audioDir.resolve(filename));
    }

    private static String sha256(String text) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
