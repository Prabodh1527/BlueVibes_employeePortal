<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String userEmail = (String) session.getAttribute("userEmail");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>BlueVibes | Download Payslips</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/@phosphor-icons/web"></script>
    <style>
        :root { --sidebar-width: 260px; --primary: #0f172a; --accent: #0284c7; --bg: #f1f5f9; --border: #e2e8f0; --text: #1e293b; }
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Inter', sans-serif; background: var(--bg); color: var(--text); display: flex; height: 100vh; overflow: hidden; }

        .sidebar { width: var(--sidebar-width); background: var(--primary); display: flex; flex-direction: column; flex-shrink: 0; border-right: 1px solid rgba(255,255,255,0.1); }

        .sidebar-header {
            padding: 2.5rem 1.5rem;
            text-align: center;
            border-bottom: 1px solid rgba(255,255,255,0.05);
        }
        .brand-container {
            display: flex;
            justify-content: center;
            align-items: center;
            gap: 12px;
            margin-bottom: 10px;
        }
        .brand-name {
            font-size: 1.8rem;
            font-weight: 800;
            color: white;
            letter-spacing: 4px;
            margin-top: 10px;
            background: linear-gradient(to right, #fff, #64748b);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }
        .sidebar-logo {
            width: 80px;
            height: 80px;
            object-fit: contain;
        }

        .nav-links { list-style: none; padding: 1.5rem 1rem; flex-grow: 1; }
        .nav-item { display: flex; align-items: center; gap: 12px; padding: 0.8rem 1rem; border-radius: 8px; color: #94a3b8; text-decoration: none; margin-bottom: 0.5rem; transition: 0.3s; font-size: 0.9rem; }
        .nav-item:hover, .nav-item.active { background: var(--accent); color: white; }

        .logout-sect { padding: 1.5rem; border-top: 1px solid rgba(255,255,255,0.1); }
        .logout-btn { color: #f87171; text-decoration: none; display: flex; align-items: center; gap: 10px; font-weight: 600; font-size: 0.9rem; }

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
            <img src="logo1.jpeg" class="sidebar-logo">
            <img src="logo.jpeg" class="sidebar-logo">
        </div>
        <div class="brand-name">BlueVibes</div>
    </div>
    <nav class="nav-links">
        <a href="homepage.jsp" class="nav-item"><i class="ph ph-house"></i> Dashboard</a>
        <a href="payslips.jsp" class="nav-item active"><i class="ph ph-receipt"></i> Payslips</a>
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
                        <span style="font-weight:700;">${formatMonth(p.month_year)}</span>
                        <p style="font-size:0.8rem;color:#64748b;">Uploaded on ${p.uploaded_at}</p>
                    </div>
                    <a href="PayslipServlet?action=view&id=${p.id}" target="_blank" class="btn-dl">
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
    if (!val.includes('-')) return val;
    const [y, m] = val.split('-');
    const months = ["January","February","March","April","May","June","July","August","September","October","November","December"];
    return months[parseInt(m) - 1] + " " + y;
}
</script>

</body>
</html>
