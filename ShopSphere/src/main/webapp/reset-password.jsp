<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Reset password | ShopSphere</title>

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

        .hint { font-size: 12px; color: #7f8f84; margin-top: 6px; }

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

        <h1>Choose a new password</h1>
        <p>Pick something you have not used before.</p>

        <c:if test="${not empty error}">
            <p class="error"><c:out value="${error}"/></p>
        </c:if>

        <c:if test="${empty error}">
            <form method="post"
                  action="${pageContext.request.contextPath}/reset-password">

                <input type="hidden" name="csrfToken"
                       value="<c:out value='${csrfToken}'/>">
                <input type="hidden" name="token"
                       value="<c:out value='${token}'/>">

                <label for="password">New password</label>
                <input id="password" name="password" type="password"
                       minlength="8" maxlength="64"
                       autocomplete="new-password" required>
                <p class="hint">At least 8 characters.</p>

                <label for="confirmPassword">Confirm new password</label>
                <input id="confirmPassword" name="confirmPassword"
                       type="password" minlength="8" maxlength="64"
                       autocomplete="new-password" required>

                <button type="submit">Save new password</button>
            </form>
        </c:if>

        <p class="back">
            <a href="${pageContext.request.contextPath}/login">
                ← Back to sign in
            </a>
        </p>
    </main>
</body>
</html>
