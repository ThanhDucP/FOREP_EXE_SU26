package com.forep.exe.controller;

import com.forep.exe.domain.Enums.PaymentStatus;
import com.forep.exe.domain.Enums.PaymentTransactionStatus;
import com.forep.exe.domain.Enums.WorkspaceSubscriptionStatus;
import com.forep.exe.dto.ApiResponse;
import com.forep.exe.persistence.PaymentTransactionEntity;
import com.forep.exe.persistence.PaymentTransactionRepository;
import com.forep.exe.persistence.SubscriptionPlanEntity;
import com.forep.exe.persistence.SubscriptionPlanRepository;
import com.forep.exe.persistence.WorkspaceRegistrationEntity;
import com.forep.exe.persistence.WorkspaceRegistrationRepository;
import com.forep.exe.persistence.WorkspaceSubscriptionEntity;
import com.forep.exe.persistence.WorkspaceSubscriptionRepository;
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
import java.util.Set;
import java.util.UUID;

@RestControllerAdvice(assignableTypes = AdminPlatformController.class)
public class PlatformDashboardResponseEnricher implements ResponseBodyAdvice<Object> {
    private static final String PLATFORM_OVERVIEW_PATH = "/api/admin/dashboard/overview";
    private static final String REVENUE_MONTHLY_PATH = "/api/admin/dashboard/revenue/monthly";
    private static final String REVENUE_QUARTERLY_PATH = "/api/admin/dashboard/revenue/quarterly";
    private static final String REVENUE_YEARLY_PATH = "/api/admin/dashboard/revenue/yearly";
    private static final String REVENUE_BY_PLAN_PATH = "/api/admin/dashboard/revenue/by-plan";
    private static final Set<WorkspaceSubscriptionStatus> REVENUE_SUBSCRIPTION_STATUSES = Set.of(
            WorkspaceSubscriptionStatus.ACTIVE,
            WorkspaceSubscriptionStatus.EXPIRED,
            WorkspaceSubscriptionStatus.UPGRADED,
            WorkspaceSubscriptionStatus.DOWNGRADED
    );

    private final PaymentTransactionRepository paymentTransactions;
    private final SubscriptionPlanRepository subscriptionPlans;
    private final WorkspaceSubscriptionRepository workspaceSubscriptions;
    private final WorkspaceRegistrationRepository workspaceRegistrations;

    public PlatformDashboardResponseEnricher(PaymentTransactionRepository paymentTransactions,
                                             SubscriptionPlanRepository subscriptionPlans,
                                             WorkspaceSubscriptionRepository workspaceSubscriptions,
                                             WorkspaceRegistrationRepository workspaceRegistrations) {
        this.paymentTransactions = paymentTransactions;
        this.subscriptionPlans = subscriptionPlans;
        this.workspaceSubscriptions = workspaceSubscriptions;
        this.workspaceRegistrations = workspaceRegistrations;
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
            RevenueSource source = revenueSource();
            return ApiResponse.ok(seriesPayload("Doanh thu theo tháng", monthlySeries(source), source));
        }
        if (matches(path, REVENUE_QUARTERLY_PATH)) {
            RevenueSource source = revenueSource();
            return ApiResponse.ok(seriesPayload("Doanh thu theo quý", quarterlySeries(source), source));
        }
        if (matches(path, REVENUE_YEARLY_PATH)) {
            RevenueSource source = revenueSource();
            return ApiResponse.ok(seriesPayload("Doanh thu theo năm", yearlySeries(source), source));
        }
        if (matches(path, REVENUE_BY_PLAN_PATH)) {
            RevenueSource source = revenueSource();
            return ApiResponse.ok(seriesPayload("Doanh thu theo gói", byPlanSeries(source), source));
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

        RevenueSource source = revenueSource();
        BigDecimal totalRevenue = totalRevenue(source);
        BigDecimal monthRevenue = revenueForMonth(source, YearMonth.now());
        OffsetDateTime now = OffsetDateTime.now();
        BigDecimal quarterRevenue = revenueForQuarter(source, now.getYear(), quarterOf(now));
        BigDecimal yearRevenue = revenueForYear(source, now.getYear());

        enriched.put("totalRevenue", totalRevenue);
        enriched.put("revenue", totalRevenue);
        enriched.put("revenueThisMonth", monthRevenue);
        enriched.put("revenueThisQuarter", quarterRevenue);
        enriched.put("revenueThisYear", yearRevenue);
        enriched.put("currency", "VND");
        enriched.put("successfulPayments", source.successfulPayments().size());
        enriched.put("rawSuccessfulPaymentCount", source.successfulPayments().size());
        enriched.put("activatedSubscriptionCount", source.activatedSubscriptions().size());
        enriched.put("confirmedRegistrationCount", source.confirmedRegistrations().size());
        enriched.put("revenueSource", source.kind().name());

        return ApiResponse.ok(enriched);
    }

