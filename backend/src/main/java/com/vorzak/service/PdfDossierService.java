package com.vorzak.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.vorzak.model.*;
import com.vorzak.rules.Rule;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfDossierService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneId.of("UTC"));

    public byte[] generateDossier(
            InvestigationEntity investigation,
            EvidenceEntity submittedImage,
            List<EvidenceEntity> evidenceList,
            List<ComparisonEntity> comparisons,
            List<FindingEntity> findings,
            List<Rule.ContradictionAspect> contradictionMatrix,
            List<EventEntity> events
    ) throws DocumentException {
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        // Fonts
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(15, 23, 42));
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(30, 41, 59));
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(51, 65, 85));
        Font boldBody = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(15, 23, 42));
        Font monoFont = FontFactory.getFont(FontFactory.COURIER, 8, new Color(71, 85, 105));
        Font disclaimerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, new Color(100, 116, 139));

        // 1. Header & Title
        Paragraph title = new Paragraph("VORZAK EVIDENCE DOSSIER", titleFont);
        title.setSpacingAfter(4);
        document.add(title);

        Paragraph subtitle = new Paragraph("Autonomous Evidence-Backed Image Provenance Investigation", headerFont);
        subtitle.setSpacingAfter(12);
        document.add(subtitle);

        // 2. Mode Banner (LIVE or REPLAY)
        PdfPTable modeTable = new PdfPTable(1);
        modeTable.setWidthPercentage(100);
        PdfPCell modeCell;
        if ("REPLAY".equalsIgnoreCase(investigation.getMode())) {
            modeCell = new PdfPCell(new Phrase("REPLAY MODE: Preserved Historical Case (Original Retrieval Timestamps Intact)", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)));
            modeCell.setBackgroundColor(new Color(180, 83, 9)); // Amber dark
        } else {
            modeCell = new PdfPCell(new Phrase("LIVE INVESTIGATION DOSSIER", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)));
            modeCell.setBackgroundColor(new Color(16, 185, 129)); // Emerald
        }
        modeCell.setPadding(6);
        modeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        modeTable.addCell(modeCell);
        modeTable.setSpacingAfter(14);
        document.add(modeTable);

        // 3. Case Metadata Table
        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(100);
        metaTable.setWidths(new float[]{25f, 75f});

        addMetaRow(metaTable, "Case ID:", investigation.getId().toString(), boldBody, monoFont);
        addMetaRow(metaTable, "Investigated At:", FORMATTER.format(investigation.getCreatedAt()), boldBody, bodyFont);
        addMetaRow(metaTable, "Investigation State:", investigation.getState(), boldBody, bodyFont);
        addMetaRow(metaTable, "Submitted File SHA-256:", investigation.getInputSha256(), boldBody, monoFont);
        addMetaRow(metaTable, "Claim Under Investigation:", investigation.getClaimText(), boldBody, bodyFont);
        metaTable.setSpacingAfter(14);
        document.add(metaTable);

        // 4. Primary Assessment & Findings
        document.add(new Paragraph("DETERMINISTIC RULE FINDINGS", headerFont));
        PdfPTable findingsTable = new PdfPTable(4);
        findingsTable.setWidthPercentage(100);
        findingsTable.setWidths(new float[]{20f, 15f, 15f, 50f});

        addHeaderCell(findingsTable, "Rule ID");
        addHeaderCell(findingsTable, "Outcome");
        addHeaderCell(findingsTable, "Confidence");
        addHeaderCell(findingsTable, "Explanation & Evidence Trace");

        for (FindingEntity f : findings) {
            findingsTable.addCell(new Phrase(f.getRuleId(), monoFont));
            findingsTable.addCell(new Phrase(f.getOutcome(), boldBody));
            findingsTable.addCell(new Phrase(f.getConfidence(), bodyFont));
            String exp = f.getExplanation() + " [Linked Evidence: " + f.getEvidenceIds().size() + " records]";
            findingsTable.addCell(new Phrase(exp, bodyFont));
        }
        findingsTable.setSpacingAfter(14);
        document.add(findingsTable);

        // 5. Contradiction Matrix
        if (contradictionMatrix != null && !contradictionMatrix.isEmpty()) {
            document.add(new Paragraph("CONTRADICTION ANALYSIS MATRIX", headerFont));
            PdfPTable matrixTable = new PdfPTable(4);
            matrixTable.setWidthPercentage(100);
            matrixTable.setWidths(new float[]{20f, 30f, 35f, 15f});

            addHeaderCell(matrixTable, "Aspect");
            addHeaderCell(matrixTable, "Claim Stated");
            addHeaderCell(matrixTable, "Discovered Record");
            addHeaderCell(matrixTable, "Status");

            for (Rule.ContradictionAspect aspect : contradictionMatrix) {
                matrixTable.addCell(new Phrase(aspect.aspect(), monoFont));
                matrixTable.addCell(new Phrase(aspect.claimClaimed(), bodyFont));
                matrixTable.addCell(new Phrase(aspect.evidenceFound(), bodyFont));
                matrixTable.addCell(new Phrase(aspect.status(), boldBody));
            }
            matrixTable.setSpacingAfter(14);
            document.add(matrixTable);
        }

        // 6. Evidence Ledger Table
        document.add(new Paragraph("EVIDENCE LEDGER", headerFont));
        PdfPTable evidenceTable = new PdfPTable(5);
        evidenceTable.setWidthPercentage(100);
        evidenceTable.setWidths(new float[]{18f, 18f, 16f, 28f, 20f});

        addHeaderCell(evidenceTable, "Kind");
        addHeaderCell(evidenceTable, "Source Tier");
        addHeaderCell(evidenceTable, "Date Value");
        addHeaderCell(evidenceTable, "Source URL");
        addHeaderCell(evidenceTable, "SHA-256");

        for (EvidenceEntity ev : evidenceList) {
            evidenceTable.addCell(new Phrase(ev.getKind(), monoFont));
            evidenceTable.addCell(new Phrase(ev.getSourceTier() != null ? ev.getSourceTier() : "UNKNOWN", bodyFont));
            String dt = ev.getDateValue() != null ? FORMATTER.format(ev.getDateValue()) : "UNKNOWN";
            evidenceTable.addCell(new Phrase(dt, bodyFont));
            evidenceTable.addCell(new Phrase(ev.getSourceUrl() != null ? ev.getSourceUrl() : "Local Upload", monoFont));
            String shortSha = ev.getContentSha256().length() >= 12 ? ev.getContentSha256().substring(0, 12) + "..." : ev.getContentSha256();
            evidenceTable.addCell(new Phrase(shortSha, monoFont));
        }
        evidenceTable.setSpacingAfter(14);
        document.add(evidenceTable);

        // 7. Visual Comparison Metrics
        if (comparisons != null && !comparisons.isEmpty()) {
            document.add(new Paragraph("VISUAL ALIGNMENT & DIFFERENCE METRICS", headerFont));
            PdfPTable compTable = new PdfPTable(5);
            compTable.setWidthPercentage(100);
            compTable.setWidths(new float[]{20f, 20f, 20f, 20f, 20f});

            addHeaderCell(compTable, "Status");
            addHeaderCell(compTable, "ORB Inliers");
            addHeaderCell(compTable, "SSIM Score");
            addHeaderCell(compTable, "Diff Regions");
            addHeaderCell(compTable, "Evaluation");

            for (ComparisonEntity c : comparisons) {
                compTable.addCell(new Phrase(c.getAlignmentStatus(), boldBody));
                compTable.addCell(new Phrase(String.valueOf(c.getInlierCount() != null ? c.getInlierCount() : 0), monoFont));
                compTable.addCell(new Phrase(c.getSsimNormalized() != null ? String.format("%.4f", c.getSsimNormalized()) : "N/A", monoFont));
                compTable.addCell(new Phrase(String.valueOf(c.getDiffRegionCount() != null ? c.getDiffRegionCount() : 0), monoFont));
                String eval = c.getDiffRegionCount() != null && c.getDiffRegionCount() > 0 ? "Differences Detected" : "Near-Identical";
                compTable.addCell(new Phrase(eval, bodyFont));
            }
            compTable.setSpacingAfter(14);
            document.add(compTable);
        }

        // 8. Limitations & Methodology
        document.add(new Paragraph("LIMITATIONS & SCOPE NOTICE", headerFont));
        Paragraph limits = new Paragraph(
                "1. Earliest Discovered Appearance: VORZAK reports only the earliest appearance discovered within indexed public sources. It does not certify the true original capture.\n" +
                "2. Difference Regions: Unexplained pixel delta regions indicate visual disparity relative to comparison candidates; they do not establish intentional manipulation or tampering.\n" +
                "3. Missing Dates: Sources without explicit metadata timestamps remain UNKNOWN. Dates are never inferred or fabricated.\n" +
                "4. Coverage: Web crawls and index queries represent a bounded slice of the public web; unindexed private forums or offline archives are not covered.",
                bodyFont
        );
        limits.setSpacingAfter(14);
        document.add(limits);

        // 9. Mandatory Rule 5 Disclaimer
        PdfPTable disTable = new PdfPTable(1);
        disTable.setWidthPercentage(100);
        PdfPCell disCell = new PdfPCell(new Phrase("Integrity-checked evidence log. Not a legal certification and not proof of authenticity.", disclaimerFont));
        disCell.setBackgroundColor(new Color(241, 245, 249));
        disCell.setPadding(8);
        disCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        disTable.addCell(disCell);
        document.add(disTable);

        document.close();
        return baos.toByteArray();
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
        cell.setBackgroundColor(new Color(30, 41, 59));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addMetaRow(PdfPTable table, String label, String value, Font labelFont, Font valFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setPadding(3);
        PdfPCell c2 = new PdfPCell(new Phrase(value, valFont));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setPadding(3);
        table.addCell(c1);
        table.addCell(c2);
    }
}
