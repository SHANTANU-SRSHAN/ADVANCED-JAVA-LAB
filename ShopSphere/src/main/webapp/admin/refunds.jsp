<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin · Refunds | ShopSphere</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">
</head>

<body>
<%@ include file="../partials/header.jspf" %>
<%@ include file="../partials/admin-nav.jspf" %>

<main class="page-wrap">
    <p class="eyebrow">ADMIN</p>
    <h1>Refunds</h1>
    <p class="page-sub">Reverse a payment that has already been taken.</p>

    <c:if test="${not empty param.error}">
        <div class="alert alert-error">
            ⚠ <c:out value="${param.error}"/>
        </div>
    </c:if>

    <c:if test="${param.refunded eq '1'}">
        <div class="alert alert-success">✅ The refund was processed.</div>
    </c:if>

    <div class="panel-grid" style="grid-template-columns:1.3fr 1fr;">

        <!-- ============ refundable payments ============ -->
        <div class="panel" style="padding:0;overflow:hidden;">
            <div style="padding:16px 18px;border-bottom:1px solid var(--border);">
                <h2 style="font-size:16px;">Payments that can be refunded</h2>
                <p class="muted-text" style="margin-top:4px;">
                    Choose a payment and give a reason.
                </p>
            </div>

            <table class="data-table">
                <thead>
                    <tr>
                        <th>Order</th>
                        <th>Method</th>
                        <th>Transaction</th>
                        <th class="right">Amount</th>
                        <th style="text-align:right;">Refund</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="payment" items="${payments}">
                        <tr>
                            <td style="font-size:12px;">
                                <a href="${pageContext.request.contextPath}/order?id=${payment.orderId}">
                                    #${payment.orderId}
                                </a>
                            </td>
                            <td style="font-size:12px;">
                                <c:out value="${payment.methodLabel}"/>
                                <c:if test="${not empty payment.cardBrand}">
                                    <div style="font-size:11px;color:var(--muted);">
                                        <c:out value="${payment.cardBrand}"/>
                                        ••••<c:out value="${payment.cardLast4}"/>
                                    </div>
                                </c:if>
                            </td>
                            <td style="font-size:11px;color:var(--muted);">
                                <c:out value="${payment.transactionId}"/>
                            </td>
                            <td class="right" style="font-size:12px;">
                                &#8377;<fmt:formatNumber value="${payment.amount}"
                                                         pattern="#,##0.00"/>
                            </td>
                            <td style="text-align:right;">
                                <form method="post"
                                      action="${pageContext.request.contextPath}/admin/refunds"
                                      onsubmit="return confirm('Refund this payment?');">
                                    <input type="hidden" name="csrfToken"
                                           value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="refund">
                                    <input type="hidden" name="paymentId"
                                           value="${payment.paymentId}">
                                    <input type="text" name="reason"
                                           maxlength="300"
                                           placeholder="Reason"
                                           required
                                           style="padding:7px 9px;font-size:12px;
                                                  border:1px solid #dce5de;
                                                  border-radius:7px;width:140px;">
                                    <button type="submit" class="btn btn-sm btn-danger">
                                        Refund
                                    </button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>

                    <c:if test="${empty payments}">
                        <tr>
                            <td colspan="5" style="color:var(--muted);padding:25px;">
                                There are no payments waiting for a refund.
                            </td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </div>

        <!-- ============ refund history ============ -->
        <div class="panel" style="padding:0;overflow:hidden;">
            <div style="padding:16px 18px;border-bottom:1px solid var(--border);">
                <h2 style="font-size:16px;">Refunds already issued</h2>
            </div>

            <table class="data-table">
                <thead>
                    <tr>
                        <th>Order</th>
                        <th>Reason</th>
                        <th class="right">Amount</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="refund" items="${refunds}">
                        <tr>
                            <td style="font-size:12px;">
                                <a href="${pageContext.request.contextPath}/order?id=${refund.orderId}">
                                    #${refund.orderId}
                                </a>
                                <div style="font-size:11px;color:var(--muted);">
                                    <c:out value="${refund.customerName}"/>
                                </div>
                            </td>
                            <td style="font-size:12px;">
                                <c:out value="${refund.reason}"/>
                                <div style="font-size:11px;color:var(--muted);">
                                    <c:if test="${not empty refund.createdAt}">
                                        <c:out value="${refund.createdAt.toLocalDate()}"/>
                                    </c:if>
                                    <c:if test="${not empty refund.processedBy}">
                                        · by <c:out value="${refund.processedBy}"/>
                                    </c:if>
                                </div>
                            </td>
                            <td class="right" style="font-size:12px;">
                                &#8377;<fmt:formatNumber value="${refund.amount}"
                                                         pattern="#,##0.00"/>
                            </td>
                        </tr>
                    </c:forEach>

                    <c:if test="${empty refunds}">
                        <tr>
                            <td colspan="3" style="color:var(--muted);padding:25px;">
                                No refunds have been issued yet.
                            </td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>
</main>

<%@ include file="../partials/footer.jspf" %>
</body>
</html>
