package com.owuor.educue.common.report;

import com.owuor.educue.results.dto.MyAcademicResultsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfessionalPdfService {

    private final GenericTablePdfRenderer tableRenderer;
    private final ProvisionalTranscriptPdfRenderer transcriptRenderer;

    public byte[] tableReport(
            String title,
            LinkedHashMap<String, String> details,
            List<String> headers,
            List<List<String>> rows
    ) {
        return tableRenderer.render(
                title,
                details,
                headers,
                rows
        );
    }

    public byte[] tableReport(
            String title,
            LinkedHashMap<String, String> details,
            List<String> headers,
            List<List<String>> rows,
            float[] columnWidths
    ) {
        return tableRenderer.render(title, details, headers, rows, columnWidths);
    }

    public byte[] provisionalAcademicTranscript(
            LinkedHashMap<String, String> studentDetails,
            MyAcademicResultsResponse academicResults
    ) {
        return transcriptRenderer.render(
                studentDetails,
                academicResults
        );
    }
}
