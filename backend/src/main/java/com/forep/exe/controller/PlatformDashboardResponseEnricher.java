package com.forep.exe.controller;

import com.forep.exe.domain.Enums.PaymentTransactionStatus;
import com.forep.exe.dto.ApiResponse;
import com.forep.exe.persistence.PaymentTransactionEntity;
import com.forep.exe.persistence.PaymentTransactionRepository;
import com.forep.exe.persistence.SubscriptionPlanEntity;
import com.forep.exe.persistence.SubscriptionPlanRepository;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestControllerAdvice(assignableTypes = AdminPlatformController.class)
public class PlatformDashboardResponseEnricher implements ResponseBodyAdvice<Object> {
    private static final String PLATFORM_OVERVIEW_PATH = "/api/admin/dashboard/overview";
    private static final String REVENUE_MONTHLY_PATH = "/api/admin/dashboard/revenue/monthly";
    private static final String REVENUE_QUARTERLY_PATH = "/api/admin/dashboard/revenue/quarterly";
    private static final String REVENUE_YEARLY_PATH = "/api/admin/dashboard/revenue/yearly";
    private static final String REVENUE_BY_PLAN_PATH = "/api/admin/dashboard/revenue/by-plan";

    private final PaymentTransactionRepository paymentTransactions;
    private final SubscriptionPlanRepository subscriptionPlans;

    public PlatformDashboardResponseEnricher(PaymentTransactionRepository paymentTransactions,
                                             SubscriptionPlanRepository subscriptionPlans) {
        this.paymentTransactions = paymentTransactions;
        this.subscriptionPlans = subscriptionPlans;
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        String path = request.getURI().getPath();

        if (matches(path, PLATFORM_OVERVIEW_PATH)) {
            return enrichOverview(body);
        }
        if (matches(path, REVENUE_MONTHLY_PATH)) {
            return ApiResponse.ok(seriesPayload("Doanh thu theo tháng", monthlySeries()));
        }
        if (matches(path, REVENUE_QUARTERLY_PATH)) {
            return ApiResponse.ok(seriesPayload("Doanh thu theo quý", quarterlySeries()));
        }
        if (matches(path, REVENUE_YEARLY_PATH)) {
            return ApiResponse.ok(seriesPayload("Doanh thu theo năm", yearlySeries()));
        }
        if (matches(path, REVENUE_BY_PLAN_PATH)) {
            return ApiResponse.ok(seriesPayload("Doanh thu theo gói", byPlanSeries()));
        }
        return body;
    }

    private Object enrichOverview(Object body) {
        if (!(body instanceof ApiResponse<?> apiResponse)) {
            return body;
        }
        if (!(apiResponse.data() instanceof Map<?, ?> existingData)) {
            return body;
        }

        Map<String, Object> enriched = new LinkedHashMap<>();
        existingData.forEach((key, value) -> {
            if (key instanceof String stringKey) {
                enriched.put(stringKey, value);
            }
        });

        putAliasIfMissing(enriched, "newWorkspaces", existingData.get("newWorkspacesThisMonth"));
        putAliasIfMissing(enriched, "feedbackAverage", existingData.get("businessFeedbackRatingAverage"));

        Object aiUsageStatistics = existingData.get("aiUsageStatistics");
        if (!enriched.containsKey("aiUsage") && aiUsageStatistics instanceof Map<?, ?> aiMap) {
            Object totalHistoryCalls = aiMap.get("totalHistoryCalls");
            if (totalHistoryCalls instanceof Number number) {
                enriched.put("aiUsage", number.longValue());
            }
        }

        List<PaymentTransactionEntity> successful = successfulPayments();
        BigDecimal totalRevenue = sumAmounts(successful);
        BigDecimal monthRevenue = sumForMonth(successful, YearMonth.now());
        int currentQuarter = quarterOf(OffsetDateTime.now());
        int currentYear = OffsetDateTime.now().getYear();
        BigDecimal quarterRevenue = sumForQuarter(successful, currentYear, currentQuarter);
        BigDecimal yearRevenue = sumForYear(successful, currentYear);

        // Override the old dashboard helpers with values computed directly from real paid rows.
        enriched.put("totalRevenue", totalRevenue);
        enriched.put("revenue", totalRevenue);
        enriched.put("revenueThisMonth", monthRevenue);
        enriched.put("revenueThisQuarter", quarterRevenue);
        enriched.put("revenueThisYear", yearRevenue);
        enriched.put("currency", "VND");
        enriched.put("successfulPayments", successful.size());

        return ApiResponse.ok(enriched);
    }

    private Map<String, Object> seriesPayload(String title, List<Map<String, Object>> series) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("series", series);
        payload.put("currency", "VND");
        payload.put("totalRevenue", series.stream()
                .map(point -> point.get("value"))
                .filter(BigDecimal.class::isInstance)
                .map(BigDecimal.class::cast)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return payload;
    }

