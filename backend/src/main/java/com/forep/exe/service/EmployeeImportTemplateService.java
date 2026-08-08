package com.forep.exe.service;

import com.forep.exe.domain.Enums.DepartmentStatus;
import com.forep.exe.domain.Enums.JobPositionStatus;
import com.forep.exe.domain.Enums.Permission;
import com.forep.exe.persistence.DepartmentEntity;
import com.forep.exe.persistence.DepartmentRepository;
import com.forep.exe.persistence.JobPositionEntity;
import com.forep.exe.persistence.JobPositionRepository;
import com.forep.exe.security.AuthorizationService;
import com.forep.exe.security.SecurityContext;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EmployeeImportTemplateService {
    /**
     * Keep these names exactly aligned with EmployeeImportService. The parser
     * identifies columns by header name, so the downloadable template must not
     * translate or decorate the machine-readable headers.
     */
    private static final List<String> HEADERS = List.of(
            "fullName", "email", "phone",
            "departmentCode", "departmentName",
            "positionCode", "positionName", "positionPermissionGroup",
            "skills", "seniorityLevel", "yearsOfExperience", "employmentType", "employeeLevel",
            "monthlyWorkingCapacityHours", "mainExpertise", "secondaryExpertise"
    );

    private final DepartmentRepository departments;
    private final JobPositionRepository positions;
    private final AuthorizationService authorization;
    private final SecurityContext securityContext;

    public EmployeeImportTemplateService(DepartmentRepository departments,
                                         JobPositionRepository positions,
                                         AuthorizationService authorization,
                                         SecurityContext securityContext) {
        this.departments = departments;
        this.positions = positions;
        this.authorization = authorization;
        this.securityContext = securityContext;
    }

    @Transactional(readOnly = true)
    public EmployeeImportService.FilePayload template() {
        authorization.require(Permission.EMPLOYEE_IMPORT);
        UUID workspaceId = securityContext.currentUser().workspaceId();
        if (workspaceId == null) {
            throw new IllegalArgumentException("Tài khoản hiện tại không thuộc workspace.");
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CellStyle requiredHeader = headerStyle(workbook, IndexedColors.LIGHT_ORANGE);
            CellStyle masterHeader = headerStyle(workbook, IndexedColors.LIGHT_CORNFLOWER_BLUE);
            CellStyle optionalHeader = headerStyle(workbook, IndexedColors.GREY_25_PERCENT);
            CellStyle tableHeader = headerStyle(workbook, IndexedColors.LIGHT_BLUE);

            createEmployeesSheet(workbook, requiredHeader, masterHeader, optionalHeader);
            createInstructionsSheet(workbook, tableHeader);
            createExamplesSheet(workbook, requiredHeader, masterHeader, optionalHeader);
            createMasterDataSheet(workbook, workspaceId, tableHeader);

            workbook.setActiveSheet(0);
            workbook.write(output);
            return new EmployeeImportService.FilePayload(
                    "forep-employee-import-template.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray()
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tạo file mẫu import nhân viên.", exception);
        }
    }

    private void createEmployeesSheet(Workbook workbook,
                                      CellStyle requiredHeader,
                                      CellStyle masterHeader,
                                      CellStyle optionalHeader) {
        Sheet sheet = workbook.createSheet("Employees");
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, HEADERS.size() - 1));

        Row header = sheet.createRow(0);
        for (int index = 0; index < HEADERS.size(); index++) {
            Cell cell = header.createCell(index);
            cell.setCellValue(HEADERS.get(index));
            cell.setCellStyle(styleForColumn(index, requiredHeader, masterHeader, optionalHeader));
            sheet.setColumnWidth(index, widthForColumn(index) * 256);
        }

        // Do not put sample employees in the real import sheet. Users can copy
        // examples from the Examples sheet without accidentally importing them.
    }

    private void createInstructionsSheet(Workbook workbook, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Instructions");
        String[] headers = {"Nhóm", "Cột", "Mức độ", "Quy tắc nghiệp vụ", "Giá trị/Ví dụ"};
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        String[][] rows = {
                {"Nhân viên", "fullName", "Bắt buộc", "Họ tên nhân viên.", "Nguyễn Văn An"},
                {"Nhân viên", "email", "Bắt buộc", "Email phải hợp lệ, không trùng trong file và không tồn tại trong hệ thống.", "an@company.vn"},
                {"Nhân viên", "phone", "Tuỳ chọn", "Số điện thoại nhân viên.", "0900000000"},
                {"Phòng ban", "departmentCode", "Bắt buộc", "Mã dùng để đối chiếu phòng ban. Nếu mã chưa tồn tại, FOREP sẽ báo để người dùng chọn tạo.", "ENG"},
                {"Phòng ban", "departmentName", "Bắt buộc khi tạo mới", "Nếu departmentCode chưa tồn tại thì phải điền tên phòng ban nhất quán ở mọi dòng dùng cùng mã.", "Engineering"},
                {"Vị trí", "positionCode", "Bắt buộc", "Mã dùng để đối chiếu vị trí. Nếu mã chưa tồn tại, FOREP sẽ báo để người dùng chọn tạo.", "BE-JAVA"},
                {"Vị trí", "positionName", "Bắt buộc khi tạo mới", "Nếu positionCode chưa tồn tại thì phải điền tên vị trí nhất quán ở mọi dòng dùng cùng mã.", "Java Backend Developer"},
                {"Vị trí", "positionPermissionGroup", "Khuyến nghị khi tạo mới", "Nhóm quyền của position mới. Để trống hệ thống mặc định EMPLOYEE.", "EMPLOYEE / MANAGER / EXECUTIVE"},
                {"Vị trí", "Liên kết Department", "Tự động", "Position mới luôn được tạo thuộc departmentCode của cùng dòng. Nếu Department cũng mới, phải chọn tạo Department trước/cùng lúc.", "BE-JAVA → ENG"},
                {"Master data", "Trạng thái", "Tự động", "Department và Position được tạo từ import sẽ ở trạng thái ACTIVE.", "ACTIVE"},
                {"Master data", "Mô tả", "Tự động", "Mô tả master data được hệ thống gắn nhãn là tạo từ import nhân viên Excel.", "Tạo tự động từ import nhân viên Excel."},
                {"Hồ sơ", "skills", "Tuỳ chọn", "Danh sách kỹ năng chính.", "Java, Spring Boot, PostgreSQL"},
                {"Hồ sơ", "seniorityLevel", "Tuỳ chọn", "Cấp độ chuyên môn.", "INTERN / JUNIOR / MIDDLE / SENIOR / LEAD"},
                {"Hồ sơ", "yearsOfExperience", "Tuỳ chọn", "Số năm kinh nghiệm, số nguyên không âm.", "3"},
                {"Hồ sơ", "employmentType", "Tuỳ chọn", "Loại hình làm việc.", "FULL_TIME / PART_TIME / CONTRACTOR / INTERN"},
                {"Hồ sơ", "employeeLevel", "Tuỳ chọn", "Cấp nhân viên.", "INTERN / FRESHER / JUNIOR / MIDDLE / SENIOR / LEAD / MANAGER"},
                {"Hồ sơ", "monthlyWorkingCapacityHours", "Tuỳ chọn", "Năng lực giờ làm việc theo tháng, phải lớn hơn 0 nếu nhập.", "168"},
                {"Hồ sơ", "mainExpertise", "Tuỳ chọn", "Chuyên môn chính.", "Backend"},
                {"Hồ sơ", "secondaryExpertise", "Tuỳ chọn", "Chuyên môn phụ.", "Database"},
                {"File", "Giới hạn", "Bắt buộc", "Không đổi tên header. Sheet Employees phải là sheet đầu tiên. Tối đa 1000 dòng, file .xlsx tối đa 10 MB.", ""},
                {"File", "Quy trình", "Thông tin", "Upload → preview → tick Department/Position muốn tạo → xác nhận → tạo master data → validate lại → tạo nhân viên.", ""}
        };

        for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
            Row row = sheet.createRow(rowIndex + 1);
            for (int column = 0; column < rows[rowIndex].length; column++) {
                row.createCell(column).setCellValue(rows[rowIndex][column]);
            }
        }
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, rows.length, 0, headers.length - 1));
        sheet.setColumnWidth(0, 20 * 256);
        sheet.setColumnWidth(1, 30 * 256);
        sheet.setColumnWidth(2, 25 * 256);
        sheet.setColumnWidth(3, 95 * 256);
        sheet.setColumnWidth(4, 55 * 256);
    }

    private void createExamplesSheet(Workbook workbook,
                                     CellStyle requiredHeader,
                                     CellStyle masterHeader,
                                     CellStyle optionalHeader) {
        Sheet sheet = workbook.createSheet("Examples");
        Row header = sheet.createRow(0);
        for (int index = 0; index < HEADERS.size(); index++) {
            Cell cell = header.createCell(index);
            cell.setCellValue(HEADERS.get(index));
            cell.setCellStyle(styleForColumn(index, requiredHeader, masterHeader, optionalHeader));
            sheet.setColumnWidth(index, widthForColumn(index) * 256);
        }

        // Example 1: use existing master data. Names may be kept for readability.
        writeExample(sheet.createRow(1), List.of(
                "Nguyen Van An", "an@example.com", "0900000000",
                "ENG", "Engineering",
                "BE-JAVA", "Java Backend Developer", "EMPLOYEE",
                "Java, Spring Boot", "MIDDLE", "3", "FULL_TIME", "MIDDLE",
                "168", "Backend", "PostgreSQL"
        ));

        // Example 2: a new department + new position. After upload the preview
        // will offer both records as selectable master data to create.
        writeExample(sheet.createRow(2), List.of(
                "Tran Thi Binh", "binh@example.com", "0911111111",
                "DATA", "Data & Analytics",
                "DATA-ANL", "Data Analyst", "EMPLOYEE",
                "SQL, Power BI", "JUNIOR", "1", "FULL_TIME", "JUNIOR",
                "168", "Data Analysis", "Reporting"
        ));
        sheet.createFreezePane(0, 1);
    }

    private void createMasterDataSheet(Workbook workbook, UUID workspaceId, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("MasterData");
        String[] headers = {
                "departmentCode", "departmentName", "departmentStatus",
                "positionCode", "positionName", "positionDepartmentCode", "positionDepartmentName",
                "positionPermissionGroup", "positionStatus"
        };
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 28 * 256);
        }

        List<DepartmentEntity> departmentList = departments.findByWorkspaceIdOrderByNameAsc(workspaceId);
        Map<UUID, DepartmentEntity> departmentById = new HashMap<>();
        for (DepartmentEntity department : departmentList) {
            departmentById.put(department.getId(), department);
        }
        List<JobPositionEntity> positionList = positions.findByWorkspaceIdOrderByNameAsc(workspaceId);

        int max = Math.max(departmentList.size(), positionList.size());
        for (int index = 0; index < max; index++) {
            Row row = sheet.createRow(index + 1);
            if (index < departmentList.size()) {
                DepartmentEntity department = departmentList.get(index);
                row.createCell(0).setCellValue(nullSafe(department.getCode()));
                row.createCell(1).setCellValue(nullSafe(department.getName()));
                row.createCell(2).setCellValue(department.getStatus() == null ? DepartmentStatus.ACTIVE.name() : department.getStatus().name());
            }
            if (index < positionList.size()) {
                JobPositionEntity position = positionList.get(index);
                DepartmentEntity department = departmentById.get(position.getDepartmentId());
                row.createCell(3).setCellValue(nullSafe(position.getCode()));
                row.createCell(4).setCellValue(nullSafe(position.getTitle()));
                row.createCell(5).setCellValue(department == null ? "" : nullSafe(department.getCode()));
                row.createCell(6).setCellValue(department == null ? nullSafe(position.getDepartmentName()) : nullSafe(department.getName()));
                row.createCell(7).setCellValue(position.getPermissionGroup() == null ? "EMPLOYEE" : position.getPermissionGroup().name());
                row.createCell(8).setCellValue(position.getStatus() == null ? JobPositionStatus.ACTIVE.name() : position.getStatus().name());
            }
        }
        sheet.createFreezePane(0, 1);
        if (max > 0) {
            sheet.setAutoFilter(new CellRangeAddress(0, max, 0, headers.length - 1));
        }
    }

    private CellStyle headerStyle(Workbook workbook, IndexedColors fillColor) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(fillColor.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setHorizontalAlignment(HorizontalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle styleForColumn(int index,
                                     CellStyle requiredHeader,
                                     CellStyle masterHeader,
                                     CellStyle optionalHeader) {
        if (index <= 1) return requiredHeader;
        if (index >= 3 && index <= 7) return masterHeader;
        return optionalHeader;
    }

    private int widthForColumn(int index) {
        if (index == 0 || index == 4 || index == 6) return 30;
        if (index == 1 || index == 8 || index == 14 || index == 15) return 34;
        return 24;
    }

    private void writeExample(Row row, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            row.createCell(index).setCellValue(values.get(index));
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
