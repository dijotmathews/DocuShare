package com.dijo.demo;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a small one-page PDF with a few lines of text, so demo documents
 * open in a PDF viewer instead of being empty placeholders.
 */
final class DemoPdf {

    private DemoPdf() {
    }

    static byte[] withLines(List<String> lines) {
        StringBuilder text = new StringBuilder("BT\n/F1 22 Tf\n72 760 Td\n");
        for (int i = 0; i < lines.size(); i++) {
            if (i == 1) {
                text.append("/F1 12 Tf\n0 -36 Td\n");
            } else if (i > 1) {
                text.append("0 -20 Td\n");
            }
            text.append('(').append(escape(lines.get(i))).append(") Tj\n");
        }
        text.append("ET\n");
        byte[] stream = text.toString().getBytes(StandardCharsets.ISO_8859_1);

        List<String> objects = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
                        + "/Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>",
                "<< /Length " + stream.length + " >>\nstream\n"
                        + new String(stream, StandardCharsets.ISO_8859_1) + "endstream",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, "%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(out.size());
            write(out, (i + 1) + " 0 obj\n" + objects.get(i) + "\nendobj\n");
        }
        int xref = out.size();
        StringBuilder table = new StringBuilder("xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n");
        offsets.forEach(o -> table.append(String.format("%010d 00000 n \n", o)));
        table.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n")
                .append("startxref\n").append(xref).append("\n%%EOF\n");
        write(out, table.toString());
        return out.toByteArray();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private static void write(ByteArrayOutputStream out, String s) {
        out.writeBytes(s.getBytes(StandardCharsets.ISO_8859_1));
    }
}
