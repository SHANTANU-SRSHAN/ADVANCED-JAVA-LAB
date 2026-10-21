<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Create account | ShopSphere</title>

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

        .register-card {
            width: 100%;
            max-width: 460px;
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
            margin: 18px 0 7px;
            font-size: 14px;
        }

        input {
            width: 100%;
            padding: 13px;
            color: white;
            background: #0b100d;
            border: 1px solid #405146;
            border-radius: 8px;
            font: inherit;
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
            font-size: 15px;
        }

        button:hover { background: #65d6a0; }

        a { color: #65d6a0; }

        .error {
            color: #ff9292;
            background: #2a1414;
            border: 1px solid #5a2626;
            padding: 10px 12px;
            border-radius: 8px;
            font-size: 14px;
        }

        .hint { font-size: 12px; color: #7f8f84; margin-top: 6px; }
    </style>
</head>

<body>
<main class="register-card">
    <a class="logo" href="${pageContext.request.contextPath}/home">
        ShopSphere<span>.</span>
    </a>

    <h1>Create your account</h1>
    <p>Join ShopSphere and start shopping in seconds.</p>

    <c:if test="${not empty error}">
        <p class="error"><c:out value="${error}"/></p>
    </c:if>

    <form method="post"
          action="${pageContext.request.contextPath}/register">

        <%-- CSRF protection --%>
        <input type="hidden" name="csrfToken"
               value="<c:out value='${csrfToken}'/>">

        <label for="fullName">Full name</label>
        <input id="fullName" name="fullName" type="text"
               maxlength="100" autocomplete="name" required
               value="<c:out value='${fullName}'/>">

        <label for="email">Email address</label>
        <input id="email" name="email" type="email"
               maxlength="150" autocomplete="email" required
               value="<c:out value='${email}'/>">

        <label for="phone">Phone number (optional)</label>
        <input id="phone" name="phone" type="tel"
               maxlength="10" pattern="[0-9]{10}"
               placeholder="10-digit mobile number"
               autocomplete="tel"
               value="<c:out value='${phone}'/>">

        <label for="password">Password</label>
        <input id="password" name="password" type="password"
               minlength="8" maxlength="64"
               autocomplete="new-password" required>
        <p class="hint">At least 8 characters, with a letter and a number.</p>

        <label for="confirmPassword">Confirm password</label>
        <input id="confirmPassword" name="confirmPassword" type="password"
               minlength="8" maxlength="64"
               autocomplete="new-password" required>

        <button type="submit">Create Account</button>
    </form>

    <p>
        Already have an account?
        <a href="${pageContext.request.contextPath}/login">Log in</a>
        &nbsp;·&nbsp;
        <a href="${pageContext.request.contextPath}/home">Back home</a>
    </p>
</main>
</body>
</html>
