<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Orders | ShopSphere</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">

    <style>
        .orders-page {
            max-width: 1000px;
            margin: 45px auto;
            padding: 0 24px;
        }

        .orders-page h1 { color: #12372a; margin-bottom: 6px; }
        .orders-intro { color: #6b776f; margin-bottom: 25px; }

        .order-card {
            border: 1px solid #e5ebe6;
            border-radius: 16px;
            padding: 20px;
            margin-bottom: 16px;
            background: white;
            display: grid;
            grid-template-columns: 1fr auto;
            gap: 15px;
            align-items: center;
        }

        .order-card:hover { border-color: #cfe4d6; }

        .order-top {
            display: flex;
            gap: 14px;
            align-items: center;
            flex-wrap: wrap;
            margin-bottom: 8px;
        }

        .order-id {
            font-weight: 800;
            color: #12372a;
            font-size: 16px;
        }

        .order-date { color: #6b776f; font-size: 13px; }

        .order-items-line {
            color: #4a574f;
            font-size: 14px;
            margin-top: 4px;
        }

        .order-total {
            color: #168052;
            font-weight: 800;
            font-size: 18px;
        }

        .order-actions {
            display: flex;
            flex-direction: column;
            gap: 8px;
            align-items: stretch;
        }

        .order-actions .primary-btn,
        .order-actions .secondary-btn {
            padding: 9px 15px;
            font-size: 13px;
            text-align: center;
        }

        .cancel-btn {
            border: 1px solid #f3c9c5;
            background: #fff6f5;
            color: #b42318;
            padding: 9px 15px;
            border-radius: 10px;
            font-size: 13px;
            font-weight: 700;
            cursor: pointer;
        }
        .cancel-btn:hover { background: #ffecec; }

        .pill {
            padding: 4px 11px;
            border-radius: 999px;
            font-size: 11px;
            font-weight: 800;
            letter-spacing: .4px;
            text-transform: uppercase;
        }

        .pill.PLACED    { background: #fff4e0; color: #8a5a00; }
        .pill.CONFIRMED { background: #e6f1ff; color: #175cd3; }
        .pill.SHIPPED   { background: #ede9ff; color: #5925dc; }
        .pill.DELIVERED { background: #e6f7ee; color: #168052; }
        .pill.CANCELLED { background: #ffecec; color: #b42318; }
        .pill.PAID      { background: #e6f7ee; color: #168052; }
        .pill.PENDING   { background: #f2f4f2; color: #6b776f; }

        .empty-state {
            padding: 55px 25px;
            text-align: center;
            background: #f6f8f6;
            border-radius: 16px;
        }
        .empty-state .big { font-size: 44px; }
        .empty-state h2 { color: #12372a; margin: 10px 0 6px; }
        .empty-state p { color: #6b776f; margin-bottom: 20px; }

        @media (max-width: 640px) {
            .order-card { grid-template-columns: 1fr; }
            .order-actions { flex-direction: row; flex-wrap: wrap; }
        }
    </style>
</head>

<body>
<%@ include file="partials/header.jspf" %>

<main class="orders-page">
    <p class="eyebrow">MY ACCOUNT</p>
    <h1>My orders</h1>
    <p class="orders-intro">Track deliveries, view receipts or cancel recent orders.</p>

    <%@ include file="partials/alerts.jspf" %>

    <c:choose>
        <c:when test="${empty orders}">
            <div class="empty-state">
                <div class="big">📦</div>
                <h2>No orders yet</h2>
                <p>When you place an order it will appear here.</p>
                <a class="primary-btn"
                   href="${pageContext.request.contextPath}/products">
                    Start shopping →
                </a>
            </div>
        </c:when>

        <c:otherwise>
            <c:forEach var="order" items="${orders}">
                <div class="order-card">
                    <div>
                        <div class="order-top">
                            <span class="order-id">Order #<c:out
                                    value="${empty order.orderNumber ? order.orderId : order.orderNumber}"/></span>
                            <span class="pill ${order.orderStatus}">
                                ${order.orderStatus}
                            </span>
                            <span class="pill ${order.paymentStatus}">
                                ${order.paymentStatus}
                            </span>
                            <span class="order-date">
                                <c:if test="${not empty order.createdAt}">
                                    ${order.createdAt.toLocalDate()}
                                    ${order.createdAt.toLocalTime().toString().substring(0, 5)}
                                </c:if>
                            </span>
                        </div>

                        <div class="order-items-line">
                            ${fn:length(order.items)} item(s) ·
                            ${order.paymentMethod} ·
                            <c:out value="${order.shippingAddress}"/>
                        </div>

                        <div class="order-total">
                            &#8377;<fmt:formatNumber value="${order.totalAmount}"
                                                     pattern="#,##0.00"/>
                        </div>
                    </div>

                    <div class="order-actions">
                        <a class="primary-btn"
                           href="${pageContext.request.contextPath}/order?id=${order.orderId}">
                            View details
                        </a>

                        <c:if test="${order.cancellable}">
                            <form method="post"
                                  action="${pageContext.request.contextPath}/orders"
                                  onsubmit="return confirm('Cancel this order?');">
                                <input type="hidden" name="csrfToken"
                                       value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="cancel">
                                <input type="hidden" name="orderId" value="${order.orderId}">
                                <button type="submit" class="cancel-btn">
                                    Cancel order
                                </button>
                            </form>
                        </c:if>
                    </div>
                </div>
            </c:forEach>
        </c:otherwise>
    </c:choose>
</main>

<%@ include file="partials/footer.jspf" %>
</body>
</html>
