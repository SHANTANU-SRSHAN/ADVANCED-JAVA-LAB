<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Wishlist | ShopSphere</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">

    <style>
        .wishlist-page {
            max-width: 1100px;
            margin: 45px auto;
            padding: 0 24px;
        }

        .wishlist-page h1 { color: #12372a; margin-bottom: 6px; }
        .wishlist-intro { color: #6b776f; margin-bottom: 25px; }

        .empty-state {
            padding: 55px 25px;
            text-align: center;
            background: #f6f8f6;
            border-radius: 16px;
        }
        .empty-state .big { font-size: 44px; }
        .empty-state h2 { color: #12372a; margin: 10px 0 6px; }
        .empty-state p { color: #6b776f; margin-bottom: 20px; }

        .card-actions {
            display: flex;
            gap: 8px;
            margin-top: 12px;
        }

        .card-actions form { flex: 1; }

        .btn-add, .btn-remove {
            width: 100%;
            padding: 9px 10px;
            border-radius: 9px;
            font-size: 13px;
            font-weight: 700;
            cursor: pointer;
        }

        .btn-add {
            border: 0;
            background: #12372a;
            color: white;
        }
        .btn-add:hover { background: #22a06b; }

        .btn-remove {
            border: 1px solid #f3c9c5;
            background: #fff6f5;
            color: #b42318;
        }
        .btn-remove:hover { background: #ffecec; }
    </style>
</head>

<body>
<%@ include file="partials/header.jspf" %>

<main class="wishlist-page">
    <p class="eyebrow">SAVED FOR LATER</p>
    <h1>My wishlist</h1>
    <p class="wishlist-intro">Products you saved — move them to your cart when you are ready.</p>

    <%@ include file="partials/alerts.jspf" %>

    <c:choose>
        <c:when test="${empty products}">
            <div class="empty-state">
                <div class="big">💚</div>
                <h2>Your wishlist is empty</h2>
                <p>Tap the heart on any product to save it here.</p>
                <a class="primary-btn"
                   href="${pageContext.request.contextPath}/products">
                    Browse products →
                </a>
            </div>
        </c:when>

        <c:otherwise>
            <div class="product-grid">
                <c:forEach var="product" items="${products}">
                    <article class="product-card">
                        <a href="${pageContext.request.contextPath}/product?id=${product.productId}">
                            <div class="product-image">
                                <img src="<c:out value='${product.resolveImage(pageContext.request.contextPath)}'/>"
                                     alt="<c:out value='${product.name}'/>"
                                     loading="lazy">
                            </div>

                            <div class="product-info">
                                <p class="product-category">
                                    <c:out value="${product.category}"/>
                                </p>
                                <h3><c:out value="${product.name}"/></h3>
                                <p class="product-price">
                                    &#8377;<fmt:formatNumber value="${product.price}"
                                                             pattern="#,##0.00"/>
                                </p>
                                <p class="product-stock">
                                    <c:choose>
                                        <c:when test="${product.stock gt 0}">
                                            In stock: ${product.stock}
                                        </c:when>
                                        <c:otherwise>Out of stock</c:otherwise>
                                    </c:choose>
                                </p>
                            </div>
                        </a>

                        <div class="product-info" style="padding-top:0;">
                            <div class="card-actions">
                                <form method="post"
                                      action="${pageContext.request.contextPath}/wishlist">
                                    <input type="hidden" name="csrfToken"
                                           value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="moveToCart">
                                    <input type="hidden" name="productId" value="${product.productId}">
                                    <input type="hidden" name="returnUrl"
                                           value="${pageContext.request.contextPath}/wishlist">
                                    <button type="submit" class="btn-add"
                                            <c:if test="${product.stock le 0}">disabled</c:if>>
                                        🛒 Move to cart
                                    </button>
                                </form>

                                <form method="post"
                                      action="${pageContext.request.contextPath}/wishlist">
                                    <input type="hidden" name="csrfToken"
                                           value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="toggle">
                                    <input type="hidden" name="productId" value="${product.productId}">
                                    <input type="hidden" name="returnUrl"
                                           value="${pageContext.request.contextPath}/wishlist">
                                    <button type="submit" class="btn-remove">
                                        ✖ Remove
                                    </button>
                                </form>
                            </div>
                        </div>
                    </article>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</main>

<%@ include file="partials/footer.jspf" %>
</body>
</html>
