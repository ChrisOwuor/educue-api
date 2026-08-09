package com.owuor.educue.certificate.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.owuor.educue.certificate.entity.GraduationCertificate;
import com.owuor.educue.institution.repository.InstitutionProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.*;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class CertificatePdfRenderer {
    private final InstitutionProfileRepository institutionRepository;

    public byte[] render(GraduationCertificate certificate) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
            PdfWriter writer = PdfWriter.getInstance(document, output);
            document.open();
            PdfContentByte canvas = writer.getDirectContent();
            float width = document.getPageSize().getWidth(), height = document.getPageSize().getHeight();
            canvas.setColorFill(new Color(252, 250, 244));
            canvas.rectangle(0, 0, width, height);
            canvas.fill();
            canvas.setColorStroke(new Color(31, 65, 89));
            canvas.setLineWidth(4);
            canvas.rectangle(20, 20, width - 40, height - 40);
            canvas.stroke();
            canvas.setColorStroke(new Color(184, 145, 61));
            canvas.setLineWidth(1.4f);
            canvas.rectangle(28, 28, width - 56, height - 56);
            canvas.stroke();
            canvas.rectangle(34, 34, width - 68, height - 68);
            canvas.stroke();
            var institution = institutionRepository.findById(1L).orElse(null);
            String school = institution == null ? "EDUCUE TRAINING INSTITUTION" : institution.getName().toUpperCase();
            String motto = institution == null ? "" : institution.getMotto();
            File logo = new File("./storage/logo.png");
            if (logo.isFile()) {
                Image image = Image.getInstance(logo.getAbsolutePath());
                image.scaleToFit(76, 76);
                image.setAbsolutePosition((width - image.getScaledWidth()) / 2, height - 112);
                document.add(image);
            }
            centered(canvas, school, height - 132, 22, Font.BOLD, new Color(31, 65, 89));
            if (motto != null && !motto.isBlank())
                centered(canvas, motto, height - 151, 10, Font.ITALIC, Color.DARK_GRAY);
            centered(canvas, "CERTIFICATE OF AWARD", height - 190, 18, Font.BOLD, new Color(184, 145, 61));
            centered(canvas, "This is to certify that", height - 224, 12, Font.NORMAL, Color.DARK_GRAY);
            centered(canvas, certificate.getStudentName().toUpperCase(), height - 266, 24, Font.BOLD, new Color(20, 35, 48));
            canvas.setColorStroke(new Color(184, 145, 61));
            canvas.setLineWidth(.8f);
            canvas.moveTo(width / 2 - 190, height - 274);
            canvas.lineTo(width / 2 + 190, height - 274);
            canvas.stroke();
            centered(canvas, "having fulfilled all the prescribed requirements has been duly awarded", height - 304, 11, Font.NORMAL, Color.DARK_GRAY);
            centeredWrapped(canvas, certificate.getAwardTitle(), height - 320, 16, Font.BOLD, new Color(31, 65, 89), width - 180, 45);
            // was:
// if (certificate.getAwardClassification() != null && !certificate.getAwardClassification().isBlank())
//     centered(canvas, "Classification: " + certificate.getAwardClassification(), height - 382, 11, Font.BOLD, new Color(31, 65, 89));
            if (certificate.getAwardClassification() != null && !certificate.getAwardClassification().isBlank())
                classificationBadge(canvas, certificate.getAwardClassification(), height - 382);
            centered(canvas, "Conferred on " + certificate.getGraduationDate().format(DateTimeFormatter.ofPattern("d MMMM yyyy")), height - 405, 11, Font.NORMAL, Color.DARK_GRAY);
            lineWithLabel(canvas, 145, 100, "REGISTRAR");
            lineWithLabel(canvas, width - 285, 100, "PRINCIPAL / VICE CHANCELLOR");
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, new Phrase("Certificate No: " + certificate.getCertificateNumber(), new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY)), 48, 48, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT, new Phrase("Verification: " + certificate.getVerificationCode(), new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY)), width - 48, 48, 0);
            document.close();
            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Could not render graduation certificate", e);
        }
    }

    private void centered(PdfContentByte c, String value, float y, float size, int style, Color color) {
        ColumnText.showTextAligned(c, Element.ALIGN_CENTER, new Phrase(value, new Font(Font.TIMES_ROMAN, size, style, color)), c.getPdfWriter().getPageSize().getWidth() / 2, y, 0);
    }

    private void centeredWrapped(PdfContentByte c, String value, float top, float size, int style, Color color, float width, float height) throws DocumentException {
        float pageWidth = c.getPdfWriter().getPageSize().getWidth();
        ColumnText column = new ColumnText(c);
        Paragraph p = new Paragraph(value, new Font(Font.TIMES_ROMAN, size, style, color));
        p.setAlignment(Element.ALIGN_CENTER);
        column.setSimpleColumn((pageWidth - width) / 2, top - height, (pageWidth + width) / 2, top, 18, Element.ALIGN_CENTER);
        column.addElement(p);
        column.go();
    }

    private void lineWithLabel(PdfContentByte c, float x, float y, String label) {
        c.setColorStroke(new Color(31, 65, 89));
        c.setLineWidth(.8f);
        c.moveTo(x, y);
        c.lineTo(x + 210, y);
        c.stroke();
        ColumnText.showTextAligned(c, Element.ALIGN_CENTER, new Phrase(label, new Font(Font.HELVETICA, 8, Font.BOLD, Color.DARK_GRAY)), x + 105, y - 14, 0);
    }
    private void classificationBadge(PdfContentByte c, String classification, float y) throws DocumentException, IOException {
        String text = classification.toUpperCase();
        Color gold = new Color(184, 145, 61);
        BaseFont bf = BaseFont.createFont(BaseFont.TIMES_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        float size = 11.5f;
        float charSpacing = 2.4f;
        float pageWidth = c.getPdfWriter().getPageSize().getWidth();
        float centerX = pageWidth / 2;
        float textWidth = bf.getWidthPoint(text, size) + charSpacing * Math.max(text.length() - 1, 0);

        // hairline rules either side of the text
        float ruleGap = 14, ruleLen = 46;
        c.setColorStroke(gold);
        c.setLineWidth(0.6f);
        c.moveTo(centerX - textWidth / 2 - ruleGap - ruleLen, y + 3.5f);
        c.lineTo(centerX - textWidth / 2 - ruleGap, y + 3.5f);
        c.stroke();
        c.moveTo(centerX + textWidth / 2 + ruleGap, y + 3.5f);
        c.lineTo(centerX + textWidth / 2 + ruleGap + ruleLen, y + 3.5f);
        c.stroke();

        // small diamond flourishes at the rule ends
        drawDiamond(c, centerX - textWidth / 2 - ruleGap - ruleLen - 6, y + 3.5f, gold);
        drawDiamond(c, centerX + textWidth / 2 + ruleGap + ruleLen + 6, y + 3.5f, gold);

        // letter-spaced gold small-caps text
        c.saveState();
        c.beginText();
        c.setFontAndSize(bf, size);
        c.setCharacterSpacing(charSpacing);
        c.setColorFill(gold);
        c.showTextAligned(Element.ALIGN_CENTER, text, centerX, y, 0);
        c.endText();
        c.restoreState();
    }

    private void drawDiamond(PdfContentByte c, float x, float y, Color color) {
        float r = 2.6f;
        c.setColorFill(color);
        c.moveTo(x, y + r);
        c.lineTo(x + r, y);
        c.lineTo(x, y - r);
        c.lineTo(x - r, y);
        c.closePath();
        c.fill();
    }
}
