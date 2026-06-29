# Hợp Đồng API Và Dữ Liệu FOREP EXE

Base path: `/api/v1`  
Mô hình MVP: Workspace + OWNER + EMPLOYEE.

## Response Chuẩn

```json
{
  "data": {},
  "meta": {},
  "errors": []
}
```

## DTO Chính

### WorkspaceDTO

```json
{
  "id": "uuid",
  "name": "Shop ABC",
  "logo": "/files/logo.png",
  "address": "Quận 1, TP. Hồ Chí Minh",
  "ownerId": "uuid",
  "createdAt": "2026-06-24T09:00:00Z"
}
```

### UserDTO

```json
{
  "id": "uuid",
  "workspaceId": "uuid",
  "fullName": "Nguyễn Văn A",
  "email": "a@example.com",
  "phone": "0900000000",
  "role": "EMPLOYEE",
  "avatar": null,
  "status": "ACTIVE",
  "createdAt": "2026-06-24T09:00:00Z",
  "updatedAt": "2026-06-24T09:00:00Z"
}
```

### TaskDTO

```json
{
  "id": "uuid",
  "workspaceId": "uuid",
  "title": "Chuẩn bị hàng khuyến mãi",
  "requirements": "Kiểm đủ số lượng và dán nhãn trước 17:00.",
  "description": "Ưu tiên quầy A.",
  "assigneeId": "uuid",
  "creatorId": "uuid",
  "priority": "HIGH",
  "deadline": "2026-06-25T17:00:00Z",
  "estimatedHours": 3,
  "progressPercent": 20,
  "status": "IN_PROGRESS",
  "createdAt": "2026-06-24T09:00:00Z",
  "updatedAt": "2026-06-24T09:00:00Z",
  "completedAt": null
}
```

### WorkloadDTO

```json
{
  "employeeId": "uuid",
  "fullName": "Trần Thị B",
  "openTasks": 2,
  "inProgressTasks": 1,
  "completedTasks": 8,
  "overdueTasks": 0,
  "estimatedWorkload": 6,
  "workloadLevel": "LOW"
}
```

### AIRecommendAssigneeRequest

```json
{
  "title": "Chuẩn bị hàng khuyến mãi",
  "requirements": "Kiểm đủ số lượng và dán nhãn trước 17:00.",
  "deadline": "2026-06-25T17:00:00Z",
  "estimatedHours": 3
}
```

### AIRecommendAssigneeResponse

```json
{
  "recommendations": [
    {
      "employeeId": "uuid",
      "fullName": "Trần Thị B",
      "score": 88,
      "workloadLevel": "LOW",
      "reason": "Chỉ có 2 task đang mở và không có task quá hạn.",
      "risk": "Không có"
    }
  ]
}
```

## Validation

- Task chính thức bắt buộc có assigneeId, title, deadline, requirements.
- progressPercent từ 0 đến 100.
- Task status chỉ gồm ASSIGNED, IN_PROGRESS, BLOCKED, COMPLETED, CANCELLED.
- Role chỉ gồm OWNER và EMPLOYEE.
- EMPLOYEE chỉ cập nhật task được giao.
- AI không auto-assign.

