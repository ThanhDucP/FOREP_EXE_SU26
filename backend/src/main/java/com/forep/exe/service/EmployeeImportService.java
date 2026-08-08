package com.forep.exe.service;

import com.fasterxml.jackson.core.type.TypeReference;
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

    /**
     * departmentName / positionName are used as creation suggestions when a code
     * does not exist yet. positionPermissionGroup defaults to EMPLOYEE.
     */
    private static final List<String> HEADERS = List.of(
            "fullName", "email", "phone",
            "departmentCode", "departmentName",
            "positionCode", "positionName", "positionPermissionGroup",
            "skills", "seniorityLevel", "yearsOfExperience", "employmentType", "employeeLevel",
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
            employees.createFreezePane(0, 1);
            Row header = employees.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            for (int index = 0; index < HEADERS.size(); index++) {
                Cell cell = header.createCell(index);
                cell.setCellValue(HEADERS.get(index));
                cell.setCellStyle(headerStyle);
                employees.setColumnWidth(index, 24 * 256);
            }

            Row sample = employees.createRow(1);
            List<String> example = List.of(
                    "Nguyen Van An", "an@example.com", "0900000000",
                    "ENG", "Engineering",
                    "BE-JAVA", "Java Backend Developer", "EMPLOYEE",
                    "Java, Spring Boot", "MIDDLE", "3", "FULL_TIME", "MIDDLE",
                    "168", "Backend", "PostgreSQL"
            );
            for (int index = 0; index < example.size(); index++) {
                sample.createCell(index).setCellValue(example.get(index));
            }

            Sheet instructions = workbook.createSheet("Instructions");
            String[][] instructionRows = {
                    {"Quy tắc", "Mô tả"},
                    {"Cột bắt buộc", "fullName, email, departmentCode, positionCode"},
                    {"Department mới", "Nếu departmentCode chưa tồn tại, điền departmentName. Sau upload hệ thống sẽ cho chọn phòng ban cần tạo."},
                    {"Position mới", "Nếu positionCode chưa tồn tại, điền positionName và departmentCode. Sau upload hệ thống sẽ cho chọn vị trí cần tạo."},
                    {"positionPermissionGroup", "EMPLOYEE / MANAGER / EXECUTIVE. Để trống sẽ mặc định EMPLOYEE."},
                    {"Enum", "seniorityLevel: INTERN/JUNIOR/MIDDLE/SENIOR/LEAD; employmentType: FULL_TIME/PART_TIME/CONTRACTOR/INTERN; employeeLevel: INTERN/FRESHER/JUNIOR/MIDDLE/SENIOR/LEAD/MANAGER"},
                    {"Lưu ý", "Không đổi tên header. Tối đa 1000 dòng, file .xlsx tối đa 10 MB."}
            };
            for (int r = 0; r < instructionRows.length; r++) {
                Row row = instructions.createRow(r);
                row.createCell(0).setCellValue(instructionRows[r][0]);
                row.createCell(1).setCellValue(instructionRows[r][1]);
                if (r == 0) {
                    row.getCell(0).setCellStyle(headerStyle);
                    row.getCell(1).setCellStyle(headerStyle);
                }
            }
            instructions.setColumnWidth(0, 28 * 256);
            instructions.setColumnWidth(1, 110 * 256);

            Sheet master = workbook.createSheet("MasterData");
            master.createRow(0).createCell(0).setCellValue("Departments");
            master.getRow(0).createCell(3).setCellValue("Positions");
            int rowIndex = 1;
            for (DepartmentEntity department : departments.findByWorkspaceIdAndStatusOrderByNameAsc(workspaceId, DepartmentStatus.ACTIVE)) {
                Row item = master.createRow(rowIndex++);
                item.createCell(0).setCellValue(nullSafe(department.getCode()));
                item.createCell(1).setCellValue(department.getName());
            }
            rowIndex = 1;
            for (JobPositionEntity position : positions.findByWorkspaceIdOrderByNameAsc(workspaceId)) {
                Row item = master.getRow(rowIndex);
                if (item == null) item = master.createRow(rowIndex);
                item.createCell(3).setCellValue(nullSafe(position.getCode()));
                item.createCell(4).setCellValue(position.getTitle());
                item.createCell(5).setCellValue(nullSafe(position.getDepartmentName()));
                item.createCell(6).setCellValue(position.getPermissionGroup() == null ? "" : position.getPermissionGroup().name());
                rowIndex++;
            }
            for (int index = 0; index <= 6; index++) master.setColumnWidth(index, 28 * 256);

            workbook.write(output);
            return new FilePayload(
                    "forep-employee-import-template.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray()
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tạo file mẫu import nhân viên.", exception);
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
        updateBatchCounts(batch, parsed);
        batch.setUpdatedAt(OffsetDateTime.now());
        batch = batches.save(batch);
        return view(batch, parsed, List.of(), List.of());
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
        return view(batch, rows.findByBatchIdOrderByRowNumberAsc(batchId), List.of(), List.of());
    }

    /** Backward-compatible confirm. Missing master data must be resolved explicitly. */
    public BatchView confirm(UUID batchId) {
        EmployeeImportBatchEntity batch = requireBatch(batchId);
        List<EmployeeImportRowEntity> batchRows = rows.findByBatchIdOrderByRowNumberAsc(batchId);
        MasterDataSummary missing = masterDataSummary(batch.getWorkspaceId(), batchRows);
        if (!missing.departments().isEmpty() || !missing.positions().isEmpty()) {
            throw new IllegalArgumentException(
                    "Workbook có phòng ban/vị trí chưa tồn tại. Vui lòng chọn các mục cần tạo trước khi xác nhận import."
            );
        }
        return confirm(batchId, new ConfirmRequest(List.of(), List.of()));
    }

    /**
     * Creates only the missing departments/positions selected by the user, then
     * re-validates all rows and imports rows that are valid after that resolution.
     */
    public BatchView confirm(UUID batchId, ConfirmRequest request) {
        authorization.require(Permission.EMPLOYEE_IMPORT);
        EmployeeImportBatchEntity batch = requireBatch(batchId);
        List<EmployeeImportRowEntity> batchRows = rows.findByBatchIdOrderByRowNumberAsc(batchId);
        if ("CONFIRMED".equals(batch.getStatus())) return view(batch, batchRows, List.of(), List.of());
        if (!"VALIDATED".equals(batch.getStatus())) {
            throw new IllegalArgumentException("Chỉ batch đã kiểm tra mới có thể xác nhận import.");
        }

        ConfirmRequest safeRequest = request == null ? new ConfirmRequest(List.of(), List.of()) : request;
        Set<String> selectedDepartmentCodes = normalizeCodes(safeRequest.createDepartmentCodes());
        Set<String> selectedPositionCodes = normalizeCodes(safeRequest.createPositionCodes());
        MasterDataSummary before = masterDataSummary(batch.getWorkspaceId(), batchRows);

        List<String> createdDepartments = createSelectedDepartments(batch.getWorkspaceId(), before.departments(), selectedDepartmentCodes);
        List<String> createdPositions = createSelectedPositions(batch.getWorkspaceId(), before.positions(), selectedPositionCodes);

        revalidateRows(batch.getWorkspaceId(), batchRows);
        rows.saveAll(batchRows);

        int imported = 0;
        for (EmployeeImportRowEntity row : batchRows) {
            if (!row.isValid() || row.isImported()) continue;
            try {
                CreateEmployeeRequest employee = employeeRequest(batch.getWorkspaceId(), source(row), new ArrayList<>());
                var created = forepService.createEmployee(employee);
                row.setImported(true);
                row.setImportedUserId(created.user().id());
                row.setErrors(null);
                imported++;
            } catch (Exception exception) {
                row.setValid(false);
                row.setErrors(write(List.of(new RowIssue("IMPORT_FAILED", null, "Import failed: " + safeError(exception)))));
            }
        }
        rows.saveAll(batchRows);

        OffsetDateTime now = OffsetDateTime.now();
        batch.setImportedRows(batch.getImportedRows() + imported);
        updateBatchCounts(batch, batchRows);
        batch.setStatus("CONFIRMED");
        batch.setConfirmedBy(securityContext.currentUser().userId());
        batch.setConfirmedAt(now);
        batch.setUpdatedAt(now);
        batch = batches.save(batch);
        return view(batch, batchRows, createdDepartments, createdPositions);
    }

    public BatchView cancel(UUID batchId) {
        authorization.require(Permission.EMPLOYEE_IMPORT);
        EmployeeImportBatchEntity batch = requireBatch(batchId);
        if ("CONFIRMED".equals(batch.getStatus())) throw new IllegalArgumentException("Batch đã import không thể hủy.");
        batch.setStatus("CANCELLED");
        batch.setCancelledAt(OffsetDateTime.now());
        batch.setUpdatedAt(OffsetDateTime.now());
        return view(batches.save(batch), rows.findByBatchIdOrderByRowNumberAsc(batchId), List.of(), List.of());
    }

    @Transactional(readOnly = true)
    public FilePayload errorReport(UUID batchId) {
        authorization.require(Permission.EMPLOYEE_IMPORT);
        EmployeeImportBatchEntity batch = requireBatch(batchId);
        List<EmployeeImportRowEntity> invalidRows = rows.findByBatchIdOrderByRowNumberAsc(batchId)
                .stream().filter(row -> !row.isValid()).toList();
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
                row.createCell(1).setCellValue(humanErrors(invalid.getErrors()));
                row.createCell(2).setCellValue(invalid.getRawData());
            }
            sheet.setColumnWidth(1, 70 * 256);
            sheet.setColumnWidth(2, 100 * 256);
            workbook.write(output);
            return new FilePayload(
                    "employee-import-errors-" + batch.getId() + ".xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray()
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tạo báo cáo lỗi import.", exception);
        }
    }

    private List<EmployeeImportRowEntity> parseRows(MultipartFile file, EmployeeImportBatchEntity batch, UUID workspaceId) {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) throw new IllegalArgumentException("Workbook không có sheet dữ liệu.");
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() < 2) throw new IllegalArgumentException("Excel không có dòng nhân viên.");
            Row header = sheet.getRow(0);
            if (header == null) throw new IllegalArgumentException("Excel thiếu dòng header.");
            Map<String, Integer> columns = columns(header);
            for (String required : List.of("fullName", "email", "departmentCode", "positionCode")) {
                if (!columns.containsKey(required)) throw new IllegalArgumentException("Thiếu cột bắt buộc: " + required);
            }

            DataFormatter formatter = new DataFormatter();
            List<EmployeeImportRowEntity> result = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row excelRow = sheet.getRow(rowIndex);
                if (excelRow == null || rowEmpty(excelRow, formatter)) continue;
                if (result.size() >= MAX_ROWS) throw new IllegalArgumentException("File import vượt quá giới hạn 1000 dòng.");

                Map<String, String> values = new LinkedHashMap<>();
                for (String headerName : HEADERS) {
                    Integer column = columns.get(headerName);
                    values.put(headerName, column == null ? "" : formatter.formatCellValue(excelRow.getCell(column)).trim());
                }

                EmployeeImportRowEntity row = new EmployeeImportRowEntity();
                row.setBatchId(batch.getId());
                row.setWorkspaceId(workspaceId);
                row.setRowNumber(rowIndex + 1);
                row.setRawData(write(values));
                row.setImported(false);
                row.setCreatedAt(OffsetDateTime.now());
                result.add(row);
            }
            if (result.isEmpty()) throw new IllegalArgumentException("Excel không có dòng nhân viên.");
            revalidateRows(workspaceId, result);
            return result;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Không thể đọc workbook .xlsx. Hãy tải lại file mẫu và kiểm tra định dạng file.", exception);
        }
    }

    private void revalidateRows(UUID workspaceId, List<EmployeeImportRowEntity> batchRows) {
        Map<String, DepartmentEntity> departmentByCode = departmentMap(workspaceId);
        Map<String, JobPositionEntity> positionByCode = positionMap(workspaceId);
        Set<String> emailsInFile = new HashSet<>();
        WorkspaceEntity workspace = workspaces.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace không tồn tại."));
        long currentEmployees = users.findByWorkspaceId(workspaceId).stream()
                .filter(user -> !List.of(Role.BUSINESS_OWNER, Role.OWNER).contains(user.getRole()))
                .count();
        int remaining = Math.max(0, workspace.getMaxEmployeeAccounts() - (int) currentEmployees);
        int acceptedRows = 0;

        for (EmployeeImportRowEntity row : batchRows) {
            if (row.isImported()) continue;
            Map<String, String> source = source(row);
            List<RowIssue> issues = new ArrayList<>();
            validateSource(workspaceId, source, departmentByCode, positionByCode, emailsInFile, issues);
            if (issues.isEmpty() && acceptedRows >= remaining) {
                issues.add(new RowIssue("WORKSPACE_EMPLOYEE_LIMIT", null, "Số nhân viên sẽ vượt giới hạn của gói hiện tại."));
            }
            if (issues.isEmpty()) acceptedRows++;
            row.setValid(issues.isEmpty());
            row.setErrors(issues.isEmpty() ? null : write(issues));
        }
    }

    private void validateSource(UUID workspaceId,
                                Map<String, String> source,
                                Map<String, DepartmentEntity> departmentByCode,
                                Map<String, JobPositionEntity> positionByCode,
                                Set<String> emailsInFile,
                                List<RowIssue> issues) {
        String fullName = text(source, "fullName");
        String email = text(source, "email").toLowerCase(Locale.ROOT);
        String departmentCode = normalize(text(source, "departmentCode"));
        String positionCode = normalize(text(source, "positionCode"));

        if (fullName.isBlank()) issues.add(new RowIssue("REQUIRED", "fullName", "Họ tên là bắt buộc."));
        if (!EMAIL.matcher(email).matches()) issues.add(new RowIssue("INVALID_EMAIL", "email", "Email không hợp lệ."));
        if (!email.isBlank() && !emailsInFile.add(email)) issues.add(new RowIssue("DUPLICATE_IN_FILE", "email", "Email bị trùng trong file."));
        if (!email.isBlank() && users.existsByEmailIgnoreCase(email)) issues.add(new RowIssue("EMAIL_EXISTS", "email", "Email đã tồn tại trong hệ thống."));
        if (departmentCode.isBlank()) issues.add(new RowIssue("REQUIRED", "departmentCode", "departmentCode là bắt buộc."));
        if (positionCode.isBlank()) issues.add(new RowIssue("REQUIRED", "positionCode", "positionCode là bắt buộc."));

        DepartmentEntity department = departmentByCode.get(departmentCode);
        JobPositionEntity position = positionByCode.get(positionCode);
        if (!departmentCode.isBlank()) {
            if (department == null) {
                issues.add(new RowIssue("MISSING_DEPARTMENT", "departmentCode", "Phòng ban '" + departmentCode + "' chưa tồn tại."));
            } else if (department.getStatus() != DepartmentStatus.ACTIVE) {
                issues.add(new RowIssue("INACTIVE_DEPARTMENT", "departmentCode", "Phòng ban '" + departmentCode + "' đang ngừng hoạt động."));
            }
        }
        if (!positionCode.isBlank()) {
            if (position == null) {
                issues.add(new RowIssue("MISSING_POSITION", "positionCode", "Vị trí '" + positionCode + "' chưa tồn tại."));
            } else if (position.getStatus() != JobPositionStatus.ACTIVE) {
                issues.add(new RowIssue("INACTIVE_POSITION", "positionCode", "Vị trí '" + positionCode + "' đang ngừng hoạt động."));
            }
        }
        if (department != null && position != null && !department.getId().equals(position.getDepartmentId())) {
            issues.add(new RowIssue("POSITION_DEPARTMENT_MISMATCH", "positionCode", "Vị trí không thuộc phòng ban đã khai báo."));
        }

        enumValue(SeniorityLevel.class, text(source, "seniorityLevel"), "seniorityLevel", issues);
        enumValue(EmploymentType.class, text(source, "employmentType"), "employmentType", issues);
        enumValue(EmployeeLevel.class, text(source, "employeeLevel"), "employeeLevel", issues);
        enumValue(PermissionGroup.class, text(source, "positionPermissionGroup"), "positionPermissionGroup", issues);
        integer(text(source, "yearsOfExperience"), "yearsOfExperience", issues, true);
        integer(text(source, "monthlyWorkingCapacityHours"), "monthlyWorkingCapacityHours", issues, false);
    }

    private CreateEmployeeRequest employeeRequest(UUID workspaceId, Map<String, String> source, List<RowIssue> issues) {
        Map<String, DepartmentEntity> departmentByCode = departmentMap(workspaceId);
        Map<String, JobPositionEntity> positionByCode = positionMap(workspaceId);
        DepartmentEntity department = departmentByCode.get(normalize(text(source, "departmentCode")));
        JobPositionEntity position = positionByCode.get(normalize(text(source, "positionCode")));
        if (department == null || position == null) {
            throw new IllegalArgumentException("Department/position của dòng import chưa tồn tại.");
        }
        return new CreateEmployeeRequest(
                text(source, "fullName"),
                text(source, "email").toLowerCase(Locale.ROOT),
                emptyToNull(text(source, "phone")),
                position.getTitle(),
                enumValue(SeniorityLevel.class, text(source, "seniorityLevel"), "seniorityLevel", issues),
                null,
                integer(text(source, "yearsOfExperience"), "yearsOfExperience", issues, true),
                emptyToNull(text(source, "skills")),
                department.getId(),
                position.getId(),
                null, null, null, null,
                enumValue(EmploymentType.class, text(source, "employmentType"), "employmentType", issues),
                WorkingStatus.WORKING,
                enumValue(EmployeeLevel.class, text(source, "employeeLevel"), "employeeLevel", issues),
                integer(text(source, "monthlyWorkingCapacityHours"), "monthlyWorkingCapacityHours", issues, false),
                emptyToNull(text(source, "mainExpertise")),
                emptyToNull(text(source, "secondaryExpertise"))
        );
    }

    private List<String> createSelectedDepartments(UUID workspaceId,
                                                   List<MissingDepartmentView> missing,
                                                   Set<String> selectedCodes) {
        if (selectedCodes.isEmpty()) return List.of();
        Map<String, MissingDepartmentView> byCode = new HashMap<>();
        missing.forEach(item -> byCode.put(normalize(item.code()), item));
        List<String> created = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();

        for (String code : selectedCodes) {
            MissingDepartmentView candidate = byCode.get(code);
            if (candidate == null) {
                if (departments.findByWorkspaceIdAndCodeIgnoreCase(workspaceId, code).isPresent()) continue;
                throw new IllegalArgumentException("Phòng ban '" + code + "' không nằm trong danh sách thiếu của workbook.");
            }
            if (candidate.conflict()) {
                throw new IllegalArgumentException("Phòng ban '" + code + "' có dữ liệu tên mâu thuẫn giữa các dòng Excel.");
            }
            String name = candidate.name().isBlank() ? code : candidate.name();
            if (departments.existsByWorkspaceIdAndNameIgnoreCase(workspaceId, name)) {
                throw new IllegalArgumentException("Tên phòng ban '" + name + "' đã tồn tại với mã khác.");
            }
            DepartmentEntity department = new DepartmentEntity();
            department.setWorkspaceId(workspaceId);
            department.setCode(code);
            department.setName(name);
            department.setDescription("Tạo tự động từ import nhân viên Excel.");
            department.setStatus(DepartmentStatus.ACTIVE);
            department.setCreatedAt(now);
            department.setUpdatedAt(now);
            departments.save(department);
            created.add(code);
        }
        departments.flush();
        return List.copyOf(created);
    }

    private List<String> createSelectedPositions(UUID workspaceId,
                                                 List<MissingPositionView> missing,
                                                 Set<String> selectedCodes) {
        if (selectedCodes.isEmpty()) return List.of();
        Map<String, MissingPositionView> byCode = new HashMap<>();
        missing.forEach(item -> byCode.put(normalize(item.code()), item));
        Map<String, DepartmentEntity> departmentByCode = departmentMap(workspaceId);
        List<String> created = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();

        for (String code : selectedCodes) {
            MissingPositionView candidate = byCode.get(code);
            if (candidate == null) {
                if (positionMap(workspaceId).containsKey(code)) continue;
                throw new IllegalArgumentException("Vị trí '" + code + "' không nằm trong danh sách thiếu của workbook.");
            }
            if (candidate.conflict()) {
                throw new IllegalArgumentException("Vị trí '" + code + "' có dữ liệu mâu thuẫn giữa các dòng Excel.");
            }
            DepartmentEntity department = departmentByCode.get(normalize(candidate.departmentCode()));
            if (department == null || department.getStatus() != DepartmentStatus.ACTIVE) {
                throw new IllegalArgumentException(
                        "Không thể tạo vị trí '" + code + "' vì phòng ban '" + candidate.departmentCode()
                                + "' chưa tồn tại/hoạt động. Hãy chọn tạo phòng ban trước."
                );
            }
            String name = candidate.name().isBlank() ? code : candidate.name();
            if (positions.existsByWorkspaceIdAndNameIgnoreCase(workspaceId, name)) {
                throw new IllegalArgumentException("Tên vị trí '" + name + "' đã tồn tại với mã khác.");
            }
            JobPositionEntity position = new JobPositionEntity();
            position.setWorkspaceId(workspaceId);
            position.setCode(code);
            position.setName(name);
            position.setPermissionGroup(candidate.permissionGroup() == null ? PermissionGroup.EMPLOYEE : candidate.permissionGroup());
            position.setDepartmentId(department.getId());
            position.setDepartmentName(department.getName());
            position.setDescription("Tạo tự động từ import nhân viên Excel.");
            position.setStatus(JobPositionStatus.ACTIVE);
            position.setCreatedAt(now);
            position.setUpdatedAt(now);
            positions.save(position);
            created.add(code);
        }
        positions.flush();
        return List.copyOf(created);
    }

    private MasterDataSummary masterDataSummary(UUID workspaceId, List<EmployeeImportRowEntity> batchRows) {
        Map<String, DepartmentEntity> existingDepartments = departmentMap(workspaceId);
        Map<String, JobPositionEntity> existingPositions = positionMap(workspaceId);
        Map<String, MissingDepartmentAccumulator> missingDepartments = new LinkedHashMap<>();
        Map<String, MissingPositionAccumulator> missingPositions = new LinkedHashMap<>();

        for (EmployeeImportRowEntity row : batchRows) {
            Map<String, String> source = source(row);
            String departmentCode = normalize(text(source, "departmentCode"));
            String departmentName = text(source, "departmentName").trim();
            String positionCode = normalize(text(source, "positionCode"));
            String positionName = text(source, "positionName").trim();
            PermissionGroup permissionGroup = permissionGroupOrDefault(text(source, "positionPermissionGroup"));

            if (!departmentCode.isBlank() && !existingDepartments.containsKey(departmentCode)) {
                missingDepartments.computeIfAbsent(departmentCode, ignored -> new MissingDepartmentAccumulator(departmentCode))
                        .add(departmentName.isBlank() ? departmentCode : departmentName, row.getRowNumber());
            }
            if (!positionCode.isBlank() && !existingPositions.containsKey(positionCode)) {
                missingPositions.computeIfAbsent(positionCode, ignored -> new MissingPositionAccumulator(positionCode))
                        .add(positionName.isBlank() ? positionCode : positionName,
                                departmentCode, permissionGroup, row.getRowNumber());
            }
        }

        List<MissingDepartmentView> departmentViews = missingDepartments.values().stream()
                .map(MissingDepartmentAccumulator::view).toList();
        List<MissingPositionView> positionViews = missingPositions.values().stream()
                .map(MissingPositionAccumulator::view).toList();
        return new MasterDataSummary(departmentViews, positionViews);
    }

    private Map<String, DepartmentEntity> departmentMap(UUID workspaceId) {
        Map<String, DepartmentEntity> result = new HashMap<>();
        departments.findByWorkspaceIdOrderByNameAsc(workspaceId)
                .forEach(item -> result.put(normalize(item.getCode()), item));
        return result;
    }

    private Map<String, JobPositionEntity> positionMap(UUID workspaceId) {
        Map<String, JobPositionEntity> result = new HashMap<>();
        positions.findByWorkspaceIdOrderByNameAsc(workspaceId)
                .forEach(item -> result.put(normalize(item.getCode()), item));
        return result;
    }

    private Map<String, String> source(EmployeeImportRowEntity row) {
        try {
            Map<String, Object> raw = objectMapper.readValue(row.getRawData(), new TypeReference<LinkedHashMap<String, Object>>() {});
            Map<String, String> result = new LinkedHashMap<>();
            raw.forEach((key, value) -> result.put(key, value == null ? "" : String.valueOf(value)));
            return result;
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể đọc dữ liệu batch import tại dòng " + row.getRowNumber() + ".", exception);
        }
    }

    private List<RowIssue> rowIssues(EmployeeImportRowEntity row) {
        if (row.getErrors() == null || row.getErrors().isBlank()) return List.of();
        try {
            return objectMapper.readValue(row.getErrors(), new TypeReference<List<RowIssue>>() {});
        } catch (Exception ignored) {
            return Arrays.stream(row.getErrors().split(";"))
                    .map(String::trim).filter(value -> !value.isBlank())
                    .map(value -> new RowIssue("VALIDATION_ERROR", null, value)).toList();
        }
    }

    private String humanErrors(String encoded) {
        if (encoded == null || encoded.isBlank()) return "";
        try {
            List<RowIssue> parsed = objectMapper.readValue(encoded, new TypeReference<List<RowIssue>>() {});
            return parsed.stream().map(RowIssue::message).reduce((a, b) -> a + "; " + b).orElse("");
        } catch (Exception ignored) {
            return encoded;
        }
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, String value, String field, List<RowIssue> issues) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            issues.add(new RowIssue("INVALID_ENUM", field, field + " có giá trị không hợp lệ."));
            return null;
        }
    }

    private PermissionGroup permissionGroupOrDefault(String value) {
        if (value == null || value.isBlank()) return PermissionGroup.EMPLOYEE;
        try {
            return PermissionGroup.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return PermissionGroup.EMPLOYEE;
        }
    }

    private Integer integer(String value, String field, List<RowIssue> issues, boolean allowZero) {
        if (value == null || value.isBlank()) return null;
        try {
            int parsed = Integer.parseInt(value.trim().replace(".0", ""));
            if (parsed < 0 || (!allowZero && parsed == 0)) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException exception) {
            issues.add(new RowIssue("INVALID_NUMBER", field,
                    field + (allowZero ? " phải là số nguyên không âm." : " phải là số nguyên lớn hơn 0.")));
            return null;
        }
    }

    private Map<String, Integer> columns(Row header) {
        Map<String, Integer> result = new HashMap<>();
        DataFormatter formatter = new DataFormatter();
        for (Cell cell : header) {
            String name = formatter.formatCellValue(cell).trim();
            if (!name.isBlank()) result.put(name, cell.getColumnIndex());
        }
        return result;
    }

    private boolean rowEmpty(Row row, DataFormatter formatter) {
        for (Cell cell : row) if (!formatter.formatCellValue(cell).isBlank()) return false;
        return true;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn file XLSX.");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("File XLSX không được vượt quá 10 MB.");
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".xlsx")) throw new IllegalArgumentException("Chỉ chấp nhận file .xlsx.");
    }

    private EmployeeImportBatchEntity requireBatch(UUID id) {
        return batches.findByIdAndWorkspaceId(id, workspaceId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy batch import nhân viên."));
    }

    private void updateBatchCounts(EmployeeImportBatchEntity batch, List<EmployeeImportRowEntity> batchRows) {
        batch.setTotalRows(batchRows.size());
        batch.setValidRows((int) batchRows.stream().filter(EmployeeImportRowEntity::isValid).count());
        batch.setInvalidRows(batchRows.size() - batch.getValidRows());
    }

    private UUID workspaceId() {
        UUID workspaceId = securityContext.currentUser().workspaceId();
        if (workspaceId == null) throw new IllegalArgumentException("Tài khoản hiện tại không thuộc workspace.");
        return workspaceId;
    }

    private Set<String> normalizeCodes(List<String> values) {
        if (values == null) return Set.of();
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalize(value);
            if (!normalized.isBlank()) result.add(normalized);
        }
        return result;
    }

    private String text(Map<String, String> source, String key) {
        return source.getOrDefault(key, "") == null ? "" : source.getOrDefault(key, "").trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String safeFileName(String value) {
        return value == null ? "employees.xlsx" : value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String safeError(Exception exception) {
        return exception.getMessage() == null ? "business validation failed" : exception.getMessage();
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private BatchSummary summary(EmployeeImportBatchEntity batch) {
        return new BatchSummary(
                batch.getId(), batch.getFileName(), batch.getStatus(),
                batch.getTotalRows(), batch.getValidRows(), batch.getInvalidRows(), batch.getImportedRows(),
                batch.getCreatedAt(), batch.getConfirmedAt()
        );
    }

    private BatchView view(EmployeeImportBatchEntity batch,
                           List<EmployeeImportRowEntity> batchRows,
                           List<String> createdDepartmentCodes,
                           List<String> createdPositionCodes) {
        MasterDataSummary missing = masterDataSummary(batch.getWorkspaceId(), batchRows);
        List<RowView> rowViews = batchRows.stream().map(row -> new RowView(
                row.getId(), row.getRowNumber(), row.isValid(), rowIssues(row),
                row.isImported(), row.getImportedUserId(), source(row)
        )).toList();
        return new BatchView(
                batch.getId(),
                summary(batch),
                rowViews,
                missing.departments(),
                missing.positions(),
                !missing.departments().isEmpty() || !missing.positions().isEmpty(),
                createdDepartmentCodes == null ? List.of() : List.copyOf(createdDepartmentCodes),
                createdPositionCodes == null ? List.of() : List.copyOf(createdPositionCodes)
        );
    }

    public record ConfirmRequest(List<String> createDepartmentCodes, List<String> createPositionCodes) {}
    public record RowIssue(String code, String field, String message) {}
    public record MissingDepartmentView(String code, String name, List<Integer> rowNumbers, boolean conflict) {}
    public record MissingPositionView(String code, String name, String departmentCode,
                                      PermissionGroup permissionGroup, List<Integer> rowNumbers, boolean conflict) {}
    public record BatchSummary(UUID id, String fileName, String status, int totalRows, int validRows,
                               int invalidRows, int importedRows, OffsetDateTime createdAt, OffsetDateTime confirmedAt) {}
    public record RowView(UUID id, int rowNumber, boolean valid, List<RowIssue> errors,
                          boolean imported, UUID importedUserId, Map<String, String> data) {}
    public record BatchView(UUID batchId, BatchSummary batch, List<RowView> rows,
                            List<MissingDepartmentView> missingDepartments,
                            List<MissingPositionView> missingPositions,
                            boolean requiresMasterDataAction,
                            List<String> createdDepartmentCodes,
                            List<String> createdPositionCodes) {}
    public record FilePayload(String fileName, String contentType, byte[] content) {}
    private record MasterDataSummary(List<MissingDepartmentView> departments, List<MissingPositionView> positions) {}

    private static final class MissingDepartmentAccumulator {
        private final String code;
        private final LinkedHashSet<String> names = new LinkedHashSet<>();
        private final List<Integer> rows = new ArrayList<>();

        private MissingDepartmentAccumulator(String code) { this.code = code; }
        private void add(String name, int row) {
            if (name != null && !name.isBlank()) names.add(name.trim());
            rows.add(row);
        }
        private MissingDepartmentView view() {
            String name = names.isEmpty() ? code : names.iterator().next();
            return new MissingDepartmentView(code, name, List.copyOf(rows), names.size() > 1);
        }
    }

    private static final class MissingPositionAccumulator {
        private final String code;
        private final LinkedHashSet<String> names = new LinkedHashSet<>();
        private final LinkedHashSet<String> departmentCodes = new LinkedHashSet<>();
        private final LinkedHashSet<PermissionGroup> permissionGroups = new LinkedHashSet<>();
        private final List<Integer> rows = new ArrayList<>();

        private MissingPositionAccumulator(String code) { this.code = code; }
        private void add(String name, String departmentCode, PermissionGroup permissionGroup, int row) {
            if (name != null && !name.isBlank()) names.add(name.trim());
            if (departmentCode != null && !departmentCode.isBlank()) departmentCodes.add(departmentCode.trim().toUpperCase(Locale.ROOT));
            if (permissionGroup != null) permissionGroups.add(permissionGroup);
            rows.add(row);
        }
        private MissingPositionView view() {
            String name = names.isEmpty() ? code : names.iterator().next();
            String departmentCode = departmentCodes.isEmpty() ? "" : departmentCodes.iterator().next();
            PermissionGroup permissionGroup = permissionGroups.isEmpty() ? PermissionGroup.EMPLOYEE : permissionGroups.iterator().next();
            boolean conflict = names.size() > 1 || departmentCodes.size() > 1 || permissionGroups.size() > 1;
            return new MissingPositionView(code, name, departmentCode, permissionGroup, List.copyOf(rows), conflict);
        }
    }
}
