package com.ezfinanz.knowledge.service;

import com.ezfinanz.common.ApiException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class DocumentTextExtractor {

    public String extract(byte[] bytes, String originalName) {
        String ext = extension(originalName);
        try {
            return switch (ext) {
                case "txt", "md" -> new String(bytes, StandardCharsets.UTF_8);
                case "pdf" -> extractPdf(bytes);
                default -> throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "FILE_TYPE_INVALID",
                        "Upload a .txt, .md, or .pdf file."
                );
            };
        } catch (ApiException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "TEXT_EXTRACT_FAILED",
                    "Could not read text from this document."
            );
        }
    }

    private static String extractPdf(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return text == null ? "" : text;
        }
    }

    private static String extension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
