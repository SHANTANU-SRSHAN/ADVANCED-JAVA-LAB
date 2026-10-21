
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Products | ShopSphere</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">

    <style>
        .catalog {
            padding: 45px 7%;
            min-height: 65vh;
        }

        .catalog h1 {
            margin: 8px 0;
            color: #12372a;
        }

        .catalog-intro {
            color: #6b776f;
            margin-bottom: 25px;
        }

        .catalog-toolbar {
            display: grid;
            grid-template-columns: minmax(200px, 2fr) minmax(150px, 1fr)
                               minmax(150px, 1fr) auto;
            gap: 12px;
            margin-bottom: 30px;
        }

        .catalog-toolbar input,
        .catalog-toolbar select {
            width: 100%;
            min-width: 0;
            padding: 12px;
            border: 1px solid #dce5de;
            border-radius: 9px;
            background: white;
            color: #17251e;
            font: inherit;
        }

        .catalog-toolbar button {
            padding: 12px 18px;
            border: 0;
            border-radius: 9px;
            background: #12372a;
            color: white;
            font-weight: 700;
            cursor: pointer;
        }

        .catalog-toolbar button:hover {
            background: #22a06b;
        }

        .catalog-results {
            margin-bottom: 18px;
            color: #6b776f;
            font-size: 14px;
        }

        .catalog .product-card {
            display: block;
            color: inherit;
        }

        .catalog .product-card h3 {
            color: #12372a;
        }

        .catalog .product-card h3:hover {
            color: #22a06b;
        }

        .catalog .product-description {
            color: #6b776f;
            font-size: 13px;
            margin-top: 8px;
        }

        .catalog .product-price {
            color: #168052;
            font-size: 20px;
            font-weight: 800;
            margin-top: 12px;
        }

        .catalog .product-stock {
            color: #6b776f;
            font-size: 13px;
            margin-top: 7px;
        }

        .catalog .product-image {
            min-height: 180px;
            display: grid;
            place-items: center;
            background: #f6f8f6;
        }

        .catalog .product-image img {
            width: 100%;
            height: 190px;
            object-fit: cover;
        }

        .catalog .no-image {
            color: #6b776f;
            padding: 25px 8px;
            text-align: center;
            font-size: 13px;
        }

        .details-link {
            display: inline-block;
            margin-top: 14px;
            color: #168052;
            font-weight: 700;
            font-size: 14px;
        }

        .empty-message {
            padding: 30px;
            background: #f6f8f6;
            border-radius: 12px;
        }

        @media (max-width: 850px) {
            .catalog-toolbar {
                grid-template-columns: 1fr 1fr;
            }
        }

        @media (max-width: 520px) {
            .catalog {
                padding: 30px 5%;
            }

            .catalog-toolbar {
                grid-template-columns: 1fr;
            }
        }
    </style>
</head>

<body>
<%@ include file="partials/header.jspf" %>

