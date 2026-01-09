<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String userName = (String) session.getAttribute("userName");
    if (userName == null) { response.sendRedirect("index.html"); return; }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>BlueVibes | Company Policies</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/@phosphor-icons/web"></script>
    <style>
        :root { --sidebar-width: 260px; --primary: #0f172a; --accent: #0284c7; --bg: #f1f5f9; --border: #e2e8f0; --text: #1e293b; }
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Inter', sans-serif; background: var(--bg); color: var(--text); display: flex; height: 100vh; overflow: hidden; }

        .sidebar { width: var(--sidebar-width); background: var(--primary); display: flex; flex-direction: column; flex-shrink: 0; border-right: 1px solid rgba(255,255,255,0.1); }
        .sidebar-header { padding: 2.5rem 1.5rem; text-align: center; border-bottom: 1px solid rgba(255,255,255,0.05); }
        .brand-container { display: flex; justify-content: center; align-items: center; gap: 12px; margin-bottom: 10px; }
        .brand-name { font-size: 1.8rem; font-weight: 800; color: white; letter-spacing: 4px; margin-top: 10px; background: linear-gradient(to right, #fff, #64748b); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
        .sidebar-logo { width: 80px; height: 80px; object-fit: contain; filter: drop-shadow(0 4px 6px rgba(0,0,0,0.3)); }

        .nav-links { list-style: none; padding: 1.5rem 1rem; flex-grow: 1; }
        .nav-item { display: flex; align-items: center; gap: 12px; padding: 0.8rem 1rem; border-radius: 8px; color: #94a3b8; text-decoration: none; margin-bottom: 0.5rem; transition: 0.3s; font-size: 0.9rem; }
        .nav-item:hover, .nav-item.active { background: var(--accent); color: white; }

        .logout-sect { padding: 1.5rem; border-top: 1px solid rgba(255,255,255,0.1); }
        .logout-btn { color: #f87171; text-decoration: none; display: flex; align-items: center; gap: 10px; font-weight: 600; font-size: 0.9rem; }

        .main-wrapper { flex-grow: 1; display: flex; flex-direction: column; overflow: hidden; }
        .top-header { height: 70px; background: var(--primary); color: white; display: flex; align-items: center; justify-content: space-between; padding: 0 2.5rem; }
        .content-area { padding: 2.5rem; overflow-y: auto; flex-grow: 1; }

        .policy-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 20px; }
        .policy-card { background: white; border-radius: 12px; border: 1px solid var(--border); padding: 1.5rem; display: flex; align-items: center; gap: 15px; transition: 0.3s; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }
        .policy-card:hover { transform: translateY(-5px); box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1); border-color: var(--accent); }
        .policy-icon { width: 50px; height: 50px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 1.5rem; }
        .policy-info { flex-grow: 1; }
        .policy-info h3 { font-size: 0.95rem; margin-bottom: 4px; color: var(--primary); font-weight: 700; word-break: break-all; }
        .btn-download { color: var(--accent); text-decoration: none; font-weight: 700; font-size: 0.8rem; display: flex; align-items: center; gap: 4px; border: none; background: transparent; cursor: pointer; }
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
        <a href="payslips.html" class="nav-item"><i class="ph ph-receipt"></i> Payslips</a>
        <a href="appraisals.html" class="nav-item"><i class="ph ph-chart-line-up"></i> KRA Appraisals</a>
        <a href="policyview.jsp" class="nav-item active"><i class="ph ph-scroll"></i> Company Policies</a>
        <a href="leavereq.html" class="nav-item"><i class="ph ph-calendar-x"></i> Leave Request</a>
    </nav>
    <div class="logout-sect">
        <a href="index.html" class="logout-btn"><i class="ph ph-power"></i> Logout Account</a>
    </div>
</aside>

<main class="main-wrapper">
    <header class="top-header"><h2 style="font-weight: 700;"> Company Policies</h2></header>
    <div class="content-area">
        <div id="policyContainer" class="policy-grid">
            <p style="color: #94a3b8;">Syncing with BlueVibes document server...</p>
        </div>
    </div>
</main>

<script>
    function loadUserPolicies() {
        fetch('PolicyServlet')
            .then(res => res.json())
            .then(data => {
                const container = document.getElementById('policyContainer');
                container.innerHTML = "";

                if(!data || data.length === 0) {
                    container.innerHTML = '<p style="padding: 2rem; color: #94a3b8;">No documents found.</p>';
                    return;
                }

                data.forEach(p => {
                    const name = p.name.toLowerCase();
                    let icon = name.endsWith('.pdf') ? "ph-file-pdf" : "ph-file-xls";
                    let color = name.endsWith('.pdf') ? "#ef4444" : "#10b981";

                    container.innerHTML += `
                    <div class="policy-card">
                        <div class="policy-icon" style="color: ${color}; background: ${color}15;">
                            <i class="ph ${icon}"></i>
                        </div>
                        <div class="policy-info">
                            <h3>${p.name}</h3>
                            <a href="PolicyServlet?action=view&id=` + p.id + `" class="btn-download">
                                <i class="ph ph-download-simple"></i> Download BlueVibes Policy
                            </a>
                        </div>
                    </div>`;
                });
            })
            .catch(err => console.error("Fetch error:", err));
    }
    window.onload = loadUserPolicies;
</script>
</body>
</html>