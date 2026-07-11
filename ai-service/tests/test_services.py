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
    DailyReportInsightsRequest,
    EmployeeWorkload,
    MonthlyWorkloadDetail,
    MissingReportEmployee,
    MissingReportsRequest,
    RecommendAssigneeRequest,
    RecommendationExplanationRequest,
    RecommendationTaskContext,
    RankedCandidate,
    TaskDescriptionAnalysisRequest,
    WorkloadRiskRequest,
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
        services._PROVIDER_COOLDOWNS.clear()
        with patch("app.services._call_gemini", side_effect=RuntimeError("gemini down")), \
                patch("app.services._call_groq", return_value='{"summary":"ok"}'):
            output = services._ask_llm_json("Return schema {\"summary\":\"string\"}", {}, "TEST")

        self.assertEqual(output, {"summary": "ok"})

    def test_provider_failure_returns_structured_provider_error(self):
        services._PROVIDER_COOLDOWNS.clear()
        with patch("app.services._call_gemini", side_effect=RuntimeError("gemini down")), \
                patch("app.services._call_groq", side_effect=RuntimeError("groq down")):
            with self.assertRaises(services.AiProviderError) as raised:
                services._ask_llm_json("Return JSON", {}, "TEST")

        self.assertEqual(raised.exception.code, "AI_PROVIDERS_UNAVAILABLE")
        self.assertEqual(raised.exception.http_status, 503)
        self.assertEqual(len(raised.exception.provider_errors), 2)

    def test_provider_failure_gemini_quota_groq_forbidden_is_structured(self):
        services._PROVIDER_COOLDOWNS.clear()
        with patch("app.services._call_gemini", side_effect=services.ProviderQuotaExceeded(
            "GEMINI",
            "gemini-2.5-flash",
            "quota",
            status_code=429,
            provider_status="RESOURCE_EXHAUSTED",
            retry_after_seconds=7,
        )), patch("app.services._call_groq", side_effect=services.ProviderForbidden(
            "GROQ",
            "llama-3.3-70b-versatile",
            "forbidden",
            status_code=403,
            provider_error_code="1010",
        )):
            with self.assertRaises(services.AiProviderError) as raised:
                services._ask_llm_json("Return JSON", {}, "TEST")

        self.assertEqual(raised.exception.code, "AI_PROVIDERS_UNAVAILABLE")
        self.assertEqual(raised.exception.http_status, 503)
        self.assertEqual(raised.exception.retry_after_seconds, 7)

    def test_daily_report_insights_reuses_cache(self):
        services._DAILY_REPORT_INSIGHT_CACHE.clear()
        services._DAILY_REPORT_INSIGHT_IN_FLIGHT.clear()
        payload = DailyReportInsightsRequest(reports=[])
        with patch("app.services._ask_llm_json", return_value={
            "summary": "ok",
            "blockers": [],
            "actionSuggestions": [],
        }) as ask:
            first = services.daily_report_insights(payload)
            second = services.daily_report_insights(payload)

        self.assertEqual(first.summary, "ok")
        self.assertEqual(second.summary, "ok")
        self.assertEqual(ask.call_count, 1)

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

    def test_task_analysis_uses_only_available_catalogs(self):
        payload = TaskDescriptionAnalysisRequest(
            workspaceId="ws1",
            taskTitle="Build CRM export",
            taskDescription="Export customer list",
            availableTaskTypes=["BACKEND"],
            availableJobPositions=["Backend Engineer"],
            availableSkills=["Java"],
            availableDepartments=["Engineering"],
            deadline=None,
        )

        with patch("app.services._ask_llm_json", return_value={
            "taskType": "MADE_UP",
            "taskDomain": "CRM",
            "suggestedDifficulty": "MEDIUM",
            "suggestedEmployeeLevel": "MIDDLE",
            "requiredSkills": ["Java", "FakeSkill"],
            "requiredJobPositions": ["Backend Engineer", "Fake Role"],
            "relatedDepartment": "Unknown Department",
            "estimatedWorkingHoursSuggestion": {"value": 16, "reason": "Two days", "confidence": 2},
            "missingInformation": [],
            "clarifyingQuestions": [],
            "summary": "Analyze task.",
        }):
            result = services.analyze_task_description(payload)

        self.assertEqual(result.task_type, "UNKNOWN")
        self.assertEqual(result.required_skills, ["Java"])
        self.assertEqual(result.required_job_positions, ["Backend Engineer"])
        self.assertEqual(result.related_department, "UNKNOWN")
        self.assertIn("deadline", result.missing_information)
        self.assertEqual(result.estimated_working_hours_suggestion.confidence, 1.0)

    def test_individual_explanation_preserves_backend_rank_and_numbers(self):
        payload = RecommendationExplanationRequest(
            workspaceId="ws1",
            recommendationType="INDIVIDUAL",
            task=RecommendationTaskContext(title="Task", requiredSkills=["Java"]),
            candidates=[
                RankedCandidate(
                    rank=1,
                    employeeId="e1",
                    fullName="An",
                    skillMatchScore=90,
                    roleSuitabilityScore=80,
                    jobPositionSuitabilityScore=70,
                    similarTaskCount=0,
                    completionRate=95,
                    overdueRate=5,
                    currentMonthlyHours=80,
                    monthlyCapacityHours=168,
                    finalRankingScore=88,
                )
            ],
        )

        with patch("app.services._ask_llm_json", return_value={
            "recommendationType": "INDIVIDUAL",
            "taskSummary": "Task summary",
            "rankedCandidates": [{
                "rank": 99,
                "employeeId": "e1",
                "fullName": "An",
                "recommendationLabel": "HIGHLY_RECOMMENDED",
                "summaryReason": "Phù hợp.",
                "strengths": ["Có kinh nghiệm tương tự"],
                "risks": [],
                "numbers": {
                    "finalRankingScore": 1,
                    "skillMatchScore": 1,
                    "roleSuitabilityScore": 1,
                    "similarTaskCount": 99,
                    "completionRate": 1,
                    "overdueRate": 1,
                    "currentMonthlyHours": 1,
                    "monthlyCapacityHours": 1,
                },
            }],
            "finalNote": "Manager quyết định.",
        }):
            result = services.explain_individual_recommendation(payload)

        self.assertEqual(result.ranked_candidates[0].rank, 1)
        self.assertEqual(result.ranked_candidates[0].numbers.final_ranking_score, 88)
        self.assertEqual(result.ranked_candidates[0].numbers.similar_task_count, 0)
        self.assertEqual(result.ranked_candidates[0].strengths, [])

    def test_workload_risk_uses_backend_numbers_and_overload_high(self):
        payload = WorkloadRiskRequest(
            workspaceId="ws1",
            employeeName="An",
            monthlyCapacityHours=168,
            monthlyWorkloadEvaluation=[
                MonthlyWorkloadDetail(
                    month="2026-07",
                    existingHours=150,
                    newTaskHours=50,
                    totalHoursAfterAssignment=200,
                    usagePercentage=119.05,
                    workloadStatus="Quá tải",
                )
            ],
        )

        with patch("app.services._ask_llm_json", return_value={
            "overallRisk": "LOW",
            "monthlyWarnings": [{
                "month": "2026-07",
                "status": "Rảnh rỗi",
                "message": "Rủi ro cao.",
                "numbers": {
                    "existingHours": 0,
                    "newTaskHours": 0,
                    "totalHours": 0,
                    "capacityHours": 0,
                    "usagePercentage": 0,
                },
            }],
            "recommendation": "Cân nhắc giảm tải.",
        }):
            result = services.explain_workload_risk(payload)

        self.assertEqual(result.overall_risk, "HIGH")
        self.assertEqual(result.monthly_warnings[0].status, "Quá tải")
        self.assertEqual(result.monthly_warnings[0].numbers.total_hours, 200)


if __name__ == "__main__":
    unittest.main()
