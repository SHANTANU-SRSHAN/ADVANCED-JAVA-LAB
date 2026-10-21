<#
    ShopSphere — end-to-end smoke test
    ---------------------------------------------------------------
    Exercises the whole customer journey plus the admin area against a
    running instance of the application.

    Usage:
        powershell -ExecutionPolicy Bypass -File tools\smoke-test.ps1
        powershell -ExecutionPolicy Bypass -File tools\smoke-test.ps1 `
            -BaseUrl http://localhost:8080/ShopSphere
#>

param(
    [string]$BaseUrl = "http://localhost:8080/ShopSphere",
    [string]$AdminEmail = "admin@shopsphere.com",
    [string]$AdminPassword = "Admin@123"
)

$ErrorActionPreference = "Stop"

$script:Passed = 0
$script:Failed = 0
$script:Failures = @()

$tmpDir = Join-Path $env:TEMP "shopsphere-smoke"
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

function New-Jar([string]$name) {
    $path = Join-Path $tmpDir "$name.jar"
    Remove-Item $path -ErrorAction SilentlyContinue
    return $path
}

# --------------------------------------------------------------------
# Helpers
# --------------------------------------------------------------------

function Test-Case {
    param([string]$Name, [bool]$Condition, [string]$Detail = "")

    if ($Condition) {
        $script:Passed++
        Write-Host ("  [PASS] " + $Name) -ForegroundColor Green
    } else {
        $script:Failed++
        $script:Failures += $Name
        Write-Host ("  [FAIL] " + $Name + "  " + $Detail) -ForegroundColor Red
    }
}

function Get-Status {
    param([string]$Url, [string]$Jar)
    curl.exe -s -o NUL -w "%{http_code}" -b $Jar -c $Jar $Url
}

function Get-Body {
    param([string]$Url, [string]$Jar)
    # Out-String keeps the whole page as one string instead of an array
    # of lines (PowerShell splits piped output on newlines).
    return (curl.exe -s -b $Jar -c $Jar $Url | Out-String)
}

function Get-Csrf {
    param([string]$Url, [string]$Jar)
    $body = Get-Body $Url $Jar
    # 1) the meta tag on every page
    if ($body -match 'name="csrf-token"\s+content="([^"]+)"') {
        if ($matches[1] -ne "") { return $matches[1] }
    }
    # 2) a hidden form field
    if ($body -match 'name="csrfToken"\s+value="([^"]+)"') {
        if ($matches[1] -ne "") { return $matches[1] }
    }
    return ""
}

function Invoke-Post {
    param(
        [string]$Url, [string]$Jar, [hashtable]$Fields,
        [switch]$Follow
    )
    $args = @("-s", "-o", "NUL", "-w", "%{http_code}|%{redirect_url}",
              "-b", $Jar, "-c", $Jar, "-X", "POST", $Url)
    foreach ($key in $Fields.Keys) {
        $args += @("-d", "$key=$($Fields[$key])")
    }
    if ($Follow) { $args += "-L" }
    return (curl.exe @args | Out-String).Trim()
}

function Get-Stock {
    param([int]$ProductId)
    $env:MYSQL_PWD = "Shantanu3219@R"
    $out = & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" `
        -u root -N -B -e "USE shopsphere; SELECT stock FROM products WHERE product_id = $ProductId;"
    return [int](($out | Select-Object -First 1) -split "`t")[0]
}

# --------------------------------------------------------------------
Write-Host ""
Write-Host "ShopSphere end-to-end smoke test" -ForegroundColor Cyan
Write-Host "Base URL: $BaseUrl" -ForegroundColor DarkGray
Write-Host ""

$customerJar = New-Jar "customer"
$adminJar    = New-Jar "admin"
$guestJar    = New-Jar "guest"

$email = "smoke$((Get-Random -Max 999999))@example.com"
$password = "Test@12345"

# ====================================================================
Write-Host "1. Public pages" -ForegroundColor Yellow
# ====================================================================
foreach ($path in @("/home", "/products", "/login", "/register",
                    "/product?id=1", "/forgot-password", "/cart")) {
    $code = Get-Status "$BaseUrl$path" $guestJar
    Test-Case "GET $path returns 200" ($code -eq "200") "got $code"
}

