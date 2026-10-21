# Downloads the realistic product/store photography from Unsplash's CDN
# into src/main/webapp/images so the site works offline.
#
# Photos are used under the Unsplash License (free to use, no attribution
# required). Source links are recorded in images/CREDITS.md.

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$images = Join-Path $root "src\main\webapp\images"
$products = Join-Path $images "products"
$categories = Join-Path $images "categories"

New-Item -ItemType Directory -Force $products, $categories | Out-Null

$photos = @(
    # Products -----------------------------------------------------------
    @{ Out = "products\product-1.jpg";  Id = "photo-1505740420928-5e560c06d30e"; W = 800; H = 600 },
    @{ Out = "products\product-2.jpg";  Id = "photo-1521572163474-6864f9cf17ab"; W = 800; H = 600 },
    @{ Out = "products\product-3.jpg";  Id = "photo-1507473885765-e6ed057f782c"; W = 800; H = 600 },
    @{ Out = "products\product-4.jpg";  Id = "photo-1546868871-7041f2a55e12"; W = 800; H = 600 },
    @{ Out = "products\product-5.jpg";  Id = "photo-1544716278-ca5e3f4abd8c"; W = 800; H = 600 },
    @{ Out = "products\product-6.jpg";  Id = "photo-1583394838336-acd977736f90"; W = 800; H = 600 },
    @{ Out = "products\product-7.jpg";  Id = "photo-1596755094514-f87e34085b2c"; W = 800; H = 600 },
    @{ Out = "products\product-8.jpg";  Id = "photo-1513506003901-1e6a229e2d15"; W = 800; H = 600 },
    @{ Out = "products\product-9.jpg";  Id = "photo-1590658268037-6bf12165a8df"; W = 800; H = 600 },
    @{ Out = "products\product-10.jpg"; Id = "photo-1579586337278-3befd40fd17a"; W = 800; H = 600 },
    @{ Out = "products\product-11.jpg"; Id = "photo-1503341504253-dff4815485f1"; W = 800; H = 600 },
    @{ Out = "products\product-12.jpg"; Id = "photo-1512820790803-83ca734da794"; W = 800; H = 600 },
    @{ Out = "products\product-13.jpg"; Id = "pexels-9695849"; W = 800; H = 600 },

    # Category tiles -----------------------------------------------------
    @{ Out = "categories\electronics.jpg"; Id = "photo-1498049794561-7780e7231661"; W = 400; H = 400 },
    @{ Out = "categories\fashion.jpg";     Id = "photo-1445205170230-053b83016050"; W = 400; H = 400 },
    @{ Out = "categories\home-living.jpg"; Id = "photo-1616486338812-3dadae4b4ace"; W = 400; H = 400 },
    @{ Out = "categories\books.jpg";       Id = "photo-1507842217343-583bb7270b66"; W = 400; H = 400 },

    # Hero ---------------------------------------------------------------
    @{ Out = "hero.jpg"; Id = "photo-1483985988355-763728e1935b"; W = 1200; H = 800 }
)

$failed = 0

foreach ($p in $photos) {
    $dest = Join-Path $images $p.Out

    if ($p.Id -like "pexels-*") {
        $pexelsId = $p.Id.Substring(7)
        $url = "https://images.pexels.com/photos/$pexelsId/pexels-photo-$pexelsId.jpeg?auto=compress&cs=tinysrgb&w=$($p.W)&h=$($p.H)&fit=crop"
    } else {
        $url = "https://images.unsplash.com/$($p.Id)?auto=format&fit=crop&w=$($p.W)&h=$($p.H)&q=75&fm=jpg"
    }

    Write-Host ("  {0,-34}" -f $p.Out) -NoNewline
    $result = curl.exe -s -L --fail -o $dest -w "%{http_code} %{size_download}" $url 2>&1

    if ($LASTEXITCODE -ne 0 -or $result -notmatch "^200 ") {
        Write-Host " FAILED ($result)" -ForegroundColor Red
        $failed++
    } else {
        $kb = [math]::Round((Get-Item $dest).Length / 1KB, 1)
        Write-Host " $result -> $kb KB" -ForegroundColor Green
    }
}

if ($failed -gt 0) {
    throw "$failed image(s) failed to download."
}

Write-Host ""
Write-Host "All realistic photos downloaded." -ForegroundColor Green
