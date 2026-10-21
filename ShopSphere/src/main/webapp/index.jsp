<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ShopSphere | Discover More</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">

    <style>
        .home-add {
            position: absolute;
            right: 14px;
            bottom: 14px;
        }
        .home-add form { display: inline; }
        .home-actions {
            display: flex;
            gap: 8px;
            margin-top: 12px;
        }
        .home-actions .btn-line {
            flex: 1;
            padding: 9px 8px;
            border-radius: 9px;
            font-size: 13px;
            font-weight: 700;
            text-align: center;
            cursor: pointer;
            border: 1px solid #dce5de;
            background: #f6f8f6;
            color: #17251e;
        }
        .home-actions .btn-line:hover { border-color: #22a06b; color: #168052; }
        .home-actions .btn-line.primary {
            background: #12372a;
            border-color: #12372a;
            color: white;
        }
        .home-actions .btn-line.primary:hover { background: #22a06b; }
        .login-to-buy {
            display: inline-block;
            margin-top: 12px;
            font-size: 13px;
            font-weight: 700;
            color: #168052;
        }
    </style>
</head>
<body>

<%@ include file="partials/header.jspf" %>

<main>
    <section class="hero">
        <div class="hero-content">
            <span class="eyebrow">YOUR EVERYDAY MARKETPLACE</span>

            <h1>Find what you love.<br><span>Love what you find.</span></h1>

            <p>
                Explore thoughtfully selected products, discover
                new favourites and enjoy a better shopping experience.
            </p>

            <div class="hero-actions">
                <a href="#products" class="primary-btn">Explore Products →</a>
                <a href="#categories" class="secondary-btn">Browse Categories</a>
            </div>

            <div class="trust-note">
                <span>✓</span> Simple shopping · Secure checkout · Easy tracking
            </div>
        </div>

        <div class="hero-image">
            <img
                src="${pageContext.request.contextPath}/images/hero.jpg"
                alt="Shopper carrying colourful shopping bags">

            <div class="floating-card">
                <span class="status-dot"></span>
                <div>
                    <strong>Discover something new</strong>
                    <p>Your next favourite is waiting.</p>
                </div>
            </div>
        </div>
    </section>

    <section class="benefits">
        <div>
            <span>◇</span>
            <div>
                <strong>Curated Finds</strong>
                <p>Explore products you'll love</p>
            </div>
        </div>

        <div>
            <span>↗</span>
            <div>
                <strong>Great Value</strong>
                <p>Discover deals and offers</p>
            </div>
        </div>

        <div>
            <span>✓</span>
            <div>
                <strong>Easy Shopping</strong>
                <p>A simple buying experience</p>
            </div>
        </div>
    </section>

    <section class="section" id="categories">
        <div class="section-heading">
            <div>
                <span class="eyebrow">EXPLORE THE STORE</span>
                <h2>Shop by category</h2>
            </div>
            <p>Something for every part of your life.</p>
        </div>

        <div class="category-grid">
            <a href="${pageContext.request.contextPath}/products?category=Electronics" class="category-card">
                <img class="category-photo"
                     src="${pageContext.request.contextPath}/images/categories/electronics.jpg"
                     alt="">
                <div class="category-body">
                    <h3>Electronics</h3>
                    <p>Tech for everyday life</p>
                </div>
            </a>

            <a href="${pageContext.request.contextPath}/products?category=Fashion" class="category-card">
                <img class="category-photo"
                     src="${pageContext.request.contextPath}/images/categories/fashion.jpg"
                     alt="">
                <div class="category-body">
                    <h3>Fashion</h3>
                    <p>Make it your style</p>
                </div>
            </a>

            <a href="${pageContext.request.contextPath}/products?category=Home%20%26%20Living" class="category-card">
                <img class="category-photo"
                     src="${pageContext.request.contextPath}/images/categories/home-living.jpg"
                     alt="">
                <div class="category-body">
                    <h3>Home &amp; Living</h3>
                    <p>Make space feel special</p>
                </div>
            </a>

            <a href="${pageContext.request.contextPath}/products?category=Books" class="category-card">
                <img class="category-photo"
                     src="${pageContext.request.contextPath}/images/categories/books.jpg"
                     alt="">
                <div class="category-body">
                    <h3>Books</h3>
                    <p>Stories worth keeping</p>
                </div>
            </a>
        </div>
    </section>

    <!-- Featured products are loaded from the database by HomeServlet. -->
    <section class="section products-section" id="products">
        <div class="section-heading">
            <div>
                <span class="eyebrow">HANDPICKED FOR YOU</span>
                <h2>Featured products</h2>
            </div>
            <p>Discover your next great find.</p>
        </div>

        <div class="product-grid">
            <c:forEach var="product" items="${featuredProducts}">
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
                        </div>
                    </a>

                    <div class="product-info" style="padding-top:0;">
                        <div class="product-bottom">
                            <strong>
                                &#8377;<fmt:formatNumber value="${product.price}"
                                                         pattern="#,##0.00"/>
                            </strong>

                            <button type="button" class="add-btn"
                                    aria-label="Add <c:out value='${product.name}'/> to cart"
                                    <c:if test="${product.stock le 0}">disabled title="Out of stock"</c:if>
                                    onclick="document.getElementById('quick-add-${product.productId}').submit();">
                                +
                            </button>
                        </div>
                    </div>

                    <form id="quick-add-${product.productId}" method="post"
                          action="${pageContext.request.contextPath}/cart"
                          style="display:none;">
                        <input type="hidden" name="csrfToken"
                               value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="add">
                        <input type="hidden" name="productId" value="${product.productId}">
                        <input type="hidden" name="quantity" value="1">
                        <input type="hidden" name="returnUrl"
                               value="${pageContext.request.contextPath}/home">
                    </form>
                </article>
            </c:forEach>

            <c:if test="${empty featuredProducts}">
                <p style="color:#6b776f;">Products are loading — check the database connection.</p>
            </c:if>
        </div>

        <div class="hero-actions">
            <a href="${pageContext.request.contextPath}/products" class="primary-btn">
                View All Products →
            </a>
        </div>
    </section>

    <section class="cta">
        <div>
            <span class="eyebrow">YOUR NEXT DISCOVERY STARTS HERE</span>
            <h2>Good finds. Better experiences.</h2>
            <p>Create an account to start building your shopping experience.</p>
        </div>

        <c:choose>
            <c:when test="${not empty sessionScope.user}">
                <a href="${pageContext.request.contextPath}/products"
                   class="primary-btn">
                    Start Shopping →
                </a>
            </c:when>
            <c:otherwise>
                <a href="${pageContext.request.contextPath}/register"
                   class="primary-btn">
                    Create Your Account →
                </a>
            </c:otherwise>
        </c:choose>
    </section>
</main>

<%@ include file="partials/footer.jspf" %>

</body>
</html>
