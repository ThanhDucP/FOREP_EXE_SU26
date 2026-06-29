import sys
import unittest
from pathlib import Path
from unittest.mock import patch


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from app import services
from app.schemas import (
    ActionSuggestionsRequest,
    BusinessSummaryReport,
    BusinessSummaryTask,
    EmployeeWorkload,
    MissingReportEmployee,
    MissingReportsRequest,
    RecommendAssigneeRequest,
)


def employee(employee_id: str, name: str, level: str, overdue: int = 0, status: str = "ACTIVE", score: int = 80):
    return EmployeeWorkload(
        employeeId=employee_id,
        fullName=name,
        openTasks=1,
        overdueTasks=overdue,
        blockedTasks=0,
        estimatedWorkload=4,
        workloadLevel=level,
        status=status,
        candidateScore=score,
        scoreComponents={"candidateScore": score},
    )


class AiServiceTests(unittest.TestCase):
    def test_recommend_assignee_empty_employees(self):
        payload = RecommendAssigneeRequest(
            title="Task",
            requirements="Lam viec",
            deadline="2026-07-01T10:00:00Z",
            estimatedHours=2,
            employees=[],
        )

        self.assertEqual(services.recommend_assignee(payload), [])

    def test_recommend_assignee_rejects_hallucinated_employee(self):
        payload = RecommendAssigneeRequest(
            title="Task",
            requirements="Lam viec",
            deadline="2026-07-01T10:00:00Z",
            estimatedHours=2,
            employees=[employee("e1", "An", "LOW")],
        )

        with patch("app.services._ask_llm_json", return_value={
            "recommendations": [{
                "employeeId": "fake",
                "fullName": "Nguoi la",
                "score": 99,
                "workloadLevel": "LOW",
                "reason": "Hop ly",
                "risk": "Khong co rui ro lon",
            }]
        }):
            with self.assertRaises(services.AiProviderError) as raised:
                services.recommend_assignee(payload)
        self.assertEqual(raised.exception.code, "AI_SCHEMA_VALIDATION_ERROR")

    def test_recommend_assignee_filters_inactive_overloaded_and_uses_backend_score(self):
        payload = RecommendAssigneeRequest(
            title="Task",
            requirements="Lam viec",
            deadline="2026-07-01T10:00:00Z",
            estimatedHours=2,
            employees=[
                employee("active", "Binh", "LOW", score=91),
                employee("inactive", "Chi", "NO_WORK", status="INACTIVE", score=100),
                employee("overloaded", "Dung", "OVERLOADED", score=95),
            ],
        )

        with patch("app.services._ask_llm_json", return_value={
            "recommendations": [
                {"employeeId": "inactive", "fullName": "Chi", "score": 100, "workloadLevel": "NO_WORK", "reason": "Ranh", "risk": "Khong co"},
                {"employeeId": "overloaded", "fullName": "Dung", "score": 95, "workloadLevel": "OVERLOADED", "reason": "Gan", "risk": "Qua tai"},
                {"employeeId": "active", "fullName": "Binh", "score": 1, "workloadLevel": "HIGH", "reason": "Co 1 task mo", "risk": "Khong co rui ro lon"},
            ]
        }):
            result = services.recommend_assignee(payload)

        self.assertEqual(len(result), 1)
        self.assertEqual(result[0].employee_id, "active")
        self.assertEqual(result[0].score, 91)
        self.assertEqual(result[0].workload_level, "LOW")

    def test_provider_fallback_gemini_fail_groq_success(self):
        with patch("app.services._call_gemini", side_effect=RuntimeError("gemini down")), \
                patch("app.services._call_groq", return_value='{"summary":"ok"}'):
            output = services._ask_llm_json("Return schema {\"summary\":\"string\"}", {}, "TEST")

        self.assertEqual(output, {"summary": "ok"})

    def test_provider_failure_returns_ai_provider_error(self):
        with patch("app.services._call_gemini", side_effect=RuntimeError("gemini down")), \
                patch("app.services._call_groq", side_effect=RuntimeError("groq down")):
            with self.assertRaises(services.AiProviderError) as raised:
                services._ask_llm_json("Return JSON", {}, "TEST")

        self.assertEqual(raised.exception.code, "AI_PROVIDER_ERROR")

    def test_action_suggestion_rejects_unknown_target_and_clamps_confidence(self):
        payload = ActionSuggestionsRequest(
            tasks=[BusinessSummaryTask(
                taskId="task-1",
                title="Task",
                assigneeName="An",
                priority="HIGH",
                status="BLOCKED",
                deadline="2026-07-01T10:00:00Z",
                progressPercent=20,
                estimatedHours=2,
                overdue=False,
            )],
            reports=[],
            workload=[],
        )

        with patch("app.services._ask_llm_json", return_value={
            "suggestions": [
                {"actionType": "FOLLOW_UP_TASK", "targetEntityType": "TASK", "targetEntityId": "task-1", "title": "Hoi tien do", "reason": "Task dang rui ro", "confidence": 5},
                {"actionType": "FOLLOW_UP_TASK", "targetEntityType": "TASK", "targetEntityId": "fake", "title": "Sai target", "reason": "Bia id", "confidence": 0.9},
            ]
        }):
            result = services.action_suggestions(payload)

        self.assertEqual(len(result.suggestions), 1)
        self.assertEqual(result.suggestions[0].target_entity_id, "task-1")
        self.assertEqual(result.suggestions[0].confidence, 1.0)

    def test_missing_reports_uses_backend_candidates_only(self):
        payload = MissingReportsRequest(
            reportDate="2026-06-29",
            employees=[MissingReportEmployee(employeeId="e1", fullName="An", status="ACTIVE")],
            reports=[],
        )

        with patch("app.services._ask_llm_json", return_value={
            "missingReports": [
                {"employeeId": "e1", "employeeName": "An", "reportDate": "2026-06-29", "daysMissing": 1, "recommendedAction": "Nhac An gui report.", "confidence": 0.4},
                {"employeeId": "fake", "employeeName": "La", "reportDate": "2026-06-29", "daysMissing": 1, "recommendedAction": "Nhac.", "confidence": 1},
            ]
        }):
            result = services.missing_reports(payload)

        self.assertEqual(len(result.missing_reports), 1)
        self.assertEqual(result.missing_reports[0].employee_id, "e1")
        self.assertEqual(result.missing_reports[0].confidence, 1.0)


if __name__ == "__main__":
    unittest.main()
