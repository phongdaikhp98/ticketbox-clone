package com.example.ticketbox.service;

import com.example.ticketbox.exception.ResourceNotFoundException;
import com.example.ticketbox.model.Event;
import com.example.ticketbox.model.Ticket;
import com.example.ticketbox.repository.TicketRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class TicketPdfService {

    private final TicketRepository ticketRepository;
    private final QrCodeService qrCodeService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generateTicketPdf(Long userId, Long ticketId) {
        Ticket ticket = ticketRepository.findByIdAndUserId(ticketId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));

        Event event = ticket.getEvent();
        byte[] qrImage = qrCodeService.generateQrImage(ticket.getQrData(), 200, 200);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A5.rotate(), 30, 30, 20, 20);
            PdfWriter.getInstance(document, baos);
            document.open();

            // Fonts
            Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD, new Color(33, 37, 41));
            Font headerFont = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(100, 100, 100));
            Font valueFont = new Font(Font.HELVETICA, 13, Font.NORMAL, new Color(33, 37, 41));
            Font codeFont = new Font(Font.COURIER, 16, Font.BOLD, new Color(79, 70, 229));
            Font brandFont = new Font(Font.HELVETICA, 9, Font.ITALIC, new Color(150, 150, 150));

            // Main table: 2 columns (info left, QR right)
            PdfPTable mainTable = new PdfPTable(2);
            mainTable.setWidthPercentage(100);
            mainTable.setWidths(new float[]{65, 35});

            // Left cell: ticket info
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.setPaddingRight(15);

            // Event title
            Paragraph title = new Paragraph(event.getTitle(), titleFont);
            title.setSpacingAfter(12);
            leftCell.addElement(title);

            // Info rows
            addInfoRow(leftCell, "Date", event.getEventDate().format(DATE_FMT), headerFont, valueFont);
            addInfoRow(leftCell, "Location", event.getLocation(), headerFont, valueFont);
            addInfoRow(leftCell, "Ticket Type", ticket.getTicketType().getName(), headerFont, valueFont);
            addInfoRow(leftCell, "Attendee", ticket.getUser().getFullName(), headerFont, valueFont);

            // Ticket code
            Paragraph codePara = new Paragraph();
            codePara.setSpacingBefore(10);
            codePara.add(new Chunk("Code: ", headerFont));
            codePara.add(new Chunk(ticket.getTicketCode(), codeFont));
            leftCell.addElement(codePara);

            mainTable.addCell(leftCell);

            // Right cell: QR code
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            rightCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            Image qr = Image.getInstance(qrImage);
            qr.scaleToFit(150, 150);
            rightCell.addElement(qr);

            Paragraph scanText = new Paragraph("Scan to check-in", brandFont);
            scanText.setAlignment(Element.ALIGN_CENTER);
            scanText.setSpacingBefore(5);
            rightCell.addElement(scanText);

            mainTable.addCell(rightCell);
            document.add(mainTable);

            // Footer
            Paragraph footer = new Paragraph("TicketBox - Your Event, Your Ticket", brandFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(15);
            document.add(footer);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate ticket PDF", e);
        }
    }

    private void addInfoRow(PdfPCell cell, String label, String value, Font headerFont, Font valueFont) {
        Paragraph p = new Paragraph();
        p.setSpacingBefore(4);
        p.add(new Chunk(label + ": ", headerFont));
        p.add(new Chunk(value != null ? value : "N/A", valueFont));
        cell.addElement(p);
    }
}
