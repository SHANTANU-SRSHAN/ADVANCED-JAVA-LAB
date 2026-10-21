<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin · Orders | ShopSphere</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">
    <style>
        .admin-main {
            max-width: 1150px;
            margin: 35px auto 55px;
            padding: 0 24px;
        }
        .admin-main h1 { color: #12372a; margin-bottom: 4px; }
        .admin-sub { color: #6b776f; margin-bottom: 25px; font-size: 14px; }

        .panel {
            border: 1px solid #e5ebe6;
            border-radius: 16px;
            background: white;
            overflow: hidden;
        }
        .panel-head {
            padding: 15px 18px;
            border-bottom: 1px solid #eef2ee;
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 12px;
            flex-wrap: wrap;
        }
        .panel-head h2 { font-size: 16px; color: #12372a; }

        .filters { display: flex; gap: 8px; flex-wrap: wrap; }
        .filters a {
            padding: 7px 13px;
            border: 1px solid #dce5de;
            border-radius: 999px;
            font-size: 12px;
            font-weight: 700;
            color: #4a574f;
        }
        .filters a:hover { border-color: #22a06b; color: #168052; }
        .filters a.on {
            background: #12372a;
            border-color: #12372a;
            color: white;
        }

        table.data { width: 100%; border-collapse: collapse; }
        table.data th, table.data td {
            padding: 12px 14px; text-align: left; font-size: 13px;
            border-bottom: 1px solid #eef2ee; vertical-align: middle;
        }
        table.data th {
            background: #fafbf9; color: #6b776f; font-size: 11px;
            text-transform: uppercase; letter-spacing: .5px;
        }
        table.data tr:last-child td { border-bottom: 0; }
        table.data tr:hover td { background: #fcfdfc; }

        .pill {
            padding: 4px 10px; border-radius: 999px;
            font-size: 11px; font-weight: 800; display: inline-block;
        }
        .pill.PLACED    { background: #fff4e0; color: #8a5a00; }
        .pill.CONFIRMED { background: #e6f1ff; color: #175cd3; }
        .pill.SHIPPED   { background: #ede9ff; color: #5925dc; }
        .pill.DELIVERED { background: #e6f7ee; color: #168052; }
        .pill.CANCELLED { background: #ffecec; color: #b42318; }
        .pill.PAID      { background: #e6f7ee; color: #168052; }
        .pill.PENDING   { background: #f2f4f2; color: #6b776f; }

        .status-form { display: flex; gap: 6px; align-items: center; }
        .status-form select {
            padding: 7px 8px; border: 1px solid #dce5de; border-radius: 8px;
            font-size: 12px; font-family: inherit; background: white;
        }
        .status-form button {
            padding: 7px 12px; border: 0; border-radius: 8px;
            background: #22a06b; color: white; font-size: 12px;
            font-weight: 700; cursor: pointer;
        }
        .status-form button:hover { background: #168052; }

        .alert-ok {
            background: #f0faf4; border: 1px solid #c9ead6; color: #168052;
            padding: 10px 14px; border-radius: 9px; font-size: 14px;
            margin-bottom: 16px;
        }
        .alert-bad {
            background: #fff1f1; border: 1px solid #ffc9c9; color: #b42318;
            padding: 10px 14px; border-radius: 9px; font-size: 14px;
            margin-bottom: 16px;
        }

        .cust { color: #12372a; font-weight: 700; }
        .muted { color: #6b776f; font-size: 12px; }

        @media (max-width: 820px) {
            table.data { display: block; overflow-x: auto; }
        }
    </style>
</head>

<body>
<%@ include file="../partials/header.jspf" %>
<%@ include file="../partials/admin-nav.jspf" %>

<main class="admin-main">
    <p class="eyebrow">ADMIN</p>
    <h1>Orders</h1>
    <p class="admin-sub">Move orders through PLACED → CONFIRMED → SHIPPED → DELIVERED.</p>

    <c:if test="${param.updated eq '1'}">
        <div class="alert-ok">✅ Order status updated.</div>
    </c:if>
    <c:if test="${not empty param.error}">
        <div class="alert-bad">${param.error}</div>
    </c:if>

    <div class="panel">
        <div class="panel-head">
            <h2>All orders (${fn:length(orders)})</h2>

            <div class="filters">
                <c:forEach var="s"
                           items="ALL,PLACED,CONFIRMED,SHIPPED,DELIVERED,CANCELLED">
                    <a href="${pageContext.request.contextPath}/admin/orders?status=${s}"
                       class="${statusFilter eq s ? 'on' : ''}">${s}</a>
                </c:forEach>
            </div>
        </div>

        <c:choose>
            <c:when test="${empty orders}">
                <p style="padding:25px;color:#6b776f;">No orders in this view.</p>
            </c:when>
            <c:otherwise>
                <table class="data">
                    <thead>
                    <tr>
                        <th>Order</th>
                        <th>Customer</th>
                        <th>Items</th>
                        <th>Total</th>
                        <th>Payment</th>
                        <th>Status</th>
                        <th>Update status</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="o" items="${orders}">
                        <tr>
                            <td>
                                <a href="${pageContext.request.contextPath}/order?id=${o.orderId}"
                                   style="color:#168052;font-weight:700;">
                                    #${o.orderId}
                                </a>
                                <div class="muted">
                                    <c:if test="${not empty o.createdAt}">
                                        ${o.createdAt.toLocalDate()}
                                    </c:if>
                                </div>
                            </td>
                            <td>
                                <span class="cust">
                                    <c:out value="${o.customerName}"/>
                                </span>
                                <div class="muted">
                                    <c:out value="${o.customerEmail}"/>
                                </div>
                                <div class="muted">
                                    📞 <c:out value="${o.phone}"/>
                                </div>
                            </td>
                            <td>${fn:length(o.items)}</td>
                            <td style="font-weight:800;color:#12372a;">
                                &#8377;<fmt:formatNumber value="${o.totalAmount}"
                                                         pattern="#,##0.00"/>
                            </td>
                            <td>
                                <c:out value="${o.paymentMethod}"/>
                                <div class="muted">${o.paymentStatus}</div>
                            </td>
                            <td>
                                <span class="pill ${o.orderStatus}">${o.orderStatus}</span>
                            </td>
                            <td>
                                <form class="status-form" method="post"
                                      action="${pageContext.request.contextPath}/admin/orders">
                                    <input type="hidden" name="csrfToken"
                                           value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="status">
                                    <input type="hidden" name="orderId" value="${o.orderId}">

                                    <select name="status">
                                        <c:forEach var="s"
                                                   items="PLACED,CONFIRMED,PACKED,SHIPPED,DELIVERED,CANCELLED">
                                            <option value="${s}"
                                                <c:if test="${o.orderStatus eq s}">selected</c:if>>
                                                ${s}
                                            </option>
                                        </c:forEach>
                                    </select>

                                    <button type="submit">Save</button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </div>
</main>

<%@ include file="../partials/footer.jspf" %>
</body>
</html>
