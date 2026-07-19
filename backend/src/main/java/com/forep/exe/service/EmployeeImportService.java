package com.forep.exe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forep.exe.domain.Enums.*;
import com.forep.exe.dto.Requests.CreateEmployeeRequest;
import com.forep.exe.persistence.*;
import com.forep.exe.security.AuthorizationService;
import com.forep.exe.security.SecurityContext;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
@Transactional
public class EmployeeImportService {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int MAX_ROWS = 1000;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final List<String> HEADERS = List.of(
            "fullName", "email", "phone", "departmentCode", "positionCode", "skills",
            "seniorityLevel", "yearsOfExperience", "employmentType", "employeeLevel",
            "monthlyWorkingCapacityHours", "mainExpertise", "secondaryExpertise"
    );

    private final EmployeeImportBatchRepository batches;
    private final EmployeeImportRowRepository rows;
    private final DepartmentRepository departments;
    private final JobPositionRepository positions;
    private final UserRepository users;
    private final WorkspaceRepository workspaces;
    private final AuthorizationService authorization;
    private final SecurityContext securityContext;
    private final ObjectMapper objectMapper;
    private final ForepService forepService;

    public EmployeeImportService(EmployeeImportBatchRepository batches,
                                 EmployeeImportRowRepository rows,
                                 DepartmentRepository departments,
                                 JobPositionRepository positions,
                                 UserRepository users,
                                 WorkspaceRepository workspaces,
                                 AuthorizationService authorization,
                                 SecurityContext securityContext,
                                 ObjectMapper objectMapper,
                                 ForepService forepService) {
        this.batches = batches;
        this.rows = rows;
        this.departments = departments;
        this.positions = positions;
        this.users = users;
        this.workspaces = workspaces;
        this.authorization = authorization;
        this.securityContext = securityContext;
        this.objectMapper = objectMapper;
        this.forepService = forepService;
    }

