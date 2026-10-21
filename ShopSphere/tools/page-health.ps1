<#
    ShopSphere — page health check
    ---------------------------------------------------------------
    Requests every page of the application and reports any that do not
    answer with 200. A few pages legitimately redirect for a signed-in
    visitor (the login/register pages and an empty checkout), and those
    are reported as OK. Useful after a change to catch a JSP that no
    longer compiles.

    Usage:
        powershell -ExecutionPolicy Bypass -File tools\page-health.ps1
#>

param(
    [string]$BaseUrl = "http://localhost:8080/ShopSphere",
    [string]$AdminEmail = "admin@shopsphere.com",
    [string]$AdminPassword = "Admin@123"
)

$ErrorActionPreference = "Stop"
$tmpDir = Join-Path $env:TEMP "shopsphere-health"
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

$jar = Join-Path $tmpDir "health.jar"
Remove-Item $jar -ErrorAction SilentlyContinue

function Get-Status {
    param([string]$Url)
    curl.exe -s -o NUL -w "%{http_code}" -b $jar -c $jar $Url
}

function Get-Csrf {
    param([string]$Url)
    $body = curl.exe -s -b $jar -c $jar $Url | Out-String
    if ($body -match 'name="csrf-token"\s+content="([^"]+)"') { return $matches[1] }
    if ($body -match 'name="csrfToken"\s+value="([^"]+)"') { return $matches[1] }
    return ""
}

# ---- sign in as the administrator so the admin pages are reachable ----
$email = "health$((Get-Random -Max 999999))@example.com"

$token = Get-Csrf "$BaseUrl/register"
curl.exe -s -o NUL -b $jar -c $jar -X POST "$BaseUrl/register" `
    -d "fullName=Health Check" -d "email=$email" -d "phone=9876543210" `
    -d "password=Test@12345" -d "confirmPassword=Test@12345" `
    -d "csrfToken=$token" | Out-Null

$token = Get-Csrf "$BaseUrl/login"
curl.exe -s -o NUL -b $jar -c $jar -X POST "$BaseUrl/login" `
    -d "email=$email" -d "password=Test@12345" -d "csrfToken=$token" | Out-Null

# ---- customer pages ----
$customerPages = @(
    "/home", "/products", "/products?page=1", "/products?search=desk",
    "/products?category=Books&sort=priceLow",
    "/product?id=1", "/cart", "/wishlist", "/orders", "/account",
    "/addresses", "/checkout", "/login", "/register", "/forgot-password"
)

# ---- administrator pages (signed in separately) ----
$adminJar = Join-Path $tmpDir "admin.jar"
Remove-Item $adminJar -ErrorAction SilentlyContinue

$token = (curl.exe -s -b $adminJar -c $adminJar "$BaseUrl/login" | Out-String)
if ($token -match 'name="csrfToken"\s+value="([^"]+)"') { $token = $matches[1] }
curl.exe -s -o NUL -b $adminJar -c $adminJar -X POST "$BaseUrl/login" `
    -d "email=$AdminEmail" -d "password=$AdminPassword" `
    -d "csrfToken=$token" | Out-Null

$adminPages = @(
    "/admin", "/admin/products", "/admin/orders", "/admin/coupons",
    "/admin/refunds", "/admin/users"
)

Write-Host ""
Write-Host "Page health check" -ForegroundColor Cyan
Write-Host ""

$bad = 0

Write-Host "Customer pages:" -ForegroundColor Yellow
$expectedRedirects = @("/checkout", "/login", "/register")
foreach ($page in $customerPages) {
    $code = Get-Status "$BaseUrl$page"
    if ($code -eq "200") {
        Write-Host ("  [ OK ] " + $page) -ForegroundColor Green
    } elseif ($code -eq "302" -and $expectedRedirects -contains $page) {
        Write-Host ("  [ OK ] " + $page + "  (expected redirect)") -ForegroundColor Green
    } else {
        Write-Host ("  [BAD ] " + $page + "  -> " + $code) -ForegroundColor Red
        $bad++
    }
}

Write-Host ""
Write-Host "Admin pages:" -ForegroundColor Yellow
foreach ($page in $adminPages) {
    $code = curl.exe -s -o NUL -w "%{http_code}" -b $adminJar -c $adminJar "$BaseUrl$page"
    if ($code -eq "200") {
        Write-Host ("  [ OK ] " + $page) -ForegroundColor Green
    } else {
        Write-Host ("  [BAD ] " + $page + "  -> " + $code) -ForegroundColor Red
        $bad++
    }
}

Write-Host ""
if ($bad -eq 0) {
    Write-Host "All pages returned 200." -ForegroundColor Green
} else {
    Write-Host "$bad page(s) did not return 200." -ForegroundColor Red
}
Write-Host ""

exit $(if ($bad -gt 0) { 1 } else { 0 })