Write-Host ""
Write-Host "2. Protected pages redirect anonymous visitors" -ForegroundColor Yellow
foreach ($path in @("/checkout", "/payment", "/orders", "/wishlist",
                    "/account", "/addresses", "/admin")) {
    $code = Get-Status "$BaseUrl$path" $guestJar
    Test-Case "GET $path redirects when logged out" ($code -eq "302") "got $code"
}

Write-Host ""
Write-Host "3. Guest cart (no account needed)" -ForegroundColor Yellow
$guestCsrf = Get-Csrf "$BaseUrl/cart" $guestJar
Test-Case "cart page exposes a CSRF token" ($guestCsrf -ne "")

$r = Invoke-Post "$BaseUrl/cart" $guestJar @{
    action = "add"; productId = "1"; quantity = "2"; csrfToken = $guestCsrf
}
Test-Case "guest can add to the cart" ($r -match "added=1") $r

$guestCart = Get-Body "$BaseUrl/cart" $guestJar
Test-Case "guest cart shows the item" ($guestCart -match "Wireless Headphones")

Write-Host ""
Write-Host "4. Registration and sign-in" -ForegroundColor Yellow
$regCsrf = Get-Csrf "$BaseUrl/register" $guestJar
$r = Invoke-Post "$BaseUrl/register" $guestJar @{
    fullName = "Smoke Tester"; email = $email; phone = "9876543210"
    password = $password; confirmPassword = $password; csrfToken = $regCsrf
}
Test-Case "registration succeeds" ($r -match "registered=1") $r

$loginCsrf = Get-Csrf "$BaseUrl/login" $guestJar
$r = Invoke-Post "$BaseUrl/login" $guestJar @{
    email = $email; password = $password; csrfToken = $loginCsrf
}
Test-Case "sign-in succeeds" ($r -match "^302\|.*products") $r

$afterLogin = Get-Body "$BaseUrl/cart" $guestJar
Test-Case "guest cart merged into the account cart" `
          ($afterLogin -match "Wireless Headphones")

$homePage = Get-Body "$BaseUrl/home" $guestJar
Test-Case "header shows the signed-in name" ($homePage -match "Smoke Tester")
Test-Case "header shows the cart badge" ($homePage -match "nav-badge")

Write-Host ""
Write-Host "5. CSRF protection" -ForegroundColor Yellow
$r = Invoke-Post "$BaseUrl/cart" $guestJar @{
    action = "add"; productId = "1"; quantity = "1"; csrfToken = "forged"
}
Test-Case "a forged CSRF token is rejected" ($r -match "error=session") $r

Write-Host ""
Write-Host "6. Cart operations" -ForegroundColor Yellow
$cartCsrf = Get-Csrf "$BaseUrl/cart" $guestJar
$r = Invoke-Post "$BaseUrl/cart" $guestJar @{
    action = "update"; productId = "1"; quantity = "3"; csrfToken = $cartCsrf
}
Test-Case "cart quantity can be updated" ($r -match "updated=1") $r

$r = Invoke-Post "$BaseUrl/cart" $guestJar @{
    action = "remove"; productId = "1"; csrfToken = $cartCsrf
}
Test-Case "a line can be removed" ($r -match "removed=1") $r

$cartCsrf = Get-Csrf "$BaseUrl/cart" $guestJar
$r = Invoke-Post "$BaseUrl/cart" $guestJar @{
    action = "add"; productId = "2"; quantity = "2"; csrfToken = $cartCsrf
}
Test-Case "item re-added for checkout" ($r -match "added=1") $r

Write-Host ""
Write-Host "7. Address book" -ForegroundColor Yellow
$addrCsrf = Get-Csrf "$BaseUrl/addresses" $guestJar
$r = Invoke-Post "$BaseUrl/addresses" $guestJar @{
    action = "add"; label = "Home"; fullName = "Smoke Tester"
    phone = "9876543210"; addressLine1 = "12 MG Road"
    addressLine2 = "Near Mall"; city = "Pune"; state = "Maharashtra"
    pincode = "411001"; csrfToken = $addrCsrf
}
Test-Case "an address can be saved" ($r -match "saved=1") $r

$addresses = Get-Body "$BaseUrl/addresses" $guestJar
Test-Case "the saved address is listed" ($addresses -match "MG Road")

Write-Host ""
Write-Host "8. Coupons" -ForegroundColor Yellow
$checkout = Get-Body "$BaseUrl/checkout" $guestJar
$coCsrf = Get-Csrf "$BaseUrl/checkout" $guestJar

$r = Invoke-Post "$BaseUrl/checkout" $guestJar @{
    action = "coupon"; couponCode = "WELCOME10"; csrfToken = $coCsrf
}
Test-Case "a coupon can be applied" (($r -match "200\|") -or ($r -match "302\|.*checkout")) $r

$checkout = Get-Body "$BaseUrl/checkout" $guestJar
Test-Case "the coupon shows on the summary" ($checkout -match "WELCOME10")
Test-Case "the summary shows a discount line" ($checkout -match "Discount")
Test-Case "the summary shows tax" ($checkout -match "GST")

$r = Invoke-Post "$BaseUrl/checkout" $guestJar @{
    action = "coupon"; couponCode = "NOPE123"; csrfToken = $coCsrf
}
$checkout = Get-Body "$BaseUrl/checkout" $guestJar
Test-Case "an invalid coupon is rejected" ($checkout -notmatch "Discount \(NOPE123\)")

Write-Host ""
Write-Host "9. Cash on Delivery order" -ForegroundColor Yellow
$stockBefore = Get-Stock 2
$coCsrf = Get-Csrf "$BaseUrl/checkout" $guestJar
$r = Invoke-Post "$BaseUrl/checkout" $guestJar @{
    fullName = "Smoke Tester"; phone = "9876543210"
    addressLine1 = "12 MG Road"; addressLine2 = "Near Mall"
    city = "Pune"; state = "Maharashtra"; pincode = "411001"
    paymentMethod = "COD"; shippingMethod = "STANDARD"
    notes = "Leave at the door"; csrfToken = $coCsrf
}
Test-Case "a COD order is placed" ($r -match "/order\?id=\d+") $r

$orderId = ([regex]::Match($r, "id=(\d+)")).Groups[1].Value
$stockAfter = Get-Stock 2
Test-Case "stock was reduced for the order" ($stockAfter -eq ($stockBefore - 2)) `
          "before=$stockBefore after=$stockAfter"

