package com.owuor.educue.graduation.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.owuor.educue.academics.service.PdfFooterPageEvent;
import com.owuor.educue.common.report.InstitutionPdfHeaderRendererBase;
import com.owuor.educue.graduation.entity.GraduationCandidate;
import com.owuor.educue.graduation.entity.GraduationList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.owuor.educue.common.report.PdfReportHelper.font;
import static com.owuor.educue.common.report.PdfReportHelper.safe;

@Component
@RequiredArgsConstructor
public class ProvisionalGraduationListPdfRenderer {
    private static final Color BORDER = new Color(80, 80, 80);
    private static final Color MUTED = new Color(85, 85, 85);
    private final InstitutionPdfHeaderRendererBase headerRenderer;

    public byte[] render(GraduationList list, List<GraduationCandidate> candidates) {
        return render(list, candidates, false);
    }

    public byte[] renderFinal(GraduationList list, List<GraduationCandidate> candidates) {
        return render(list, candidates, true);
    }

    private byte[] render(GraduationList list, List<GraduationCandidate> candidates, boolean finalList) {
        try {
            var output = new ByteArrayOutputStream();
            var document = new Document(PageSize.A4.rotate(), 28, 28, 28, 48);
            var writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new PdfFooterPageEvent());
            document.open();

            headerRenderer.render(document, reference(list));
            addTitle(document, list, finalList);
            addListDetails(document, list, candidates.size());
            addCandidates(document, candidates);
            addNotice(document, list, finalList);
            addApprovalSection(document);

            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not generate provisional graduation list", exception);
        }
    }

    private void addTitle(Document document, GraduationList list, boolean finalList) throws DocumentException {
        String title = finalList ? "FINAL GRADUATION LIST" : "PROVISIONAL".equals(list.getStatus())
                ? "PROVISIONAL DEPARTMENTAL GRADUATION LIST"
                : "DEPARTMENTAL GRADUATION LIST";
        var heading = new Paragraph(title, font(13, Font.BOLD, Color.BLACK));
        heading.setAlignment(Element.ALIGN_CENTER);
        heading.setSpacingBefore(8);
        heading.setSpacingAfter(2);
        document.add(heading);

        var subtitle = new Paragraph("ACADEMIC AWARDS AND CLASSIFICATIONS", font(7.5f, Font.BOLD, MUTED));
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(10);
        document.add(subtitle);
    }

    private void addListDetails(Document document, GraduationList list, int total) throws DocumentException {
        var details = new PdfPTable(new float[]{1.1f, 3.2f, 1.1f, 2.2f, 1.1f, 1.4f});
        details.setWidthPercentage(100);
        details.setKeepTogether(true);
        details.setSpacingAfter(9);
        label(details, "Department"); value(details, list.getDepartment().getName());
        label(details, "Academic year"); value(details, list.getAcademicYear().getCode());
        label(details, "Candidates"); value(details, String.valueOf(total));
        label(details, "List status"); value(details, list.getStatus().replace('_', ' '));
        label(details, "Published on"); value(details, list.getPublishedAt() == null ? "Not yet published" : list.getPublishedAt().toLocalDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        label(details, "Printed on"); value(details, LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        document.add(details);
    }

    private void addCandidates(Document document, List<GraduationCandidate> candidates) throws DocumentException {
        var table = new PdfPTable(new float[]{.45f, 1.25f, 2.15f, 1.05f, 3.15f, .8f, 2.05f});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setSplitLate(false);
        table.setSplitRows(true);
        header(table, "#", Element.ALIGN_CENTER);
        header(table, "Admission No.", Element.ALIGN_LEFT);
        header(table, "Candidate Name", Element.ALIGN_LEFT);
        header(table, "Course", Element.ALIGN_LEFT);
        header(table, "Award", Element.ALIGN_LEFT);
        header(table, "Cumulative", Element.ALIGN_CENTER);
        header(table, "Classification", Element.ALIGN_LEFT);

        int index = 1;
        for (var candidate : candidates) {
            body(table, String.valueOf(index++), Element.ALIGN_CENTER);
            body(table, candidate.getAdmissionNumberSnapshot(), Element.ALIGN_LEFT);
            body(table, candidate.getGraduationName(), Element.ALIGN_LEFT);
            body(table, candidate.getCourseCodeSnapshot(), Element.ALIGN_LEFT);
            body(table, candidate.getAwardTitle(), Element.ALIGN_LEFT);
            body(table, candidate.getFinalCumulativeAverage() == null ? "-" : candidate.getFinalCumulativeAverage().setScale(2, RoundingMode.HALF_UP) + "%", Element.ALIGN_CENTER);
            body(table, candidate.getAwardClassification() == null ? "-" : candidate.getAwardClassification().getDisplayName(), Element.ALIGN_LEFT);
        }
        if (candidates.isEmpty()) {
            var empty = new PdfPCell(new Phrase("No candidates have been added to this graduation list.", font(8, Font.ITALIC, MUTED)));
            empty.setColspan(7);
            empty.setBorder(Rectangle.BOTTOM);
            empty.setBorderColor(BORDER);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            empty.setPadding(14);
            table.addCell(empty);
        }
        document.add(table);
    }

    private void addNotice(Document document, GraduationList list, boolean finalList) throws DocumentException {
        document.add(new LineSeparator(.5f, 100, BORDER, Element.ALIGN_CENTER, 0));
        if (finalList) {
            var notice = new Paragraph("FINAL LIST: Every candidate on this document has completed clearance and received formal Registrar approval for graduation.", font(6.8f, Font.ITALIC, MUTED));
            notice.setAlignment(Element.ALIGN_JUSTIFIED);
            notice.setLeading(9);
            notice.setSpacingBefore(5);
            notice.setSpacingAfter(18);
            document.add(notice);
            return;
        }
        String state = "PROVISIONAL".equals(list.getStatus()) ? "PROVISIONAL LIST NOTICE" : "WORKING LIST NOTICE";
        var notice = new Paragraph(state + ": This list is subject to verification of academic records, candidate details, graduation clearance and formal approval by the Registrar. Inclusion on this document does not by itself confer an award or confirm graduation.", font(6.8f, Font.ITALIC, MUTED));
        notice.setAlignment(Element.ALIGN_JUSTIFIED);
        notice.setLeading(9);
        notice.setSpacingBefore(5);
        notice.setSpacingAfter(18);
        document.add(notice);
    }

    private void addApprovalSection(Document document) throws DocumentException {
        var signatures = new PdfPTable(2);
        signatures.setWidthPercentage(100);
        signatures.setWidths(new float[]{1, 1});
        signatures.setKeepTogether(true);
        signatures.addCell(signature("HEAD OF DEPARTMENT", "Departmental recommendation"));
        signatures.addCell(signature("REGISTRAR / ACADEMIC OFFICER", "Academic verification"));
        document.add(signatures);
    }

    private PdfPCell signature(String role, String purpose) {
        var cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingRight(28);
        cell.addElement(new Paragraph("Signature: __________________________________", font(7, Font.NORMAL, Color.BLACK)));
        var roleLine = new Paragraph(role, font(7.5f, Font.BOLD, Color.BLACK));
        roleLine.setSpacingBefore(3); cell.addElement(roleLine);
        cell.addElement(new Paragraph(purpose, font(6.8f, Font.ITALIC, MUTED)));
        var date = new Paragraph("Date: __________________________", font(7, Font.NORMAL, Color.BLACK));
        date.setSpacingBefore(6); cell.addElement(date);
        return cell;
    }

    private void label(PdfPTable table, String text) {
        var cell = new PdfPCell(new Phrase(text.toUpperCase() + ":", font(6.8f, Font.BOLD, MUTED)));
        cell.setBorder(Rectangle.NO_BORDER); cell.setPadding(2); table.addCell(cell);
    }

    private void value(PdfPTable table, String text) {
        var cell = new PdfPCell(new Phrase(safe(text), font(7.4f, Font.NORMAL, Color.BLACK)));
        cell.setBorder(Rectangle.NO_BORDER); cell.setPadding(2); table.addCell(cell);
    }

    private void header(PdfPTable table, String text, int alignment) {
        var cell = new PdfPCell(new Phrase(text, font(7, Font.BOLD, Color.BLACK)));
        cell.setBorder(Rectangle.BOTTOM); cell.setBorderColor(BORDER); cell.setBorderWidthBottom(.8f);
        cell.setHorizontalAlignment(alignment); cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5); table.addCell(cell);
    }

    private void body(PdfPTable table, String text, int alignment) {
        var cell = new PdfPCell(new Phrase(safe(text), font(7.2f, Font.NORMAL, Color.BLACK)));
        cell.setBorder(Rectangle.BOTTOM); cell.setBorderColor(new Color(220, 220, 220)); cell.setBorderWidthBottom(.35f);
        cell.setHorizontalAlignment(alignment); cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(5); cell.setPaddingBottom(5); cell.setPaddingLeft(4); cell.setPaddingRight(4);
        table.addCell(cell);
    }

    private String reference(GraduationList list) {
        return "GRAD/LIST/" + list.getAcademicYear().getCode().replace('/', '-') + "/" + list.getUuid().toString().substring(0, 8).toUpperCase();
    }
}