    @Transactional(readOnly = true)
    public FilePayload template() {
        authorization.require(Permission.EMPLOYEE_IMPORT);
        UUID workspaceId = workspaceId();
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet employees = workbook.createSheet("Employees");
            Row header = employees.createRow(0);
            CellStyle required = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            required.setFont(font);
            for (int index = 0; index < HEADERS.size(); index++) {
                Cell cell = header.createCell(index);
                cell.setCellValue(HEADERS.get(index));
                cell.setCellStyle(required);
                employees.setColumnWidth(index, 24 * 256);
            }
            Row sample = employees.createRow(1);
            List<String> example = List.of("Nguyen Van An", "an@example.com", "0900000000", "ENG", "BE-JAVA", "Java, Spring Boot", "MIDDLE", "3", "FULL_TIME", "MIDDLE", "168", "Backend", "PostgreSQL");
            for (int index = 0; index < example.size(); index++) sample.createCell(index).setCellValue(example.get(index));

            Sheet master = workbook.createSheet("MasterData");
            master.createRow(0).createCell(0).setCellValue("Departments");
            int row = 1;
            for (DepartmentEntity department : departments.findByWorkspaceIdAndStatusOrderByNameAsc(workspaceId, DepartmentStatus.ACTIVE)) {
                Row item = master.createRow(row++);
                item.createCell(0).setCellValue(department.getCode());
                item.createCell(1).setCellValue(department.getName());
            }
            row = 1;
            master.getRow(0).createCell(3).setCellValue("Positions");
            for (JobPositionEntity position : positions.findByWorkspaceIdOrderByNameAsc(workspaceId)) {
                Row item = master.getRow(row);
                if (item == null) item = master.createRow(row);
                item.createCell(3).setCellValue(position.getCode());
                item.createCell(4).setCellValue(position.getTitle());
                item.createCell(5).setCellValue(position.getDepartmentName());
                row++;
            }
            workbook.write(output);
            return new FilePayload("forep-employee-import-template.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create employee import template.", exception);
        }
    }

    public BatchView validate(MultipartFile file) {
        authorization.require(Permission.EMPLOYEE_IMPORT);
        validateFile(file);
        UUID workspaceId = workspaceId();
        OffsetDateTime now = OffsetDateTime.now();
        EmployeeImportBatchEntity batch = new EmployeeImportBatchEntity();
        batch.setWorkspaceId(workspaceId);
        batch.setFileName(safeFileName(file.getOriginalFilename()));
        batch.setStatus("VALIDATED");
        batch.setCreatedBy(securityContext.currentUser().userId());
        batch.setCreatedAt(now);
        batch.setUpdatedAt(now);
        batch = batches.save(batch);

        List<EmployeeImportRowEntity> parsed = parseRows(file, batch, workspaceId);
        rows.saveAll(parsed);
        batch.setTotalRows(parsed.size());
        batch.setValidRows((int) parsed.stream().filter(EmployeeImportRowEntity::isValid).count());
        batch.setInvalidRows(parsed.size() - batch.getValidRows());
        batch.setUpdatedAt(OffsetDateTime.now());
        return view(batches.save(batch), parsed);
    }

    @Transactional(readOnly = true)
    public List<BatchSummary> history() {
        authorization.require(Permission.EMPLOYEE_IMPORT);
        return batches.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId()).stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public BatchView batch(UUID batchId) {
        authorization.require(Permission.EMPLOYEE_IMPORT);
        EmployeeImportBatchEntity batch = requireBatch(batchId);
        return view(batch, rows.findByBatchIdOrderByRowNumberAsc(batchId));
    }

    public BatchView confirm(UUID batchId) {
        authorization.require(Permission.EMPLOYEE_IMPORT);
        EmployeeImportBatchEntity batch = requireBatch(batchId);
        List<EmployeeImportRowEntity> batchRows = rows.findByBatchIdOrderByRowNumberAsc(batchId);
        if ("CONFIRMED".equals(batch.getStatus())) return view(batch, batchRows);
        if (!"VALIDATED".equals(batch.getStatus())) throw new IllegalArgumentException("Only a validated import batch can be confirmed.");

        int imported = 0;
        for (EmployeeImportRowEntity row : batchRows) {
            if (!row.isValid() || row.isImported()) continue;
            try {
                CreateEmployeeRequest request = objectMapper.readValue(row.getRawData(), CreateEmployeeRequest.class);
                var created = forepService.createEmployee(request);
                row.setImported(true);
                row.setImportedUserId(created.user().id());
                imported++;
            } catch (Exception exception) {
                row.setValid(false);
                row.setErrors("Import failed: " + safeError(exception));
            }
        }
        rows.saveAll(batchRows);
        OffsetDateTime now = OffsetDateTime.now();
        batch.setImportedRows(batch.getImportedRows() + imported);
        batch.setValidRows((int) batchRows.stream().filter(EmployeeImportRowEntity::isValid).count());
        batch.setInvalidRows(batchRows.size() - batch.getValidRows());
        batch.setStatus("CONFIRMED");
        batch.setConfirmedBy(securityContext.currentUser().userId());
        batch.setConfirmedAt(now);
        batch.setUpdatedAt(now);
        return view(batches.save(batch), batchRows);
    }

    public BatchView cancel(UUID batchId) {
        authorization.require(Permission.EMPLOYEE_IMPORT);
        EmployeeImportBatchEntity batch = requireBatch(batchId);
        if ("CONFIRMED".equals(batch.getStatus())) throw new IllegalArgumentException("A confirmed import batch cannot be cancelled.");
        batch.setStatus("CANCELLED");
        batch.setCancelledAt(OffsetDateTime.now());
        batch.setUpdatedAt(OffsetDateTime.now());
        return view(batches.save(batch), rows.findByBatchIdOrderByRowNumberAsc(batchId));
    }

    @Transactional(readOnly = true)
    public FilePayload errorReport(UUID batchId) {
        authorization.require(Permission.EMPLOYEE_IMPORT);
        EmployeeImportBatchEntity batch = requireBatch(batchId);
        List<EmployeeImportRowEntity> invalidRows = rows.findByBatchIdOrderByRowNumberAsc(batchId).stream().filter(row -> !row.isValid()).toList();
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Errors");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("rowNumber");
            header.createCell(1).setCellValue("errors");
            header.createCell(2).setCellValue("data");
            int index = 1;
            for (EmployeeImportRowEntity invalid : invalidRows) {
                Row row = sheet.createRow(index++);
                row.createCell(0).setCellValue(invalid.getRowNumber());
                row.createCell(1).setCellValue(invalid.getErrors());
                row.createCell(2).setCellValue(invalid.getRawData());
            }
            sheet.setColumnWidth(1, 50 * 256);
            sheet.setColumnWidth(2, 100 * 256);
            workbook.write(output);
            return new FilePayload("employee-import-errors-" + batch.getId() + ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create import error report.", exception);
        }
    }

    private List<EmployeeImportRowEntity> parseRows(MultipartFile file, EmployeeImportBatchEntity batch, UUID workspaceId) {
        Map<String, DepartmentEntity> departmentByCode = new HashMap<>();
        departments.findByWorkspaceIdAndStatusOrderByNameAsc(workspaceId, DepartmentStatus.ACTIVE)
                .forEach(item -> departmentByCode.put(normalize(item.getCode()), item));
        Map<String, JobPositionEntity> positionByCode = new HashMap<>();
        positions.findByWorkspaceIdOrderByNameAsc(workspaceId)
                .forEach(item -> positionByCode.put(normalize(item.getCode()), item));
        Set<String> emailsInFile = new HashSet<>();
        WorkspaceEntity workspace = workspaces.findById(workspaceId).orElseThrow();
        long currentEmployees = users.findByWorkspaceId(workspaceId).stream().filter(user -> !List.of(Role.BUSINESS_OWNER, Role.OWNER).contains(user.getRole())).count();
        int remaining = Math.max(0, workspace.getMaxEmployeeAccounts() - (int) currentEmployees);

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) throw new IllegalArgumentException("Excel file has no employee rows.");
            Map<String, Integer> columns = columns(sheet.getRow(0));
            for (String required : List.of("fullName", "email", "departmentCode", "positionCode")) {
                if (!columns.containsKey(required)) throw new IllegalArgumentException("Missing required column: " + required);
            }
            DataFormatter formatter = new DataFormatter();
            List<EmployeeImportRowEntity> result = new ArrayList<>();
            int acceptedRows = 0;
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row excelRow = sheet.getRow(rowIndex);
                if (excelRow == null || rowEmpty(excelRow, formatter)) continue;
                if (result.size() >= MAX_ROWS) throw new IllegalArgumentException("Import file exceeds the 1000-row limit.");
                Map<String, String> values = new LinkedHashMap<>();
                columns.forEach((name, column) -> values.put(name, formatter.formatCellValue(excelRow.getCell(column)).trim()));
                List<String> errors = new ArrayList<>();
                String fullName = values.getOrDefault("fullName", "");
                String email = values.getOrDefault("email", "").toLowerCase(Locale.ROOT);
                DepartmentEntity department = departmentByCode.get(normalize(values.get("departmentCode")));
                JobPositionEntity position = positionByCode.get(normalize(values.get("positionCode")));
                if (fullName.isBlank()) errors.add("fullName is required");
                if (!EMAIL.matcher(email).matches()) errors.add("email is invalid");
                if (!emailsInFile.add(email)) errors.add("email is duplicated in this file");
                if (users.existsByWorkspaceIdAndEmailIgnoreCase(workspaceId, email)) errors.add("email already exists in workspace");
                if (department == null) errors.add("departmentCode is unknown or inactive");
                if (position == null) errors.add("positionCode is unknown");
                if (department != null && position != null && !department.getId().equals(position.getDepartmentId())) errors.add("position does not belong to department");
                if (errors.isEmpty() && acceptedRows >= remaining) errors.add("workspace employee limit would be exceeded");
                if (errors.isEmpty()) acceptedRows++;

                CreateEmployeeRequest request = request(values, fullName, email, department, position, errors);
                EmployeeImportRowEntity row = new EmployeeImportRowEntity();
                row.setBatchId(batch.getId());
                row.setWorkspaceId(workspaceId);
                row.setRowNumber(rowIndex + 1);
                row.setRawData(write(request));
                row.setValid(errors.isEmpty());
                row.setErrors(errors.isEmpty() ? null : String.join("; ", errors));
                row.setImported(false);
                row.setCreatedAt(OffsetDateTime.now());
                result.add(row);
            }
            if (result.isEmpty()) throw new IllegalArgumentException("Excel file has no employee rows.");
            return result;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not read XLSX file.", exception);
        }
    }

    private CreateEmployeeRequest request(Map<String, String> values, String fullName, String email,
                                          DepartmentEntity department, JobPositionEntity position, List<String> errors) {
        return new CreateEmployeeRequest(fullName, email, values.get("phone"), position == null ? null : position.getTitle(),
                enumValue(SeniorityLevel.class, values.get("seniorityLevel"), errors), null, integer(values.get("yearsOfExperience"), "yearsOfExperience", errors),
                values.get("skills"), department == null ? null : department.getId(), position == null ? null : position.getId(), null, null, null, null,
                enumValue(EmploymentType.class, values.get("employmentType"), errors), WorkingStatus.WORKING,
                enumValue(EmployeeLevel.class, values.get("employeeLevel"), errors), integer(values.get("monthlyWorkingCapacityHours"), "monthlyWorkingCapacityHours", errors),
                values.get("mainExpertise"), values.get("secondaryExpertise"));
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, String value, List<String> errors) {
        if (value == null || value.isBlank()) return null;
        try { return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) { errors.add(type.getSimpleName() + " value is invalid"); return null; }
    }

    private Integer integer(String value, String field, List<String> errors) {
        if (value == null || value.isBlank()) return null;
        try { int parsed = Integer.parseInt(value.replace(".0", "")); if (parsed < 0) throw new NumberFormatException(); return parsed; }
        catch (NumberFormatException exception) { errors.add(field + " must be a non-negative integer"); return null; }
    }

    private Map<String, Integer> columns(Row header) {
        Map<String, Integer> result = new HashMap<>();
        DataFormatter formatter = new DataFormatter();
        for (Cell cell : header) result.put(formatter.formatCellValue(cell).trim(), cell.getColumnIndex());
        return result;
    }

    private boolean rowEmpty(Row row, DataFormatter formatter) {
        for (Cell cell : row) if (!formatter.formatCellValue(cell).isBlank()) return false;
        return true;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("XLSX file is required.");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("XLSX file must not exceed 10 MB.");
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".xlsx")) throw new IllegalArgumentException("Only XLSX files are accepted.");
    }

    private EmployeeImportBatchEntity requireBatch(UUID id) {
        return batches.findByIdAndWorkspaceId(id, workspaceId()).orElseThrow(() -> new IllegalArgumentException("Employee import batch not found."));
    }

    private UUID workspaceId() { return securityContext.currentUser().workspaceId(); }
    private String normalize(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
    private String safeFileName(String value) { return value == null ? "employees.xlsx" : value.replaceAll("[^A-Za-z0-9._-]", "_"); }
    private String safeError(Exception exception) { return exception.getMessage() == null ? "business validation failed" : exception.getMessage(); }
    private String write(Object value) { try { return objectMapper.writeValueAsString(value); } catch (Exception exception) { throw new IllegalStateException(exception); } }

    private BatchSummary summary(EmployeeImportBatchEntity batch) {
        return new BatchSummary(batch.getId(), batch.getFileName(), batch.getStatus(), batch.getTotalRows(), batch.getValidRows(), batch.getInvalidRows(), batch.getImportedRows(), batch.getCreatedAt(), batch.getConfirmedAt());
    }

    private BatchView view(EmployeeImportBatchEntity batch, List<EmployeeImportRowEntity> batchRows) {
        return new BatchView(summary(batch), batchRows.stream().map(row -> new RowView(row.getId(), row.getRowNumber(), row.isValid(), row.getErrors(), row.isImported(), row.getImportedUserId(), row.getRawData())).toList());
    }

    public record BatchSummary(UUID id, String fileName, String status, int totalRows, int validRows, int invalidRows, int importedRows, OffsetDateTime createdAt, OffsetDateTime confirmedAt) {}
    public record RowView(UUID id, int rowNumber, boolean valid, String errors, boolean imported, UUID importedUserId, String data) {}
    public record BatchView(BatchSummary batch, List<RowView> rows) {}
    public record FilePayload(String fileName, String contentType, byte[] content) {}
}
