<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Dashboard | ShopSphere</title>
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

        .stat-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
            gap: 16px;
            margin-bottom: 30px;
        }
        .stat-card {
            border: 1px solid #e5ebe6;
            border-radius: 16px;
            padding: 20px;
            background: #fafbf9;
        }
        .stat-card .icon { font-size: 22px; }
        .stat-card .num {
            font-size: 27px;
            font-weight: 800;
            color: #12372a;
            margin-top: 8px;
        }
        .stat-card .label {
            font-size: 12px;
            color: #6b776f;
            text-transform: uppercase;
            letter-spacing: .6px;
            font-weight: 700;
        }

        .panel {
            border: 1px solid #e5ebe6;
            border-radius: 16px;
            overflow: hidden;
            margin-bottom: 25px;
            background: white;
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

        table.data {
            width: 100%;
            border-collapse: collapse;
        }
        table.data th, table.data td {
            padding: 12px 16px;
            text-align: left;
            font-size: 14px;
            border-bottom: 1px solid #eef2ee;
        }
        table.data th {
            background: #fafbf9;
            color: #6b776f;
            font-size: 12px;
            text-transform: uppercase;
            letter-spacing: .5px;
        }
        table.data tr:last-child td { border-bottom: 0; }
        table.data tr:hover td { background: #fcfdfc; }

        .pill {
            padding: 4px 10px;
            border-radius: 999px;
            font-size: 11px;
            font-weight: 800;
        }
        .pill.PLACED    { background: #fff4e0; color: #8a5a00; }
        .pill.CONFIRMED { background: #e6f1ff; color: #175cd3; }
        .pill.SHIPPED   { background: #ede9ff; color: #5925dc; }
        .pill.DELIVERED { background: #e6f7ee; color: #168052; }
        .pill.CANCELLED { background: #ffecec; color: #b42318; }
        .pill.PAID      { background: #e6f7ee; color: #168052; }
        .pill.PENDING   { background: #f2f4f2; color: #6b776f; }

        .quick-links {
            display: flex;
            gap: 12px;
            flex-wrap: wrap;
        }
        .quick-links a {
            padding: 11px 16px;
            background: #12372a;
            color: white;
            border-radius: 10px;
            font-size: 13px;
            font-weight: 700;
        }
        .quick-links a:hover { background: #22a06b; }
    </style>
</head>

<body>
<%@ include file="../partials/header.jspf" %>
<%@ include file="../partials/admin-nav.jspf" %>

<main class="admin-main">
    <p class="eyebrow">ADMIN</p>
    <h1>Dashboard</h1>
    <p class="admin-sub">Store performance at a glance.</p>

    <div class="stat-grid">
        <div class="stat-card">
            <div class="icon">💰</div>
            <div class="num">&#8377;<fmt:formatNumber
                    value="${stats.revenue}" pattern="#,##0.00"/></div>
            <div class="label">Revenue (paid)</div>
        </div>

        <div class="stat-card">
            <div class="icon">🧾</div>
            <div class="num">${stats.orders}</div>
            <div class="label">Total orders</div>
        </div>

        <div class="stat-card">
            <div class="icon">👥</div>
            <div class="num">${stats.customers}</div>
            <div class="label">Customers</div>
        </div>

        <div class="stat-card">
            <div class="icon">📦</div>
            <div class="num">${stats.products}</div>
            <div class="label">Products</div>
        </div>

        <div class="stat-card">
            <div class="icon">✅</div>
            <div class="num">${stats.delivered}</div>
            <div class="label">Delivered</div>
        </div>

        <div class="stat-card">
            <div class="icon">🗂</div>
            <div class="num">${stats.stock}</div>
            <div class="label">Units in stock</div>
        </div>
    </div>

    <div class="panel">
        <div class="panel-head">
            <h2>Low stock</h2>
            <a href="${pageContext.request.contextPath}/admin/products"
               style="color:#168052;font-weight:700;font-size:13px;">
                Manage inventory →
            </a>
        </div>

        <c:choose>
            <c:when test="${empty lowStock}">
                <p style="padding:20px;color:#6b776f;">
                    Every product is well stocked.
                </p>
            </c:when>
            <c:otherwise>
                <table class="data">
                    <thead>
                    <tr>
                        <th>Product</th>
                        <th>Category</th>
                        <th>Stock left</th>
                        <th>Price</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="p" items="${lowStock}">
                        <tr>
                            <td>
                                <a href="${pageContext.request.contextPath}/product?id=${p.productId}"
                                   style="color:#168052;font-weight:700;">
                                    <c:out value="${p.name}"/>
                                </a>
                            </td>
                            <td><c:out value="${p.category}"/></td>
                            <td><span class="pill"
                                      style="background:#fff3cd;color:#7a5c00;font-weight:700;">
                                ${p.stock} left
                            </span></td>
                            <td>&#8377;<fmt:formatNumber value="${p.price}"
                                                         pattern="#,##0.00"/></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </div>

    <div class="panel">
        <div class="panel-head">
            <h2>Newest orders</h2>
            <a href="${pageContext.request.contextPath}/admin/orders"
               style="color:#168052;font-weight:700;font-size:13px;">
                View all orders →
            </a>
        </div>

        <c:choose>
            <c:when test="${empty recentOrders}">
                <p style="padding:20px;color:#6b776f;">No orders yet.</p>
            </c:when>
            <c:otherwise>
                <table class="data">
                    <thead>
                    <tr>
                        <th>Order</th>
                        <th>Customer</th>
                        <th>Total</th>
                        <th>Payment</th>
                        <th>Status</th>
                        <th>Date</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="o" items="${recentOrders}">
                        <tr>
                            <td>
                                <a href="${pageContext.request.contextPath}/order?id=${o.orderId}"
                                   style="color:#168052;font-weight:700;">
                                    #${o.orderId}
                                </a>
                            </td>
                            <td>
                                <c:out value="${o.customerName}"/>
                                <br><span style="color:#6b776f;font-size:12px;">
                                    <c:out value="${o.customerEmail}"/>
                                </span>
                            </td>
                            <td>&#8377;<fmt:formatNumber value="${o.totalAmount}"
                                                         pattern="#,##0.00"/></td>
                            <td>
                                <c:out value="${o.paymentMethod}"/>
                                <span class="pill ${o.paymentStatus}">${o.paymentStatus}</span>
                            </td>
                            <td><span class="pill ${o.orderStatus}">${o.orderStatus}</span></td>
                            <td><c:if test="${not empty o.createdAt}">${o.createdAt.toLocalDate()}</c:if></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </div>

    <div class="quick-links">
        <a href="${pageContext.request.contextPath}/admin/products">Manage products</a>
        <a href="${pageContext.request.contextPath}/admin/orders">Manage orders</a>
    </div>
</main>

<%@ include file="../partials/footer.jspf" %>
</body>
</html>
