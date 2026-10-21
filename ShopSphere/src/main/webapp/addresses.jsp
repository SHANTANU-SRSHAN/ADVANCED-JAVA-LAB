<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Address Book | ShopSphere</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">
</head>

<body>
<%@ include file="partials/header.jspf" %>

<main class="page-wrap">
    <p class="eyebrow">ADDRESS BOOK</p>
    <h1>Your delivery addresses</h1>
    <p class="page-sub">Save an address once and use it at checkout.</p>

    <c:if test="${not empty param.error}">
        <div class="alert alert-error">
            ⚠ <c:out value="${param.error}"/>
        </div>
    </c:if>

    <c:if test="${param.saved eq '1'}">
        <div class="alert alert-success">✅ Address saved.</div>
    </c:if>

    <c:if test="${param.deleted eq '1'}">
        <div class="alert alert-success">✅ Address removed.</div>
    </c:if>

    <c:if test="${param.isDefault eq '1'}">
        <div class="alert alert-success">✅ Default address updated.</div>
    </c:if>

    <div class="panel-grid" style="grid-template-columns:1.2fr 1fr;">

        <!-- ============ saved addresses ============ -->
        <div>
            <c:choose>
                <c:when test="${empty addresses}">
                    <div class="empty-state">
                        <h2>No saved addresses</h2>
                        <p>Add one using the form and it will appear here.</p>
                    </div>
                </c:when>

                <c:otherwise>
                    <c:forEach var="addr" items="${addresses}">
                        <div class="panel" style="margin-bottom:14px;">
                            <div style="display:flex;justify-content:space-between;
                                        align-items:flex-start;gap:12px;
                                        flex-wrap:wrap;">
                                <div>
                                    <strong><c:out value="${addr.label}"/></strong>
                                    <c:if test="${addr.defaultAddress}">
                                        <span class="pill PAID">Default</span>
                                    </c:if>
                                    <p class="muted-text" style="margin-top:6px;">
                                        <c:out value="${addr.fullName}"/><br>
                                        <c:out value="${addr.addressLine1}"/>
                                        <c:if test="${not empty addr.addressLine2}">
                                            , <c:out value="${addr.addressLine2}"/>
                                        </c:if><br>
                                        <c:out value="${addr.city}"/>
                                        <c:if test="${not empty addr.state}">
                                            , <c:out value="${addr.state}"/>
                                        </c:if>
                                        - <c:out value="${addr.pincode}"/><br>
                                        📞 <c:out value="${addr.phone}"/>
                                    </p>
                                </div>

                                <div style="display:flex;gap:8px;flex-wrap:wrap;">
                                    <c:if test="${not addr.defaultAddress}">
                                        <form method="post"
                                              action="${pageContext.request.contextPath}/addresses">
                                            <input type="hidden" name="csrfToken"
                                                   value="${sessionScope.csrfToken}">
                                            <input type="hidden" name="action" value="default">
                                            <input type="hidden" name="addressId"
                                                   value="${addr.addressId}">
                                            <button type="submit" class="btn btn-sm">
                                                Make default
                                            </button>
                                        </form>
                                    </c:if>

                                    <form method="post"
                                          action="${pageContext.request.contextPath}/addresses"
                                          onsubmit="return confirm('Remove this address?');">
                                        <input type="hidden" name="csrfToken"
                                               value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="action" value="delete">
                                        <input type="hidden" name="addressId"
                                               value="${addr.addressId}">
                                        <button type="submit" class="btn btn-sm btn-danger">
                                            Remove
                                        </button>
                                    </form>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
        </div>

        <!-- ============ add address ============ -->
        <form class="panel" method="post"
              action="${pageContext.request.contextPath}/addresses">
            <input type="hidden" name="csrfToken"
                   value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="add">

            <h2>Add a new address</h2>
            <p class="sub">All fields are required.</p>

            <div class="field">
                <label for="label">Label</label>
                <input id="label" name="label" type="text" maxlength="40"
                       placeholder="Home, Office…" value="Home">
            </div>

            <div class="field">
                <label for="fullName">Recipient full name</label>
                <input id="fullName" name="fullName" type="text"
                       maxlength="100" required
                       value="<c:out value='${sessionScope.userName}'/>">
            </div>

            <div class="field">
                <label for="phone">Phone (10 digits)</label>
                <input id="phone" name="phone" type="tel" maxlength="10"
                       pattern="[0-9]{10}" required
                       value="<c:out value='${sessionScope.user.phone}'/>">
            </div>

            <div class="field">
                <label for="addressLine1">Street address</label>
                <textarea id="addressLine1" name="addressLine1" maxlength="300"
                          required
                          placeholder="House / flat, street, area"></textarea>
            </div>

            <div class="field">
                <label for="addressLine2">Landmark (optional)</label>
                <input id="addressLine2" name="addressLine2" type="text"
                       maxlength="300">
            </div>

            <div class="field-row">
                <div class="field">
                    <label for="city">City</label>
                    <input id="city" name="city" type="text" maxlength="100"
                           required>
                </div>

                <div class="field">
                    <label for="state">State</label>
                    <input id="state" name="state" type="text" maxlength="100">
                </div>
            </div>

            <div class="field">
                <label for="pincode">Pincode (6 digits)</label>
                <input id="pincode" name="pincode" type="text" maxlength="6"
                       pattern="[0-9]{6}" required>
            </div>

            <button type="submit" class="primary-btn full-btn">
                Save address
            </button>
        </form>
    </div>
</main>

<%@ include file="partials/footer.jspf" %>
</body>
</html>
