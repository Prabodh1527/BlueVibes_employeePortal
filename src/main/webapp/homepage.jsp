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
        :root{
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

        .brand-name{
            font-size:1.5rem;
            font-weight:800;
            color:white;
            letter-spacing:2px;
            margin-top:5px;
            background:linear-gradient(to right,#fff,#94a3b8);
            -webkit-background-clip:text;
            -webkit-text-fill-color:transparent;
        }
        .sidebar-logo { width: 80px; height: 80px; background: transparent; padding: 0; border-radius: 8px; object-fit: contain; filter: drop-shadow(0 4px 6px rgba(0,0,0,0.3)); }

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
            min-width:18px;
        }
        
        .nav-item:hover{
            background:rgba(2,132,199,.18);
            color:white;
        }
        
        .nav-item.active{
            background:#0284c7;
            color:white;
            box-shadow:0 4px 12px rgba(2,132,199,.35);
        }
        
        .sidebar-footer{
            padding:12px;
            border-top:1px solid rgba(255,255,255,.08);
        }
        
        .logout-item{
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
        
        .logout-item:hover{
            background:rgba(239,68,68,.12);
        }

        .award-card{
            background:#f8fafc;
            border:1px solid #e2e8f0;
            border-left:5px solid #0284c7;
            border-radius:12px;
            padding:18px;
            margin-bottom:15px;
            transition:0.25s;
        }
        
        .award-card:hover{
            transform:translateY(-2px);
            box-shadow:0 6px 15px rgba(0,0,0,.08);
        }
        
        .award-title{
            font-size:18px;
            font-weight:700;
            color:#0f172a;
            margin-bottom:8px;
        }
        
        .award-description{
            color:#64748b;
            margin-bottom:12px;
        }
        
        .award-votes{
            display:inline-block;
            background:#dbeafe;
            color:#1d4ed8;
            padding:6px 12px;
            border-radius:20px;
            font-weight:600;
            font-size:13px;
        }

        .main-wrapper { flex-grow: 1; display: flex; flex-direction: column; overflow: hidden; }
        .top-header { height: 70px; background: var(--primary); color: white; display: flex; align-items: center; justify-content: space-between; padding: 0 2.5rem; }
        .content-area { padding: 2.5rem; overflow-y: auto; flex-grow: 1; }

        .welcome-box { padding: 2rem; border-radius: 20px; color: white; margin-bottom: 1.2rem; background: linear-gradient(135deg, #0f172a 0%, #0284c7 100%); position: relative; overflow: hidden; }
        .welcome-box h1 { font-size: 2.1rem; margin-bottom: 6px; }
        .welcome-box p { font-size: 1rem; opacity: 0.9; margin: 0; }

        .stat-container { display: flex; gap: 20px; margin-top: 25px; }
        .stat-item { background: rgba(255,255,255,0.1); padding: 15px 25px; border-radius: 12px; backdrop-filter: blur(5px); border: 1px solid rgba(255,255,255,0.2); min-width: 200px; }
        .stat-label { display: block; font-size: 0.8rem; opacity: 0.8; }
        .stat-value { font-size: 1.8rem; font-weight: 700; }

        #chatButton{
            position:fixed;
            bottom:25px;
            right:25px;
            width:60px;
            height:60px;
            border-radius:50%;
            background:#0284c7;
            color:white;
            display:flex;
            align-items:center;
            justify-content:center;
            font-size:28px;
            cursor:pointer;
            box-shadow:0 5px 15px rgba(0,0,0,0.2);
            z-index:9999;
        }
        
        #chatBox{
            position:fixed;
            bottom:95px;
            right:25px;
            width:320px;
            height:420px;
            background:white;
            border-radius:15px;
            border:1px solid #e2e8f0;
            display:none;
            flex-direction:column;
            overflow:hidden;
            z-index:9999;
        }
        
        .chat-header{
            background:#0f172a;
            color:white;
            padding:15px;
            font-weight:600;
        }
        
        .chat-messages{
            flex:1;
            padding:15px;
            overflow-y:auto;
        }
        
        .bot-msg{
            background:#f1f5f9;
            padding:10px;
            border-radius:10px;
            margin-bottom:10px;
        }
        
        .user-msg{
            background:#0284c7;
            color:white;
            padding:10px;
            border-radius:10px;
            margin-bottom:10px;
            text-align:right;
        }
        
        .chat-input-area{
            display:flex;
            border-top:1px solid #e2e8f0;
        }
        
        .chat-input-area input{
            flex:1;
            border:none;
            padding:12px;
            outline:none;
        }
        
        .chat-input-area button{
            border:none;
            background:#0284c7;
            color:white;
            padding:0 15px;
            cursor:pointer;
        }
        .help-option{
            background:#f8fafc;
            border:1px solid #e2e8f0;
            padding:12px;
            border-radius:10px;
            margin-bottom:10px;
            cursor:pointer;
            transition:0.3s;
        }
        
        .help-option:hover{
            background:#0284c7;
            color:white;
        }
        
        #answerBox{
            margin-top:15px;
            padding:12px;
            border-radius:10px;
            background:#e0f2fe;
            display:none;
        }
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
        <a href="know_colleagues.html" class="nav-item"><i class="ph ph-users-three"></i> Know Your Colleagues</a>
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
        </div>

        <div class="summary-card" style="margin-bottom:25px;">
            <h3>🏆 My Awards</h3>
        
            <div id="myAwardsContainer">
        
                <div style="color:#64748b;">
                    Loading your awards...
                </div>
        
            </div>
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
        
                    
        
                    document.getElementById("totalTasks").innerText = total;
                    document.getElementById("completedTasks").innerText = completed;
                    document.getElementById("openTasks").innerText = open;
                    document.getElementById("inProgressTasks").innerText = inProgress;
                })
                .catch(err => {
                    console.error("Error loading task summary:", err);
                });
        }

        function showCelebrationPopup(data){

            let html = "";
        
            if(data.birthdays && data.birthdays.length > 0){
                html +=
                    "<h2 style='color:#0284c7;'>🎂 Birthday</h2>" +
                    "<h3 style='color:#0f172a;'>" +
                    data.birthdays.join("<br>") +
                    "</h3><br>";
            }
        
            if(data.anniversaries && data.anniversaries.length > 0){
                html +=
                    "<h2 style='color:#16a34a;'>🏆 Work Anniversary</h2>" +
                    "<h3 style='color:#0f172a;'>" +
                    data.anniversaries.join("<br>") +
                    "</h3>";
            }
        
            if(html === ""){
                return;
            }
        
            document.getElementById("celebrationContent").innerHTML = html;
            document.getElementById("celebrationModal").style.display = "flex";
        }
        
        function closeCelebrationModal(){
        
            document.getElementById(
            "celebrationModal").style.display = "none";
        }

        function loadMyAwards(){

            fetch("MyAwardsServlet")
            .then(res => {
        
                alert("HTTP Status : " + res.status);
        
                return res.text();
        
            })
            .then(text => {
        
                alert(text);
        
            })
            .catch(err => {
        
                alert("ERROR : " + err);
        
            });
        
        }

        window.onload = function() {

            const options = {
                weekday:'long',
                year:'numeric',
                month:'long',
                day:'numeric'
            };
        
            document.getElementById(
            'currentDateDisplay').innerText =
            new Date().toLocaleDateString(
            undefined,
            options);
        
            loadStats();
        
            loadTaskSummary();
            loadMyAwards();
        
            fetch(
            "DashboardCelebrationServlet")
            .then(res=>res.json())
            .then(data=>{
        
                showCelebrationPopup(data);
        
            });
        };
        
        function toggleChat(){
            const box = document.getElementById("chatBox");
        
            if(box.style.display==="flex"){
                box.style.display="none";
            }else{
                box.style.display="flex";
            }
        }
        
        function sendMessage(){
    
        }
        function showAnswer(type){

            const box=document.getElementById("answerBox");
        
            let html="";
        
            if(type==="leave"){
                html=`
                <b>Leave Request</b><br><br>
                <a href="leavereq.html">Open Leave Request</a>
                `;
            }
        
            if(type==="wsr"){
                html=`
                <b>Weekly Status Report</b><br><br>
                <a href="weeklyreport.html">Open WSR</a>
                `;
            }
        
            if(type==="notification"){
                html=`
                <b>Notifications</b><br><br>
                <a href="user_notifications.html">View Notifications</a>
                `;
            }
        
            if(type==="payslip"){
                html=`
                <b>Payslips</b><br><br>
                <a href="payslips.jsp">Open Payslips</a>
                `;
            }
        
            if(type==="policy"){
                html=`
                <b>Company Policies</b><br><br>
                <a href="policyview.jsp">View Policies</a>
                `;
            }
        
            if(type==="faq"){
                html=`
                <b>Frequently Asked Questions</b>
                <br><br>
        
                • Apply leave from Leave Request page.
                <br><br>
        
                • Submit weekly work in WSR section.
                <br><br>
        
                • Payslips are available in Payslips page.
                <br><br>
        
                • Notifications contain all company announcements.
                `;
            }
        
            box.style.display="block";
            box.innerHTML=html;
        }
        
    </script>
    <div id="chatButton" onclick="toggleChat()">
        <i class="ph ph-robot"></i>
    </div>
    
    <div id="chatBox">
        <div class="chat-header">
            BlueVibes Assistant
        </div>
    
        <div id="chatMessages" class="chat-messages">

            <div class="bot-msg">
                👋 Welcome to BlueVibes Assistant
                <br><br>
                Select an option below:
            </div>
        
            <div class="help-option" onclick="showAnswer('leave')">
                📅 Apply Leave
            </div>
        
            <div class="help-option" onclick="showAnswer('wsr')">
                📝 Update Weekly Status Report
            </div>
        
            <div class="help-option" onclick="showAnswer('notification')">
                🔔 Notifications
            </div>
        
            <div class="help-option" onclick="showAnswer('payslip')">
                💰 Payslips
            </div>
        
            <div class="help-option" onclick="showAnswer('policy')">
                📖 Company Policies
            </div>
        
            <div class="help-option" onclick="showAnswer('faq')">
                ❓ Frequently Asked Questions
            </div>
        
            <div id="answerBox"></div>
        
        </div>
    
    </div>
    <div id="celebrationModal" style="
    display:none;
    position:fixed;
    top:0;
    left:0;
    width:100%;
    height:100%;
    background:rgba(0,0,0,0.55);
    z-index:10000;
    justify-content:center;
    align-items:center;
    ">
    
        <div style="
        width:500px;
        max-width:90%;
        background:white;
        border-radius:20px;
        padding:30px;
        text-align:center;
        box-shadow:0 15px 40px rgba(0,0,0,0.2);
        position:relative;
        ">
    
            <button onclick="closeCelebrationModal()"
            style="
            position:absolute;
            top:15px;
            right:15px;
            border:none;
            background:none;
            font-size:22px;
            cursor:pointer;
            ">
            ✖
            </button>
    
            <div style="font-size:50px;">
                🎉
            </div>
    
            <h2 style="
            color:#0f172a;
            margin-top:10px;
            margin-bottom:15px;
            ">
            Today's Celebrations
            </h2>
    
            <div id="celebrationContent"></div>
    
        </div>
    
    </div>
</body>
</html>

