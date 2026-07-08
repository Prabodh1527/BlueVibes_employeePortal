<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String userName = (String) session.getAttribute("userName");
    if (userName == null) { response.sendRedirect("index.html"); return; }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>BlueVibes | Employee Dashboard</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=Sora:wght@500;600;700;800&family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/@phosphor-icons/web"></script>
    <style>
        :root{
            --sidebar-width:250px;
            --navy-950:#070b14;
            --navy-900:#0d1424;
            --navy-800:#131c33;
            --navy-700:#1c2942;
            --cyan-400:#22d3ee;
            --blue-500:#3b82f6;
            --slate-300:#a8b4c8;
            --slate-500:#6b7a94;
            --white:#f4f7fb;
            --success:#4ade80;
            --error:#f87171;
        }
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Inter', sans-serif; background: var(--navy-950); color: var(--white); display: flex; height: 100vh; overflow: hidden; }

        .sidebar{
            width:var(--sidebar-width);
            background:linear-gradient(180deg, var(--navy-900) 0%, var(--navy-950) 100%);
            display:flex;
            flex-direction:column;
            flex-shrink:0;
            overflow:hidden;
            border-right:1px solid rgba(255,255,255,0.07);
        }

        .sidebar-header{
            padding:22px 15px 16px;
            text-align:center;
            border-bottom:1px solid rgba(255,255,255,0.07);
        }
        .brand-container{
            position:relative;
            display:flex;
            flex-direction:column;
            align-items:center;
            gap:6px;
            margin-bottom:6px;
        }
        .brand-ring{
            position:absolute;
            top:-8px;
            width:56px;
            height:56px;
            border-radius:50%;
            border:1px solid rgba(34,211,238,0.35);
            animation:pulse 2.6s ease-out infinite;
        }
        @keyframes pulse{
            0%   { transform: scale(0.85); opacity: 0.7; }
            70%  { transform: scale(1.4); opacity: 0; }
            100% { transform: scale(1.4); opacity: 0; }
        }
        .logo-main{
            width:40px;
            height:40px;
            object-fit:contain;
            border-radius:8px;
            background:rgba(255,255,255,0.92);
            padding:5px;
        }

        .logo-secondary{
            height:20px;
            width:auto;
            object-fit:contain;
            border-radius:5px;
            background:rgba(255,255,255,0.92);
            padding:3px 6px;
            margin-top:6px;
        }

        .brand-name{
            font-family:'Sora', sans-serif;
            font-size:1.35rem;
            font-weight:700;
            color:var(--white);
            letter-spacing:1px;
            margin-top:8px;
        }

        .nav-links{
            list-style:none;
            padding:14px 12px;
            flex-grow:1;
            overflow-y:auto;
            scrollbar-width:thin;
        }

        .nav-links::-webkit-scrollbar{ width:5px; }
        .nav-links::-webkit-scrollbar-thumb{ background:var(--navy-700); border-radius:10px; }
        .nav-links::-webkit-scrollbar-track{ background:transparent; }

        .nav-item{
            display:flex;
            align-items:center;
            gap:12px;
            padding:11px 14px;
            border-radius:10px;
            color:var(--slate-300);
            text-decoration:none;
            margin-bottom:5px;
            transition:all .25s ease;
            font-size:14px;
            font-weight:500;
        }

        .nav-item i{ font-size:18px; min-width:18px; }

        .nav-item:hover{
            background:rgba(34,211,238,0.1);
            color:var(--white);
        }

        .nav-item.active{
            background:linear-gradient(135deg, var(--cyan-400), var(--blue-500));
            color:#06111f;
            font-weight:700;
            box-shadow:0 4px 14px -4px rgba(59,130,246,.5);
        }

        .sidebar-footer{
            padding:12px;
            border-top:1px solid rgba(255,255,255,.07);
        }

        .logout-item{
            color:var(--error) !important;
            text-decoration:none;
            display:flex;
            align-items:center;
            gap:12px;
            padding:11px 14px;
            border-radius:10px;
            transition:.25s;
            font-size:14px;
        }

        .logout-item:hover{ background:rgba(248,113,113,.1); }

        .main-wrapper { flex-grow: 1; display: flex; flex-direction: column; overflow: hidden; background: var(--navy-950); }
        .top-header {
            height: 68px;
            background: var(--navy-900);
            border-bottom: 1px solid rgba(255,255,255,0.07);
            color: var(--white);
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0 2.5rem;
            flex-shrink:0;
        }
        .top-header h2 { font-family:'Sora', sans-serif; }
        .content-area { padding: 2.25rem 2.5rem; overflow-y: auto; flex-grow: 1; }

        .welcome-box {
            padding: 2.2rem 2.4rem;
            border-radius: 18px;
            color: white;
            margin-bottom: 1.4rem;
            background: linear-gradient(135deg, var(--navy-900) 0%, var(--blue-500) 130%);
            position: relative;
            overflow: hidden;
            border:1px solid rgba(255,255,255,0.08);
        }
        .welcome-box::before{
            content:"";
            position:absolute;
            inset:0;
            background-image:
                linear-gradient(rgba(255,255,255,0.05) 1px, transparent 1px),
                linear-gradient(90deg, rgba(255,255,255,0.05) 1px, transparent 1px);
            background-size:36px 36px;
            mask-image:radial-gradient(ellipse 80% 100% at 100% 0%, black 30%, transparent 80%);
            pointer-events:none;
        }
        .welcome-box h1 { font-family:'Sora', sans-serif; font-size: 1.9rem; margin-bottom: 6px; position:relative; }
        .welcome-box p { font-size: 1rem; opacity: 0.9; margin: 0; position:relative; }

        .stat-container { display: flex; gap: 20px; margin-top: 25px; position:relative; }
        .stat-item { background: rgba(255,255,255,0.08); padding: 15px 25px; border-radius: 12px; backdrop-filter: blur(6px); border: 1px solid rgba(255,255,255,0.15); min-width: 200px; }
        .stat-label { display: block; font-size: 0.8rem; opacity: 0.8; }
        .stat-value { font-size: 1.8rem; font-weight: 700; }

        .quick-actions-card, .summary-card{
            background: linear-gradient(180deg, rgba(19,28,51,0.9), rgba(13,20,36,0.92));
            border-radius:16px;
            padding:1.6rem;
            border:1px solid rgba(255,255,255,0.08);
            margin-bottom:1.4rem;
        }
        .quick-actions-card h3, .summary-card h3{
            font-family:'Sora', sans-serif;
            margin-bottom:1.2rem;
            font-weight:600;
            font-size:1.05rem;
        }

        .quick-grid{
            display:grid;
            grid-template-columns:repeat(auto-fit, minmax(160px, 1fr));
            gap:16px;
        }

        .quick-btn{
            text-decoration:none;
            background:rgba(255,255,255,0.03);
            border:1px solid rgba(255,255,255,0.08);
            border-radius:12px;
            padding:22px;
            text-align:center;
            transition:0.25s;
            color:var(--white);
        }

        .quick-btn:hover{
            background:linear-gradient(135deg, var(--cyan-400), var(--blue-500));
            color:#06111f;
            border-color:transparent;
            transform:translateY(-3px);
        }

        .quick-btn i{ font-size:1.9rem; display:block; margin-bottom:10px; color:var(--cyan-400); transition:color 0.25s; }
        .quick-btn:hover i{ color:#06111f; }
        .quick-btn span{ font-weight:600; font-size:0.9rem; }

        .summary-grid{
            display:grid;
            grid-template-columns:repeat(auto-fit, minmax(140px, 1fr));
            gap:16px;
        }

        .summary-box{
            background:rgba(255,255,255,0.03);
            padding:20px;
            border-radius:12px;
            text-align:center;
            border:1px solid rgba(255,255,255,0.08);
        }

        .summary-box span{
            display:block;
            color:var(--slate-300);
            margin-bottom:10px;
            font-size:0.85rem;
        }

        .summary-box strong{
            font-family:'Sora', sans-serif;
            font-size:2rem;
            color:var(--cyan-400);
        }

        #chatButton{
            position:fixed;
            bottom:25px;
            right:25px;
            width:58px;
            height:58px;
            border-radius:50%;
            background:linear-gradient(135deg, var(--cyan-400), var(--blue-500));
            color:#06111f;
            display:flex;
            align-items:center;
            justify-content:center;
            font-size:26px;
            cursor:pointer;
            box-shadow:0 8px 20px -6px rgba(59,130,246,.6);
            z-index:9999;
            transition:transform 0.2s ease;
        }
        #chatButton:hover{ transform:translateY(-2px); }

        #chatBox{
            position:fixed;
            bottom:93px;
            right:25px;
            width:320px;
            height:420px;
            background:var(--navy-900);
            border-radius:16px;
            border:1px solid rgba(255,255,255,0.1);
            display:none;
            flex-direction:column;
            overflow:hidden;
            z-index:9999;
            box-shadow:0 20px 45px -12px rgba(0,0,0,.6);
        }

        .chat-header{
            background:var(--navy-950);
            color:var(--white);
            padding:15px;
            font-weight:600;
            font-family:'Sora', sans-serif;
            border-bottom:1px solid rgba(255,255,255,0.07);
        }

        .chat-messages{
            flex:1;
            padding:15px;
            overflow-y:auto;
        }

        .bot-msg{
            background:rgba(255,255,255,0.04);
            color:var(--white);
            padding:10px 12px;
            border-radius:10px;
            margin-bottom:10px;
            font-size:0.88rem;
            border:1px solid rgba(255,255,255,0.06);
        }

        .user-msg{
            background:linear-gradient(135deg, var(--cyan-400), var(--blue-500));
            color:#06111f;
            padding:10px 12px;
            border-radius:10px;
            margin-bottom:10px;
            text-align:right;
            font-weight:600;
        }

        .chat-input-area{
            display:flex;
            border-top:1px solid rgba(255,255,255,0.07);
        }

        .chat-input-area input{
            flex:1;
            border:none;
            padding:12px;
            outline:none;
            background:var(--navy-900);
            color:var(--white);
            font-family:inherit;
        }
        .chat-input-area input::placeholder{ color:var(--slate-500); }

        .chat-input-area button{
            border:none;
            background:var(--cyan-400);
            color:#06111f;
            padding:0 15px;
            cursor:pointer;
            font-weight:700;
        }

        .help-option{
            background:rgba(255,255,255,0.03);
            border:1px solid rgba(255,255,255,0.08);
            color:var(--white);
            padding:12px;
            border-radius:10px;
            margin-bottom:10px;
            cursor:pointer;
            transition:0.25s;
            font-size:0.88rem;
        }

        .help-option:hover{
            background:linear-gradient(135deg, var(--cyan-400), var(--blue-500));
            color:#06111f;
        }

        #answerBox{
            margin-top:15px;
            padding:12px;
            border-radius:10px;
            background:rgba(34,211,238,0.1);
            border:1px solid rgba(34,211,238,0.25);
            color:var(--white);
            font-size:0.88rem;
            display:none;
        }
        #answerBox a{ color:var(--cyan-400); font-weight:600; }

        @media (max-width: 900px){
            .sidebar{ display:none; }
            .content-area{ padding:1.5rem; }
        }
    </style>