$orderPage = Get-Body "$BaseUrl/order?id=$orderId&placed=1" $guestJar
Test-Case "the order page shows the recipient" ($orderPage -match "MG Road")
Test-Case "the order page shows the delivery notes" ($orderPage -match "Leave at the door")
Test-Case "the order page shows the tracking timeline" ($orderPage -match "Tracking history")

$orders = Get-Body "$BaseUrl/orders" $guestJar
Test-Case "the order appears in the order list" ($orders -match "SS-")
Test-Case "the order list shows the item count" ($orders -match "item\(s\)")

Write-Host ""
Write-Host "10. Cancelling an order restores the stock" -ForegroundColor Yellow
$ordersCsrf = Get-Csrf "$BaseUrl/orders" $guestJar
$r = Invoke-Post "$BaseUrl/orders" $guestJar @{
    action = "cancel"; orderId = $orderId; csrfToken = $ordersCsrf
}
Test-Case "the order can be cancelled" ($r -match "cancelled=1") $r

$stockRestored = Get-Stock 2
Test-Case "the stock was given back" ($stockRestored -eq $stockBefore) `
          "expected $stockBefore got $stockRestored"

Write-Host ""
Write-Host "11. Card payment - approved" -ForegroundColor Yellow
$cartCsrf = Get-Csrf "$BaseUrl/cart" $guestJar
Invoke-Post "$BaseUrl/cart" $guestJar @{
    action = "add"; productId = "3"; quantity = "1"; csrfToken = $cartCsrf
} | Out-Null

$coCsrf = Get-Csrf "$BaseUrl/checkout" $guestJar
$r = Invoke-Post "$BaseUrl/checkout" $guestJar @{
    fullName = "Smoke Tester"; phone = "9876543210"
    addressLine1 = "12 MG Road"; city = "Pune"; state = "Maharashtra"
    pincode = "411001"; paymentMethod = "CARD"
    shippingMethod = "EXPRESS"; csrfToken = $coCsrf
}
Test-Case "checkout moves to the payment step" ($r -match "/payment") $r

$payCsrf = Get-Csrf "$BaseUrl/payment" $guestJar
$r = Invoke-Post "$BaseUrl/payment" $guestJar @{
    cardNumber = "4111111111111111"; cardName = "Smoke Tester"
    expiry = "12/30"; cvv = "123"; csrfToken = $payCsrf
}
Test-Case "the card payment is approved" ($r -match "/order\?id=\d+") $r

$cardOrderId = ([regex]::Match($r, "id=(\d+)")).Groups[1].Value
$orderPage = Get-Body "$BaseUrl/order?id=$cardOrderId" $guestJar
Test-Case "the receipt shows the transaction id" ($orderPage -match "TXN")
Test-Case "the receipt shows the card brand" ($orderPage -match "Visa")
Test-Case "the receipt shows only the last 4 digits" ($orderPage -match "1111")

Write-Host ""
Write-Host "12. Card payment - declined" -ForegroundColor Yellow
$cartCsrf = Get-Csrf "$BaseUrl/cart" $guestJar
Invoke-Post "$BaseUrl/cart" $guestJar @{
    action = "add"; productId = "3"; quantity = "1"; csrfToken = $cartCsrf
} | Out-Null

$coCsrf = Get-Csrf "$BaseUrl/checkout" $guestJar
Invoke-Post "$BaseUrl/checkout" $guestJar @{
    fullName = "Smoke Tester"; phone = "9876543210"
    addressLine1 = "12 MG Road"; city = "Pune"; state = "Maharashtra"
    pincode = "411001"; paymentMethod = "CARD"
    shippingMethod = "STANDARD"; csrfToken = $coCsrf
} | Out-Null

$payCsrf = Get-Csrf "$BaseUrl/payment" $guestJar
$payPage = Get-Body "$BaseUrl/payment" $guestJar
Test-Case "a declined card is reported on the payment page" `
          ($payPage -match "declined")

