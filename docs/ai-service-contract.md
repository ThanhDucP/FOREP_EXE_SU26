# FOREP EXE AI Service Contract

Base path nội bộ: `/internal/ai`

Security:

- Header bắt buộc: `X-Internal-Service-Token`
- Chỉ Backend API Service được gọi.
- Frontend không có `AI_SERVICE_URL` hoặc token.

## POST /internal/ai/recommend-assignee

Request:

```json
{
  "title": "Kiểm hàng tồn",
  "requirements": "Kiểm đủ số lượng trước 17:00",
  "deadline": "2026-06-25T10:00:00Z",
  "estimatedHours": 3,
  "employees": [
    {
      "employeeId": "uuid",
      "fullName": "Nguyễn Văn A",
      "openTasks": 2,
      "overdueTasks": 0,
      "blockedTasks": 0,
      "estimatedWorkload": 6,
      "workloadLevel": "LOW"
    }
  ]
}
```

Response:

```json
{
  "recommendations": [
    {
      "employeeId": "uuid",
      "fullName": "Nguyễn Văn A",
      "score": 85,
      "workloadLevel": "LOW",
      "reason": "Hiện có 2 task đang mở và workload ở mức LOW.",
      "risk": "Không có"
    }
  ]
}
```

## POST /internal/ai/workload-summary

Request:

```json
{
  "employees": []
}
```

Response:

```json
{
  "summary": "Có 1 nhân viên quá tải, 2 nhân viên đang rảnh và 1 nhân viên có task quá hạn.",
  "overloadedEmployees": [],
  "idleEmployees": [],
  "overdueEmployees": []
}
```

## POST /internal/ai/delay-risks

Request:

```json
{
  "tasks": [
    {
      "taskId": "uuid",
      "title": "Gọi khách hàng",
      "assigneeName": "Trần Thị B",
      "deadline": "2026-06-23T10:00:00Z",
      "progressPercent": 20,
      "overdue": true
    }
  ]
}
```

Response:

```json
{
  "risks": [
    {
      "taskId": "uuid",
      "title": "Gọi khách hàng",
      "riskLevel": "HIGH",
      "reason": "Task đã quá hạn.",
      "recommendedAction": "Liên hệ Trần Thị B để cập nhật trạng thái ngay."
    }
  ]
}
```

## POST /internal/ai/daily-summary

Request:

```json
{
  "completedTasks": 10,
  "overdueTasks": 2,
  "overloadedEmployees": 1,
  "idleEmployees": 3
}
```

Response:

```json
{
  "summary": "Hôm nay có 10 task hoàn thành, 2 task quá hạn, 1 nhân viên quá tải và 3 nhân viên đang rảnh."
}
```

