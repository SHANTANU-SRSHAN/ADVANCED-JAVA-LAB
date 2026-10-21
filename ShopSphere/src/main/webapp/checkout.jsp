<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Checkout | ShopSphere</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">
</head>

<body>
<%@ include file="partials/header.jspf" %>

<main class="page-wrap">
    <p class="eyebrow">SECURE CHECKOUT</p>
    <h1>Checkout</h1>
    <p class="page-sub">Where should we send your order?</p>

    <c:if test="${not empty error}">
        <div class="alert alert-error"><c:out value="${error}"/></div>
    </c:if>

    <div class="steps" style="display:flex;gap:10px;align-items:center;
                            font-size:13px;color:var(--muted);margin-bottom:25px;
                            flex-wrap:wrap;">
        <span>1. Cart</span><span style="color:#c7d2ca;">→</span>
        <span style="color:var(--accent);font-weight:800;">2. Address &amp; payment</span>
        <span style="color:#c7d2ca;">→</span>
        <span>3. Confirmation</span>
    </div>

    <div class="panel-grid" style="grid-template-columns:1.6fr 1fr;">>

        <!-- ============ LEFT: the form ============ -->
        <form class="panel" method="post"
              action="${pageContext.request.contextPath}/checkout">

            <input type="hidden" name="csrfToken"
                   value="<c:out value='${csrfToken}'/>">

            <!-- ---------- saved addresses ---------- -->
            <c:if test="${not empty addresses}">
                <h2>Deliver to</h2>
                <p class="sub">Pick one of your saved addresses, or enter a
                    new one below.</p>

                <div class="tag-list" style="flex-direction:column;
                                            align-items:stretch;gap:10px;
                                            margin-bottom:22px;">
                    <c:forEach var="addr" items="${addresses}">
                        <label class="method-card"
                               style="text-align:left;display:block;
                                      cursor:pointer;">
                            <input type="radio" name="savedAddress"
                                   value="${addr.addressId}"
                                   class="saved-address"
                                   data-fullname="<c:out value='${addr.fullName}'/>"
                                   data-phone="<c:out value='${addr.phone}'/>"
                                   data-line1="<c:out value='${addr.addressLine1}'/>"
                                   data-line2="<c:out value='${addr.addressLine2}'/>"
                                   data-city="<c:out value='${addr.city}'/>"
                                   data-state="<c:out value='${addr.state}'/>"
                                   data-pincode="<c:out value='${addr.pincode}'/>"
                                   <c:if test="${addr.defaultAddress}">checked</c:if>>
                            <strong>
                                <c:out value="${addr.label}"/>
                                <c:if test="${addr.defaultAddress}">
                                    <span class="pill PAID">Default</span>
                                </c:if>
                            </strong>
                            <span style="display:block;font-size:12px;
                                         color:var(--muted);margin-top:3px;">
                                <c:out value="${addr.fullName}"/> ·
                                <c:out value="${addr.addressLine1}"/>,
                                <c:out value="${addr.city}"/> -
                                <c:out value="${addr.pincode}"/>
                            </span>
                        </label>
                    </c:forEach>
                </div>
            </c:if>

            <!-- ---------- address ---------- -->
            <h2>Shipping address</h2>
            <p class="sub">All fields are required.</p>

            <div class="field">
                <label for="fullName">Recipient full name</label>
                <input id="fullName" name="fullName" type="text"
                       maxlength="100" required
                       value="<c:out value='${empty fullName ? sessionScope.userName : fullName}'/>">
            </div>

            <div class="field-row">
                <div class="field">
                    <label for="phone">Phone (10 digits)</label>
                    <input id="phone" name="phone" type="tel"
                           maxlength="10" pattern="[0-9]{10}" required
                           value="<c:out value='${phone}'/>">
                </div>

                <div class="field">
                    <label for="pincode">Pincode (6 digits)</label>
                    <input id="pincode" name="pincode" type="text"
                           maxlength="6" pattern="[0-9]{6}" required
                           value="<c:out value='${pincode}'/>">
                </div>
            </div>

            <div class="field">
                <label for="addressLine1">Street address</label>
                <textarea id="addressLine1" name="addressLine1" maxlength="300"
                          required
                          placeholder="House / flat, street, area"><c:out value='${addressLine1}'/></textarea>
            </div>

            <div class="field">
                <label for="addressLine2">Landmark (optional)</label>
                <input id="addressLine2" name="addressLine2" type="text"
                       maxlength="300"
                       value="<c:out value='${addressLine2}'/>">
            </div>

            <div class="field-row">
                <div class="field">
                    <label for="city">City</label>
                    <input id="city" name="city" type="text" maxlength="100"
                           required value="<c:out value='${city}'/>">
                </div>

                <div class="field">
                    <label for="state">State</label>
                    <input id="state" name="state" type="text" maxlength="100"
                           value="<c:out value='${state}'/>">
                </div>
            </div>

            <div class="checkbox-row">
                <input id="saveAddress" name="saveAddress" type="checkbox"
                       value="on" checked>
                <label for="saveAddress" style="margin:0;">
                    Save this address to my address book
                </label>
            </div>

            <!-- ---------- delivery option ---------- -->
            <h2 style="margin-top:26px;">Delivery speed</h2>
            <p class="sub">How quickly do you need it?</p>

            <div class="method-grid">
                <c:forEach var="method" items="${shippingMethods}">
                    <label class="method-option">
                        <input type="radio" name="shippingMethod"
                               value="${method.code}"
                               class="shipping-choice"
                               data-price="${method.price}"
                               data-name="<c:out value='${method.name}'/>"
                               <c:if test="${(empty shipping && method.code eq 'STANDARD') || (not empty shipping && shipping.code eq method.code)}">checked</c:if>>
                        <span class="method-card">
                            <span class="icon">🚚</span>
                            <strong><c:out value="${method.name}"/></strong>
                            <span class="hint">
                                <c:choose>
                                    <c:when test="${method.price.doubleValue() == 0}">FREE</c:when>
                                    <c:otherwise>
                                        &#8377;<fmt:formatNumber value="${method.price}" pattern="#,##0.00"/>
                                    </c:otherwise>
                                </c:choose>
                            </span>
                            <span class="hint"><c:out value="${method.deliveryEstimate}"/></span>
                        </span>
                    </label>
                </c:forEach>
            </div>

            <!-- ---------- payment method ---------- -->
            <h2 style="margin-top:26px;">Payment method</h2>
            <p class="sub">Choose how you would like to pay.</p>

            <div class="method-grid">
                <label class="method-option">
                    <input type="radio" name="paymentMethod" value="COD"
                           <c:if test="${empty paymentMethod || paymentMethod eq 'COD'}">checked</c:if>>
                    <span class="method-card">
                        <span class="icon">💵</span>
                        <strong>Cash on Delivery</strong>
                        <span class="hint">Pay when it arrives</span>
                    </span>
                </label>

                <label class="method-option">
                    <input type="radio" name="paymentMethod" value="CARD"
                           <c:if test="${paymentMethod eq 'CARD'}">checked</c:if>>
                    <span class="method-card">
                        <span class="icon">💳</span>
                        <strong>Card</strong>
                        <span class="hint">Credit / Debit card</span>
                    </span>
                </label>

                <label class="method-option">
                    <input type="radio" name="paymentMethod" value="UPI"
                           <c:if test="${paymentMethod eq 'UPI'}">checked</c:if>>
                    <span class="method-card">
                        <span class="icon">📱</span>
                        <strong>UPI</strong>
                        <span class="hint">GPay / PhonePe / Paytm</span>
                    </span>
                </label>

                <label class="method-option">
                    <input type="radio" name="paymentMethod" value="NETBANKING"
                           <c:if test="${paymentMethod eq 'NETBANKING'}">checked</c:if>>
                    <span class="method-card">
                        <span class="icon">🏦</span>
                        <strong>Net Banking</strong>
                        <span class="hint">All major banks</span>
                    </span>
                </label>

                <label class="method-option">
                    <input type="radio" name="paymentMethod" value="WALLET"
                           <c:if test="${paymentMethod eq 'WALLET'}">checked</c:if>>
                    <span class="method-card">
                        <span class="icon">👛</span>
                        <strong>Wallet</strong>
                        <span class="hint">Paytm / Amazon Pay</span>
                    </span>
                </label>
            </div>

            <!-- ---------- notes ---------- -->
            <div class="field" style="margin-top:22px;">
                <label for="notes">Delivery notes (optional)</label>
                <textarea id="notes" name="notes" maxlength="500"
                          placeholder="e.g. Please leave the parcel at the front desk."><c:out value='${notes}'/></textarea>
            </div>

            <button type="submit" class="primary-btn full-btn"
                    style="margin-top:22px;">
                Continue to payment →
            </button>

            <p class="muted-text" style="margin-top:14px;text-align:center;">
                🔒 Your details are sent securely. Demo store — no real
                charges are made.
            </p>
        </form>

        <!-- ============ RIGHT: the summary ============ -->
        <div>
            <aside class="panel" style="position:sticky;top:20px;">
                <h2>Order summary</h2>

                <c:forEach var="item" items="${items}">
                    <div class="summary-row" style="align-items:flex-start;">
                        <span>
                            <c:out value="${item.product.name}"/>
                            <span style="color:var(--muted);">× ${item.quantity}</span>
                        </span>
                        <span style="white-space:nowrap;">
                            &#8377;<fmt:formatNumber value="${item.lineTotal}"
                                                     pattern="#,##0.00"/>
                        </span>
                    </div>
                </c:forEach>

                <div class="summary-row">
                    <span>Subtotal</span>
                    <span>&#8377;<fmt:formatNumber value="${subtotal}"
                                                   pattern="#,##0.00"/></span>
                </div>

                <c:if test="${totals.hasDiscount()}">
                    <div class="summary-row discount">
                        <span>Discount (<c:out value="${coupon.code}"/>)</span>
                        <span>− &#8377;<fmt:formatNumber value="${discount}"
                                                          pattern="#,##0.00"/></span>
                    </div>
                </c:if>

                <div class="summary-row">
                    <span>Delivery <span id="shipName"
                        style="font-size:11px;color:var(--muted);"></span></span>
                    <span id="shipAmount">
                        <c:choose>
                            <c:when test="${totals.isFreeShipping()}">
                                <span style="color:#168052;font-weight:700;">FREE</span>
                            </c:when>
                            <c:otherwise>
                                &#8377;<fmt:formatNumber value="${shippingAmount}" pattern="#,##0.00"/>
                            </c:otherwise>
                        </c:choose>
                    </span>
                </div>

                <div class="summary-row">
                    <span>GST (5%)</span>
                    <span>&#8377;<fmt:formatNumber value="${taxAmount}"
                                                   pattern="#,##0.00"/></span>
                </div>

                <div class="summary-row total">
                    <span>Total</span>
                    <span id="grandTotal">&#8377;<fmt:formatNumber
                        value="${total}" pattern="#,##0.00"/></span>
                </div>

                <!-- ---------- coupon ---------- -->
                <hr style="border:none;border-top:1px solid var(--border);
                           margin:18px 0;">

                <c:choose>
                    <c:when test="${not empty coupon}">
                        <div class="info-note">
                            ✅ Coupon <strong><c:out value="${coupon.code}"/></strong>
                            is applied.
                            <button type="submit" name="action"
                                    value="coupon-remove"
                                    formaction="${pageContext.request.contextPath}/checkout"
                                    class="btn btn-sm"
                                    style="margin-top:9px;">Remove</button>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="field" style="margin-bottom:10px;">
                            <label for="couponCode">Have a coupon?</label>
                            <input id="couponCode" name="couponCode" type="text"
                                   placeholder="e.g. WELCOME10" maxlength="50"
                                   style="width:100%;padding:10px 12px;
                                          border:1px solid #dce5de;border-radius:9px;">
                        </div>
                        <button type="submit" name="action" value="coupon"
                                class="btn btn-sm">Apply coupon</button>
                    </c:otherwise>
                </c:choose>

                <c:if test="${not empty availableCoupons}">
                    <div style="margin-top:16px;">
                        <p class="muted-text" style="margin-bottom:7px;">
                            Available to you:
                        </p>
                        <div class="tag-list">
                            <c:forEach var="c" items="${availableCoupons}">
                                <span class="tag tag-accent"
                                      title="<c:out value='${c.description}'/>">
                                    <c:out value="${c.code}"/>
                                </span>
                            </c:forEach>
                        </div>
                    </div>
                </c:if>

                <p class="muted-text" style="margin-top:16px;">
                    Cash on Delivery pays at the door. Card, UPI, Net Banking
                    and Wallet take you to the payment step next.
                </p>
            </aside>
        </div>
    </div>
