
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:out value="${product.name}"/> | ShopSphere</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">

    <style>
        .details-container {
            max-width: 1100px;
            margin: 45px auto;
            padding: 0 24px;
        }

        .breadcrumb {
            margin-bottom: 25px;
            color: #6b776f;
            font-size: 14px;
        }

        .breadcrumb a {
            color: #168052;
        }

        .details-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 45px;
            align-items: start;
        }

        .details-image {
            min-height: 340px;
            display: grid;
            place-items: center;
            background: #f6f8f6;
            border: 1px solid #e5ebe6;
            border-radius: 18px;
            overflow: hidden;
        }

        .details-image img {
            width: 100%;
            max-height: 480px;
            object-fit: contain;
        }

        .details-image .no-image {
            padding: 60px 20px;
            color: #6b776f;
        }

        .details-info h1 {
            margin: 12px 0;
            color: #12372a;
            font-size: clamp(28px, 4vw, 42px);
            line-height: 1.2;
        }

        .details-category {
            color: #168052;
            font-weight: 700;
        }

        .details-price {
            margin: 22px 0;
            color: #168052;
            font-size: 30px;
            font-weight: 800;
        }

        .details-description {
            color: #58665d;
            line-height: 1.8;
            white-space: pre-line;
        }

        .stock-status {
            margin: 20px 0;
            font-weight: 700;
        }

        .in-stock {
            color: #168052;
        }

        .out-of-stock {
            color: #b42318;
        }

        .back-link {
            display: inline-block;
            margin-top: 28px;
            color: #12372a;
            font-weight: 700;
        }

        .buy-form {
            margin-top: 22px;
            padding: 18px;
            border: 1px solid #e5ebe6;
            border-radius: 14px;
            background: #fafbf9;
        }

        .qty-label {
            display: block;
            font-size: 13px;
            font-weight: 700;
            color: #17251e;
            margin-bottom: 7px;
        }

        .qty-row {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 14px;
        }

        .qty-row input {
            width: 74px;
            padding: 10px;
            text-align: center;
            border: 1px solid #dce5de;
            border-radius: 9px;
            font: inherit;
        }

        .qty-step {
            width: 38px;
            height: 38px;
            border: 1px solid #dce5de;
            background: white;
            border-radius: 9px;
            font-size: 18px;
            cursor: pointer;
            color: #12372a;
        }

        .qty-step:hover { border-color: #22a06b; color: #168052; }

        .buy-buttons {
            display: flex;
            gap: 10px;
            flex-wrap: wrap;
        }

        .buy-main {
            flex: 1;
            min-width: 150px;
            border: 0;
            cursor: pointer;
        }

        .buy-main:disabled, .buy-now:disabled {
            background: #b9c4bc;
            cursor: not-allowed;
            transform: none;
        }

        .buy-now {
            flex: 1;
            min-width: 130px;
            cursor: pointer;
            background: white;
        }

        .wishlist-form { margin-top: 11px; }

        .wishlist-btn {
            width: 100%;
            padding: 12px;
            border: 1px solid #dce5de;
            background: white;
            border-radius: 10px;
            font-size: 14px;
            font-weight: 700;
            cursor: pointer;
            color: #12372a;
        }

        .wishlist-btn:hover { border-color: #ff7a90; color: #d6336c; }

        .login-box {
            margin-top: 22px;
            padding: 20px;
            border: 1px dashed #cfe0d4;
            border-radius: 14px;
            background: #f6fbf7;
            text-align: center;
        }

        .login-box p {
            color: #6b776f;
            font-size: 14px;
            margin-bottom: 14px;
        }

        @media (max-width: 700px) {
            .details-grid {
                grid-template-columns: 1fr;
                gap: 25px;
            }

            .details-image {
                min-height: 250px;
            }
        }
    </style>
</head>

<body>
<%@ include file="partials/header.jspf" %>

<main class="details-container">

    <p class="breadcrumb">
        <a href="${pageContext.request.contextPath}/home">Home</a>
        /
        <a href="${pageContext.request.contextPath}/products">Products</a>
        /
        <c:out value="${product.name}"/>
    </p>

    <%@ include file="partials/alerts.jspf" %>

    <div class="details-grid">

        <div class="details-image">
            <img
                src="<c:out value='${product.resolveImage(pageContext.request.contextPath)}'/>"
                alt="<c:out value='${product.name}'/>"
            >
        </div>

        <section class="details-info">
            <p class="details-category">
                <c:out value="${product.category}"/>
            </p>

            <h1><c:out value="${product.name}"/></h1>

            <p class="details-price">
                &#8377;<fmt:formatNumber value="${product.price}"
                                         pattern="#,##0.00"/>
            </p>

            <h3>Product description</h3>
            <p class="details-description">
                <c:choose>
                    <c:when test="${not empty product.description}">
                        <c:out value="${product.description}"/>
                    </c:when>
                    <c:otherwise>
                        No description is available for this product.
                    </c:otherwise>
                </c:choose>
            </p>

            <p class="stock-status">
                <c:choose>
                    <c:when test="${product.stock gt 0}">
                        <span class="in-stock">
                            In stock — <c:out value="${product.stock}"/> available
                        </span>
                    </c:when>
                    <c:otherwise>
                        <span class="out-of-stock">Currently out of stock</span>
                    </c:otherwise>
                </c:choose>
            </p>

            <%-- ------------------- Buy actions ------------------- --%>
            <c:choose>
                <c:when test="${not empty sessionScope.user}">
                    <form class="buy-form"
                          method="post"
                          action="${pageContext.request.contextPath}/cart"
                          id="addToCartForm">
                        <input type="hidden" name="csrfToken"
                               value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="add">
                        <input type="hidden" name="productId" value="${product.productId}">
                        <input type="hidden" name="returnUrl" value="${returnUrl}">

                        <label class="qty-label" for="quantity">Quantity</label>

                        <div class="qty-row">
                            <button type="button" class="qty-step"
                                    onclick="changeQty(-1)" aria-label="Decrease quantity">−</button>

                            <input id="quantity" name="quantity" type="number"
                                   min="1" max="${product.stock}" value="1"
                                   <c:if test="${product.stock le 0}">disabled</c:if>>

                            <button type="button" class="qty-step"
                                    onclick="changeQty(1)" aria-label="Increase quantity">+</button>
                        </div>

                        <div class="buy-buttons">
                            <button type="submit" class="primary-btn buy-main"
                                    <c:if test="${product.stock le 0}">disabled</c:if>>
                                🛒 Add to Cart
                            </button>

                            <button type="button" class="secondary-btn buy-now"
                                    <c:if test="${product.stock le 0}">disabled</c:if>
                                    onclick="buyNow()">
                                ⚡ Buy Now
                            </button>
                        </div>
                    </form>

                    <form method="post"
                          action="${pageContext.request.contextPath}/wishlist"
                          class="wishlist-form">
                        <input type="hidden" name="csrfToken"
                               value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="toggle">
                        <input type="hidden" name="productId" value="${product.productId}">
                        <input type="hidden" name="returnUrl" value="${returnUrl}">

                        <button type="submit" class="wishlist-btn">
                            <c:choose>
                                <c:when test="${inWishlist}">❤️ Saved to wishlist — click to remove</c:when>
                                <c:otherwise>🤍 Add to wishlist</c:otherwise>
                            </c:choose>
                        </button>
                    </form>
                </c:when>

                <c:otherwise>
                    <div class="login-box">
                        <p>Log in to buy this product and see your cart.</p>
                        <a class="primary-btn"
                           href="${pageContext.request.contextPath}/login?next=${pageContext.request.requestURI}%3Fid%3D${product.productId}">
                            🔒 Log in to purchase
                        </a>
                    </div>
                </c:otherwise>
            </c:choose>

            <a class="primary-btn" style="margin-top:18px;"
               href="${pageContext.request.contextPath}/products">
                Continue shopping
            </a>

            <br>

            <a class="back-link"
               href="${pageContext.request.contextPath}/products">
                &#8592; Back to products
            </a>
        </section>

    </div>

    <%-- ================= REVIEWS ================= --%>
    <section class="panel" style="max-width:1120px;margin:0 auto 50px;
                                 padding:0 24px;border:none;background:none;">

        <h2 style="color:var(--green);font-size:22px;margin-bottom:4px;">
            Customer reviews
        </h2>
        <p class="muted-text" style="margin-bottom:20px;">
            <c:choose>
                <c:when test="${reviewCount > 0}">
                    <span class="stars"><c:out value="${'★★★★★'.substring(0, averageRating < 0.5 ? 0 : (averageRating < 1.5 ? 1 : (averageRating < 2.5 ? 2 : (averageRating < 3.5 ? 3 : (averageRating < 4.5 ? 4 : 5)))))}"/></span>
                    <strong><fmt:formatNumber value="${averageRating}" pattern="#,##0.0"/></strong>
                    out of 5 · ${reviewCount} review(s)
                </c:when>
                <c:otherwise>
                    No reviews yet — be the first to write one.
                </c:otherwise>
            </c:choose>
        </p>

        <%-- ---- write a review ---- --%>
        <c:if test="${not empty sessionScope.user}">
            <div class="panel" style="margin-bottom:22px;">
                <h2 style="font-size:16px;">
                    <c:choose>
                        <c:when test="${not empty myReview}">Update your review</c:when>
                        <c:otherwise>Write a review</c:otherwise>
                    </c:choose>
                </h2>

                <form method="post"
                      action="${pageContext.request.contextPath}/review">
                    <input type="hidden" name="csrfToken"
                           value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="add">
                    <input type="hidden" name="productId" value="${product.productId}">

                    <div class="field">
                        <label>Your rating</label>
                        <div class="stars-input">
                            <input type="radio" name="rating" id="r5" value="5"
                                   <c:if test="${myReview.rating == 5}">checked</c:if>>
                            <label for="r5">★</label>
                            <input type="radio" name="rating" id="r4" value="4"
                                   <c:if test="${myReview.rating == 4}">checked</c:if>>
                            <label for="r4">★</label>
                            <input type="radio" name="rating" id="r3" value="3"
                                   <c:if test="${myReview.rating == 3}">checked</c:if>>
                            <label for="r3">★</label>
                            <input type="radio" name="rating" id="r2" value="2"
                                   <c:if test="${myReview.rating == 2}">checked</c:if>>
                            <label for="r2">★</label>
                            <input type="radio" name="rating" id="r1" value="1"
                                   <c:if test="${myReview.rating == 1}">checked</c:if>>
                            <label for="r1">★</label>
                        </div>
                    </div>

                    <div class="field">
                        <label for="reviewTitle">Title (optional)</label>
                        <input id="reviewTitle" name="title" type="text"
                               maxlength="120"
                               value="<c:out value='${myReview.title}'/>">
                    </div>

                    <div class="field">
                        <label for="reviewComment">Your review</label>
                        <textarea id="reviewComment" name="comment"
                                  maxlength="1000" required
                                  placeholder="What did you like or dislike?"><c:out value='${myReview.comment}'/></textarea>
                    </div>

                    <c:if test="${hasPurchased}">
                        <p class="muted-text" style="margin-bottom:12px;">
                            ✅ Verified purchase
                        </p>
                    </c:if>

                    <button type="submit" class="primary-btn">
                        <c:choose>
                            <c:when test="${not empty myReview}">Update review</c:when>
                            <c:otherwise>Post review</c:otherwise>
                        </c:choose>
                    </button>
                </form>
            </div>
        </c:if>

        <%-- ---- existing reviews ---- --%>
        <c:choose>
            <c:when test="${empty reviews}">
                <div class="empty-state">
                    <h2>No reviews yet</h2>
                    <p>Be the first to tell other shoppers what you think.</p>
                </div>
            </c:when>

            <c:otherwise>
                <c:forEach var="review" items="${reviews}">
                    <div class="panel" style="margin-bottom:14px;">
                        <div style="display:flex;justify-content:space-between;
                                    align-items:center;gap:12px;flex-wrap:wrap;">
                            <strong><c:out value="${review.customerName}"/></strong>
                            <span class="stars"><c:out value="${review.stars}"/></span>
                        </div>

                        <c:if test="${not empty review.title}">
                            <p style="font-weight:700;margin-top:6px;">
                                <c:out value="${review.title}"/>
                            </p>
                        </c:if>

                        <p class="muted-text" style="margin-top:6px;">
                            <c:out value="${review.comment}"/>
                        </p>

                        <p style="margin-top:9px;font-size:12px;color:var(--muted);">
                            <c:if test="${review.verifiedPurchase}">
                                <span class="pill PAID">Verified purchase</span>
                            </c:if>
                            <c:if test="${not empty review.createdAt}">
                                <c:out value="${review.createdAt.toLocalDate()}"/>
                            </c:if>
                        </p>
                    </div>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </section>
</main>

<%@ include file="partials/footer.jspf" %>

<script>
    function changeQty(delta) {
        var input = document.getElementById('quantity');
        if (!input) return;

        var min = parseInt(input.min || '1', 10);
        var max = parseInt(input.max || '9999', 10);

        var value = parseInt(input.value || '1', 10) + delta;

        if (value < min) value = min;
        if (value > max) value = max;

        input.value = value;
    }

    // Buy Now: add the item to the cart and land on the checkout page.
    function buyNow() {
        var form = document.getElementById('addToCartForm');
        if (!form) return;

        var returnUrl = form.querySelector('input[name="returnUrl"]');
        if (returnUrl) {
            returnUrl.value = '${pageContext.request.contextPath}/checkout';
        }

        form.submit();
    }
</script>
</body>
</html>