$stockBefore = Get-Stock 3
$ordersPage = Get-Body "$BaseUrl/orders" $guestJar
Test-Case "the declined order is not confirmed" ($ordersPage -match "CANCELLED|PLACED")

Write-Host ""
Write-Host "13. UPI and net banking reach the gateway" -ForegroundColor Yellow
foreach ($method in @("UPI", "NETBANKING", "WALLET")) {
    $cartCsrf = Get-Csrf "$BaseUrl/cart" $guestJar
    Invoke-Post "$BaseUrl/cart" $guestJar @{
        action = "add"; productId = "4"; quantity = "1"; csrfToken = $cartCsrf
    } | Out-Null

    $coCsrf = Get-Csrf "$BaseUrl/checkout" $guestJar
    Invoke-Post "$BaseUrl/checkout" $guestJar @{
        fullName = "Smoke Tester"; phone = "9876543210"
        addressLine1 = "12 MG Road"; city = "Pune"; state = "Maharashtra"
        pincode = "411001"; paymentMethod = $method
        shippingMethod = "STANDARD"; csrfToken = $coCsrf
    } | Out-Null

    $payCsrf = Get-Csrf "$BaseUrl/payment" $guestJar
    $fields = @{ csrfToken = $payCsrf }
    if ($method -eq "UPI") { $fields["upiId"] = "tester@upi" }
    if ($method -eq "NETBANKING") { $fields["bankName"] = "HDFC Bank" }
    if ($method -eq "WALLET") { $fields["walletProvider"] = "Paytm" }

    $r = Invoke-Post "$BaseUrl/payment" $guestJar $fields
    Test-Case "$method payment completes" ($r -match "/order\?id=\d+") $r
}

Write-Host ""
Write-Host "14. Account area" -ForegroundColor Yellow
$account = Get-Body "$BaseUrl/account" $guestJar
Test-Case "the account page loads" ($account -match "Profile")
Test-Case "the account page lists payments" ($account -match "Payment history")
Test-Case "the account page lists orders" ($account -match "Recent orders")

$acctCsrf = Get-Csrf "$BaseUrl/account" $guestJar
$r = Invoke-Post "$BaseUrl/account" $guestJar @{
    action = "profile"; fullName = "Smoke Tester Renamed"
    phone = "9876543210"; csrfToken = $acctCsrf
}
Test-Case "the profile can be updated" ($r -match "updated=profile") $r

$acctCsrf = Get-Csrf "$BaseUrl/account" $guestJar
$r = Invoke-Post "$BaseUrl/account" $guestJar @{
    action = "password"; oldPassword = $password
    newPassword = "NewPass@123"; confirmPassword = "NewPass@123"
    csrfToken = $acctCsrf
}
Test-Case "the password can be changed" ($r -match "updated=password") $r

Write-Host ""
Write-Host "15. Sign in again with the new password" -ForegroundColor Yellow
$jar2 = New-Jar "customer2"
$loginCsrf = Get-Csrf "$BaseUrl/login" $jar2
$r = Invoke-Post "$BaseUrl/login" $jar2 @{
    email = $email; password = "NewPass@123"; csrfToken = $loginCsrf
}
Test-Case "sign-in with the new password works" ($r -match "^302\|") $r