    private Map<String, Object> seriesPayload(String title,
                                              List<Map<String, Object>> series,
                                              RevenueSource source) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("series", series);
        payload.put("currency", "VND");
        payload.put("revenueSource", source.kind().name());
        payload.put("totalRevenue", series.stream()
                .map(point -> point.get("value"))
                .filter(BigDecimal.class::isInstance)
                .map(BigDecimal.class::cast)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return payload;
    }

    private RevenueSource revenueSource() {
        List<PaymentTransactionEntity> successful = successfulPayments();
        List<WorkspaceSubscriptionEntity> activated = activatedSubscriptions();
        List<WorkspaceRegistrationEntity> confirmed = confirmedRegistrations();

        if (sumPaymentAmounts(successful).compareTo(BigDecimal.ZERO) > 0) {
            return new RevenueSource(successful, activated, confirmed, RevenueSourceKind.PAYMENT_TRANSACTIONS);
        }
        if (sumSubscriptionPrices(activated).compareTo(BigDecimal.ZERO) > 0) {
            return new RevenueSource(successful, activated, confirmed, RevenueSourceKind.SUBSCRIPTION_FALLBACK);
        }
        return new RevenueSource(successful, activated, confirmed, RevenueSourceKind.REGISTRATION_FALLBACK);
    }

    private List<PaymentTransactionEntity> successfulPayments() {
        return paymentTransactions.findAll().stream()
                .filter(this::isSuccessful)
                .toList();
    }

