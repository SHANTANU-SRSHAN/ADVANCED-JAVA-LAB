<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Order #${order.orderId} | ShopSphere</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">

    <style>
        .order-page {
            max-width: 950px;
            margin: 45px auto;
            padding: 0 24px;
        }

        .success-hero {
            background: #f0faf4;
            border: 1px solid #c9ead6;
            border-radius: 18px;
            padding: 30px;
            text-align: center;
            margin-bottom: 28px;
        }
        .success-hero .tick {
            width: 58px; height: 58px; margin: 0 auto 12px;
            background: #22a06b; color: white; border-radius: 50%;
            display: grid; place-items: center; font-size: 28px;
        }
        .success-hero h1 { color: #12372a; font-size: 26px; }
        .success-hero p { color: #6b776f; font-size: 14px; margin-top: 7px; }

        .order-page h2 {
            color: #12372a; font-size: 18px; margin: 26px 0 12px;
        }

        .meta-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
            gap: 14px;
            margin-bottom: 8px;
        }

        .meta-box {
            border: 1px solid #e5ebe6;
            border-radius: 13px;
            padding: 14px 16px;
            background: #fafbf9;
        }
        .meta-box .label {
            font-size: 11px; color: #6b776f; text-transform: uppercase;
            letter-spacing: .6px; font-weight: 700;
        }
        .meta-box .value {
            font-size: 14px; color: #17251e; font-weight: 700; margin-top: 5px;
        }

        .timeline {
            display: flex;
            align-items: flex-start;
            justify-content: space-between;
            margin: 22px 0 8px;
            position: relative;
            gap: 4px;
        }

        .timeline::before {
            content: "";
            position: absolute;
            top: 17px; left: 8%; right: 8%;
            height: 3px; background: #e5ebe6; z-index: 0;
        }

        .step {
            position: relative; z-index: 1;
            flex: 1; text-align: center; font-size: 12px; color: #6b776f;
        }

        .step .dot {
            width: 34px; height: 34px; margin: 0 auto 8px;
            border-radius: 50%; background: #eef2ee; color: #9aa79f;
            display: grid; place-items: center; font-size: 15px;
            border: 3px solid white;
        }

        .step.done .dot { background: #22a06b; color: white; }
        .step.done { color: #168052; font-weight: 700; }
        .step.current .dot { box-shadow: 0 0 0 4px #d8f2e4; }

        .cancelled-banner {
            background: #fff1f1; border: 1px solid #ffc9c9; color: #b42318;
            padding: 13px 16px; border-radius: 11px; font-weight: 700;
            font-size: 14px; text-align: center; margin-top: 14px;
        }

        .items-table, .info-table {
            width: 100%;
            border-collapse: collapse;
            border: 1px solid #e5ebe6;
            border-radius: 14px;
            overflow: hidden;
        }

        .items-table th, .items-table td,
        .info-table th, .info-table td {
            padding: 12px 14px;
            text-align: left;
            font-size: 14px;
            border-bottom: 1px solid #eef2ee;
        }

        .items-table th, .info-table th {
            background: #fafbf9;
            color: #6b776f;
            font-size: 12px;
            text-transform: uppercase;
            letter-spacing: .5px;
        }

        .items-table tr:last-child td,
        .info-table tr:last-child td { border-bottom: 0; }

        .items-table .right, .info-table .right { text-align: right; }

        .item-cell { display: flex; align-items: center; gap: 12px; }
        .item-cell img {
            width: 48px; height: 42px; object-fit: cover; border-radius: 8px;
            background: #f6f8f6;
        }
        .item-cell a { font-weight: 700; color: #12372a; }
        .item-cell a:hover { color: #22a06b; }

        .totals {
            margin-top: 14px;
            margin-left: auto;
            max-width: 320px;
        }
        .totals .row {
            display: flex; justify-content: space-between;
            padding: 7px 0; font-size: 14px; color: #6b776f;
        }
        .totals .row.grand {
            border-top: 2px solid #e5ebe6; margin-top: 6px; padding-top: 12px;
            font-size: 19px; font-weight: 800; color: #12372a;
        }

        .actions-row {
            display: flex; gap: 12px; flex-wrap: wrap; margin-top: 28px;
        }

        .pill {
            padding: 4px 11px; border-radius: 999px;
            font-size: 11px; font-weight: 800; letter-spacing: .4px;
        }
        .pill.PLACED    { background: #fff4e0; color: #8a5a00; }
        .pill.CONFIRMED { background: #e6f1ff; color: #175cd3; }
        .pill.SHIPPED   { background: #ede9ff; color: #5925dc; }
        .pill.DELIVERED { background: #e6f7ee; color: #168052; }
        .pill.CANCELLED { background: #ffecec; color: #b42318; }
        .pill.PAID      { background: #e6f7ee; color: #168052; }
        .pill.PENDING   { background: #f2f4f2; color: #6b776f; }

        @media (max-width: 620px) {
            .timeline { flex-wrap: wrap; gap: 14px; }
            .timeline::before { display: none; }
            .step { flex: 1 1 40%; }
        }
    </style>
</head>

<body>
<%@ include file="partials/header.jspf" %>

<main class="order-page">

    <c:if test="${placed}">
        <div class="success-hero">
            <div class="tick">✓</div>
            <h1>Thank you! Your order is confirmed.</h1>
            <p>
                Order <strong>#${order.orderId}</strong> was placed on
                <c:if test="${not empty order.createdAt}">
                    ${order.createdAt.toLocalDate()}
                </c:if>.
                A confirmation is shown below.
            </p>
        </div>
    </c:if>

    <h2>Order status</h2>

    <div class="meta-grid">
        <div class="meta-box">
            <div class="label">Order</div>
            <div class="value">#${order.orderId}
                <span class="pill ${order.orderStatus}">${order.orderStatus}</span>
            </div>
        </div>

        <div class="meta-box">
            <div class="label">Payment</div>
            <div class="value">
                <c:out value="${order.paymentMethod}"/>
                <span class="pill ${order.paymentStatus}">${order.paymentStatus}</span>
            </div>
        </div>

        <div class="meta-box">
            <div class="label">Total</div>
            <div class="value">
                &#8377;<fmt:formatNumber value="${order.totalAmount}"
                                         pattern="#,##0.00"/>
            </div>
        </div>

        <div class="meta-box">
            <div class="label">Placed on</div>
            <div class="value">
                <c:choose>
                    <c:when test="${not empty order.createdAt}">
                        ${order.createdAt.toLocalDate()}
                    </c:when>
                    <c:otherwise>—</c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>

    <c:choose>
        <c:when test="${order.statusStep ge 0}">
            <div class="timeline">
                <div class="step ${order.statusStep ge 0 ? 'done' : ''} ${order.statusStep eq 0 ? 'current' : ''}">
                    <div class="dot">🧾</div>Placed
                </div>
                <div class="step ${order.statusStep ge 1 ? 'done' : ''} ${order.statusStep eq 1 ? 'current' : ''}">
                    <div class="dot">✓</div>Confirmed
                </div>
                <div class="step ${order.statusStep ge 2 ? 'done' : ''} ${order.statusStep eq 2 ? 'current' : ''}">
                    <div class="dot">🚚</div>Shipped
                </div>
                <div class="step ${order.statusStep ge 3 ? 'done' : ''} ${order.statusStep eq 3 ? 'current' : ''}">
                    <div class="dot">🏠</div>Delivered
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <div class="cancelled-banner">
                ✖ This order was cancelled.
            </div>
        </c:otherwise>
    </c:choose>

    <h2>Items</h2>
    <table class="items-table">
        <thead>
        <tr>
            <th>Product</th>
            <th class="right">Price</th>
            <th class="right">Qty</th>
            <th class="right">Total</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="item" items="${order.items}">
            <tr>
                <td>
                    <div class="item-cell">
                        <img src="<c:out value='${item.resolveImage(pageContext.request.contextPath)}'/>" alt="">
                        <a href="${pageContext.request.contextPath}/product?id=${item.productId}">
                            <c:out value="${item.productName}"/>
                        </a>
                    </div>
                </td>
                <td class="right">
                    &#8377;<fmt:formatNumber value="${item.unitPrice}"
                                             pattern="#,##0.00"/>
                </td>
                <td class="right">${item.quantity}</td>
                <td class="right">
                    &#8377;<fmt:formatNumber value="${item.lineTotal}"
                                             pattern="#,##0.00"/>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>

    <div class="totals">
        <div class="row"><span>Subtotal</span>
            <span>&#8377;<fmt:formatNumber value="${order.subtotalAmount}"
                                           pattern="#,##0.00"/></span></div>

        <c:if test="${order.discountAmount.doubleValue() > 0}">
            <div class="row" style="color:#168052;">
                <span>Discount
                    <c:if test="${not empty order.couponCode}">
                        (<c:out value="${order.couponCode}"/>)
                    </c:if>
                </span>
                <span>− &#8377;<fmt:formatNumber value="${order.discountAmount}"
                                                  pattern="#,##0.00"/></span>
            </div>
        </c:if>

        <div class="row"><span>Delivery
            <c:if test="${not empty order.shippingMethodName}">
                (<c:out value="${order.shippingMethodName}"/>)
            </c:if>
        </span>
            <span>
                <c:choose>
                    <c:when test="${order.shippingAmount.doubleValue() == 0}">
                        FREE
                    </c:when>
                    <c:otherwise>
                        &#8377;<fmt:formatNumber value="${order.shippingAmount}" pattern="#,##0.00"/>
                    </c:otherwise>
                </c:choose>
            </span>
        </div>

        <div class="row"><span>Tax</span>
            <span>&#8377;<fmt:formatNumber value="${order.taxAmount}"
                                           pattern="#,##0.00"/></span></div>

        <div class="row grand"><span>Total</span>
            <span>&#8377;<fmt:formatNumber value="${order.totalAmount}"
                                           pattern="#,##0.00"/></span></div>
    </div>

    <h2>Shipping &amp; payment details</h2>
    <table class="info-table">
        <tbody>
        <tr>
            <th>Deliver to</th>
            <td>
                <strong>
                    <c:choose>
                        <c:when test="${not empty order.fullName}">
                            <c:out value="${order.fullName}"/>
                        </c:when>
                        <c:otherwise>
                            <c:out value="${order.customerName}"/>
                        </c:otherwise>
                    </c:choose>
                </strong><br>
                <c:out value="${order.shippingAddress}"/>
            </td>
        </tr>
        <tr>
            <th>Phone</th>
            <td><c:out value="${order.phone}"/></td>
        </tr>
        <tr>
            <th>Payment</th>
            <td>
                <c:out value="${order.paymentMethodLabel}"/> —
                <c:choose>
                    <c:when test="${order.paymentStatus eq 'PAID'}">
                        Paid online
                    </c:when>
                    <c:when test="${order.paymentStatus eq 'REFUNDED'}">
                        Refunded
                    </c:when>
                    <c:when test="${order.paymentStatus eq 'FAILED'}">
                        Failed
                    </c:when>
                    <c:otherwise>Pay on delivery</c:otherwise>
                </c:choose>
            </td>
        </tr>

        <%-- the transaction details of the successful payment --%>
        <c:if test="${not empty payments}">
            <c:forEach var="payment" items="${payments}" begin="0" end="0">
                <c:if test="${payment.successful}">
                    <tr>
                        <th>Transaction</th>
                        <td>
                            <c:out value="${payment.transactionId}"/><br>
                            <span style="font-size:12px;color:var(--muted);">
                                Reference <c:out value="${payment.referenceId}"/>
                                <c:if test="${not empty payment.cardBrand}">
                                    · <c:out value="${payment.cardBrand}"/>
                                    ••••<c:out value="${payment.cardLast4}"/>
                                </c:if>
                                <c:if test="${not empty payment.upiId}">
                                    · <c:out value="${payment.upiId}"/>
                                </c:if>
                                <c:if test="${not empty payment.bankName}">
                                    · <c:out value="${payment.bankName}"/>
                                </c:if>
                            </span>
                        </td>
                    </tr>
                </c:if>
            </c:forEach>
        </c:if>

        <c:if test="${not empty order.trackingNumber}">
            <tr>
                <th>Tracking number</th>
                <td><c:out value="${order.trackingNumber}"/></td>
            </tr>
        </c:if>

        <c:if test="${not empty order.estimatedDelivery}">
            <tr>
                <th>Estimated delivery</th>
                <td><c:out value="${order.estimatedDelivery}"/></td>
            </tr>
        </c:if>

        <c:if test="${not empty order.notes}">
            <tr>
                <th>Delivery notes</th>
                <td><c:out value="${order.notes}"/></td>
            </tr>
        </c:if>

        <tr>
            <th>Status</th>
            <td>
                <span class="pill ${order.orderStatus}">${order.orderStatus}</span>
            </td>
        </tr>
        </tbody>
    </table>

    <%-- ================= tracking history ================= --%>
    <c:if test="${not empty order.events}">
        <h2>Tracking history</h2>
        <ul class="timeline">
            <c:forEach var="event" items="${order.events}">
                <li class="done">
                    <div class="tl-title"><c:out value="${event.title}"/></div>
                    <c:if test="${not empty event.note}">
                        <div class="tl-note"><c:out value="${event.note}"/></div>
                    </c:if>
                    <div class="tl-when">
                        <c:if test="${not empty event.createdAt}">
                            <c:out value="${event.createdAt.toLocalDate()}"/>
                            at
                            <c:out value="${event.createdAt.toLocalTime().toString().substring(0,5)}"/>
                        </c:if>
                        <c:if test="${not empty event.changedBy}">
                            · by <c:out value="${event.changedBy}"/>
                        </c:if>
                    </div>
                </li>
            </c:forEach>
        </ul>
    </c:if>

    <div class="actions-row">
        <a class="primary-btn"
           href="${pageContext.request.contextPath}/orders">
            ← All my orders
        </a>
        <a class="secondary-btn"
           href="${pageContext.request.contextPath}/products">
            Continue shopping
        </a>
        <c:if test="${order.cancellable}">
            <form method="post"
                  action="${pageContext.request.contextPath}/orders"
                  onsubmit="return confirm('Cancel this order?');">
                <input type="hidden" name="csrfToken"
                       value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="cancel">
                <input type="hidden" name="orderId" value="${order.orderId}">
                <button type="submit" class="cancel-btn"
                        style="border:1px solid #f3c9c5;background:#fff6f5;color:#b42318;padding:12px 17px;border-radius:10px;font-weight:700;cursor:pointer;">
                    Cancel order
                </button>
            </form>
        </c:if>
    </div>
</main>

<%@ include file="partials/footer.jspf" %>
</body>
</html>
