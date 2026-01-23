package com.example.pproject.common.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
public class PdfUtils {

    public String extractTextFromPdf(MultipartFile file) {
        if (file.isEmpty()) {
            return "";
        }

        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();

            return stripper.getText(document);

        } catch (IOException e) {
            throw new RuntimeException("PDF 파일 텍스트 추출 중 오류가 발생했습니다.", e);
        }
    }
}