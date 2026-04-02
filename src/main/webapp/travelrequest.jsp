<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String userName = (String) session.getAttribute("userName");
    if (userName == null) { response.sendRedirect("index.html"); return; }
%>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>BlueVibes | Travel Request</title>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
<script src="https://unpkg.com/@phosphor-icons/web"></script>

<style>
:root { --sidebar-width: 260px; --primary: #0f172a; --accent: #0284c7; --bg: #f1f5f9; --border: #e2e8f0; --text: #1e293b; }
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: 'Inter', sans-serif; background: var(--bg); color: var(--text); display: flex; height: 100vh; overflow: hidden; }

.sidebar { width: var(--sidebar-width); background: var(--primary); display: flex; flex-direction: column; border-right: 1px solid rgba(255,255,255,0.1); }
.sidebar-header { padding: 2.5rem 1.5rem; text-align: center; border-bottom: 1px solid rgba(255,255,255,0.05); }
.brand-container { display: flex; justify-content: center; gap: 12px; margin-bottom: 10px; }
.brand-name { font-size: 1.8rem; font-weight: 800; color: white; letter-spacing: 4px; margin-top: 10px; background: linear-gradient(to right, #fff, #64748b); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.sidebar-logo { width: 80px; height: 80px; object-fit: contain; }

.nav-links { list-style: none; padding: 1.5rem 1rem; flex-grow: 1; }
.nav-item { display: flex; align-items: center; gap: 12px; padding: 0.8rem 1rem; border-radius: 8px; color: #94a3b8; text-decoration: none; margin-bottom: 0.5rem; transition: 0.3s; font-size: 0.9rem; }
.nav-item:hover, .nav-item.active { background: var(--accent); color: white; }

.logout-sect { padding: 1.5rem; border-top: 1px solid rgba(255,255,255,0.1); }
.logout-btn { color: #f87171; text-decoration: none; display: flex; align-items: center; gap: 10px; font-weight: 600; font-size: 0.9rem; }

.main-wrapper { flex-grow: 1; display: flex; flex-direction: column; }
.top-header { height: 70px; background: var(--primary); color: white; display: flex; align-items: center; padding: 0 2.5rem; }
.content-area { padding: 2.5rem; overflow-y:auto; }

.card { background: white; border-radius: 12px; border: 1px solid var(--border); padding: 2rem; }

table { width:100%; border-collapse: collapse; }
th { background:#0f172a; color:white; padding:10px; font-size:0.8rem; }
td { padding:8px; border:1px solid var(--border); }
input, select { width:100%; padding:6px; border:1px solid var(--border); border-radius:6px; }

.btn-submit {
    background: var(--accent);
    color: white;
    padding: 10px 18px;
    border: none;
    border-radius: 8px;
    font-weight: 600;
    cursor: pointer;
    margin-right:10px;
}
</style>
</head>

<body>

<aside class="sidebar">
<div class="sidebar-header">
<div class="brand-container">
<img src="logo1.jpeg" class="sidebar-logo">
<img src="logo.jpeg" class="sidebar-logo">
</div>
<div class="brand-name">BlueVibes</div>
</div>

<nav class="nav-links">
<a href="homepage.jsp" class="nav-item"><i class="ph ph-house"></i> Dashboard</a>
<a href="LoadProfileServlet" class="nav-item"><i class="ph ph-user"></i> My Profile</a>
<a href="attendance.html" class="nav-item"><i class="ph ph-calendar-check"></i> Attendance & Calender</a>
<a href="weeklyreport.html" class="nav-item"><i class="ph ph-clipboard-text"></i> Weekly Status Report</a>
<a href="user_notifications.html" class="nav-item"><i class="ph ph-broadcast"></i> Notifications</a>
<a href="payslips.jsp" class="nav-item"><i class="ph ph-receipt"></i> Payslips</a>
<a href="appraisals.html" class="nav-item"><i class="ph ph-chart-line-up"></i> KRA Appraisals</a>
<a href="policyview.jsp" class="nav-item"><i class="ph ph-scroll"></i> Company Policies</a>

<!-- ✅ ADDED -->
<a href="travelrequest.jsp" class="nav-item active"><i class="ph ph-airplane"></i> Travel Request</a>

<a href="leavereq.html" class="nav-item"><i class="ph ph-calendar-x"></i> Leave Request</a>
</nav>

<div class="logout-sect">
<a href="index.html" class="logout-btn"><i class="ph ph-power"></i> Logout Account</a>
</div>
</aside>

<main class="main-wrapper">
<header class="top-header"><h2 style="font-weight:700;">Tour Advance / Reimbursement</h2></header>

<div class="content-area">
<div class="card">

<table id="travelTable">
<tr>
<th>Type</th>
<th>From</th>
<th>To</th>
<th>From Date</th>
<th>To Date</th>
<th>Purpose</th>
<th>Amount</th>
<th>Remarks</th>
</tr>

<tr>
<td>
<select>
<option>Travel</option>
<option>Accommodation</option>
</select>
</td>
<td><input type="text"></td>
<td><input type="text"></td>
<td><input type="date"></td>
<td><input type="date"></td>
<td><input type="text"></td>
<td><input type="number" class="amt" oninput="calcTotal()"></td>
<td><input type="text"></td>
</tr>

<tr>
<td colspan="6" style="text-align:right;font-weight:bold;">Total</td>
<td><input type="number" id="totalAmount" readonly></td>
<td></td>
</tr>
</table>

<br>

<button class="btn-submit" onclick="addRow()">+ Add Row</button>
<button class="btn-submit" onclick="submitData()">Save</button>

</div>
</div>
</main>

<script>
function addRow(){
    let table=document.getElementById("travelTable");
    let row=table.insertRow(table.rows.length-1);

    row.innerHTML=`
    <td>
    <select>
    <option>Travel</option>
    <option>Accommodation</option>
    </select>
    </td>
    <td><input type="text"></td>
    <td><input type="text"></td>
    <td><input type="date"></td>
    <td><input type="date"></td>
    <td><input type="text"></td>
    <td><input type="number" class="amt" oninput="calcTotal()"></td>
    <td><input type="text"></td>
    `;
}

function calcTotal(){
    let total=0;
    document.querySelectorAll(".amt").forEach(a=>{
        total+=Number(a.value)||0;
    });
    document.getElementById("totalAmount").value=total;
}

function submitData(){
    alert("Next step: backend integration");
}
</script>

</body>
</html>
