package lk.ac.kln.hallsync.service;

import lk.ac.kln.hallsync.model.ReportTable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/** Quotes CSV cells and neutralises spreadsheet formula prefixes in exported text. */
public final class CsvExporter {
    private CsvExporter() { }
    public static void write(Path file, ReportTable report) throws IOException {
        try (var writer = Files.newBufferedWriter(file,StandardCharsets.UTF_8)) {
            writer.write('\uFEFF'); // Helps Windows spreadsheet programs recognise UTF-8.
            writer.write(line(report.columns())); writer.write("\r\n");
            for (List<String> row : report.rows()) { writer.write(line(row)); writer.write("\r\n"); }
        }
    }
    private static String line(List<String> cells) { return cells.stream().map(CsvExporter::cell).collect(Collectors.joining(",")); }
    private static String cell(String text) {
        String value = text == null ? "" : text;
        String probe = value.stripLeading();
        if (!probe.isEmpty() && "=+-@".indexOf(probe.charAt(0)) >= 0) { value = "'" + value; }
        return "\"" + value.replace("\"","\"\"") + "\"";
    }
}
