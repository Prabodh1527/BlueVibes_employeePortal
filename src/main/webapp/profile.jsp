<%@ page import="java.util.*" %>
<%
    String userName = (String) session.getAttribute("userName");
    Map<String, String> user = (Map<String, String>) request.getAttribute("user");
    List<Map<String, String>> quals = (List<Map<String, String>>) request.getAttribute("quals");
    List<Map<String, String>> exps = (List<Map<String, String>>) request.getAttribute("exps");
    List<Map<String, String>> userCerts = (List<Map<String, String>>) request.getAttribute("userCerts");

    List<Map<String, String>> masterQuals = (List<Map<String, String>>) request.getAttribute("masterQuals");
    List<Map<String, String>> masterOrgs = (List<Map<String, String>>) request.getAttribute("masterOrgs");
    List<Map<String, String>> masterCerts = (List<Map<String, String>>) request.getAttribute("masterCerts");
    List<Map<String, String>> masterDesigs = (List<Map<String, String>>) request.getAttribute("masterDesigs");

    if (userName == null || user == null) {
        response.sendRedirect("index.html");
        return;
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>BlueVibes | My Profile</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/@phosphor-icons/web"></script>
    <style>
        :root { --sidebar-width: 260px; --primary: #0f172a; --accent: #0284c7; --bg: #f1f5f9; --border: #e2e8f0; --text: #1e293b; }
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Inter', sans-serif; background: var(--bg); color: var(--text); display: flex; height: 100vh; overflow: hidden; }

        /* Sidebar Styling */
        .sidebar { width: var(--sidebar-width); background: var(--primary); display: flex; flex-direction: column; flex-shrink: 0; border-right: 1px solid rgba(255,255,255,0.1); }

        /* FIXED: SQUAD Dual Logo Branding Style with less white space */
        .sidebar-header { padding: 2.5rem 1.5rem; text-align: center; border-bottom: 1px solid rgba(255,255,255,0.05); }
        .brand-container {
            display: flex;
            justify-content: center;
            align-items: center;
            gap: 12px;
            margin-bottom: 10px;
        }
        .brand-name { font-size: 1.8rem; font-weight: 800; color: white; letter-spacing: 4px; margin-top: 10px; background: linear-gradient(to right, #fff, #64748b); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }

        /* Adjusted width and removed padding to reduce white space - 80px Size */
        .sidebar-logo {
            width: 80px;
            height: 80px;
            background: transparent;
            padding: 0;
            border-radius: 8px;
            object-fit: contain;
            filter: drop-shadow(0 4px 6px rgba(0,0,0,0.3));
        }

        .nav-links { list-style: none; padding: 1.5rem 1rem; flex-grow: 1; }
        .nav-item { display: flex; align-items: center; gap: 12px; padding: 0.8rem 1rem; border-radius: 8px; color: #94a3b8; text-decoration: none; margin-bottom: 0.5rem; transition: 0.3s; font-size: 0.9rem; }
        .nav-item:hover, .nav-item.active { background: var(--accent); color: white; }

        .logout-sect { padding: 1.5rem; border-top: 1px solid rgba(255,255,255,0.1); }
        .logout-btn { color: #f87171; text-decoration: none; display: flex; align-items: center; gap: 10px; font-weight: 600; font-size: 0.9rem; }

        .main-wrapper { flex-grow: 1; display: flex; flex-direction: column; overflow: hidden; }

        /* Blue Header Restored */
        .top-header { height: 70px; background: var(--primary); color: white; display: flex; align-items: center; justify-content: space-between; padding: 0 2.5rem; flex-shrink: 0; }

        .content-area { padding: 2.5rem; overflow-y: auto; flex-grow: 1; }
        .card { background: white; border-radius: 12px; border: 1px solid var(--border); padding: 1.5rem; margin-bottom: 2rem; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
        .card h3 { font-size: 1.1rem; color: var(--primary); border-bottom: 2px solid var(--bg); padding-bottom: 10px; margin-bottom: 1.5rem; display: flex; align-items: center; gap: 10px; }
        .form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 1.5rem; }
        .form-group { display: flex; flex-direction: column; gap: 5px; }
        label { font-size: 0.85rem; font-weight: 600; color: #64748b; }
        .form-input { padding: 10px; border: 1.5px solid var(--border); border-radius: 8px; font-size: 0.9rem; background: #fff; width: 100%; font-family: inherit; }
        .form-input[readonly] { background: #f8fafc; color: #94a3b8; cursor: not-allowed; }
        .data-table { width: 100%; border-collapse: collapse; margin-top: 1rem; }
        .data-table th { background: #f8fafc; text-align: left; padding: 12px; font-size: 0.8rem; text-transform: uppercase; color: #64748b; border-bottom: 2px solid var(--border); }
        .data-table td { padding: 10px; border-bottom: 1px solid var(--border); }
        .btn-add { background: var(--accent); color: white; border: none; padding: 6px 12px; border-radius: 6px; cursor: pointer; font-weight: 600; font-size: 0.8rem; display: flex; align-items: center; gap: 5px; }
        .btn-save { background: var(--primary); color: white; border: none; padding: 1rem 2.5rem; border-radius: 10px; font-weight: 700; cursor: pointer; width: 100%; margin-top: 1rem; transition: 0.2s; }
        .file-upload-info { font-size: 0.75rem; color: var(--accent); margin-top: 4px; }

        .checkbox-group { display: flex; align-items: center; gap: 8px; margin-top: 10px; cursor: pointer; }
        .checkbox-group input { cursor: pointer; }
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
        <a href="LoadProfileServlet" class="nav-item active"><i class="ph ph-user"></i> My Profile</a>
        <a href="attendance.html" class="nav-item"><i class="ph ph-calendar-check"></i> Attendance & Calender</a>
        <a href="weeklyreport.html" class="nav-item"><i class="ph ph-clipboard-text"></i> Weekly Status Report</a>
        <a href="user_notifications.html" class="nav-item"><i class="ph ph-broadcast"></i> Notifications</a>
        <a href="payslips.html" class="nav-item"><i class="ph ph-receipt"></i> Payslips</a>
        <a href="appraisals.html" class="nav-item"><i class="ph ph-chart-line-up"></i> KRA Appraisals</a>
        <a href="policyview.jsp" class="nav-item"><i class="ph ph-scroll"></i> Company Policies</a>
        <a href="leavereq.html" class="nav-item"><i class="ph ph-calendar-x"></i> Leave Request</a>
    </nav>
    <div class="logout-sect">
        <a href="index.html" class="logout-btn"><i class="ph ph-power"></i> Logout Account</a>
    </div>
</aside>

<main class="main-wrapper">
    <header class="top-header">
        <h2 style="font-weight: 700;">My Profile</h2>
        <div style="font-size: 0.9rem; color: white;">Emp ID: <span style="font-weight: 700;"><%= user.getOrDefault("employee_id", "N/A") %></span></div>
    </header>

    <div class="content-area">
        <div class="card">
            <h3><i class="ph ph-lock-key"></i> Security & Password</h3>
            <form action="UpdatePasswordServlet" method="POST">
                <div class="form-grid">
                    <div class="form-group"><label>New Password</label><input type="password" name="newPassword" class="form-input" required></div>
                    <div class="form-group"><label>Confirm Password</label><input type="password" name="confirmPassword" class="form-input" required></div>
                </div>
                <button type="submit" style="background:var(--accent); color:white; border:none; padding:8px 15px; border-radius:6px; margin-top:10px; cursor:pointer;">Update Password</button>
            </form>
        </div>

        <form action="UpdateProfileServlet" method="POST" enctype="multipart/form-data">
            <div class="card">
                <h3><i class="ph ph-user-circle"></i> 1. Personal Identity</h3>
                <div class="form-grid">
                    <div class="form-group"><label>Full Name</label><input type="text" class="form-input" readonly value="<%= user.getOrDefault("fullname", "") %>"></div>
                    <div class="form-group"><label>Date of Birth</label><input type="date" name="dob" class="form-input" value="<%= user.getOrDefault("dob", "") %>"></div>
                    <div class="form-group"><label>Gender</label><input type="text" class="form-input" readonly value="<%= user.getOrDefault("gender", "") %>"></div>
                    <div class="form-group"><label>Aadhar Number</label><input type="text" name="aadhar" class="form-input" value="<%= user.getOrDefault("aadhar", "") %>"></div>
                    <div class="form-group"><label>PAN Number</label><input type="text" name="pan" class="form-input" value="<%= user.getOrDefault("pan", "") %>"></div>
                </div>
            </div>

            <div class="card">
                <h3><i class="ph ph-phone"></i> 2. Family & Contact Details</h3>
                <div class="form-grid">
                    <div class="form-group"><label>Personal Mobile</label><input type="text" name="phone" class="form-input" value="<%= user.getOrDefault("phone", "") %>"></div>
                    <div class="form-group"><label>Father's Mobile</label><input type="text" name="mobile_father" class="form-input" value="<%= user.getOrDefault("m_father", "") %>"></div>
                    <div class="form-group"><label>Mother's Mobile</label><input type="text" name="mobile_mother" class="form-input" value="<%= user.getOrDefault("m_mother", "") %>"></div>
                    <div class="form-group"><label>Guardian's Mobile</label><input type="text" name="mobile_guardian" class="form-input" value="<%= user.getOrDefault("m_guardian", "") %>"></div>
                </div>
            </div>

            <div class="card">
                <h3><i class="ph ph-map-pin"></i> 3. Address</h3>
                <div class="form-grid">
                    <div class="form-group" style="grid-column: span 2;">
                        <label>Permanent Address</label>
                        <textarea id="perm_addr" name="perm_address" class="form-input" style="height: 80px; resize: vertical; white-space: pre-wrap;"><%= user.getOrDefault("p_addr", "") %></textarea>

                        <label class="checkbox-group">
                            <input type="checkbox" id="same_as_perm" onclick="copyAddress()">
                            <span style="font-size: 0.8rem; color: var(--accent);">Same as Permanent Address</span>
                        </label>
                    </div>
                    <div class="form-group" style="grid-column: span 2;">
                        <label>Communication Address</label>
                        <textarea id="comm_addr" name="comm_address" class="form-input" style="height: 80px; resize: vertical; white-space: pre-wrap;"><%= user.getOrDefault("c_addr", "") %></textarea>
                    </div>
                </div>
            </div>

            <div class="card">
                <h3><i class="ph ph-briefcase"></i> 4. Professional Details</h3>
                <div class="form-grid">
                    <div class="form-group"><label>Designation</label><input type="text" class="form-input" readonly value="<%= user.getOrDefault("designation", "") %>"></div>
                    <div class="form-group"><label>Joining Date</label><input type="text" class="form-input" readonly value="<%= user.getOrDefault("doj", "") %>"></div>
                    <div class="form-group"><label>PF Number</label><input type="text" name="pf_number" class="form-input" value="<%= user.getOrDefault("pf_num", "") %>"></div>
                </div>
            </div>

            <div class="card">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                    <h3><i class="ph ph-graduation-cap"></i> 5. Qualifications</h3>
                    <button type="button" class="btn-add" onclick="addRow('qual-body')"><i class="ph ph-plus"></i> Add row</button>
                </div>
                <table class="data-table">
                    <thead><tr><th>Qualification</th><th>Year</th><th>Grade</th><th>%</th></tr></thead>
                    <tbody id="qual-body">
                    <% if (quals != null && !quals.isEmpty()) {
                        for (Map<String, String> q : quals) { %>
                    <tr>
                        <td>
                            <select name="qual_name" class="form-input">
                                <option value="<%= q.getOrDefault("name", "") %>"><%= q.getOrDefault("name", "") %></option>
                                <% if(masterQuals != null) { for(Map<String,String> m : masterQuals) { %>
                                <option value="<%= m.get("name") %>"><%= m.get("name") %></option>
                                <% } } %>
                            </select>
                        </td>
                        <td><input type="text" name="qual_year" class="form-input" value="<%= q.getOrDefault("year", "") %>"></td>
                        <td><input type="text" name="qual_grade" class="form-input" value="<%= q.getOrDefault("grade", "") %>"></td>
                        <td><input type="text" name="qual_perc" class="form-input" value="<%= q.getOrDefault("perc", "") %>"></td>
                    </tr>
                    <% } } else { %>
                    <tr>
                        <td>
                            <select name="qual_name" class="form-input">
                                <option value="">Select Qualification</option>
                                <% if(masterQuals != null) { for(Map<String,String> m : masterQuals) { %>
                                <option value="<%= m.get("name") %>"><%= m.get("name") %></option>
                                <% } } %>
                            </select>
                        </td>
                        <td><input type="text" name="qual_year" class="form-input"></td>
                        <td><input type="text" name="qual_grade" class="form-input"></td>
                        <td><input type="text" name="qual_perc" class="form-input"></td>
                    </tr>
                    <% } %>
                    </tbody>
                </table>
            </div>

            <div class="card">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                    <h3><i class="ph ph-buildings"></i> 6.Professional Experience</h3>
                    <button type="button" class="btn-add" onclick="addRow('exp-body')"><i class="ph ph-plus"></i> Add row</button>
                </div>
                <table class="data-table">
                    <thead><tr><th>Company</th><th>Designation</th><th>Joined</th><th>Left</th></tr></thead>
                    <tbody id="exp-body">
                    <% if (exps != null && !exps.isEmpty()) {
                        for (Map<String, String> e : exps) { %>
                    <tr>
                        <td>
                            <select name="exp_company" class="form-input">
                                <option value="<%= e.getOrDefault("company", "") %>"><%= e.getOrDefault("company", "") %></option>
                                <% if(masterOrgs != null) { for(Map<String,String> m : masterOrgs) { %>
                                <option value="<%= m.get("name") %>"><%= m.get("name") %></option>
                                <% } } %>
                            </select>
                        </td>
                        <td>
                            <select name="exp_desig" class="form-input">
                                <option value="<%= e.getOrDefault("desig", "") %>"><%= e.getOrDefault("desig", "") %></option>
                                <% if(masterDesigs != null) { for(Map<String,String> m : masterDesigs) { %>
                                <option value="<%= m.get("name") %>"><%= m.get("name") %></option>
                                <% } } %>
                            </select>
                        </td>
                        <td><input type="date" name="exp_joined" class="form-input" value="<%= e.getOrDefault("joined", "") %>"></td>
                        <td><input type="date" name="exp_left" class="form-input" value="<%= e.getOrDefault("left", "") %>"></td>
                    </tr>
                    <% } } else { %>
                    <tr>
                        <td>
                            <select name="exp_company" class="form-input">
                                <option value="">Select Organization</option>
                                <% if(masterOrgs != null) { for(Map<String,String> m : masterOrgs) { %>
                                <option value="<%= m.get("name") %>"><%= m.get("name") %></option>
                                <% } } %>
                            </select>
                        </td>
                        <td>
                            <select name="exp_desig" class="form-input">
                                <option value="">Select Designation</option>
                                <% if(masterDesigs != null) { for(Map<String,String> m : masterDesigs) { %>
                                <option value="<%= m.get("name") %>"><%= m.get("name") %></option>
                                <% } } %>
                            </select>
                        </td>
                        <td><input type="date" name="exp_joined" class="form-input"></td>
                        <td><input type="date" name="exp_left" class="form-input"></td>
                    </tr>
                    <% } %>
                    </tbody>
                </table>
            </div>

            <div class="card">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                    <h3><i class="ph ph-certificate"></i> 7. Professional Certifications</h3>
                    <button type="button" class="btn-add" onclick="addRow('cert-body')"><i class="ph ph-plus"></i> Add row</button>
                </div>
                <table class="data-table">
                    <thead><tr><th>Certification Title</th><th>Issue Date</th><th>Attachment</th></tr></thead>
                    <tbody id="cert-body">
                    <% if (userCerts != null && !userCerts.isEmpty()) {
                        for (Map<String, String> c : userCerts) { %>
                    <tr>
                        <td>
                            <select name="cert_name" class="form-input">
                                <option value="<%= c.getOrDefault("name", "") %>"><%= c.getOrDefault("name", "") %></option>
                                <% if(masterCerts != null) { for(Map<String,String> m : masterCerts) { %>
                                <option value="<%= m.get("name") %>"><%= m.get("name") %></option>
                                <% } } %>
                            </select>
                        </td>
                        <td><input type="date" name="cert_date" class="form-input" value="<%= c.getOrDefault("date", "") %>"></td>
                        <td>
                            <input type="file" name="cert_image" class="form-input" accept="image/*">
                            <% if (c.get("path") != null && !c.get("path").isEmpty()) { %>
                            <div class="file-upload-info">
                                <i class="ph ph-check-circle" style="color:green;"></i>
                                <a href="<%= c.get("path") %>" target="_blank">View Current Copy</a>
                            </div>
                            <% } %>
                        </td>
                    </tr>
                    <% } } else { %>
                    <tr>
                        <td>
                            <select name="cert_name" class="form-input">
                                <option value="">Select Certification</option>
                                <% if(masterCerts != null) { for(Map<String,String> m : masterCerts) { %>
                                <option value="<%= m.get("name") %>"><%= m.get("name") %></option>
                                <% } } %>
                            </select>
                        </td>
                        <td><input type="date" name="cert_date" class="form-input"></td>
                        <td>
                            <input type="file" name="cert_image" class="form-input" accept="image/*">
                            <span class="file-upload-info">Upload certificate copy</span>
                        </td>
                    </tr>
                    <% } %>
                    </tbody>
                </table>
            </div>

            <button type="submit" class="btn-save">Update Full Profile</button>
        </form>
    </div>
</main>

<script>
    function copyAddress() {
        const checkbox = document.getElementById('same_as_perm');
        const perm = document.getElementById('perm_addr');
        const comm = document.getElementById('comm_addr');

        if (checkbox.checked) {
            comm.value = perm.value;
        } else {
            comm.value = "";
        }
    }

    function addRow(tbodyId) {
        const tbody = document.getElementById(tbodyId);
        const firstRow = tbody.rows[0];
        const newRow = firstRow.cloneNode(true);
        const inputs = newRow.querySelectorAll('input, select');
        const uploadInfos = newRow.querySelectorAll('.file-upload-info');

        for (let i = 0; i < inputs.length; i++) {
            inputs[i].value = "";
        }
        uploadInfos.forEach(info => info.remove());

        tbody.appendChild(newRow);
    }
</script>
</body>
</html>
