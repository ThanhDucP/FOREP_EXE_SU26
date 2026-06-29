# FOREP EXE AI Service

AI Service là service nội bộ. Frontend không được gọi service này trực tiếp.

Luồng đúng:

```text
Frontend -> Backend API -> AI Service
```

## Chạy local bằng Docker

```bash
docker build -t forep-exe-ai:local .
docker run --rm -p 8000:8000 -e AI_SERVICE_TOKEN=dev-internal-token forep-exe-ai:local
```

## Endpoint

Health:

- GET `/health`

Internal AI:

- POST `/internal/ai/recommend-assignee`
- POST `/internal/ai/workload-summary`
- POST `/internal/ai/delay-risks`
- POST `/internal/ai/daily-summary`
- POST `/internal/ai/voice/extract-tasks`

Mọi internal endpoint yêu cầu header:

```text
X-Internal-Service-Token: <AI_SERVICE_TOKEN>
```

## MVP Behavior

Hiện tại service dùng rule-based logic để ổn định MVP:

- Gợi ý người nhận việc dựa trên open tasks, overdue tasks, blocked tasks và estimated workload.
- Tóm tắt workload dựa trên danh sách nhân viên.
- Phát hiện delay risk dựa trên overdue và progress thấp.
- Daily summary sinh text tiếng Việt.

Sau này có thể thay logic rule-based bằng LLM provider nhưng vẫn giữ response schema như hiện tại.

