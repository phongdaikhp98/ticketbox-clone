package com.example.ticketbox.service;

import com.example.ticketbox.model.Order;
import com.example.ticketbox.model.OrderStatus;
import com.example.ticketbox.model.User;
import com.example.ticketbox.repository.OrderItemRepository;
import com.example.ticketbox.repository.OrderRepository;
import com.example.ticketbox.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] ORDER_HEADERS = {
            "ID", "Khách hàng", "Email", "Sự kiện", "Tổng tiền (VND)",
            "Trạng thái", "Trạng thái TT", "Ngày tạo"
    };
    private static final String[] USER_HEADERS = {
            "ID", "Họ tên", "Email", "Số điện thoại", "Vai trò", "Hoạt động", "Xác thực email", "Ngày đăng ký"
    };
    private static final String[] REVENUE_HEADERS = {
            "Sự kiện", "Doanh thu (VND)"
    };

    public byte[] exportOrders() {
        List<Order> orders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdDate"));
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Đơn hàng");
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle currencyStyle = createCurrencyStyle(wb);

            writeHeaders(sheet, headerStyle, ORDER_HEADERS);

            int rowNum = 1;
            for (Order o : orders) {
                String eventTitle = o.getOrderItems().isEmpty() ? ""
                        : o.getOrderItems().get(0).getEvent().getTitle();
                Row row = sheet.createRow(rowNum++);
                setCell(row, 0, o.getId());
                setCell(row, 1, o.getUser().getFullName());
                setCell(row, 2, o.getUser().getEmail());
                setCell(row, 3, eventTitle);
                setCurrencyCell(row, 4, o.getTotalAmount(), currencyStyle);
                setCell(row, 5, translateOrderStatus(o.getStatus().name()));
                setCell(row, 6, o.getPaymentStatus() != null ? o.getPaymentStatus().name() : "");
                setCell(row, 7, o.getCreatedDate() != null ? o.getCreatedDate().format(DATE_FMT) : "");
            }

            autoSizeColumns(sheet, ORDER_HEADERS.length);
            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Không thể xuất file Excel", e);
        }
    }

    public byte[] exportUsers() {
        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdDate"));
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Người dùng");
            CellStyle headerStyle = createHeaderStyle(wb);

            writeHeaders(sheet, headerStyle, USER_HEADERS);

            int rowNum = 1;
            for (User u : users) {
                Row row = sheet.createRow(rowNum++);
                setCell(row, 0, u.getId());
                setCell(row, 1, u.getFullName());
                setCell(row, 2, u.getEmail());
                setCell(row, 3, u.getPhone() != null ? u.getPhone() : "");
                setCell(row, 4, translateRole(u.getRole().name()));
                setCell(row, 5, Boolean.TRUE.equals(u.getIsActive()) ? "Có" : "Không");
                setCell(row, 6, Boolean.TRUE.equals(u.getEmailVerified()) ? "Có" : "Không");
                setCell(row, 7, u.getCreatedDate() != null ? u.getCreatedDate().format(DATE_FMT) : "");
            }

            autoSizeColumns(sheet, USER_HEADERS.length);
            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Không thể xuất file Excel", e);
        }
    }

    public byte[] exportRevenue() {
        // Use existing aggregate query — fetch all (large page)
        List<Object[]> rows = orderItemRepository.findTopEventsByRevenue(
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE));
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Doanh thu");
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle currencyStyle = createCurrencyStyle(wb);

            writeHeaders(sheet, headerStyle, REVENUE_HEADERS);

            int rowNum = 1;
            for (Object[] r : rows) {
                String title = (String) r[1];
                BigDecimal revenue = (BigDecimal) r[2];
                Row row = sheet.createRow(rowNum++);
                setCell(row, 0, title);
                setCurrencyCell(row, 1, revenue, currencyStyle);
            }

            autoSizeColumns(sheet, REVENUE_HEADERS.length);
            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Không thể xuất file Excel", e);
        }
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private void writeHeaders(Sheet sheet, CellStyle style, String[] headers) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void setCell(Row row, int col, Object value) {
        Cell cell = row.createCell(col);
        if (value instanceof Long l) cell.setCellValue(l);
        else if (value instanceof Number n) cell.setCellValue(n.doubleValue());
        else cell.setCellValue(value != null ? value.toString() : "");
    }

    private void setCurrencyCell(Row row, int col, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value.doubleValue() : 0);
        cell.setCellStyle(style);
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        return style;
    }

    private void autoSizeColumns(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) {
            sheet.autoSizeColumn(i);
            // Add padding
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1024);
        }
    }

    private byte[] toBytes(XSSFWorkbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }

    private String translateOrderStatus(String status) {
        return switch (status) {
            case "PENDING" -> "Chờ thanh toán";
            case "COMPLETED" -> "Hoàn thành";
            case "CANCELLED" -> "Đã hủy";
            default -> status;
        };
    }

    private String translateRole(String role) {
        return switch (role) {
            case "ADMIN" -> "Quản trị";
            case "ORGANIZER" -> "Tổ chức";
            default -> "Khách hàng";
        };
    }
}
