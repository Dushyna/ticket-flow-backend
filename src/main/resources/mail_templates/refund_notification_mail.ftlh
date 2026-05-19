<#-- @ftlvariable name="totalPrice" type="java.math.BigDecimal" -->
<#-- @ftlvariable name="orderId" type="java.lang.String" -->
<#-- @ftlvariable name="t_subject" type="java.lang.String" -->
<#-- @ftlvariable name="t_header" type="java.lang.String" -->
<#-- @ftlvariable name="t_message" type="java.lang.String" -->
<#-- @ftlvariable name="t_total" type="java.lang.String" -->

<!DOCTYPE html>
<html lang="${.locale}">
<head>
    <meta charset="UTF-8">
    <title>${t_subject}</title>
    <style>
        .notification-box {
            border: 2px solid #ef4444;
            padding: 20px;
            max-width: 500px;
            font-family: sans-serif;
            background-color: #fef2f2;
            border-radius: 12px;
        }
        .header { color: #b91c1c; font-size: 18px; font-weight: bold; margin-bottom: 15px; }
        .message { color: #475569; font-size: 14px; line-height: 1.6; margin-bottom: 20px; }
        .total { color: #1e293b; font-size: 16px; font-weight: bold; border-top: 1px solid #fee2e2; padding-top: 10px; }
    </style>
</head>
<body>
<div class="notification-box">
    <div class="header">${t_header}</div>
    <div class="message">${t_message}</div>

    <div class="total">
        ${t_total}: ${totalPrice} EUR
    </div>
    <p style="color: #94a3b8; font-size: 10px; margin-top: 20px;">Order ID: ${orderId}</p>
</div>
</body>
</html>