    private List<WorkspaceSubscriptionEntity> activatedSubscriptions() {
        return workspaceSubscriptions.findAll().stream()
                .filter(subscription -> REVENUE_SUBSCRIPTION_STATUSES.contains(subscription.getStatus()))
                .filter(subscription -> subscription.getPrice() != null)
                .filter(subscription -> subscription.getPrice().compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    private List<WorkspaceRegistrationEntity> confirmedRegistrations() {
        return workspaceRegistrations.findAll().stream()
                .filter(registration -> registration.getPaymentStatus() == PaymentStatus.CONFIRMED)
                .filter(registration -> registration.getWorkspaceId() != null)
                .filter(registration -> registration.getSubscriptionPlanId() != null)
                .filter(registration -> registrationPrice(registration).compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    private BigDecimal totalRevenue(RevenueSource source) {
        return switch (source.kind()) {
            case PAYMENT_TRANSACTIONS -> sumPaymentAmounts(source.successfulPayments());
            case SUBSCRIPTION_FALLBACK -> sumSubscriptionPrices(source.activatedSubscriptions());
            case REGISTRATION_FALLBACK -> sumRegistrationPrices(source.confirmedRegistrations());
        };
    }

    private List<Map<String, Object>> monthlySeries(RevenueSource source) {
        YearMonth current = YearMonth.now();
        List<Map<String, Object>> series = new ArrayList<>();
        for (int offset = 11; offset >= 0; offset--) {
            YearMonth period = current.minusMonths(offset);
            series.add(point(String.format("%02d/%d", period.getMonthValue(), period.getYear()),
                    revenueForMonth(source, period)));
        }
        return series;
    }

    private List<Map<String, Object>> quarterlySeries(RevenueSource source) {
        OffsetDateTime now = OffsetDateTime.now();
        int currentIndex = now.getYear() * 4 + quarterOf(now) - 1;
        List<Map<String, Object>> series = new ArrayList<>();
        for (int offset = 7; offset >= 0; offset--) {
            int index = currentIndex - offset;
            int year = Math.floorDiv(index, 4);
            int quarter = Math.floorMod(index, 4) + 1;
            series.add(point("Q" + quarter + "/" + year, revenueForQuarter(source, year, quarter)));
        }
        return series;
    }

    private List<Map<String, Object>> yearlySeries(RevenueSource source) {
        int currentYear = OffsetDateTime.now().getYear();
        List<Map<String, Object>> series = new ArrayList<>();
        for (int offset = 4; offset >= 0; offset--) {
            int year = currentYear - offset;
            series.add(point(String.valueOf(year), revenueForYear(source, year)));
        }
        return series;
    }

    private List<Map<String, Object>> byPlanSeries(RevenueSource source) {
        Map<UUID, BigDecimal> totals = new LinkedHashMap<>();
        switch (source.kind()) {
            case PAYMENT_TRANSACTIONS -> source.successfulPayments().forEach(payment -> {
                if (payment.getSubscriptionPlanId() != null && payment.getAmount() != null) {
                    totals.merge(payment.getSubscriptionPlanId(), payment.getAmount(), BigDecimal::add);
                }
            });
            case SUBSCRIPTION_FALLBACK -> source.activatedSubscriptions().forEach(subscription -> {
                if (subscription.getSubscriptionPlanId() != null && subscription.getPrice() != null) {
                    totals.merge(subscription.getSubscriptionPlanId(), subscription.getPrice(), BigDecimal::add);
                }
            });
            case REGISTRATION_FALLBACK -> source.confirmedRegistrations().forEach(registration -> {
                if (registration.getSubscriptionPlanId() != null) {
                    totals.merge(registration.getSubscriptionPlanId(), registrationPrice(registration), BigDecimal::add);
                }
            });
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

    private BigDecimal revenueForMonth(RevenueSource source, YearMonth month) {
        return switch (source.kind()) {
            case PAYMENT_TRANSACTIONS -> source.successfulPayments().stream()
                    .filter(payment -> {
                        OffsetDateTime time = effectivePaymentTime(payment);
                        return time != null && YearMonth.from(time).equals(month);
                    })
                    .map(PaymentTransactionEntity::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            case SUBSCRIPTION_FALLBACK -> source.activatedSubscriptions().stream()
                    .filter(subscription -> subscription.getStartDate() != null)
                    .filter(subscription -> YearMonth.from(subscription.getStartDate()).equals(month))
                    .map(WorkspaceSubscriptionEntity::getPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            case REGISTRATION_FALLBACK -> source.confirmedRegistrations().stream()
                    .filter(registration -> {
                        OffsetDateTime time = effectiveRegistrationTime(registration);
                        return time != null && YearMonth.from(time).equals(month);
                    })
                    .map(this::registrationPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        };
    }

    private BigDecimal revenueForQuarter(RevenueSource source, int year, int quarter) {
        return switch (source.kind()) {
            case PAYMENT_TRANSACTIONS -> source.successfulPayments().stream()
                    .filter(payment -> {
                        OffsetDateTime time = effectivePaymentTime(payment);
                        return time != null && time.getYear() == year && quarterOf(time) == quarter;
                    })
                    .map(PaymentTransactionEntity::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            case SUBSCRIPTION_FALLBACK -> source.activatedSubscriptions().stream()
                    .filter(subscription -> subscription.getStartDate() != null)
                    .filter(subscription -> subscription.getStartDate().getYear() == year
                            && quarterOf(subscription.getStartDate()) == quarter)
                    .map(WorkspaceSubscriptionEntity::getPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            case REGISTRATION_FALLBACK -> source.confirmedRegistrations().stream()
                    .filter(registration -> {
                        OffsetDateTime time = effectiveRegistrationTime(registration);
                        return time != null && time.getYear() == year && quarterOf(time) == quarter;
                    })
                    .map(this::registrationPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        };
    }

    private BigDecimal revenueForYear(RevenueSource source, int year) {
        return switch (source.kind()) {
            case PAYMENT_TRANSACTIONS -> source.successfulPayments().stream()
                    .filter(payment -> {
                        OffsetDateTime time = effectivePaymentTime(payment);
                        return time != null && time.getYear() == year;
                    })
                    .map(PaymentTransactionEntity::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            case SUBSCRIPTION_FALLBACK -> source.activatedSubscriptions().stream()
                    .filter(subscription -> subscription.getStartDate() != null)
                    .filter(subscription -> subscription.getStartDate().getYear() == year)
                    .map(WorkspaceSubscriptionEntity::getPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            case REGISTRATION_FALLBACK -> source.confirmedRegistrations().stream()
                    .filter(registration -> {
                        OffsetDateTime time = effectiveRegistrationTime(registration);
                        return time != null && time.getYear() == year;
                    })
                    .map(this::registrationPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        };
    }

    private boolean isSuccessful(PaymentTransactionEntity payment) {
        return payment.getStatus() == PaymentTransactionStatus.PAID
                || payment.getStatus() == PaymentTransactionStatus.SUCCESS;
    }

    private BigDecimal sumPaymentAmounts(List<PaymentTransactionEntity> payments) {
        return payments.stream()
                .map(PaymentTransactionEntity::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumSubscriptionPrices(List<WorkspaceSubscriptionEntity> subscriptions) {
        return subscriptions.stream()
                .map(WorkspaceSubscriptionEntity::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumRegistrationPrices(List<WorkspaceRegistrationEntity> registrations) {
        return registrations.stream()
                .map(this::registrationPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal registrationPrice(WorkspaceRegistrationEntity registration) {
        if (registration.getSubscriptionPlanId() == null) return BigDecimal.ZERO;
        return subscriptionPlans.findById(registration.getSubscriptionPlanId())
                .map(SubscriptionPlanEntity::getPrice)
                .filter(Objects::nonNull)
                .orElse(BigDecimal.ZERO);
    }

    private OffsetDateTime effectivePaymentTime(PaymentTransactionEntity payment) {
        if (payment.getPaidAt() != null) return payment.getPaidAt();
        if (payment.getConfirmedAt() != null) return payment.getConfirmedAt();
        if (payment.getUpdatedAt() != null) return payment.getUpdatedAt();
        return payment.getCreatedAt();
    }

    private OffsetDateTime effectiveRegistrationTime(WorkspaceRegistrationEntity registration) {
        if (registration.getActivationDate() != null) return registration.getActivationDate();
        if (registration.getReviewedAt() != null) return registration.getReviewedAt();
        if (registration.getUpdatedAt() != null) return registration.getUpdatedAt();
        return registration.getCreatedAt();
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

    private enum RevenueSourceKind {
        PAYMENT_TRANSACTIONS,
        SUBSCRIPTION_FALLBACK,
        REGISTRATION_FALLBACK
    }

    private record RevenueSource(
            List<PaymentTransactionEntity> successfulPayments,
            List<WorkspaceSubscriptionEntity> activatedSubscriptions,
            List<WorkspaceRegistrationEntity> confirmedRegistrations,
            RevenueSourceKind kind) {
    }
}
