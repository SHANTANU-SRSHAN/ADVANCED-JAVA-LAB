<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Payment | ShopSphere</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">
</head>

<body>
<%@ include file="partials/header.jspf" %>

<main class="page-wrap">
    <p class="eyebrow">SECURE PAYMENT</p>
    <h1>Complete your payment</h1>
    <p class="page-sub">You are one step away from your order.</p>

    <c:if test="${not empty error}">
        <div class="alert alert-error">
            <c:out value="${error}"/>
            <c:if test="${not empty failedReference}">
                <br><small>Reference: <c:out value="${failedReference}"/></small>
            </c:if>
        </div>
    </c:if>

    <div class="panel-grid" style="grid-template-columns:1.5fr 1fr;">>

        <!-- ============ payment form ============ -->
        <form class="panel" method="post"
              action="${pageContext.request.contextPath}/payment">

            <input type="hidden" name="csrfToken"
                   value="<c:out value='${csrfToken}'/>">

            <h2>Pay with <c:out value="${pending.paymentMethod}"/></h2>
            <p class="sub">
                This is a demo payment page — no real money moves and no
                external gateway is contacted.
            </p>

            <!-- ---------- CARD ---------- -->
            <c:if test="${pending.paymentMethod eq 'CARD'}">
                <div class="info-note" style="margin-bottom:18px;">
                    💡 Test card <strong>4111 1111 1111 1111</strong> is
                    approved. Use <strong>4000 0000 0000 0002</strong> to
                    see a declined payment.
                </div>

                <div class="field">
                    <label for="cardNumber">Card number</label>
                    <input id="cardNumber" name="cardNumber" type="text"
                           inputmode="numeric" maxlength="23"
                           placeholder="4111 1111 1111 1111" required
                           value="<c:out value='${cardNumber}'/>">
                </div>

                <div class="field">
                    <label for="cardName">Name on card</label>
                    <input id="cardName" name="cardName" type="text"
                           maxlength="100" autocomplete="cc-name" required
                           value="<c:out value='${cardName}'/>">
                </div>

                <div class="field-row">
                    <div class="field">
                        <label for="expiry">Expiry (MM/YY)</label>
                        <input id="expiry" name="expiry" type="text"
                               maxlength="5" placeholder="12/30" required
                               value="<c:out value='${expiry}'/>">
                    </div>

                    <div class="field">
                        <label for="cvv">CVV</label>
                        <input id="cvv" name="cvv" type="text"
                               inputmode="numeric" maxlength="4"
                               placeholder="123" required
                               value="<c:out value='${cvv}'/>">
                    </div>
                </div>
            </c:if>

            <!-- ---------- UPI ---------- -->
            <c:if test="${pending.paymentMethod eq 'UPI'}">
                <div class="info-note" style="margin-bottom:18px;">
                    💡 Any valid UPI id works, e.g.
                    <strong>yourname@upi</strong>.
                </div>

                <div class="field">
                    <label for="upiId">UPI id</label>
                    <input id="upiId" name="upiId" type="text"
                           maxlength="100" placeholder="yourname@upi"
                           required
                           value="<c:out value='${upiId}'/>">
                    <p class="field-hint">
                        Open your UPI app, choose ShopSphere and enter this id.
                    </p>
                </div>
            </c:if>

            <!-- ---------- NET BANKING ---------- -->
            <c:if test="${pending.paymentMethod eq 'NETBANKING'}">
                <div class="field">
                    <label for="bankName">Choose your bank</label>
                    <select id="bankName" name="bankName" required>
                        <option value="">Select a bank…</option>
                        <option value="State Bank of India"
                            <c:if test="${bankName eq 'State Bank of India'}">selected</c:if>>State Bank of India</option>
                        <option value="HDFC Bank"
                            <c:if test="${bankName eq 'HDFC Bank'}">selected</c:if>>HDFC Bank</option>
                        <option value="ICICI Bank"
                            <c:if test="${bankName eq 'ICICI Bank'}">selected</c:if>>ICICI Bank</option>
                        <option value="Axis Bank"
                            <c:if test="${bankName eq 'Axis Bank'}">selected</c:if>>Axis Bank</option>
                        <option value="Kotak Mahindra Bank"
                            <c:if test="${bankName eq 'Kotak Mahindra Bank'}">selected</c:if>>Kotak Mahindra Bank</option>
                        <option value="Punjab National Bank"
                            <c:if test="${bankName eq 'Punjab National Bank'}">selected</c:if>>Punjab National Bank</option>
                    </select>
                </div>
            </c:if>

            <!-- ---------- WALLET ---------- -->
            <c:if test="${pending.paymentMethod eq 'WALLET'}">
                <div class="field">
                    <label for="walletProvider">Choose your wallet</label>
                    <select id="walletProvider" name="walletProvider" required>
                        <option value="">Select a wallet…</option>
                        <option value="Paytm">Paytm</option>
                        <option value="Amazon Pay">Amazon Pay</option>
                        <option value="PhonePe Wallet">PhonePe Wallet</option>
                        <option value="Google Pay">Google Pay</option>
                    </select>
                </div>
            </c:if>

            <button type="submit" class="primary-btn full-btn"
                    style="margin-top:22px;">
                Pay &#8377;<fmt:formatNumber value="${total}"
                                             pattern="#,##0.00"/>
            </button>

            <p class="muted-text" style="margin-top:14px;text-align:center;">
                🔒 Your card details are never stored — only the brand and
                the last four digits are kept with the receipt.
            </p>
        </form>

        <!-- ============ summary ============ -->
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
                <span>Delivery</span>
                <span>
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
                <span>&#8377;<fmt:formatNumber value="${total}"
                                               pattern="#,##0.00"/></span>
            </div>

            <hr style="border:none;border-top:1px solid var(--border);
                       margin:18px 0;">

            <h2 style="font-size:15px;">Delivering to</h2>
            <p class="muted-text" style="margin-top:6px;">
                <strong><c:out value="${pending.fullName}"/></strong><br>
                <c:out value="${pending.addressLine1}"/><br>
                <c:if test="${not empty pending.addressLine2}">
                    <c:out value="${pending.addressLine2}"/><br>
                </c:if>
                <c:out value="${pending.city}"/>
                <c:if test="${not empty pending.state}">
                    , <c:out value="${pending.state}"/>
                </c:if>
                - <c:out value="${pending.pincode}"/><br>
                📞 <c:out value="${pending.phone}"/>
            </p>

            <c:if test="${not empty shipping}">
                <p class="muted-text">
                    🚚 <c:out value="${shipping.name}"/> —
                    <c:out value="${shipping.deliveryEstimate}"/>
                </p>
            </c:if>

            <a href="${pageContext.request.contextPath}/checkout"
               class="btn btn-sm" style="display:inline-block;margin-top:12px;">
                ← Change address or delivery
            </a>
        </aside>
    </div>
</main>

<%@ include file="partials/footer.jspf" %>

<script>
    // Format the card number as the customer types.
    var cardInput = document.getElementById('cardNumber');
    if (cardInput) {
        cardInput.addEventListener('input', function () {
            var digits = cardInput.value.replace(/\D/g, '').slice(0, 16);
            cardInput.value = digits.replace(/(.{4})/g, '$1 ').trim();
        });
    }

    var expiryInput = document.getElementById('expiry');
    if (expiryInput) {
        expiryInput.addEventListener('input', function () {
            var digits = expiryInput.value.replace(/\D/g, '').slice(0, 4);
            expiryInput.value = digits.length > 2
                ? digits.slice(0, 2) + '/' + digits.slice(2)
                : digits;
        });
    }
</script>
</body>
</html>
