<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String userName = (String) session.getAttribute("userName");
    if (userName == null) { response.sendRedirect("index.html"); return; }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>BlueVibes | Employee Dashboard</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/@phosphor-icons/web"></script>
    <style>
        :root { --sidebar-width: 260px; --primary: #0f172a; --accent: #0284c7; --bg: #f1f5f9; --border: #e2e8f0; --text: #1e293b; --white: #ffffff; }
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Inter', sans-serif; background: var(--bg); color: var(--text); display: flex; height: 100vh; overflow: hidden; }

        .sidebar { width: var(--sidebar-width); background: var(--primary); display: flex; flex-direction: column; flex-shrink: 0; border-right: 1px solid rgba(255,255,255,0.1); }
        .sidebar-header { padding: 2.5rem 1.5rem; text-align: center; border-bottom: 1px solid rgba(255,255,255,0.05); }
        .brand-container { display: flex; justify-content: center; align-items: center; gap: 12px; margin-bottom: 10px; }
        .brand-name { font-size: 1.8rem; font-weight: 800; color: white; letter-spacing: 4px; margin-top: 10px; background: linear-gradient(to right, #fff, #64748b); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
        .sidebar-logo { width: 80px; height: 80px; background: transparent; padding: 0; border-radius: 8px; object-fit: contain; filter: drop-shadow(0 4px 6px rgba(0,0,0,0.3)); }

        .nav-links { list-style: none; padding: 1.5rem 1rem; flex-grow: 1; overflow-y: auto; }
        .nav-item { display: flex; align-items: center; gap: 12px; padding: 0.8rem 1rem; border-radius: 8px; color: #94a3b8; text-decoration: none; margin-bottom: 0.5rem; transition: 0.3s; font-size: 0.9rem; }
        .nav-item:hover, .nav-item.active { background: var(--accent); color: white; }
        .sidebar-footer { padding: 1.5rem 1rem; border-top: 1px solid rgba(255,255,255,0.1); }
        .logout-item { color: #f87171 !important; text-decoration: none; display: flex; align-items: center; gap: 12px; padding: 0.8rem 1rem; border-radius: 8px; transition: 0.3s; font-size: 0.9rem; }
        .logout-item:hover { background: rgba(239, 68, 68, 0.1) !important; color: #ef4444 !important; }

        .main-wrapper { flex-grow: 1; display: flex; flex-direction: column; overflow: hidden; }
        .top-header { height: 70px; background: var(--primary); color: white; display: flex; align-items: center; justify-content: space-between; padding: 0 2.5rem; }
        .content-area { padding: 2.5rem; overflow-y: auto; flex-grow: 1; }

        .welcome-box { padding: 3rem; border-radius: 20px; color: white; margin-bottom: 2rem; background: linear-gradient(135deg, #0f172a 0%, #0284c7 100%); position: relative; overflow: hidden; }
        .welcome-box h1 { font-size: 2.2rem; margin-bottom: 10px; }
        .welcome-box p { font-size: 1.1rem; opacity: 0.9; }

        .stat-container { display: flex; gap: 20px; margin-top: 25px; }
        .stat-item { background: rgba(255,255,255,0.1); padding: 15px 25px; border-radius: 12px; backdrop-filter: blur(5px); border: 1px solid rgba(255,255,255,0.2); min-width: 200px; }
        .stat-label { display: block; font-size: 0.8rem; opacity: 0.8; }
        .stat-value { font-size: 1.8rem; font-weight: 700; }
    </style>
</head>
<body>
<aside class="sidebar">
    <div class="sidebar-header">
        <div class="brand-container"><img src="logo1.jpeg" class="sidebar-logo"><img src="logo.jpeg" class="sidebar-logo"></div>
        <div class="brand-name">BlueVibes</div>
    </div>
    <nav class="nav-links">
        <a href="homepage.jsp" class="nav-item active"><i class="ph ph-house"></i> Dashboard</a>
        <a href="LoadProfileServlet" class="nav-item"><i class="ph ph-user"></i> My Profile</a>
        <a href="attendance.html" class="nav-item"><i class="ph ph-calendar-check"></i> Attendance & Calender</a>
        <a href="weeklyreport.html" class="nav-item"><i class="ph ph-clipboard-text"></i> Weekly Status Report</a>
        <a href="user_notifications.html" class="nav-item"><i class="ph ph-broadcast"></i> Notifications</a>
        <a href="payslips.jsp" class="nav-item"><i class="ph ph-receipt"></i> Payslips</a>
        <a href="appraisals.html" class="nav-item"><i class="ph ph-chart-line-up"></i> KRA Appraisals</a>
        <a href="policyview.jsp" class="nav-item"><i class="ph ph-scroll"></i> Company Policies</a>
        <a href="leavereq.html" class="nav-item"><i class="ph ph-calendar-x"></i> Leave Request</a>
    </nav>
    <div class="sidebar-footer"><a href="index.html" class="logout-item"><i class="ph ph-sign-out"></i> Logout Account</a></div>
</aside>

<main class="main-wrapper">
    <header class="top-header">
        <h2 style="font-weight: 700;">Dashboard</h2>
        <div style="text-align: right;"><span id="currentDateDisplay" style="font-size: 0.9rem; opacity: 0.9;"></span></div>
    </header>
    <div class="content-area">
        <div class="welcome-box">
            <h1>Welcome <%= userName %>!</h1>
            <p>You are a part of <strong>BlueVibes</strong>.</p>

            <div class="stat-container">
                <div class="stat-item">
                    <span class="stat-label">Monthly Attendance</span>
                    <strong id="statAttendance" class="stat-value">-- Days</strong>
                </div>
                <div class="stat-item">
                    <span class="stat-label">Yearly Leaves</span>
                    <strong id="statLeaves" class="stat-value">-- / 24</strong>
                </div>
            </div>
        </div>

        <div style="padding: 2rem; text-align: center; color: #94a3b8; border: 2px dashed #e2e8f0; border-radius: 15px;">
            Additional dashboard modules will be added here.
        </div>
    </div>

    <script>
        function loadStats() {
            // Fetch stats for the top cards
            fetch('AttendanceServlet?action=stats')
                .then(res => res.json())
                .then(data => {
                    document.getElementById('statAttendance').innerText = (data.present || 0) + " Days";
                    document.getElementById('statLeaves').innerText = (data.leaves || 0) + " / 24";
                })
                .catch(err => {
                    console.error("Error loading stats:", err);
                });
        }

        window.onload = function() {
            const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
            document.getElementById('currentDateDisplay').innerText = new Date().toLocaleDateString(undefined, options);
            loadStats();
        };
    </script>
</body>
</html>