<main class="catalog">
    <p class="eyebrow">OUR COLLECTION</p>
    <h1>Discover Products</h1>
    <p class="catalog-intro">
        Search, filter, and explore products available in ShopSphere.
    </p>

    <%@ include file="partials/alerts.jspf" %>

    <form class="catalog-toolbar"
          action="${pageContext.request.contextPath}/products"
          method="get">

        <input type="search"
               name="search"
               value="<c:out value='${search}'/>"
               placeholder="Search products..."
               aria-label="Search products">

        <select name="category" aria-label="Filter by category">
            <option value="">All categories</option>
            <c:forEach var="cat" items="${categories}">
                <option value="<c:out value='${cat}'/>"
                    <c:if test="${selectedCategory eq cat}">selected</c:if>>
                    <c:out value="${cat}"/>
                </option>
            </c:forEach>
        </select>

        <select name="sort" aria-label="Sort products">
            <option value="newest"
                <c:if test="${sort eq 'newest'}">selected</c:if>>
                Newest
            </option>
            <option value="priceLow"
                <c:if test="${sort eq 'priceLow'}">selected</c:if>>
                Price: low to high
            </option>
            <option value="priceHigh"
                <c:if test="${sort eq 'priceHigh'}">selected</c:if>>
                Price: high to low
            </option>
            <option value="name"
                <c:if test="${sort eq 'name'}">selected</c:if>>
                Name: A to Z
            </option>
        </select>

        <button type="submit">Search</button>
    </form>

    <p class="catalog-results">
        <c:choose>
            <c:when test="${empty products}">
                No matching products found.
            </c:when>
            <c:otherwise>
                Showing <c:out value="${products.size()}"/> product(s).
            </c:otherwise>
        </c:choose>
    </p>

    <c:choose>
        <c:when test="${empty products}">
            <div class="empty-message">
                <h3>No products found</h3>
                <p>Try another search term or category.</p>
                <a class="details-link"
                   href="${pageContext.request.contextPath}/products">
                    Clear all filters
                </a>
            </div>
        </c:when>

        <c:otherwise>
            <div class="product-grid">
                <c:forEach var="product" items="${products}">
                    <article class="product-card">
                        <a class="card-link"
                           href="${pageContext.request.contextPath}/product?id=${product.productId}">

                            <div class="product-image">
                                <img
                                    src="<c:out value='${product.resolveImage(pageContext.request.contextPath)}'/>"
                                    alt="<c:out value='${product.name}'/>"
                                    loading="lazy">
                            </div>

                            <div class="product-info">
                                <p class="product-category">
                                    <c:out value="${product.category}"/>
                                </p>

                                <h3><c:out value="${product.name}"/></h3>

                                <p class="product-description">
                                    <c:choose>
                                        <c:when test="${not empty product.description}">
                                            <c:out value="${product.description}"/>
                                        </c:when>
                                        <c:otherwise>
                                            View product for more details.
                                        </c:otherwise>
                                    </c:choose>
                                </p>

                                <p class="product-price">
                                    &#8377;<fmt:formatNumber value="${product.price}"
                                                             pattern="#,##0.00"/>
                                </p>

                                <p class="product-stock">
                                    <c:choose>
                                        <c:when test="${product.stock gt 0}">
                                            In stock: <c:out value="${product.stock}"/>
                                        </c:when>
                                        <c:otherwise>
                                            Out of stock
                                        </c:otherwise>
                                    </c:choose>
                                </p>
                            </div>
                        </a>

                        <div class="product-info" style="padding-top:0;">
                            <div class="product-bottom">
                                <a class="details-link" style="margin-top:0;"
                                   href="${pageContext.request.contextPath}/product?id=${product.productId}">
                                    View details &#8594;
                                </a>

                                <form method="post"
                                      action="${pageContext.request.contextPath}/cart">
                                    <input type="hidden" name="csrfToken"
                                           value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="add">
                                    <input type="hidden" name="productId" value="${product.productId}">
                                    <input type="hidden" name="quantity" value="1">
                                    <input type="hidden" name="returnUrl"
                                           value="${pageContext.request.requestURI}${empty pageContext.request.queryString ? '' : '?'}${pageContext.request.queryString}">

                                    <button type="submit" class="add-btn"
                                            aria-label="Add <c:out value='${product.name}'/> to cart"
                                            <c:if test="${product.stock le 0}">disabled title="Out of stock"</c:if>>
                                        +
                                    </button>
                                </form>
                            </div>
                        </div>
                    </article>
                </c:forEach>

                <%-- ------------------- pagination ------------------- --%>
                <c:if test="${not empty productPage and productPage.totalPages > 1}">
                    <nav class="pager">
                        <c:choose>
                            <c:when test="${productPage.hasPrevious}">
                                <a href="?search=<c:out value='${search}'/>&amp;category=<c:out value='${selectedCategory}'/>&amp;sort=<c:out value='${sort}'/>&amp;page=${productPage.previousPage}">‹ Previous</a>
                            </c:when>
                            <c:otherwise>
                                <span class="disabled">‹ Previous</span>
                            </c:otherwise>
                        </c:choose>

                        <c:forEach var="p" begin="1" end="${productPage.totalPages}">
                            <c:choose>
                                <c:when test="${p == currentPage}">
                                    <span class="current"><c:out value="${p}"/></span>
                                </c:when>
                                <c:otherwise>
                                    <a href="?search=<c:out value='${search}'/>&amp;category=<c:out value='${selectedCategory}'/>&amp;sort=<c:out value='${sort}'/>&amp;page=${p}"><c:out value="${p}"/></a>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>

                        <c:choose>
                            <c:when test="${productPage.hasNext}">
                                <a href="?search=<c:out value='${search}'/>&amp;category=<c:out value='${selectedCategory}'/>&amp;sort=<c:out value='${sort}'/>&amp;page=${productPage.nextPage}">Next →</a>
                            </c:when>
                            <c:otherwise>
                                <span class="disabled">Next →</span>
                            </c:otherwise>
                        </c:choose>
                    </nav>

                    <p class="muted-text" style="text-align:center;margin-top:10px;">
                        Showing ${productPage.firstItemNumber}–${productPage.lastItemNumber}
                        of ${productPage.totalItems} products
                    </p>
                </c:if>
            </div>
        </c:otherwise>
    </c:choose>
</main>

<%@ include file="partials/footer.jspf" %>
</body>
</html>