    private List<Map<String, Object>> monthlySeries() {
        List<PaymentTransactionEntity> successful = successfulPayments();
        YearMonth current = YearMonth.now();
        List<Map<String, Object>> series = new ArrayList<>();
        for (int offset = 11; offset >= 0; offset--) {
            YearMonth period = current.minusMonths(offset);
            series.add(point(String.format("%02d/%d", period.getMonthValue(), period.getYear()),
                    sumForMonth(successful, period)));
        }
        return series;
    }

    private List<Map<String, Object>> quarterlySeries() {
        List<PaymentTransactionEntity> successful = successfulPayments();
        OffsetDateTime now = OffsetDateTime.now();
        int currentIndex = now.getYear() * 4 + quarterOf(now) - 1;
        List<Map<String, Object>> series = new ArrayList<>();
        for (int offset = 7; offset >= 0; offset--) {
            int index = currentIndex - offset;
            int year = Math.floorDiv(index, 4);
            int quarter = Math.floorMod(index, 4) + 1;
            series.add(point("Q" + quarter + "/" + year, sumForQuarter(successful, year, quarter)));
        }
        return series;
    }

    private List<Map<String, Object>> yearlySeries() {
        List<PaymentTransactionEntity> successful = successfulPayments();
        int currentYear = OffsetDateTime.now().getYear();
        List<Map<String, Object>> series = new ArrayList<>();
        for (int offset = 4; offset >= 0; offset--) {
            int year = currentYear - offset;
            series.add(point(String.valueOf(year), sumForYear(successful, year)));
        }
        return series;
    }

    private List<Map<String, Object>> byPlanSeries() {
        Map<UUID, BigDecimal> totals = new LinkedHashMap<>();
        for (PaymentTransactionEntity payment : successfulPayments()) {
            if (payment.getSubscriptionPlanId() == null || payment.getAmount() == null) {
                continue;
            }
            totals.merge(payment.getSubscriptionPlanId(), payment.getAmount(), BigDecimal::add);
        }

        List<Map<String, Object>> series = new ArrayList<>();
        totals.forEach((planId, amount) -> {
            String label = subscriptionPlans.findById(planId)
                    .map(SubscriptionPlanEntity::getName)
                    .filter(name -> !name.isBlank())
                    .orElse("Gói " + planId.toString().substring(0, 8));
            series.add(point(label, amount));
        });
        return series;
    }

    private List<PaymentTransactionEntity> successfulPayments() {
        return paymentTransactions.findAll().stream()
                .filter(this::isSuccessful)
                .toList();
    }

    private boolean isSuccessful(PaymentTransactionEntity payment) {
        return payment.getStatus() == PaymentTransactionStatus.PAID
                || payment.getStatus() == PaymentTransactionStatus.SUCCESS;
    }

    private BigDecimal sumAmounts(List<PaymentTransactionEntity> payments) {
        return payments.stream()
                .map(PaymentTransactionEntity::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumForMonth(List<PaymentTransactionEntity> payments, YearMonth month) {
        return payments.stream()
                .filter(payment -> {
                    OffsetDateTime time = effectivePaymentTime(payment);
                    return time != null && YearMonth.from(time).equals(month);
                })
                .map(PaymentTransactionEntity::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumForQuarter(List<PaymentTransactionEntity> payments, int year, int quarter) {
        return payments.stream()
                .filter(payment -> {
                    OffsetDateTime time = effectivePaymentTime(payment);
                    return time != null && time.getYear() == year && quarterOf(time) == quarter;
                })
                .map(PaymentTransactionEntity::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumForYear(List<PaymentTransactionEntity> payments, int year) {
        return payments.stream()
                .filter(payment -> {
                    OffsetDateTime time = effectivePaymentTime(payment);
                    return time != null && time.getYear() == year;
                })
                .map(PaymentTransactionEntity::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OffsetDateTime effectivePaymentTime(PaymentTransactionEntity payment) {
        if (payment.getPaidAt() != null) return payment.getPaidAt();
        if (payment.getConfirmedAt() != null) return payment.getConfirmedAt();
        if (payment.getUpdatedAt() != null) return payment.getUpdatedAt();
        return payment.getCreatedAt();
    }

    private int quarterOf(OffsetDateTime time) {
        return ((time.getMonthValue() - 1) / 3) + 1;
    }

    private Map<String, Object> point(String label, BigDecimal value) {
        Map<String, Object> point = new LinkedHashMap<>();
        point.put("label", label);
        point.put("value", value);
        return point;
    }

    private boolean matches(String actual, String expected) {
        return expected.equals(actual) || (expected + "/").equals(actual);
    }

    private void putAliasIfMissing(Map<String, Object> target, String key, Object value) {
        if (!target.containsKey(key) && value != null) {
            target.put(key, value);
        }
    }
}