</main>

<%@ include file="partials/footer.jspf" %>

<script>
    // Fill the address form when a saved address is picked.
    document.querySelectorAll('.saved-address').forEach(function (radio) {
        radio.addEventListener('change', function () {
            var data = radio.dataset;
            document.getElementById('fullName').value    = data.fullname  || '';
            document.getElementById('phone').value        = data.phone     || '';
            document.getElementById('addressLine1').value = data.line1     || '';
            document.getElementById('addressLine2').value = data.line2     || '';
            document.getElementById('city').value         = data.city      || '';
            document.getElementById('state').value        = data.state     || '';
            document.getElementById('pincode').value      = data.pincode   || '';
        });
    });

    // Keep the summary in step with the chosen delivery option.
    (function () {
        var subtotal = Number('${subtotal}');
        var discount = Number('${discount}');
        var taxRate  = 0.05;

        function money(value) {
            return '₹' + value.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
        }

        function recalc() {
            var chosen = document.querySelector('.shipping-choice:checked');
            var price  = chosen ? Number(chosen.dataset.price) : 0;

            document.getElementById('shipAmount').innerHTML =
                price === 0 ? '<span style="color:#168052;font-weight:700;">FREE</span>'
                            : money(price);
            document.getElementById('shipName').textContent =
                chosen ? '(' + chosen.dataset.name + ')' : '';

            var taxed  = (subtotal - discount) * taxRate;
            var total  = (subtotal - discount) + price + taxed;

            document.getElementById('grandTotal').textContent = money(total);
        }

        document.querySelectorAll('.shipping-choice').forEach(function (radio) {
            radio.addEventListener('change', recalc);
        });

        recalc();
    })();
</script>
</body>
</html>
