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
  "shortCode": "SE",
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
  "username": "anvnse0001",
  "employeeCode": "SE0001",
  "initialPassword": "SE0001",
  "role": "EMPLOYEE",
  "avatar": null,
  "status": "ACTIVE",
  "jobTitle": "Frontend Developer",
  "seniorityLevel": "MIDDLE",
  "skillRating": 4,
  "yearsOfExperience": 3,
  "skills": "React, TypeScript, UI",
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

### AIBusinessSummaryResponse

Dung cho `/ai/business-summary/daily`, `/weekly`, `/monthly`.

```json
{
  "periodType": "WEEKLY",
  "periodStart": "2026-06-23",
  "periodEnd": "2026-06-29",
  "summary": "Tóm tắt tình hình trong kỳ.",
  "highlights": [],
  "risks": [],
  "actionSuggestions": []
}
```

### AIDailyReportInsightsResponse

```json
{
  "summary": "Tóm tắt daily reports gần đây.",
  "blockers": [
    {
      "severity": "HIGH",
      "description": "Thiếu dữ liệu đầu vào."
    }
  ],
  "actionSuggestions": []
}
```

### AIExtractTasksRequest

```json
{
  "text": "Nội dung mô tả hoặc biên bản",
  "defaultDeadline": "2026-07-01T17:00:00Z"
}
```

### AIExtractTasksResponse

```json
{
  "tasks": [
    {
      "title": "Chuẩn bị báo giá",
      "requirements": "Hoàn thành bảng báo giá và gửi owner review.",
      "description": null,
      "priority": "MEDIUM",
      "estimatedHours": 2,
      "suggestedAssigneeId": null,
      "deadlineSuggestion": "2026-07-01T17:00:00Z",
      "confidence": 0.86,
      "missingInformation": []
    }
  ]
}
```

### AITaskSplitResponse

```json
{
  "parentTaskId": "uuid",
  "subtasks": [
    {
      "title": "Kiểm tra dữ liệu đầu vào",
      "requirements": "Xác nhận file và số liệu cần xử lý.",
      "estimatedHours": 1,
      "suggestedOrder": 1,
      "dependencyNote": null,
      "confidence": 0.82
    }
  ]
}
```

### AITaskAdjustmentResponse

```json
{
  "taskId": "uuid",
  "suggestions": [
    {
      "actionType": "CHANGE_PRIORITY",
      "targetEntityId": "uuid",
      "suggestedDeadline": null,
      "suggestedPriority": "HIGH",
      "reason": "Task đang quá hạn và tiến độ thấp nên cần nâng priority.",
      "riskIfIgnored": "Rủi ro trễ hạn cao nếu không follow-up ngay.",
      "confidence": 0.88
    }
  ]
}
```

### AIMissingReportsResponse

```json
{
  "missingReports": [
    {
      "employeeId": "uuid",
      "employeeName": "Trần Thị B",
      "reportDate": "2026-06-29",
      "daysMissing": 1,
      "recommendedAction": "Nhắc nhân viên gửi daily report hôm nay.",
      "confidence": 1.0
    }
  ]
}
```

### AIActionSuggestionsResponse

```json
{
  "suggestions": [
    {
      "actionType": "FOLLOW_UP_TASK",
      "targetEntityType": "TASK",
      "targetEntityId": "uuid",
      "title": "Follow-up task quá hạn",
      "reason": "Task quá hạn và tiến độ còn thấp.",
      "confidence": 0.9
    }
  ]
}
```

### AIFallbackMetadata

Ap dung cho `/ai/workload-summary`, `/ai/delay-risks`, `/ai/action-suggestions`, `/ai/daily-reports/insights`, `/ai/daily-reports/missing` khi LLM/provider fail nhung backend tao du lieu rule-based de dashboard khong chet card.

```json
{
  "source": "RULE_BASED_FALLBACK",
  "aiProviderFailed": true,
  "fallbackReason": "Gemini and Groq both failed"
}
```

### AIRateLimitError

Dung khi backend dang co qua nhieu AI call dang chay. Rieng `/ai/recommend-assignee`, `/ai/workload-summary`, `/ai/delay-risks`, `/ai/action-suggestions`, `/ai/daily-reports/insights`, `/ai/daily-reports/missing` se uu tien fallback rule-based thay vi tra loi nay.

```json
{
  "data": null,
  "meta": {},
  "errors": [
    {
      "code": "AI_RATE_LIMITED",
      "message": "AI dang xu ly qua nhieu yeu cau. Vui long thu lai sau 15 giay.",
      "field": null
    }
  ]
}
```

### AIProviderError

```json
{
  "data": null,
  "meta": {},
  "errors": [
    {
      "code": "AI_PROVIDER_ERROR",
      "message": "Không thể tạo phân tích AI ở thời điểm này. Vui lòng thử lại sau.",
      "field": null
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
- AI không được tự bịa employee/task/report ngoài input.
- AI recommendation input rỗng phải trả list rỗng hợp lệ.
- AI output không markdown, không thêm field ngoài schema.

