package com.example.pproject.resume.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
public class PdfService {

    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "";
        }

        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.info("PDF 텍스트 추출 완료: 약 {}자", text.length());
            return text;
        } catch (IOException e) {
            log.error("PDF 처리 중 오류 발생", e);
            throw new RuntimeException("PDF 변환에 실패했습니다.", e);
        }
    }
}