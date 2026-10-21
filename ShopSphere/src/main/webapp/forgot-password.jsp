<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Forgot password | ShopSphere</title>

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

        .card {
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

        input {
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
        .success {
            color: #65d6a0;
            background: #14251b;
            border: 1px solid #2a5a3d;
            padding: 12px 14px;
            border-radius: 9px;
            font-size: 14px;
        }

        .back {
            margin-top: 24px;
            font-size: 14px;
        }
    </style>
</head>

<body>
    <main class="card">
        <a class="logo"
           href="${pageContext.request.contextPath}/home">
            ShopSphere<span>.</span>
        </a>

        <h1>Forgot your password?</h1>
        <p>Enter the e-mail address on your account and we will create a
            reset link for you.</p>

        <c:if test="${not empty error}">
            <p class="error"><c:out value="${error}"/></p>
        </c:if>

        <c:if test="${not empty message}">
            <p class="success"><c:out value="${message}"/></p>
        </c:if>

        <form method="post"
              action="${pageContext.request.contextPath}/forgot-password">

            <input type="hidden" name="csrfToken"
                   value="<c:out value='${csrfToken}'/>">

            <label for="email">Email address</label>
            <input id="email" name="email" type="email"
                   maxlength="150" autocomplete="email" required
                   value="<c:out value='${email}'/>">

            <button type="submit">Send reset link</button>
        </form>

        <p class="back">
            <a href="${pageContext.request.contextPath}/login">
                ← Back to sign in
            </a>
        </p>
    </main>
</body>
</html>
