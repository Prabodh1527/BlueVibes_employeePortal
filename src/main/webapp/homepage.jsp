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

        .sidebar { width: var(--sidebar-width); background: var(--primary); display: flex; flex-direction: column; flex-shrink: 0; border-right: 1px solid rgba(255,255,255,0.1); overflow-y:auto; scrollbar-width: thin;}
        .sidebar-header { padding: 2.5rem 1.5rem; text-align: center; border-bottom: 1px solid rgba(255,255,255,0.05); }
        .brand-container{
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 8px;
            margin-bottom: 10px;
        }
        .logo-main{
            width: 120px;
            height: auto;
            object-fit: contain;
        }
        .logo-secondary{
            width: 220px;
            height: auto;
            object-fit: contain;
        }
        .summary-card{
            background:white;
            border-radius:15px;
            padding:25px;
            border:1px solid #e2e8f0;
        }
        
        .summary-card h3{
            margin-bottom:20px;
        }
        
        .summary-grid{
            display:grid;
            grid-template-columns:repeat(4,1fr);
            gap:20px;
        }
        
        .summary-box{
            background:#f8fafc;
            padding:20px;
            border-radius:12px;
            text-align:center;
            border:1px solid #e2e8f0;
        }
        
        .summary-box span{
            display:block;
            color:#64748b;
            margin-bottom:10px;
        }
        
        .summary-box strong{
            font-size:2rem;
            color:#0f172a;
        }
        .quick-actions-card{
            background:white;
            border-radius:15px;
            padding:25px;
            border:1px solid #e2e8f0;
            margin-bottom:25px;
        }
        
        .quick-actions-card h3{
            margin-bottom:20px;
        }
        
        .quick-grid{
            display:grid;
            grid-template-columns:repeat(4,1fr);
            gap:20px;
        }
        
        .quick-btn{
            text-decoration:none;
            background:#f8fafc;
            border:1px solid #e2e8f0;
            border-radius:12px;
            padding:25px;
            text-align:center;
            transition:0.3s;
            color:#0f172a;
        }
        
        .quick-btn:hover{
            background:#0284c7;
            color:white;
            transform:translateY(-3px);
        }
        
        .quick-btn i{
            font-size:2rem;
            display:block;
            margin-bottom:12px;
        }
        
        .quick-btn span{
            font-weight:600;
        }

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

        .welcome-box { padding: 2rem; border-radius: 20px; color: white; margin-bottom: 1.2rem; background: linear-gradient(135deg, #0f172a 0%, #0284c7 100%); position: relative; overflow: hidden; }
        .welcome-box h1 { font-size: 1.8rem; margin-bottom: 6px; }
        .welcome-box p { font-size: 1rem; opacity: 0.9, margin: 0; }

        .stat-container { display: flex; gap: 20px; margin-top: 25px; }
        .stat-item { background: rgba(255,255,255,0.1); padding: 15px 25px; border-radius: 12px; backdrop-filter: blur(5px); border: 1px solid rgba(255,255,255,0.2); min-width: 200px; }
        .stat-label { display: block; font-size: 0.8rem; opacity: 0.8; }
        .stat-value { font-size: 1.8rem; font-weight: 700; }
    </style>
</head>
<body>
<aside class="sidebar">
    <div class="sidebar-header">
        <div class="brand-container"><img src="blueeye.png" class="logo-main"><img src="bluedigital.png" class="logo-secondary"></div>
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
        <a href="travelrequest.jsp" class="nav-item"><i class="ph ph-airplane"></i> Travel Request</a>
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

            
        </div>

        <div class="quick-actions-card">
            <h3>Quick Actions</h3>
        
            <div class="quick-grid">
        
                <a href="user_notifications.html" class="quick-btn">
                    <i class="ph ph-bell"></i>
                    <span>Latest Notifications</span>
                </a>
        
                <a href="weeklyreport.html" class="quick-btn">
                    <i class="ph ph-clipboard-text"></i>
                    <span>Update WSR</span>
                </a>
        
                <a href="leavereq.html" class="quick-btn">
                    <i class="ph ph-calendar-x"></i>
                    <span>Apply Leave</span>
                </a>
        
            </div>
        </div>

        <div class="summary-card">
            <h3>Weekly Status Summary</h3>
        
            <div class="summary-grid">
                <div class="summary-box">
                    <span>Total Tasks</span>
                    <strong id="totalTasks">0</strong>
                </div>
        
                <div class="summary-box">
                    <span>Completed</span>
                    <strong id="completedTasks">0</strong>
                </div>
        
                <div class="summary-box">
                    <span>In Progress</span>
                    <strong id="inProgressTasks">0</strong>
                </div>
        
                <div class="summary-box">
                    <span>Open</span>
                    <strong id="openTasks">0</strong>
                </div>
            </div>
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

        function loadTaskSummary() {
        
            fetch('WeeklyReportServlet?action=fetchMyReports')
                .then(res => res.json())
                .then(tasks => {
        
                    let total = tasks.length;
                    let completed = 0;
                    let open = 0;
                    let inProgress = 0;
        
                    tasks.forEach(task => {
        
                        if (task.status === "Completed") {
                            completed++;
                        }
                        else if (task.status === "Open") {
                            open++;
                        }
                        else {
                            inProgress++;
                        }
        
                    });
        
                    document.getElementById("statTasks").innerText = total;
        
                    document.getElementById("totalTasks").innerText = total;
                    document.getElementById("completedTasks").innerText = completed;
                    document.getElementById("openTasks").innerText = open;
                    document.getElementById("inProgressTasks").innerText = inProgress;
                })
                .catch(err => {
                    console.error("Error loading task summary:", err);
                });
        }

        window.onload = function() {
            const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
            document.getElementById('currentDateDisplay').innerText = new Date().toLocaleDateString(undefined, options);
            loadStats();
            loadTaskSummary();
        };
    </script>
</body>
</html>