</head>
<body>
<aside class="sidebar">
    <div class="sidebar-header">
        <div class="brand-container">
            <div class="brand-ring"></div>
            <img src="blueeye.png" class="logo-main">
            <img src="bluedigital.png" class="logo-secondary">
        </div>
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
        <div style="text-align: right;"><span id="currentDateDisplay" style="font-size: 0.9rem; color: var(--slate-300);"></span></div>
    </header>
    <div class="content-area">
        <div class="welcome-box">
            <h1>Welcome <%= userName %>!</h1>

            
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
                    "<h2 style='color:#22d3ee;'>🎂 Birthday</h2>" +
                    "<h3 style='color:#f4f7fb;'>" +
                    data.birthdays.join("<br>") +
                    "</h3><br>";
            }
        
            if(data.anniversaries && data.anniversaries.length > 0){
                html +=
                    "<h2 style='color:#4ade80;'>🏆 Work Anniversary</h2>" +
                    "<h3 style='color:#f4f7fb;'>" +
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
    background:rgba(0,0,0,0.6);
    z-index:10000;
    justify-content:center;
    align-items:center;
    ">
    
        <div style="
        width:500px;
        max-width:90%;
        background:linear-gradient(180deg, rgba(19,28,51,0.98), rgba(13,20,36,0.98));
        border:1px solid rgba(255,255,255,0.1);
        border-radius:20px;
        padding:30px;
        text-align:center;
        box-shadow:0 25px 60px rgba(0,0,0,0.5);
        position:relative;
        ">
    
            <button onclick="closeCelebrationModal()"
            style="
            position:absolute;
            top:15px;
            right:15px;
            border:none;
            background:none;
            color:#a8b4c8;
            font-size:22px;
            cursor:pointer;
            ">
            ✖
            </button>
    
            <div style="font-size:50px;">
                🎉
            </div>
    
            <h2 style="
            font-family:'Sora', sans-serif;
            color:#f4f7fb;
            margin-top:10px;
            margin-bottom:15px;
            ">
            Today's Celebrations
            </h2>
    
            <div id="celebrationContent" style="color:#a8b4c8;"></div>
    
        </div>
    
    </div>
</main>
</body>
</html>
