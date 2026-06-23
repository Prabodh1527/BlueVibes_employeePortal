<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String userEmail = (String) session.getAttribute("userEmail");
    if (userEmail == null) {
        response.sendRedirect("index.html");
        return;
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>BlueVibes | Download Payslips</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/@phosphor-icons/web"></script>
    <style>
        :root {
            --sidebar-width:240px;
            --primary:#0f172a;
            --accent:#0284c7;
            --bg:#f1f5f9;
            --border:#e2e8f0;
            --text:#1e293b;
            --white:#ffffff;
        }
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Inter', sans-serif; background: var(--bg); color: var(--text); display: flex; height: 100vh; overflow: hidden; }

        
        .sidebar{
            width:var(--sidebar-width);
            background:linear-gradient(180deg,#0f172a 0%,#111827 100%);
            display:flex;
            flex-direction:column;
            flex-shrink:0;
            overflow:hidden;
            border-right:1px solid rgba(255,255,255,0.08);
        }
        
        .sidebar-header{
            padding:18px 15px 12px;
            text-align:center;
            border-bottom:1px solid rgba(255,255,255,0.08);
        }
        
        .brand-container{
            display:flex;
            flex-direction:column;
            align-items:center;
            gap:6px;
            margin-bottom:6px;
        }
        
        .logo-main{
            width:75px;
            height:auto;
            object-fit:contain;
        }
        
        .logo-secondary{
            width:150px;
            height:auto;
            object-fit:contain;
        }
        
        .brand-name{
            font-size:1.45rem;
            font-weight:800;
            color:white;
            letter-spacing:2px;
            margin-top:5px;
            background:linear-gradient(to right,#ffffff,#94a3b8);
            -webkit-background-clip:text;
            -webkit-text-fill-color:transparent;
        }
        
        .nav-links{
            list-style:none;
            padding:12px;
            flex-grow:1;
            overflow-y:auto;
            scrollbar-width:thin;
        }
        
        .nav-links::-webkit-scrollbar{
            width:5px;
        }
        
        .nav-links::-webkit-scrollbar-thumb{
            background:#334155;
            border-radius:10px;
        }
        
        .nav-links::-webkit-scrollbar-track{
            background:transparent;
        }
        
        .nav-item{
            display:flex;
            align-items:center;
            gap:12px;
            padding:12px 14px;
            border-radius:12px;
            color:#cbd5e1;
            text-decoration:none;
            margin-bottom:6px;
            transition:all .25s ease;
            font-size:14px;
            font-weight:500;
        }
        
        .nav-item i{
            font-size:18px;
            min-width:20px;
        }
        
        .nav-item:hover{
            background:rgba(2,132,199,.18);
            color:white;
            transform:translateX(3px);
        }
        
        .nav-item.active{
            background:#0284c7;
            color:white;
            box-shadow:0 4px 12px rgba(2,132,199,.35);
        }
        
        .logout-sect{
            padding:12px;
            border-top:1px solid rgba(255,255,255,.08);
        }
        
        .logout-btn{
            color:#f87171 !important;
            text-decoration:none;
            display:flex;
            align-items:center;
            gap:12px;
            padding:12px 14px;
            border-radius:12px;
            transition:.25s;
            font-size:14px;
        }
        
        .logout-btn:hover{
            background:rgba(239,68,68,.12);
            color:#fca5a5 !important;
        }
        
        .logout-btn i{
            font-size:18px;
        }

        .main-wrapper { flex-grow: 1; display: flex; flex-direction: column; overflow: hidden; }
        .top-header { height: 70px; background: var(--primary); color: white; display: flex; align-items: center; padding: 0 2.5rem; }
        .content-area { padding: 2.5rem; overflow-y: auto; flex-grow: 1; }

        .card { background: white; border-radius: 12px; border: 1px solid var(--border); padding: 2rem; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
        .ps-row { display: flex; justify-content: space-between; align-items: center; padding: 1.2rem; border: 1px solid var(--border); border-radius: 12px; margin-bottom: 15px; transition: 0.2s; }
        .ps-row:hover { border-color: var(--accent); background: #f8fafc; transform: translateY(-2px); }

        .btn-dl { background: white; border: 1.5px solid var(--border); padding: 10px 20px; border-radius: 10px; font-weight: 600; display: flex; align-items: center; gap: 8px; font-size: 0.9rem; color: var(--primary); text-decoration: none; }
        .btn-dl:hover { background: var(--accent); color: white; border-color: var(--accent); }
    </style>
</head>
<body>

<aside class="sidebar">
    <div class="sidebar-header">
        <div class="brand-container">
            <img src="blueeye.png" class="logo-main">
            <img src="bluedigital.png" class="logo-secondary">
        </div>
        <div class="brand-name">BlueVibes</div>
    </div>
    <nav class="nav-links">
        <a href="homepage.jsp" class="nav-item"><i class="ph ph-house"></i> Dashboard</a>
        <a href="LoadProfileServlet" class="nav-item"><i class="ph ph-user"></i> My Profile</a>
        <a href="attendance.html" class="nav-item"><i class="ph ph-calendar-check"></i> Attendance & Calender</a>
        <a href="weeklyreport.html" class="nav-item"><i class="ph ph-clipboard-text"></i> Weekly Status Report</a>
        <a href="user_notifications.html" class="nav-item"><i class="ph ph-broadcast"></i> Notifications</a>
        <a href="payslips.jsp" class="nav-item active"><i class="ph ph-receipt"></i> Payslips</a>
        <a href="appraisals.html" class="nav-item"><i class="ph ph-chart-line-up"></i> KRA Appraisals</a>
        <a href="know_colleagues.html" class="nav-item">
            <i class="ph ph-users-three"></i> Know Your Colleagues
        </a>
        <a href="policyview.jsp" class="nav-item"><i class="ph ph-scroll"></i> Company Policies</a>
        <a href="travelrequest.jsp" class="nav-item">
            <i class="ph ph-airplane"></i> Travel Request
        </a>
        <a href="leavereq.html" class="nav-item"><i class="ph ph-calendar-x"></i> Leave Request</a>
    </nav>
    <div class="logout-sect">
        <a href="index.html" class="logout-btn"><i class="ph ph-power"></i> Logout Account</a>
    </div>
</aside>

<main class="main-wrapper">
    <header class="top-header"><h2>Payslips</h2></header>
    <div class="content-area">
        <div class="card">
            <h3><i class="ph ph-file-pdf"></i> Monthly Payslips</h3>
            <div id="db-payslip-list">
                <p style="text-align:center; padding:2rem; color:#94a3b8;">Syncing with BlueVibes Financial Records...</p>
            </div>
        </div>
    </div>
</main>

<script>
window.onload = function () {
    fetch('PayslipServlet?action=history&userEmail=<%= userEmail %>')
        .then(res => res.json())
        .then(data => {
            const list = document.getElementById('db-payslip-list');

            if (!data || data.length === 0) {
                list.innerHTML = '<p style="text-align:center; color:#94a3b8;">No payslips available in your BlueVibes record yet.</p>';
                return;
            }

            list.innerHTML = data.map(p => `
                <div class="ps-row">
                    <div>
                        <span style="font-weight:700;">\${formatMonth(p.month_year)}</span>
                        <p style="font-size:0.8rem;color:#64748b;">Uploaded on \${p.uploaded_at || 'N/A'}</p>
                    </div>
                    <a href="PayslipServlet?action=view&id=\${p.id}" target="_blank" class="btn-dl">
                        <i class="ph ph-download-simple"></i> Download PDF
                    </a>
                </div>
            `).join('');
        })
        .catch(() => {
            document.getElementById('db-payslip-list').innerHTML =
                '<p style="text-align:center;color:red;">Error connecting to server</p>';
        });
};

function formatMonth(val) {
    if (!val || !val.includes('-')) return val;
    const [y, m] = val.split('-');
    const months = ["January","February","March","April","May","June","July","August","September","October","November","December"];
    return months[parseInt(m) - 1] + " " + y;
}
</script>

</body>
</html>
