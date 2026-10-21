<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Your Cart | ShopSphere</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">

    <style>
        .cart-page {
            max-width: 1100px;
            margin: 45px auto;
            padding: 0 24px;
        }

        .cart-page h1 {
            color: #12372a;
            margin-bottom: 6px;
        }

        .cart-intro { color: #6b776f; margin-bottom: 25px; }

        .cart-layout {
            display: grid;
            grid-template-columns: 1.7fr 1fr;
            gap: 25px;
            align-items: start;
        }

        .cart-lines {
            border: 1px solid #e5ebe6;
            border-radius: 16px;
            overflow: hidden;
            background: white;
        }

        .cart-line {
            display: grid;
            grid-template-columns: 84px 1fr auto;
            gap: 16px;
            padding: 16px;
            border-bottom: 1px solid #eef2ee;
            align-items: center;
        }

        .cart-line:last-child { border-bottom: 0; }

        .cart-line img {
            width: 84px;
            height: 70px;
            object-fit: cover;
            border-radius: 10px;
            background: #f6f8f6;
        }

        .cart-line .no-img {
            width: 84px;
            height: 70px;
            display: grid;
            place-items: center;
            background: #f6f8f6;
            border-radius: 10px;
            font-size: 11px;
            color: #6b776f;
            text-align: center;
        }

        .line-name {
            font-weight: 700;
            color: #12372a;
            font-size: 15px;
        }

        .line-name:hover { color: #22a06b; }

        .line-meta {
            color: #6b776f;
            font-size: 13px;
            margin-top: 3px;
        }

        .line-stock-low { color: #b42318; }

        .qty-form {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-top: 10px;
        }

        .qty-form input {
            width: 62px;
            padding: 7px 8px;
            border: 1px solid #dce5de;
            border-radius: 8px;
            font: inherit;
            text-align: center;
        }

        .qty-form button {
            padding: 7px 11px;
            border: 1px solid #dce5de;
            background: #f6f8f6;
            border-radius: 8px;
            cursor: pointer;
            font-size: 13px;
        }

        .qty-form button:hover {
            border-color: #22a06b;
            color: #168052;
        }

        .remove-btn {
            background: none;
            border: 0;
            color: #b42318;
            font-size: 13px;
            cursor: pointer;
            padding: 6px 0;
            text-decoration: underline;
        }

        .line-total {
            font-weight: 800;
            color: #12372a;
            font-size: 16px;
            text-align: right;
        }

        .summary-card {
            border: 1px solid #e5ebe6;
            border-radius: 16px;
            padding: 22px;
            background: #fafbf9;
        }

        .summary-card h2 {
            font-size: 17px;
            color: #12372a;
            margin-bottom: 15px;
        }

        .summary-row {
            display: flex;
            justify-content: space-between;
            padding: 8px 0;
            font-size: 14px;
            color: #6b776f;
        }

        .summary-row.total {
            border-top: 1px solid #e5ebe6;
            margin-top: 8px;
            padding-top: 14px;
            font-size: 18px;
            font-weight: 800;
            color: #12372a;
        }

        .summary-card .primary-btn {
            width: 100%;
            margin-top: 16px;
        }

        .summary-note {
            font-size: 12px;
            color: #6b776f;
            margin-top: 12px;
            text-align: center;
        }

        .empty-cart {
            padding: 55px 25px;
            text-align: center;
            background: #f6f8f6;
            border-radius: 16px;
        }

        .empty-cart .big { font-size: 44px; }
        .empty-cart h2 { color: #12372a; margin: 10px 0 6px; }
        .empty-cart p { color: #6b776f; margin-bottom: 20px; }

        @media (max-width: 800px) {
            .cart-layout { grid-template-columns: 1fr; }
            .cart-line {
                grid-template-columns: 70px 1fr;
            }
            .line-total {
                grid-column: 2;
                text-align: left;
            }
        }
    </style>
</head>

<body>
<%@ include file="partials/header.jspf" %>

<main class="cart-page">
    <p class="eyebrow">SHOPPING CART</p>
    <h1>Your cart</h1>
    <p class="cart-intro">Review your items, adjust quantities, then check out.</p>

    <%@ include file="partials/alerts.jspf" %>

    <c:choose>
        <c:when test="${empty items}">
            <div class="empty-cart">
                <div class="big">🛒</div>
                <h2>Your cart is empty</h2>
                <p>Browse the catalog and add something you love.</p>
                <a class="primary-btn"
                   href="${pageContext.request.contextPath}/products">
                    Explore products →
                </a>
            </div>
        </c:when>

        <c:otherwise>
            <div class="cart-layout">
                <div class="cart-lines">
                    <c:forEach var="item" items="${items}">
                        <div class="cart-line">
                            <img src="<c:out value='${item.product.resolveImage(pageContext.request.contextPath)}'/>"
                                 alt="<c:out value='${item.product.name}'/>">

                            <div>
                                <a class="line-name"
                                   href="${pageContext.request.contextPath}/product?id=${item.productId}">
                                    <c:out value="${item.product.name}"/>
                                </a>

                                <div class="line-meta">
                                    &#8377;<fmt:formatNumber value="${item.product.price}"
                                                             pattern="#,##0.00"/> each
                                    &nbsp;·&nbsp; <c:out value="${item.product.category}"/>
                                </div>

                                <div class="line-meta <c:if test='${item.product.stock lt 5}'>line-stock-low</c:if>">
                                    <c:choose>
                                        <c:when test="${item.product.stock ge 5}">
                                            In stock
                                        </c:when>
                                        <c:when test="${item.product.stock ge item.quantity}">
                                            Only ${item.product.stock} left
                                        </c:when>
                                        <c:otherwise>
                                            Only ${item.product.stock} left — please reduce the quantity
                                        </c:otherwise>
                                    </c:choose>
                                </div>

                                <div class="qty-form">
                                    <form method="post"
                                          action="${pageContext.request.contextPath}/cart"
                                          class="qty-form">
                                        <input type="hidden" name="csrfToken"
                                               value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="action" value="update">
                                        <input type="hidden" name="productId" value="${item.productId}">

                                        <label for="qty-${item.productId}"
                                               class="line-meta">Qty</label>

                                        <input id="qty-${item.productId}" type="number"
                                               name="quantity" min="1"
                                               max="${item.product.stock}"
                                               value="${item.quantity}">

                                        <button type="submit">Update</button>
                                    </form>

                                    <form method="post"
                                          action="${pageContext.request.contextPath}/cart">
                                        <input type="hidden" name="csrfToken"
                                               value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="action" value="remove">
                                        <input type="hidden" name="productId" value="${item.productId}">
                                        <button type="submit" class="remove-btn">Remove</button>
                                    </form>
                                </div>
                            </div>

                            <div class="line-total">
                                &#8377;<fmt:formatNumber value="${item.lineTotal}"
                                                         pattern="#,##0.00"/>
                            </div>
                        </div>
                    </c:forEach>
                </div>

                <aside class="summary-card">
                    <h2>Order summary</h2>

                    <div class="summary-row">
                        <span>Items (${sessionScope.cartCount})</span>
                        <span>
                            &#8377;<fmt:formatNumber
                                value="${subtotal}" pattern="#,##0.00"/>
                        </span>
                    </div>

                    <div class="summary-row">
                        <span>Delivery</span>
                        <span style="color:#168052;font-weight:700;">FREE</span>
                    </div>

                    <div class="summary-row total">
                        <span>Total</span>
                        <span>
                            &#8377;<fmt:formatNumber
                                value="${subtotal}" pattern="#,##0.00"/>
                        </span>
                    </div>

                    <a class="primary-btn"
                       href="${pageContext.request.contextPath}/checkout">
                        Proceed to Checkout →
                    </a>

                    <a class="secondary-btn"
                       style="width:100%;margin-top:10px;text-align:center;display:block;"
                       href="${pageContext.request.contextPath}/products">
                        Continue shopping
                    </a>

                    <p class="summary-note">
                        Secure checkout · Cash on delivery, Card or UPI
                    </p>
                </aside>
            </div>
        </c:otherwise>
    </c:choose>
</main>

<%@ include file="partials/footer.jspf" %>
</body>
</html>
