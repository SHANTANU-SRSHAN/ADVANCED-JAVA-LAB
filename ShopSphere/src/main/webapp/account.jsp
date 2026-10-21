<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Account | ShopSphere</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">
</head>

<body>
<%@ include file="partials/header.jspf" %>

<main class="page-wrap">
    <p class="eyebrow">MY ACCOUNT</p>
    <h1>Hello, <c:out value="${account.firstName}"/></h1>
    <p class="page-sub">Manage your details, password and orders.</p>

    <c:if test="${not empty error}">
        <div class="alert alert-error"><c:out value="${error}"/></div>
    </c:if>

    <c:if test="${param.updated eq 'profile'}">
        <div class="alert alert-success">✅ Your details have been saved.</div>
    </c:if>

    <c:if test="${param.updated eq 'password'}">
        <div class="alert alert-success">✅ Your password has been changed.
            Other devices have been signed out.</div>
    </c:if>

    <div class="panel-grid" style="grid-template-columns:1fr 1fr;">

        <!-- ============ profile ============ -->
        <div class="panel">
            <h2>Profile</h2>
            <p class="sub">Your name, e-mail and phone number.</p>

            <form method="post"
                  action="${pageContext.request.contextPath}/account">
                <input type="hidden" name="csrfToken"
                       value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="profile">

                <div class="field">
                    <label for="fullName">Full name</label>
                    <input id="fullName" name="fullName" type="text"
                           maxlength="100" required
                           value="<c:out value='${account.fullName}'/>">
                </div>

                <div class="field">
                    <label for="email">E-mail address</label>
                    <input id="email" type="email" maxlength="150"
                           value="<c:out value='${account.email}'/>" disabled>
                    <p class="field-hint">
                        Contact support to change your e-mail address.
                    </p>
                </div>

                <div class="field">
                    <label for="phone">Phone number</label>
                    <input id="phone" name="phone" type="tel"
                           maxlength="10" pattern="[0-9]{10}"
                           placeholder="10-digit mobile number"
                           value="<c:out value='${account.phone}'/>">
                </div>

                <c:if test="${not account.emailVerified}">
                    <div class="info-note" style="margin-bottom:15px;">
                        ⚠ Your e-mail address has not been verified yet.
                        Check the application console for the verification link.
                    </div>
                </c:if>

                <button type="submit" class="primary-btn">Save changes</button>
            </form>
        </div>

        <!-- ============ password ============ -->
        <div class="panel">
            <h2>Password</h2>
            <p class="sub">Change the password you use to sign in.</p>

            <form method="post"
                  action="${pageContext.request.contextPath}/account">
                <input type="hidden" name="csrfToken"
                       value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="password">

                <div class="field">
                    <label for="oldPassword">Current password</label>
                    <input id="oldPassword" name="oldPassword" type="password"
                           autocomplete="current-password" required>
                </div>

                <div class="field">
                    <label for="newPassword">New password</label>
                    <input id="newPassword" name="newPassword" type="password"
                           minlength="8" maxlength="64"
                           autocomplete="new-password" required>
                    <p class="field-hint">
                        At least 8 characters, with a letter and a number.
                    </p>
                </div>

                <div class="field">
                    <label for="confirmPassword">Confirm new password</label>
                    <input id="confirmPassword" name="confirmPassword"
                           type="password" minlength="8" maxlength="64"
                           autocomplete="new-password" required>
                </div>

                <button type="submit" class="primary-btn">Change password</button>
            </form>

            <hr style="border:none;border-top:1px solid var(--border);
                       margin:22px 0;">

            <h2 style="font-size:15px;">Your addresses</h2>
            <p class="muted-text" style="margin:6px 0 12px;">
                Keep your delivery addresses ready for checkout.
            </p>
            <a class="btn" href="${pageContext.request.contextPath}/addresses">
                Manage address book →
            </a>
        </div>
    </div>

    <!-- ============ orders ============ -->
    <div class="panel" style="margin-top:20px;">
        <h2>Recent orders</h2>
        <p class="sub">Your five most recent orders.</p>

        <c:choose>
            <c:when test="${empty orders}">
                <div class="empty-state">
                    <h2>No orders yet</h2>
                    <p>When you place an order it will appear here.</p>
                    <a class="primary-btn"
                       href="${pageContext.request.contextPath}/products">
                        Start shopping →
                    </a>
                </div>
            </c:when>

            <c:otherwise>
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Order</th>
                            <th>Date</th>
                            <th>Items</th>
                            <th>Payment</th>
                            <th>Status</th>
                            <th class="right">Total</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="order" items="${orders}">
                            <tr>
                                <td>
                                    <a href="${pageContext.request.contextPath}/order?id=${order.orderId}">
                                        <strong><c:out value="${order.orderNumber}"/></strong>
                                    </a>
                                </td>
                                <td>
                                    <c:if test="${not empty order.createdAt}">
                                        <c:out value="${order.createdAt.toLocalDate()}"/>
                                    </c:if>
                                </td>
                                <td>${fn:length(order.items)} item(s)</td>
                                <td><c:out value="${order.paymentMethodLabel}"/></td>
                                <td>
                                    <span class="pill ${order.orderStatus}">
                                        <c:out value="${order.orderStatus}"/>
                                    </span>
                                </td>
                                <td class="right">
                                    &#8377;<fmt:formatNumber value="${order.totalAmount}"
                                                             pattern="#,##0.00"/>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>

                <p style="margin-top:14px;">
                    <a href="${pageContext.request.contextPath}/orders">
                        View all orders →
                    </a>
                </p>
            </c:otherwise>
        </c:choose>
    </div>

    <!-- ============ payments ============ -->
    <div class="panel" style="margin-top:20px;">
        <h2>Payment history</h2>
        <p class="sub">Every payment attempt made on your account.</p>

        <c:choose>
            <c:when test="${empty payments}">
                <p class="muted-text">No payments yet.</p>
            </c:when>

            <c:otherwise>
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Reference</th>
                            <th>Method</th>
                            <th>Transaction</th>
                            <th>Details</th>
                            <th>Status</th>
                            <th class="right">Amount</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="payment" items="${payments}">
                            <tr>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty payment.referenceId}">
                                            <c:out value="${payment.referenceId}"/>
                                        </c:when>
                                        <c:otherwise>—</c:otherwise>
                                    </c:choose>
                                </td>
                                <td><c:out value="${payment.methodLabel}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty payment.transactionId}">
                                            <c:out value="${payment.transactionId}"/>
                                        </c:when>
                                        <c:otherwise>—</c:otherwise>
                                    </c:choose>
                                </td>
                                <td style="font-size:12px;color:var(--muted);">
                                    <c:if test="${not empty payment.cardBrand}">
                                        <c:out value="${payment.cardBrand}"/>
                                        •••• <c:out value="${payment.cardLast4}"/>
                                    </c:if>
                                    <c:if test="${not empty payment.upiId}">
                                        <c:out value="${payment.upiId}"/>
                                    </c:if>
                                    <c:if test="${not empty payment.bankName}">
                                        <c:out value="${payment.bankName}"/>
                                    </c:if>
                                    <c:if test="${payment.method eq 'COD'}">
                                        Pay on delivery
                                    </c:if>
                                </td>
                                <td>
                                    <span class="pill ${payment.status}">
                                        <c:out value="${payment.status}"/>
                                    </span>
                                    <c:if test="${not empty payment.failureReason}">
                                        <div style="font-size:11px;color:#b42318;">
                                            <c:out value="${payment.failureReason}"/>
                                        </div>
                                    </c:if>
                                </td>
                                <td class="right">
                                    &#8377;<fmt:formatNumber value="${payment.amount}"
                                                             pattern="#,##0.00"/>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </div>

    <!-- ============ reviews ============ -->
    <div class="panel" style="margin-top:20px;">
        <h2>Your reviews</h2>
        <p class="sub">Reviews you have written for products.</p>

        <c:choose>
            <c:when test="${empty reviews}">
                <p class="muted-text">
                    You have not written any reviews yet.
                </p>
            </c:when>

            <c:otherwise>
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Product</th>
                            <th>Rating</th>
                            <th>Your review</th>
                            <th>Date</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="review" items="${reviews}">
                            <tr>
                                <td>
                                    <a href="${pageContext.request.contextPath}/product?id=${review.productId}">
                                        <c:out value="${review.productName}"/>
                                    </a>
                                </td>
                                <td><span class="stars"><c:out value="${review.stars}"/></span></td>
                                <td style="font-size:13px;">
                                    <c:if test="${not empty review.title}">
                                        <strong><c:out value="${review.title}"/></strong><br>
                                    </c:if>
                                    <c:out value="${review.comment}"/>
                                </td>
                                <td style="font-size:12px;color:var(--muted);">
                                    <c:if test="${not empty review.createdAt}">
                                        <c:out value="${review.createdAt.toLocalDate()}"/>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </div>
</main>

<%@ include file="partials/footer.jspf" %>
</body>
</html>
