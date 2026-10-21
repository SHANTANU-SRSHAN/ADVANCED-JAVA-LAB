<%@ page contentType="text/html;charset=UTF-8" %>
 <!DOCTYPE html>
 <html>
 <head>
<title>Student <body> Information</title></head> <h1>Student Information</h1> <form action="hello" method="post"> Name: <input type="text" name="name"><br><br> Email: <input type="email" name="email"><br><br> Course: <input type="text" name="course"><br><br> Age: <input type="number" name="age"><br><br> <input type="submit" value="Register"> </form> </body> </html>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Student Login</title>
    <style>
        * { box-sizing: border-box; }
        body {
            font-family: Arial, sans-serif;
            background: #f0f2f5;
            margin: 0;
            padding: 0;
        }
        .login-box {
            width: 340px;
            margin: 120px auto;
            background: #fff;
            padding: 30px;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        .login-box h1 {
            text-align: center;
            color: #333;
            margin-bottom: 20px;
        }
        .login-box label {
            display: block;
            margin-top: 12px;
            font-weight: bold;
            color: #555;
        }
        .login-box input[type="text"],
        .login-box input[type="password"] {
            width: 100%;
            padding: 9px;
            margin-top: 6px;
            border: 1px solid #ccc;
            border-radius: 4px;
        }
        .login-box button {
            width: 100%;
            padding: 10px;
            margin-top: 18px;
            background: #007bff;
            color: #fff;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 14px;
        }
        .login-box button:hover { background: #0069d9; }
    </style>
</head>
<body>
    <div class="login-box">
        <h1>Student Login</h1>
        <form action="login" method="post">
            <label for="username">Username</label>
            <input type="text" id="username" name="username" placeholder="Enter username" required>

            <label for="password">Password</label>
            <input type="password" id="password" name="password" placeholder="Enter password" required>

            <button type="submit">Login</button>
        </form>
    </div>
</body>
</html>
