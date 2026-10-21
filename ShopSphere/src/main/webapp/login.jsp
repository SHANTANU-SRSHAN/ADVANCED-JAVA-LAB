<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login | ShopSphere</title>

    <style>
        * { box-sizing: border-box; }

        body {
            margin: 0;
            min-height: 100vh;
            display: grid;
            place-items: center;
            padding: 20px;
            background: #0b100d;
            color: #f5f7f5;
            font-family: Arial, sans-serif;
        }

        .login-card {
            width: 100%;
            max-width: 420px;
            padding: 34px;
            background: #141d17;
            border: 1px solid #293b30;
            border-radius: 18px;
        }

        .logo {
            color: white;
            text-decoration: none;
            font-size: 27px;
            font-weight: bold;
        }

        .logo span { color: #22a06b; }

        p { color: #a4b3a8; }

        label {
            display: block;
            margin: 20px 0 8px;
        }

        input[type="email"], input[type="password"], input[type="text"] {
            width: 100%;
            padding: 13px;
            color: white;
            background: #0b100d;
            border: 1px solid #405146;
            border-radius: 8px;
        }

        button {
            width: 100%;
            margin-top: 24px;
            padding: 14px;
            border: 0;
            border-radius: 8px;
            background: #22a06b;
            color: #07110b;
            font-weight: bold;
            cursor: pointer;
        }

        a { color: #65d6a0; }

        .error { color: #ff9292; }
        .success { color: #65d6a0; }

        .remember-row {
            display: flex;
            align-items: center;
            gap: 9px;
            margin-top: 18px;
            font-size: 14px;
            color: #a4b3a8;
        }

        .remember-row input { width: auto; }

        .form-row {
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 12px;
            flex-wrap: wrap;
        }

        .form-row label { margin: 0; }

        .forgot-link { font-size: 13px; margin-top: 8px; }

        .footer-links {
            margin-top: 26px;
            padding-top: 18px;
            border-top: 1px solid #293b30;
            font-size: 14px;
        }
    </style>
</head>

<body>
    <main class="login-card">
        <a class="logo"
           href="${pageContext.request.contextPath}/home">
            ShopSphere<span>.</span>
        </a>

        <h1>Welcome back</h1>
        <p>Log in to your ShopSphere account.</p>

        <c:if test="${not empty error}">
            <p class="error"><c:out value="${error}"/></p>
        </c:if>

        <c:if test="${param.registered eq '1'}">
            <p class="success">
                Registration successful! Please log in.
            </p>
        </c:if>

        <c:if test="${param.reset eq '1'}">
            <p class="success">
                Your password has been changed. Please log in.
            </p>
        </c:if>

        <c:if test="${param.verified eq '1'}">
            <p class="success">
                ✅ Your e-mail address is now verified.
            </p>
        </c:if>

        <c:if test="${param.verified eq 'expired'}">
            <p class="error">That verification link has expired.</p>
        </c:if>

        <c:if test="${param.verified eq 'invalid'}">
            <p class="error">That verification link was not valid.</p>
        </c:if>

        <form method="post"
              action="${pageContext.request.contextPath}/login">

            <%-- Send the visitor back to the page they wanted. --%>
            <input type="hidden" name="next"
                   value="<c:out value='${empty next ? "" : next}'/>">

            <%-- CSRF protection --%>
            <input type="hidden" name="csrfToken"
                   value="<c:out value='${csrfToken}'/>">

            <label for="email">Email address</label>
            <input id="email" name="email" type="email"
                   maxlength="150" autocomplete="email" required
                   value="<c:out value='${email}'/>">

            <label for="password">Password</label>
            <input id="password" name="password" type="password"
                   autocomplete="current-password" required>

            <div class="form-row">
                <div class="remember-row">
                    <input id="rememberMe" name="rememberMe" type="checkbox"
                           value="on">
                    <label for="rememberMe">Keep me signed in</label>
                </div>
            </div>

            <p class="forgot-link">
                <a href="${pageContext.request.contextPath}/forgot-password">
                    Forgot your password?
                </a>
            </p>

            <button type="submit">Log In</button>
        </form>

        <p class="footer-links">
            Don't have an account?
            <a href="${pageContext.request.contextPath}/register">
                Create account
            </a>
            &nbsp;·&nbsp;
            <a href="${pageContext.request.contextPath}/home">
                Back home
            </a>
        </p>
    </main>
</body>
</html>
