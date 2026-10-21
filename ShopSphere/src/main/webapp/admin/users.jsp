<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin · Customers | ShopSphere</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">
</head>

<body>
<%@ include file="../partials/header.jspf" %>
<%@ include file="../partials/admin-nav.jspf" %>

<main class="page-wrap">
    <p class="eyebrow">ADMIN</p>
    <h1>Customers</h1>
    <p class="page-sub">
        ${customerCount} customer account(s) and every administrator.
    </p>

    <c:if test="${not empty param.error}">
        <div class="alert alert-error">
            ⚠ <c:out value="${param.error}"/>
        </div>
    </c:if>

    <c:if test="${param.updated eq '1'}">
        <div class="alert alert-success">✅ Account updated.</div>
    </c:if>

    <div class="panel" style="padding:0;overflow:hidden;">
        <table class="data-table">
            <thead>
                <tr>
                    <th>Customer</th>
                    <th>E-mail</th>
                    <th>Phone</th>
                    <th>Role</th>
                    <th>Verified</th>
                    <th>Last sign-in</th>
                    <th style="text-align:right;">Actions</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="u" items="${users}">
                    <tr>
                        <td>
                            <strong><c:out value="${u.fullName}"/></strong>
                            <div style="font-size:11px;color:var(--muted);">
                                #${u.userId}
                            </div>
                        </td>
                        <td style="font-size:12px;">
                            <c:out value="${u.email}"/>
                        </td>
                        <td style="font-size:12px;">
                            <c:out value="${u.phone}"/>
                        </td>
                        <td>
                            <span class="pill ${u.role eq 'ADMIN' ? 'PAID' : 'CONFIRMED'}">
                                <c:out value="${u.role}"/>
                            </span>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${u.emailVerified}">
                                    <span class="pill PAID">Yes</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="pill PENDING">No</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td style="font-size:12px;color:var(--muted);">
                            <c:choose>
                                <c:when test="${not empty u.lastLoginAt}">
                                    <c:out value="${u.lastLoginAt.toLocalDate()}"/>
                                </c:when>
                                <c:otherwise>Never</c:otherwise>
                            </c:choose>
                        </td>
                        <td style="text-align:right;white-space:nowrap;">
                            <form method="post" style="display:inline;"
                                  action="${pageContext.request.contextPath}/admin/users">
                                <input type="hidden" name="csrfToken"
                                       value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="role">
                                <input type="hidden" name="userId" value="${u.userId}">
                                <input type="hidden" name="role"
                                       value="${u.role eq 'ADMIN' ? 'CUSTOMER' : 'ADMIN'}">
                                <button type="submit" class="btn btn-sm">
                                    <c:choose>
                                        <c:when test="${u.role eq 'ADMIN'}">
                                            Make customer
                                        </c:when>
                                        <c:otherwise>
                                            Make admin
                                        </c:otherwise>
                                    </c:choose>
                                </button>
                            </form>

                            <c:if test="${u.lockedUntil != null}">
                                <form method="post" style="display:inline;"
                                      action="${pageContext.request.contextPath}/admin/users">
                                    <input type="hidden" name="csrfToken"
                                           value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="unlock">
                                    <input type="hidden" name="userId" value="${u.userId}">
                                    <button type="submit" class="btn btn-sm btn-danger">
                                        🔓 Unlock
                                    </button>
                                </form>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>

                <c:if test="${empty users}">
                    <tr>
                        <td colspan="7" style="color:var(--muted);padding:25px;">
                            There are no accounts yet.
                        </td>
                    </tr>
                </c:if>
            </tbody>
        </table>
    </div>
</main>

<%@ include file="../partials/footer.jspf" %>
</body>
</html>
