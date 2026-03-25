<#-- @ftlvariable name="link" type="String" -->
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Email Confirmation</title>
    <style>
        body {
            margin: 0;
            padding: 0;
            background-color: #f4f4f4;
        }

        .outer-table {
            width: 100%;
            background-color: #f4f4f4;
            margin: 0;
            padding: 0;
        }

        .container {
            width: 100%;
            max-width: 600px;
            margin: 0 auto;
            background-color: #ffffff;
            border-radius: 8px;
            overflow: hidden;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
            border-collapse: collapse;
            border-spacing: 0;
        }

        .header {
            background-color: #6366f1;
            color: #ffffff;
            padding: 20px;
            text-align: center;
            font-family: Arial, sans-serif;
        }

        .header h1 {
            margin: 0;
            font-size: 24px;
        }

        .content {
            padding: 20px;
            font-family: Arial, sans-serif;
            color: #333333;
            font-size: 16px;
            line-height: 1.5;
        }

        .content h2 {
            margin-top: 0;
        }

        .button {
            display: inline-block;
            background-color: #6366f1;
            color: #ffffff !important;
            text-decoration: none;
            padding: 12px 24px;
            border-radius: 4px;
            font-weight: bold;
        }

        .footer {
            padding: 20px;
            text-align: center;
            font-family: Arial, sans-serif;
            font-size: 12px;
            color: #aaaaaa;
        }
    </style>
</head>
<body>
<table class="outer-table">
    <tr>
        <td>
            <table class="container">
                <tr>
                    <td class="header">
                        <h1>Registration Confirmation</h1>
                    </td>
                </tr>
                <tr>
                    <td class="content">
                        <h2>Dear User!</h2>
                        <p>Thank you for registering with TicketFlow — your ultimate booking platform.</p>
                        <p>Please confirm your registration by clicking the button below:</p>
                        <p style="text-align:center; margin:30px 0;">
                            <a href="${link}" class="button">Confirm Registration</a>
                        </p>
                        <p>If the button doesn’t work, copy and paste this URL into your browser:</p>
                        <p><a href="${link}">${link}</a></p>
                    </td>
                </tr>
                <tr>
                    <td class="footer">
                        © 2025 TicketFlow SaaS. All rights reserved.
                    </td>

                </tr>
            </table>
        </td>
    </tr>
</table>
</body>
</html>
