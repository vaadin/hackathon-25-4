package com.vaadin.securitystatus.customer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * The triage as files an auditor attaches: a CSV and a PDF carrying the same
 * facts, the whole triage and never the screen's cap. Both are composed from
 * text the screen has already put into words, so neither invents a value and
 * neither needs the session to be built.
 */
public final class TriageFiles {

    /** The CSV's content type, as it leaves the application. */
    public static final String CSV = "text/csv; charset=utf-8";
    public static final String PDF = "application/pdf";

    /**
     * What both files say.
     *
     * @param title   the document's title
     * @param about   the line naming the version and the {@code generated_at}
     *                it was answered from, for the PDF
     * @param notes   the CSV's comment lines before the column names: what the
     *                file is, the version and the {@code generated_at}
     * @param lead    the headline and the three bucket counts, one line each,
     *                for the PDF
     * @param columns the seven column names, in order
     * @param rows    one row per finding, seven cells each, in the same order
     */
    public record Sheet(String title, String about, List<String> notes, List<String> lead, List<String> columns,
            List<List<String>> rows) {

        public Sheet {
            notes = List.copyOf(notes);
            lead = List.copyOf(lead);
            columns = List.copyOf(columns);
            rows = rows.stream().map(List::copyOf).toList();
        }
    }

    private static final float MARGIN = 36;
    /** The share of the line each column takes: cve, bucket, owner, component, severity, action, verdict. */
    private static final float[] WIDTHS = { 0.11f, 0.09f, 0.08f, 0.19f, 0.07f, 0.31f, 0.15f };
    private static final float TEXT = 8;
    private static final float LEADING = 10;

    private TriageFiles() {
    }

    /** Comment lines naming the file, the version and the measurements, the column names, then one line per finding. */
    public static byte[] csv(Sheet sheet) {
        var out = new StringBuilder();
        sheet.notes().forEach(note -> out.append("# ").append(note.replace('\n', ' ')).append("\r\n"));
        out.append(line(sheet.columns()));
        sheet.rows().forEach(row -> out.append(line(row)));
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String line(List<String> cells) {
        var quoted = new ArrayList<String>();
        cells.forEach(cell -> quoted.add('"' + cell.replace("\"", "\"\"") + '"'));
        return String.join(",", quoted) + "\r\n";
    }

    /** The same facts as a landscape A4 document: title, the version line, the headline, the counts, the table. */
    public static byte[] pdf(Sheet sheet) {
        try (var document = new PDDocument(); var out = new ByteArrayOutputStream()) {
            PDDocumentInformation info = document.getDocumentInformation();
            info.setTitle(sheet.title());
            new Writer(document).write(sheet);
            document.save(out);
            return out.toByteArray();
        } catch (IOException failed) {
            throw new UncheckedIOException(failed);
        }
    }

    /** Lays text out top to bottom, starting a page whenever the next block does not fit. */
    private static final class Writer {

        private final PDDocument document;
        private final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        private final PDRectangle size = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
        private PDPageContentStream page;
        private float y;

        Writer(PDDocument document) {
            this.document = document;
        }

        void write(Sheet sheet) throws IOException {
            newPage();
            try {
                text(sheet.title(), bold, 16, MARGIN, 20);
                text(sheet.about(), regular, 10, MARGIN, 16);
                for (String lead : sheet.lead()) {
                    text(lead, regular, 10, MARGIN, 14);
                }
                y -= 8;
                row(sheet.columns(), bold);
                for (List<String> cells : sheet.rows()) {
                    row(cells, regular);
                }
            } finally {
                page.close();
            }
        }

        private void row(List<String> cells, PDType1Font font) throws IOException {
            float width = size.getWidth() - 2 * MARGIN;
            var wrapped = new ArrayList<List<String>>();
            int lines = 1;
            for (int i = 0; i < cells.size(); i++) {
                List<String> cell = wrap(drawable(cells.get(i), font), font, TEXT, WIDTHS[i] * width - 4);
                wrapped.add(cell);
                lines = Math.max(lines, cell.size());
            }
            float height = lines * LEADING + 4;
            if (y - height < MARGIN) {
                page.close();
                newPage();
            }
            float x = MARGIN;
            for (int i = 0; i < wrapped.size(); i++) {
                float lineY = y;
                for (String text : wrapped.get(i)) {
                    show(text, font, TEXT, x, lineY);
                    lineY -= LEADING;
                }
                x += WIDTHS[i] * width;
            }
            y -= height;
            page.setLineWidth(0.3f);
            page.moveTo(MARGIN, y + LEADING - 2);
            page.lineTo(MARGIN + width, y + LEADING - 2);
            page.stroke();
        }

        private void text(String text, PDType1Font font, float fontSize, float x, float advance) throws IOException {
            float width = size.getWidth() - 2 * MARGIN;
            for (String line : wrap(drawable(text, font), font, fontSize, width)) {
                show(line, font, fontSize, x, y);
                y -= advance;
            }
        }

        private void show(String text, PDType1Font font, float fontSize, float x, float atY) throws IOException {
            page.beginText();
            page.setFont(font, fontSize);
            page.newLineAtOffset(x, atY);
            page.showText(text);
            page.endText();
        }

        private void newPage() throws IOException {
            var next = new PDPage(size);
            document.addPage(next);
            page = new PDPageContentStream(document, next);
            y = size.getHeight() - MARGIN - 10;
        }

        /** Words onto lines no wider than the column, a word too long for one broken where it must be. */
        private static List<String> wrap(String text, PDType1Font font, float fontSize, float width)
                throws IOException {
            var lines = new ArrayList<String>();
            var current = new StringBuilder();
            for (String word : text.split(" ")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (measure(candidate, font, fontSize) <= width) {
                    current.setLength(0);
                    current.append(candidate);
                    continue;
                }
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                String rest = word;
                while (measure(rest, font, fontSize) > width && rest.length() > 1) {
                    int cut = rest.length() - 1;
                    while (cut > 1 && measure(rest.substring(0, cut), font, fontSize) > width) {
                        cut--;
                    }
                    lines.add(rest.substring(0, cut));
                    rest = rest.substring(cut);
                }
                current.append(rest);
            }
            if (!current.isEmpty() || lines.isEmpty()) {
                lines.add(current.toString());
            }
            return lines;
        }

        private static float measure(String text, PDType1Font font, float fontSize) throws IOException {
            return font.getStringWidth(text) / 1000 * fontSize;
        }

        /** The text with every character the standard font cannot encode written as a question mark. */
        private static String drawable(String text, PDType1Font font) {
            var out = new StringBuilder();
            text.codePoints().forEach(codePoint -> {
                String character = Character.toString(Character.isWhitespace(codePoint) ? ' ' : codePoint);
                try {
                    font.encode(character);
                    out.append(character);
                } catch (IOException | IllegalArgumentException unencodable) {
                    out.append('?');
                }
            });
            return out.toString();
        }
    }
}
