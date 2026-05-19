<#-- @ftlvariable name="movieTitle" type="java.lang.String" -->
<#-- @ftlvariable name="hallName" type="java.lang.String" -->
<#-- @ftlvariable name="showtime" type="java.lang.String" -->
<#-- @ftlvariable name="ticketsCount" type="java.lang.Integer" -->
<#-- @ftlvariable name="totalPrice" type="java.math.BigDecimal" -->
<#-- @ftlvariable name="orderId" type="java.lang.String" -->
<#-- @ftlvariable name="t_thank_you" type="java.lang.String" -->
<#-- @ftlvariable name="t_order_info" type="java.lang.String" -->
<#-- @ftlvariable name="t_hall" type="java.lang.String" -->
<#-- @ftlvariable name="t_time" type="java.lang.String" -->
<#-- @ftlvariable name="t_tickets_count" type="java.lang.String" -->
<#-- @ftlvariable name="t_total" type="java.lang.String" -->
<#-- @ftlvariable name="t_title" type="java.lang.String" -->

<!DOCTYPE html>
<html lang="${.locale}">
<head>
    <meta charset="UTF-8">
    <title>${t_title}</title>
    <style>
        .ticket-box { border: 2px dashed #6366f1; padding: 20px; max-width: 400px; font-family: sans-serif; background-color: #f8fafc; border-radius: 12px; }
        .label { color: #64748b; font-size: 12px; text-transform: uppercase; font-weight: bold; }
        .value { color: #1e293b; font-size: 16px; font-weight: bold; margin-bottom: 10px; }
    </style>
</head>
<body>
<h2 style="color: #4f46e5;">${t_thank_you}</h2>
<p>${t_order_info}: <strong>${movieTitle}</strong></p>

<div class="ticket-box">
    <div class="label">${t_hall}</div>
    <div class="value">${hallName}</div>

    <div class="label">${t_time}</div>
    <div class="value">${showtime}</div>

    <div class="label">${t_tickets_count}</div>
    <div class="value">${ticketsCount}</div>
</div>

<h3>${t_total}: ${totalPrice} EUR</h3>
<p style="color: #94a3b8; font-size: 10px;">Order ID: ${orderId}</p>
</body>
</html>