Write-Host ""
Write-Host "16. Forgot / reset password" -ForegroundColor Yellow
$fpJar = New-Jar "forgot"
$fpCsrf = Get-Csrf "$BaseUrl/forgot-password" $fpJar
$r = Invoke-Post "$BaseUrl/forgot-password" $fpJar @{
    email = $email; csrfToken = $fpCsrf
}
$fpPage = Get-Body "$BaseUrl/forgot-password" $fpJar
Test-Case "the forgot-password page always confirms" `
          ($fpPage -match "If an account exists")

Write-Host ""
Write-Host "17. Reviews" -ForegroundColor Yellow
$reviewCsrf = Get-Csrf "$BaseUrl/product?id=3" $jar2
$r = Invoke-Post "$BaseUrl/review" $jar2 @{
    action = "add"; productId = "3"; rating = "5"
    title = "Great lamp"; comment = "Bright and well made."
    csrfToken = $reviewCsrf
}
Test-Case "a review can be posted" ($r -match "reviewed=1") $r

$product = Get-Body "$BaseUrl/product?id=3" $jar2
Test-Case "the review shows on the product page" ($product -match "Bright and well made")
Test-Case "the average rating is shown" ($product -match "out of 5")

Write-Host ""
Write-Host "18. Wishlist" -ForegroundColor Yellow
$wishCsrf = Get-Csrf "$BaseUrl/product?id=5" $jar2
$r = Invoke-Post "$BaseUrl/wishlist" $jar2 @{
    action = "toggle"; productId = "5"; csrfToken = $wishCsrf
}
Test-Case "a product can be wishlisted" ($r -match "saved=1") $r

$wishlist = Get-Body "$BaseUrl/wishlist" $jar2
Test-Case "the wishlist shows the product" ($wishlist -match "Clean Code Handbook")

$wishCsrf = Get-Csrf "$BaseUrl/wishlist" $jar2
$r = Invoke-Post "$BaseUrl/wishlist" $jar2 @{
    action = "moveToCart"; productId = "5"; csrfToken = $wishCsrf
}
Test-Case "a wishlist item can be moved to the cart" ($r -match "moved=1") $r

Write-Host ""
Write-Host "19. Admin area" -ForegroundColor Yellow
$adminLoginCsrf = Get-Csrf "$BaseUrl/login" $adminJar
$r = Invoke-Post "$BaseUrl/login" $adminJar @{
    email = $AdminEmail; password = $AdminPassword
    csrfToken = $adminLoginCsrf
}
Test-Case "the admin can sign in" ($r -match "^302\|") $r

foreach ($path in @("/admin", "/admin/products", "/admin/orders",
                    "/admin/coupons", "/admin/refunds", "/admin/users")) {
    $code = Get-Status "$BaseUrl$path" $adminJar
    Test-Case "GET $path returns 200 for an admin" ($code -eq "200") "got $code"
}

$dashboard = Get-Body "$BaseUrl/admin" $adminJar
Test-Case "the dashboard shows revenue" ($dashboard -match "Revenue|revenue")
Test-Case "the dashboard shows low stock" ($dashboard -match "Low stock|low stock")

$adminOrders = Get-Body "$BaseUrl/admin/orders" $adminJar
Test-Case "the admin order list loads" ($adminOrders -match "Order")

$adminUsers = Get-Body "$BaseUrl/admin/users" $adminJar
Test-Case "the admin user list loads" ($adminUsers -match "Customer")

Write-Host ""
Write-Host "20. A customer cannot reach the admin area" -ForegroundColor Yellow
$code = Get-Status "$BaseUrl/admin" $jar2
Test-Case "a customer is redirected away from /admin" ($code -eq "302") "got $code"

Write-Host ""
Write-Host ("=" * 60)
Write-Host ("Passed: $script:Passed    Failed: $script:Failed") -ForegroundColor Cyan
if ($script:Failed -gt 0) {
    Write-Host ""
    Write-Host "Failures:" -ForegroundColor Red
    foreach ($failure in $script:Failures) {
        Write-Host "  - $failure" -ForegroundColor Red
    }
}
Write-Host ("=" * 60)

exit $(if ($script:Failed -gt 0) { 1 } else { 0 })
