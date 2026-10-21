<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin · Products | ShopSphere</title>
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

        .layout {
            display: grid;
            grid-template-columns: 320px 1fr;
            gap: 22px;
            align-items: start;
        }

        .panel {
            border: 1px solid #e5ebe6;
            border-radius: 16px;
            background: white;
            overflow: hidden;
        }
        .panel-head {
            padding: 15px 18px;
            border-bottom: 1px solid #eef2ee;
        }
        .panel-head h2 { font-size: 16px; color: #12372a; }
        .panel-body { padding: 18px; }

        .field { margin-bottom: 13px; }
        .field label {
            display: block; font-size: 13px; font-weight: 700;
            margin-bottom: 5px; color: #17251e;
        }
        .field input, .field textarea, .field select {
            width: 100%; padding: 10px 11px; border: 1px solid #dce5de;
            border-radius: 8px; font: inherit;
        }
        .field textarea { min-height: 76px; resize: vertical; }
        .field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }

        .btn {
            padding: 11px 16px; border: 0; border-radius: 9px;
            font-weight: 700; cursor: pointer; font-size: 14px;
        }
        .btn-primary { background: #12372a; color: white; width: 100%; }
        .btn-primary:hover { background: #22a06b; }
        .btn-save { background: #22a06b; color: white; }
        .btn-save:hover { background: #168052; }

        .mini-btn {
            padding: 6px 11px; border-radius: 7px; font-size: 12px;
            font-weight: 700; cursor: pointer; border: 1px solid #dce5de;
            background: #f6f8f6; color: #17251e;
        }
        .mini-btn:hover { border-color: #22a06b; color: #168052; }
        .mini-danger {
            border: 1px solid #f3c9c5; background: #fff6f5; color: #b42318;
        }
        .mini-danger:hover { background: #ffecec; border-color: #e79a94; }

        table.data { width: 100%; border-collapse: collapse; }
        table.data th, table.data td {
            padding: 11px 14px; text-align: left; font-size: 13px;
            border-bottom: 1px solid #eef2ee; vertical-align: middle;
        }
        table.data th {
            background: #fafbf9; color: #6b776f; font-size: 11px;
            text-transform: uppercase; letter-spacing: .5px;
        }
        table.data tr:last-child td { border-bottom: 0; }
        table.data tr:hover td { background: #fcfdfc; }

        .thumb {
            width: 44px; height: 38px; object-fit: cover; border-radius: 7px;
            background: #f6f8f6; display: block;
        }
        .prod-cell { display: flex; align-items: center; gap: 10px; }
        .prod-cell strong { color: #12372a; font-size: 13px; }

        .stock-ok { color: #168052; font-weight: 700; }
        .stock-low { color: #b42318; font-weight: 700; }

        .toolbar {
            display: grid;
            grid-template-columns: 1fr 150px auto;
            gap: 9px;
            padding: 13px 14px;
            border-bottom: 1px solid #eef2ee;
        }
        .toolbar input, .toolbar select {
            padding: 9px 10px; border: 1px solid #dce5de; border-radius: 8px;
            font: inherit; min-width: 0;
        }
        .toolbar button {
            padding: 9px 15px; border: 0; border-radius: 8px;
            background: #12372a; color: white; font-weight: 700; cursor: pointer;
        }

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

        .form-title { font-size: 14px; color: #12372a; margin-bottom: 12px; }

        @media (max-width: 900px) {
            .layout { grid-template-columns: 1fr; }
            .toolbar { grid-template-columns: 1fr; }
        }
    </style>
</head>

<body>
<%@ include file="../partials/header.jspf" %>
<%@ include file="../partials/admin-nav.jspf" %>

<main class="admin-main">
    <p class="eyebrow">ADMIN</p>
    <h1>Products</h1>
    <p class="admin-sub">Add new items or edit existing ones. Changes appear in the store immediately.</p>

    <c:if test="${param.saved eq '1'}">
        <div class="alert-ok">✅ Product saved.</div>
    </c:if>
    <c:if test="${param.deleted eq '1'}">
        <div class="alert-ok">🗑 Product deleted.</div>
    </c:if>
    <c:if test="${not empty param.error}">
        <div class="alert-bad">${param.error}</div>
    </c:if>

    <div class="layout">
        <%-- ---------------- Add / edit form ---------------- --%>
        <div class="panel">
            <div class="panel-head">
                <h2><c:choose>
                    <c:when test="${not empty editProduct}">Edit product #${editProduct.productId}</c:when>
                    <c:otherwise>Add a product</c:otherwise>
                </c:choose></h2>
            </div>

            <div class="panel-body">
                <form method="post"
                      action="${pageContext.request.contextPath}/admin/products">
                    <input type="hidden" name="csrfToken"
                           value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action"
                           value="${not empty editProduct ? 'update' : 'add'}">

                    <c:if test="${not empty editProduct}">
                        <input type="hidden" name="productId"
                               value="${editProduct.productId}">
                    </c:if>

                    <div class="field">
                        <label for="p-name">Name *</label>
                        <input id="p-name" name="name" type="text"
                               maxlength="150" required
                               value="<c:out value='${editProduct.name}'/>">
                    </div>

                    <div class="field">
                        <label for="p-desc">Description</label>
                        <textarea id="p-desc" name="description"
                                  maxlength="2000"><c:out value='${editProduct.description}'/></textarea>
                    </div>

                    <div class="field-row">
                        <div class="field">
                            <label for="p-cat">Category</label>
                            <input id="p-cat" name="category" type="text"
                                   list="category-list" maxlength="100"
                                   value="<c:out value='${editProduct.category}'/>">
                            <datalist id="category-list">
                                <c:forEach var="c" items="${categories}">
                                    <option value="<c:out value='${c}'/>">
                                </c:forEach>
                            </datalist>
                        </div>

                        <div class="field">
                            <label for="p-price">Price (₹) *</label>
                            <input id="p-price" name="price" type="number"
                                   step="0.01" min="1" required
                                   value="<c:out value='${editProduct.price}'/>">
                        </div>
                    </div>

                    <div class="field-row">
                        <div class="field">
                            <label for="p-stock">Stock *</label>
                            <input id="p-stock" name="stock" type="number"
                                   min="0" required
                                   value="<c:out value='${empty editProduct ? 10 : editProduct.stock}'/>">
                        </div>
                    </div>

                    <div class="field">
                        <label for="p-image">Image URL or path</label>
                        <input id="p-image" name="imageUrl" type="text"
                               maxlength="500"
                               placeholder="https://... or images/products/product-1.jpg"
                               value="<c:out value='${editProduct.imageUrl}'/>">
                        <p class="field-hint">
                            Use a full URL, or a file bundled in the app such as
                            images/products/product-1.jpg.
                        </p>
                    </div>

                    <button type="submit" class="btn btn-primary">
                        <c:choose>
                            <c:when test="${not empty editProduct}">Save changes</c:when>
                            <c:otherwise>Add product</c:otherwise>
                        </c:choose>
                    </button>

                    <c:if test="${not empty editProduct}">
                        <a class="btn btn-primary"
                           style="background:#6b776f;margin-top:9px;text-decoration:none;display:block;text-align:center;"
                           href="${pageContext.request.contextPath}/admin/products">
                            Cancel edit
                        </a>
                    </c:if>
                </form>
            </div>
        </div>

        <%-- ---------------- Product list ---------------- --%>
        <div class="panel">
            <div class="panel-head">
                <h2>Catalog (${fn:length(products)} shown)</h2>
            </div>

            <form class="toolbar" method="get"
                  action="${pageContext.request.contextPath}/admin/products">
                <input type="search" name="search"
                       placeholder="Search products…"
                       value="<c:out value='${param.search}'/>">

                <select name="category">
                    <option value="">All categories</option>
                    <c:forEach var="c" items="${categories}">
                        <option value="<c:out value='${c}'/>"
                            <c:if test="${param.category eq c}">selected</c:if>>
                            <c:out value="${c}"/>
                        </option>
                    </c:forEach>
                </select>

                <button type="submit">Filter</button>
            </form>

            <table class="data">
                <thead>
                <tr>
                    <th>Product</th>
                    <th>Category</th>
                    <th>Price</th>
                    <th>Stock</th>
                    <th style="text-align:right;">Actions</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="p" items="${products}">
                    <tr>
                        <td>
                            <div class="prod-cell">
                                <img class="thumb"
                                     src="<c:out value='${p.resolveImage(pageContext.request.contextPath)}'/>" alt="">
                                <div>
                                    <strong><c:out value="${p.name}"/></strong><br>
                                    <span style="color:#6b776f;font-size:12px;">#${p.productId}</span>
                                </div>
                            </div>
                        </td>
                        <td><c:out value="${p.category}"/></td>
                        <td>&#8377;<fmt:formatNumber value="${p.price}"
                                                     pattern="#,##0.00"/></td>
                        <td>
                            <span class="${p.stock gt 5 ? 'stock-ok' : 'stock-low'}">
                                ${p.stock}
                            </span>
                        </td>
                        <td style="text-align:right;white-space:nowrap;">
                            <a class="mini-btn"
                               href="${pageContext.request.contextPath}/admin/products?edit=${p.productId}">
                                ✎ Edit
                            </a>

                            <form method="post" style="display:inline;"
                                  action="${pageContext.request.contextPath}/admin/products"
                                  onsubmit="return confirm('Delete this product?');">
                                <input type="hidden" name="csrfToken"
                                       value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="delete">
                                <input type="hidden" name="productId" value="${p.productId}">
                                <button type="submit" class="mini-btn mini-danger">
                                    🗑 Delete
                                </button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>

                <c:if test="${empty products}">
                    <tr>
                        <td colspan="5" style="color:#6b776f;padding:25px;">
                            No products match this filter.
                        </td>
                    </tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </div>
</main>

<%@ include file="../partials/footer.jspf" %>
</body>
</html>
