package com.owuor.educue.graduation.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.owuor.educue.academics.service.PdfFooterPageEvent;
import com.owuor.educue.graduation.entity.GraduationBatch;
import com.owuor.educue.graduation.entity.GraduationBatchCandidate;
import com.owuor.educue.graduation.enums.AwardClassification;
import com.owuor.educue.institution.repository.InstitutionProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class GraduationBookletPdfService {
    private static final Color NAVY = new Color(29, 55, 78);
    private static final Color GOLD = new Color(177, 137, 52);
    private static final Color BORDER = new Color(215, 219, 223);
    private final InstitutionProfileRepository institutionRepository;

    public byte[] generate(GraduationBatch batch, List<GraduationBatchCandidate> candidates) {
        try {
            var output = new ByteArrayOutputStream();
            var document = new Document(PageSize.A4, 46, 46, 44, 52);
            var writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new PdfFooterPageEvent());
            document.open();
            var institution = institutionRepository.findById(1L).orElse(null);
            cover(document, institution == null ? "EduCue Training Institution" : institution.getName(), institution == null ? null : institution.getMotto(), batch, candidates.size());

            for (var department : grouped(candidates).entrySet()) {
                document.newPage();
                heading(document, department.getKey());
                var statement = new Paragraph("The candidates listed below have satisfied the prescribed academic and graduation requirements and have qualified for the awards and classifications indicated.", font(9.5f, Font.NORMAL, Color.DARK_GRAY));
                statement.setAlignment(Element.ALIGN_JUSTIFIED);
                statement.setLeading(15);
                statement.setSpacingAfter(15);
                document.add(statement);

                for (var classification : department.getValue().entrySet()) {
                    var classificationHeading = new Paragraph(classification.getKey().toUpperCase(), font(10.5f, Font.BOLD, GOLD));
                    classificationHeading.setSpacingBefore(10);
                    classificationHeading.setSpacingAfter(5);
                    document.add(classificationHeading);
                    addCandidates(document, classification.getValue());
                }
            }

            document.newPage();
            heading(document, "Congratulations to the Graduating Class");
            centered(document, "The institution congratulates every graduate and acknowledges the faculty, staff, families and sponsors whose guidance and support made this achievement possible.");
            centered(document, "May the graduating class carry forward the values of knowledge, integrity and service.");
            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not generate graduation booklet", exception);
        }
    }

    private Map<String, LinkedHashMap<String, List<GraduationBatchCandidate>>> grouped(List<GraduationBatchCandidate> candidates) {
        var result = new TreeMap<String, LinkedHashMap<String, List<GraduationBatchCandidate>>>(String.CASE_INSENSITIVE_ORDER);
        var ordered = candidates.stream().sorted(Comparator
                .comparing((GraduationBatchCandidate candidate) -> candidate.getApplication().getEnrollment().getCourse().getDepartment().getName(), String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(candidate -> classificationOrder(candidate.getApplication().getAwardClassification()))
                .thenComparing(candidate -> candidate.getApplication().getGraduationName(), String.CASE_INSENSITIVE_ORDER)).toList();
        for (var candidate : ordered) {
            var application = candidate.getApplication();
            String department = application.getEnrollment().getCourse().getDepartment().getName();
            String classification = application.getAwardClassification() == null ? "Classification Pending" : application.getAwardClassification().getDisplayName();
            result.computeIfAbsent(department, ignored -> new LinkedHashMap<>()).computeIfAbsent(classification, ignored -> new ArrayList<>()).add(candidate);
        }
        return result;
    }

    private int classificationOrder(AwardClassification classification) {
        if (classification == null) return 99;
        return switch (classification) {
            case FIRST_CLASS_HONOURS, DISTINCTION -> 1;
            case SECOND_CLASS_HONOURS_UPPER_DIVISION, MERIT -> 2;
            case SECOND_CLASS_HONOURS_LOWER_DIVISION, CREDIT -> 3;
            case PASS -> 4;
        };
    }

    private void addCandidates(Document document, List<GraduationBatchCandidate> candidates) throws DocumentException {
        var table = new PdfPTable(new float[]{.45f, 2.8f, 1.3f, 2.8f});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setSplitLate(false);
        header(table, "#"); header(table, "Candidate"); header(table, "Admission No."); header(table, "Award");
        int number = 1;
        for (var candidate : candidates) {
            var application = candidate.getApplication();
            body(table, String.valueOf(number++), Element.ALIGN_CENTER);
            body(table, application.getGraduationName(), Element.ALIGN_LEFT);
            body(table, application.getAdmissionNumberSnapshot(), Element.ALIGN_LEFT);
            body(table, application.getAwardTitle(), Element.ALIGN_LEFT);
        }
        table.setSpacingAfter(8);
        document.add(table);
    }

    private void cover(Document document, String school, String motto, GraduationBatch batch, int count) throws Exception {
        document.add(new Paragraph("\n\n"));
        var logo = new File("./storage/logo.png");
        if (logo.isFile()) { var image = Image.getInstance(logo.getAbsolutePath()); image.scaleToFit(95, 95); image.setAlignment(Element.ALIGN_CENTER); document.add(image); }
        var institution = new Paragraph(school.toUpperCase(), font(22, Font.BOLD, NAVY)); institution.setAlignment(Element.ALIGN_CENTER); institution.setSpacingBefore(15); document.add(institution);
        if (motto != null && !motto.isBlank()) { var line = new Paragraph(motto, font(10, Font.ITALIC, Color.DARK_GRAY)); line.setAlignment(Element.ALIGN_CENTER); document.add(line); }
        var title = new Paragraph("OFFICIAL GRADUATION BOOKLET", font(18, Font.BOLD, GOLD)); title.setAlignment(Element.ALIGN_CENTER); title.setSpacingBefore(50); document.add(title);
        var event = new Paragraph(batch.getName(), font(17, Font.BOLD, NAVY)); event.setAlignment(Element.ALIGN_CENTER); event.setSpacingBefore(16); document.add(event);
        var date = new Paragraph(batch.getGraduationDate().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")), font(11, Font.NORMAL, Color.DARK_GRAY)); date.setAlignment(Element.ALIGN_CENTER); date.setSpacingBefore(8); document.add(date);
        var welcome = new Paragraph("A celebration of academic achievement, perseverance and service.", font(12, Font.NORMAL, Color.BLACK)); welcome.setAlignment(Element.ALIGN_CENTER); welcome.setSpacingBefore(52); document.add(welcome);
        var total = new Paragraph(count + " GRADUATING CANDIDATES", font(10, Font.BOLD, GOLD)); total.setAlignment(Element.ALIGN_CENTER); total.setSpacingBefore(28); document.add(total);
    }

    private void heading(Document document, String text) throws DocumentException { var heading = new Paragraph(text.toUpperCase(), font(15, Font.BOLD, NAVY)); heading.setAlignment(Element.ALIGN_CENTER); heading.setSpacingAfter(14); document.add(heading); }
    private void centered(Document document, String text) throws DocumentException { var paragraph = new Paragraph(text, font(11, Font.NORMAL, Color.DARK_GRAY)); paragraph.setAlignment(Element.ALIGN_CENTER); paragraph.setLeading(18); paragraph.setIndentationLeft(30); paragraph.setIndentationRight(30); paragraph.setSpacingAfter(18); document.add(paragraph); }
    private void header(PdfPTable table, String text) { var cell = new PdfPCell(new Phrase(text, font(7.5f, Font.BOLD, Color.BLACK))); cell.setBorder(Rectangle.BOTTOM); cell.setBorderColor(NAVY); cell.setPadding(5); table.addCell(cell); }
    private void body(PdfPTable table, String text, int alignment) { var cell = new PdfPCell(new Phrase(text == null ? "" : text, font(7.7f, Font.NORMAL, Color.BLACK))); cell.setBorder(Rectangle.BOTTOM); cell.setBorderColor(BORDER); cell.setBorderWidthBottom(.4f); cell.setHorizontalAlignment(alignment); cell.setPadding(5); table.addCell(cell); }
    private Font font(float size, int style, Color color) { return new Font(Font.HELVETICA, size, style, color); }
}
