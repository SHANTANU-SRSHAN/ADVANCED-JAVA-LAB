<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin · Coupons | ShopSphere</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">
</head>

<body>
<%@ include file="../partials/header.jspf" %>
<%@ include file="../partials/admin-nav.jspf" %>

<main class="page-wrap">
    <p class="eyebrow">ADMIN</p>
    <h1>Coupons</h1>
    <p class="page-sub">Discount codes customers can use at checkout.</p>

    <c:if test="${not empty param.error}">
        <div class="alert alert-error">
            ⚠ <c:out value="${param.error}"/>
        </div>
    </c:if>

    <c:if test="${param.created eq '1'}">
        <div class="alert alert-success">✅ Coupon created.</div>
    </c:if>

    <c:if test="${param.updated eq '1'}">
        <div class="alert alert-success">✅ Coupon updated.</div>
    </c:if>

    <c:if test="${param.deleted eq '1'}">
        <div class="alert alert-success">✅ Coupon deleted.</div>
    </c:if>

    <div class="panel-grid" style="grid-template-columns:1.4fr 1fr;">

        <!-- ============ list ============ -->
        <div class="panel" style="padding:0;overflow:hidden;">
            <table class="data-table">
                <thead>
                    <tr>
                        <th>Code</th>
                        <th>Discount</th>
                        <th>Min order</th>
                        <th>Used</th>
                        <th>Status</th>
                        <th style="text-align:right;">Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="coupon" items="${coupons}">
                        <tr>
                            <td>
                                <strong><c:out value="${coupon.code}"/></strong>
                                <div style="font-size:11px;color:var(--muted);">
                                    <c:out value="${coupon.description}"/>
                                </div>
                            </td>
                            <td style="font-size:12px;">
                                <c:choose>
                                    <c:when test="${coupon.percent}">
                                        <c:out value="${coupon.discountValue}"/>%
                                        <c:if test="${coupon.maxDiscount != null}">
                                            <div style="font-size:11px;color:var(--muted);">
                                                max &#8377;<c:out value="${coupon.maxDiscount}"/>
                                            </div>
                                        </c:if>
                                    </c:when>
                                    <c:otherwise>
                                        &#8377;<c:out value="${coupon.discountValue}"/>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td style="font-size:12px;">
                                &#8377;<fmt:formatNumber value="${coupon.minOrderAmount}"
                                                         pattern="#,##0.00"/>
                            </td>
                            <td style="font-size:12px;">
                                ${coupon.usedCount}
                                <c:if test="${coupon.usageLimit != null}">
                                    / ${coupon.usageLimit}
                                </c:if>
                                <div style="font-size:11px;color:var(--muted);">
                                    ${coupon.perUserLimit} per customer
                                </div>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${coupon.active}">
                                        <span class="pill PAID">Active</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="pill CANCELLED">Off</span>
                                    </c:otherwise>
                                </c:choose>
                                <c:if test="${not coupon.currentlyValid}">
                                    <div style="font-size:11px;color:#b42318;">
                                        expired / used up
                                    </div>
                                </c:if>
                            </td>
                            <td style="text-align:right;white-space:nowrap;">
                                <form method="post" style="display:inline;"
                                      action="${pageContext.request.contextPath}/admin/coupons">
                                    <input type="hidden" name="csrfToken"
                                           value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="toggle">
                                    <input type="hidden" name="couponId"
                                           value="${coupon.couponId}">
                                    <button type="submit" class="btn btn-sm">
                                        <c:choose>
                                            <c:when test="${coupon.active}">Disable</c:when>
                                            <c:otherwise>Enable</c:otherwise>
                                        </c:choose>
                                    </button>
                                </form>

                                <form method="post" style="display:inline;"
                                      action="${pageContext.request.contextPath}/admin/coupons"
                                      onsubmit="return confirm('Delete this coupon?');">
                                    <input type="hidden" name="csrfToken"
                                           value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="couponId"
                                           value="${coupon.couponId}">
                                    <button type="submit" class="btn btn-sm btn-danger">
                                        Delete
                                    </button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>

                    <c:if test="${empty coupons}">
                        <tr>
                            <td colspan="6" style="color:var(--muted);padding:25px;">
                                No coupons yet.
                            </td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </div>

        <!-- ============ create ============ -->
        <form class="panel" method="post"
              action="${pageContext.request.contextPath}/admin/coupons">
            <input type="hidden" name="csrfToken"
                   value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="add">

            <h2>Create a coupon</h2>
            <p class="sub">Give customers a discount code.</p>

            <div class="field">
                <label for="code">Code</label>
                <input id="code" name="code" type="text" maxlength="50"
                       placeholder="SUMMER20" required
                       style="text-transform:uppercase;">
            </div>

            <div class="field">
                <label for="description">Description</label>
                <input id="description" name="description" type="text"
                       maxlength="200" placeholder="20% off for the summer">
            </div>

            <div class="field">
                <label for="discountType">Discount type</label>
                <select id="discountType" name="discountType">
                    <option value="PERCENT">Percentage (%)</option>
                    <option value="FIXED">Fixed amount (₹)</option>
                </select>
            </div>

            <div class="field-row">
                <div class="field">
                    <label for="discountValue">Discount value</label>
                    <input id="discountValue" name="discountValue" type="number"
                           step="0.01" min="0" required value="10">
                </div>

                <div class="field">
                    <label for="maxDiscount">Max discount (₹)</label>
                    <input id="maxDiscount" name="maxDiscount" type="number"
                           step="0.01" min="0" placeholder="optional">
                </div>
            </div>

            <div class="field-row">
                <div class="field">
                    <label for="minOrderAmount">Min order (₹)</label>
                    <input id="minOrderAmount" name="minOrderAmount"
                           type="number" step="0.01" min="0" value="0">
                </div>

                <div class="field">
                    <label for="perUserLimit">Uses per customer</label>
                    <input id="perUserLimit" name="perUserLimit" type="number"
                           min="1" value="1">
                </div>
            </div>

            <div class="field-row">
                <div class="field">
                    <label for="usageLimit">Total usage limit</label>
                    <input id="usageLimit" name="usageLimit" type="number"
                           min="1" placeholder="unlimited">
                </div>

                <div class="field">
                    <label for="validTo">Valid until</label>
                    <input id="validTo" name="validTo" type="date">
                </div>
            </div>

            <button type="submit" class="primary-btn full-btn">
                Create coupon
            </button>
        </form>
    </div>
</main>

<%@ include file="../partials/footer.jspf" %>
</body>
</html>
