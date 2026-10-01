package com.eltek.components;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.webobjects.appserver.WOContext;
import com.webobjects.directtoweb.D2WContext;
import com.webobjects.eocontrol.EOEnterpriseObject;

import er.attachment.model.ERAttachment;
import er.attachment.processors.ERAttachmentProcessor;
import er.directtoweb.components.ERDCustomComponent;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

public class AttachmentDisplay extends ERDCustomComponent {

    private static final long serialVersionUID = 1L;
	private static final int MAX_ROWS = 20;
    private static final int MAX_COLS = 10;
    
    private static final int MAX_WORD_BLOCKS = 30;      // paragraphs + tables shown
    private static final int MAX_WORD_TABLE_ROWS = 10;  // rows shown per table

	private static final Logger log = LoggerFactory.getLogger(AttachmentDisplay.class);

    // Rendered previews, kept in memory (up to 200) so each file is processed only once
    private static final Map<String, String> PREVIEW_CACHE = Collections.synchronizedMap(
        new LinkedHashMap<String, String>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> e) {
                return size() > 200;
            }
        });

    public AttachmentDisplay(WOContext context) {
        super(context);
    }

    @Override
    public boolean synchronizesVariablesWithBindings() {
        return false;
    }

    public ERAttachment attachment() {
        D2WContext c = (D2WContext) valueForBinding("localContext");
        if (c == null) {
            c = (D2WContext) valueForBinding("d2wContext");
        }
        EOEnterpriseObject eo = (EOEnterpriseObject) valueForBinding("object");
        if (eo == null && c != null) {
            eo = (EOEnterpriseObject) c.valueForKey("object");
        }
        String key = (String) valueForBinding("key");
        if (key == null && c != null) {
            key = c.propertyKey();
        }
        if (eo == null || key == null) {
            return null;
        }
        return (ERAttachment) eo.valueForKeyPath(key);
    }

    // ---------- type checks ----------

    private String mimeType() {
        ERAttachment a = attachment();
        return (a == null || a.mimeType() == null) ? "" : a.mimeType().toLowerCase();
    }

    private String fileName() {
        ERAttachment a = attachment();
        return (a == null || a.originalFileName() == null) ? "" : a.originalFileName().toLowerCase();
    }

    public boolean isPdf() {
        return attachment() != null
            && (mimeType().equals("application/pdf") || fileName().endsWith(".pdf"));
    }

    public boolean isSpreadsheet() {
        String mt = mimeType();
        String name = fileName();
        return attachment() != null
            && (mt.equals("application/vnd.ms-excel")
                || mt.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                || name.endsWith(".xls")
                || name.endsWith(".xlsx"));
    }

    public boolean isOther() {return attachment() != null && !isPdf() && !isSpreadsheet() && !isWord();}
    
    // ---------- Word preview ----------

    public boolean isWord() {
        String mt = mimeType();
        String name = fileName();
        return attachment() != null
            && (mt.equals("application/msword")
                || mt.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                || name.endsWith(".doc")
                || name.endsWith(".docx"));
    }

    /** Start of the document (text and tables) as HTML. */
    public String wordPreviewHtml() {
        ERAttachment a = attachment();
        if (a == null) {
            return "";
        }
        String cacheKey = "doc-" + a.primaryKey();
        String cached = PREVIEW_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        try (InputStream in = ERAttachmentProcessor.processorForType(a).attachmentInputStream(a)) {
            byte[] data = in.readAllBytes();

            StringBuilder sb = new StringBuilder();
            sb.append("<div style=\"max-width:300px;max-height:100px;overflow:auto;")
              .append("border:1px solid #ccc;padding:8px 12px;font-size:0.85em;background:#fff;\">");

            // Decide by file content, not name: .docx is a zip (OOXML), old .doc is OLE2
            boolean truncated = (FileMagic.valueOf(data) == FileMagic.OOXML)
                ? renderDocx(data, sb)
                : renderDoc(data, sb);

            if (truncated) {
                sb.append("<p style=\"color:#666;font-style:italic;margin-top:8px;\">")
                  .append("Preview shows the beginning of the document.</p>");
            }
            sb.append("</div>");

            String html = sb.toString();
            PREVIEW_CACHE.put(cacheKey, html);
            return html;
        } catch (Exception e) {
            System.err.println("AttachmentDisplay: could not render Word preview: " + e);
            return "<em>Preview not available</em>";
        }
    }

    /** .docx: paragraphs and tables in document order. Returns true if truncated. */
    private static boolean renderDocx(byte[] data, StringBuilder sb) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(data))) {
            int blocks = 0;
            for (IBodyElement el : doc.getBodyElements()) {
                if (blocks >= MAX_WORD_BLOCKS) {
                    return true;
                }
                if (el instanceof XWPFParagraph) {
                    XWPFParagraph p = (XWPFParagraph) el;
                    String text = p.getText();
                    if (text == null || text.isBlank()) {
                        continue;
                    }
                    String style = p.getStyle() == null ? "" : p.getStyle().toLowerCase();
                    boolean heading = style.startsWith("heading") || style.equals("title");
                    sb.append(heading
                            ? "<p style=\"font-weight:bold;margin:10px 0 4px;\">"
                            : "<p style=\"margin:0 0 6px;\">")
                      .append(escape(text))
                      .append("</p>");
                    blocks++;
                } else if (el instanceof XWPFTable) {
                    XWPFTable t = (XWPFTable) el;
                    sb.append("<table style=\"border-collapse:collapse;margin:6px 0;\">");
                    int rows = 0;
                    for (XWPFTableRow row : t.getRows()) {
                        if (rows++ >= MAX_WORD_TABLE_ROWS) {
                            break;
                        }
                        sb.append("<tr>");
                        for (XWPFTableCell cell : row.getTableCells()) {
                            sb.append("<td style=\"border:1px solid #ccc;padding:2px 6px;\">")
                              .append(escape(cell.getText()))
                              .append("</td>");
                        }
                        sb.append("</tr>");
                    }
                    sb.append("</table>");
                    blocks++;
                }
            }
        }
        return false;
    }

    /** Old .doc: plain paragraphs (tables come through as text). Returns true if truncated. */
    private static boolean renderDoc(byte[] data, StringBuilder sb) throws IOException {
        try (WordExtractor ex = new WordExtractor(new ByteArrayInputStream(data))) {
            int blocks = 0;
            for (String raw : ex.getParagraphText()) {
                // Remove Word field codes and control characters (e.g. table cell markers)
                String text = WordExtractor.stripFields(raw)
                        .replaceAll("[\\p{Cntrl}&&[^\\t]]", " ")
                        .trim();
                if (text.isEmpty()) {
                    continue;
                }
                if (blocks >= MAX_WORD_BLOCKS) {
                    return true;
                }
                sb.append("<p style=\"margin:0 0 6px;\">").append(escape(text)).append("</p>");
                blocks++;
            }
        }
        return false;
    }

    // ---------- PDF preview ----------

    /** First page of the PDF as a PNG data URL, or null if it can't be rendered. */
    public String pdfPreviewSrc() {
        ERAttachment a = attachment();
        if (a == null) {
            return null;
        }
        String cacheKey = "pdf-" + a.primaryKey();
        String cached = PREVIEW_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        try (InputStream in = ERAttachmentProcessor.processorForType(a).attachmentInputStream(a);
             PDDocument doc = Loader.loadPDF(in.readAllBytes())) {
            BufferedImage page = new PDFRenderer(doc).renderImageWithDPI(0, 100);
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(page, "png", png);
            String src = "data:image/png;base64," + Base64.getEncoder().encodeToString(png.toByteArray());
            PREVIEW_CACHE.put(cacheKey, src);
            return src;
        } catch (Exception e) {
            System.err.println("AttachmentDisplay: could not render PDF preview: " + e);
            return null;
        }
    }

    // ---------- spreadsheet preview ----------

    /** First sheet as an HTML table (first MAX_ROWS rows, MAX_COLS columns). */
    public String spreadsheetPreviewHtml() {
        ERAttachment a = attachment();
        if (a == null) {
            return "";
        }
        String cacheKey = "xls-" + a.primaryKey();
        String cached = PREVIEW_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        try (InputStream in = ERAttachmentProcessor.processorForType(a).attachmentInputStream(a);
             Workbook wb = WorkbookFactory.create(in)) {

            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();
            FormulaEvaluator ev = wb.getCreationHelper().createFormulaEvaluator();

            int totalRows = sheet.getLastRowNum() + 1;
            int shownRows = Math.min(totalRows, MAX_ROWS);

            int totalCols = 0;
            for (int r = 0; r < shownRows; r++) {
                Row row = sheet.getRow(r);
                if (row != null) {
                    totalCols = Math.max(totalCols, row.getLastCellNum());
                }
            }
            int shownCols = Math.min(totalCols, MAX_COLS);

            StringBuilder sb = new StringBuilder();
            sb.append("<div style=\"max-width:100%;overflow-x:auto;\">");
            sb.append("<div style=\"font-size:0.85em;color:#666;margin-bottom:4px;\">Sheet: ")
              .append(escape(sheet.getSheetName())).append("</div>");
            sb.append("<table style=\"border-collapse:collapse;font-size:0.85em;\">");
            for (int r = 0; r < shownRows; r++) {
                Row row = sheet.getRow(r);
                sb.append("<tr>");
                for (int c = 0; c < shownCols; c++) {
                    Cell cell = (row == null) ? null : row.getCell(c);
                    sb.append("<td style=\"border:1px solid #ccc;padding:2px 6px;white-space:nowrap;\">")
                      .append(escape(cellText(cell, fmt, ev)))
                      .append("</td>");
                }
                sb.append("</tr>");
            }
            sb.append("</table>");
            if (totalRows > shownRows || totalCols > shownCols) {
                sb.append("<div style=\"font-size:0.8em;color:#666;margin-top:4px;\">Showing ")
                  .append(shownRows).append(" of ").append(totalRows).append(" rows, ")
                  .append(shownCols).append(" of ").append(totalCols).append(" columns</div>");
            }
            sb.append("</div>");

            String html = sb.toString();
            PREVIEW_CACHE.put(cacheKey, html);
            return html;
        } catch (Exception e) {
            System.err.println("AttachmentDisplay: could not render spreadsheet preview: " + e);
            return "<em>Preview not available</em>";
        }
    }

    private static String cellText(Cell cell, DataFormatter fmt, FormulaEvaluator ev) {
        if (cell == null) {
            return "";
        }
        try {
            return fmt.formatCellValue(cell, ev);   // formulas show their calculated result
        } catch (RuntimeException e) {
            return fmt.formatCellValue(cell);       // formula POI can't evaluate: show it as written
        }
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}