// Configuration : adaptez cette URL si le backend est sur un autre ordinateur.
const API_BASE_URL = (window.API_BASE_URL || "http://localhost:8080").replace(/\/$/, "");
const $ = (id) => document.getElementById(id);
let agents = [];
let sentTasks = 0;
let toastTimer;

$("apiUrlLabel").textContent = API_BASE_URL;

function notify(message) {
  const el = $("toast");
  el.textContent = message;
  el.classList.add("show");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.remove("show"), 3500);
}


function escapeHtml(value) {
  return String(value ?? "—").replace(/[&<>"']/g, c => (
    { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

function getAgentId(a) {

  return a.agentId ?? a.id ?? a.identifier ?? "";

}

function getHostname(a) {
  return a.hostname ?? a.hostName ?? a.computerName ?? a.name ?? getAgentId(a) ?? "PC inconnu";

}
function getIp(a) {

  return a.ipAddress ?? a.ip ?? a.address ?? "—";

}
function getOs(a) {
  return a.operatingSystem ?? a.os ?? a.osName ?? "—";
}

function getStatus(a) {
  return String(a.status ?? "OFFLINE").toUpperCase();
}

function getLastSeen(a) {
  const value = a.lastHeartbeat ?? a.lastSeen ?? a.lastConnection ?? a.updatedAt;
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString("fr-FR");
}
function statusHtml(a) {
  const online = getStatus(a) === "ONLINE";
  return `<span class="status-pill ${online ? "online" : "offline"}">${online ? "ONLINE" : escapeHtml(getStatus(a))}</span>`;
}

async function apiRequest(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: { "Content-Type": "application/json", ...(options.headers || {}) }
  });

  if (!response.ok) {
    const body = await response.text().catch(() => "");
    throw new Error(`HTTP ${response.status}${body ? " — " + body : ""}`);
  }
  if (response.status === 204) return null;
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

function renderDashboardAgents(list) {
  const tbody = $("dashboardAgents");
  if (!list.length) {
    tbody.innerHTML = `<tr><td colspan="4" class="empty">Aucun ordinateur enregistré.</td></tr>`;
    return;
  }

  tbody.innerHTML = list.slice(0, 6).map(a => `<tr>
    <td><span class="computer-name">${escapeHtml(getHostname(a))}</span><span class="sub-cell">${escapeHtml(getAgentId(a))}</span></td>
    <td>${escapeHtml(getIp(a))}</td><td>${statusHtml(a)}</td>
    <td><button class="small-action" data-intervene="${escapeHtml(getAgentId(a))}">Intervenir</button></td>
  </tr>`).join("");
}

function renderAgents() {
  const q = $("agentSearch").value.trim().toLowerCase();
  const filter = $("statusFilter").value;
  const list = agents.filter(a => {
    const haystack = [getHostname(a), getAgentId(a), getIp(a), getOs(a)].join(" ").toLowerCase();
    return haystack.includes(q) && (filter === "ALL" || getStatus(a) === filter);
  });

  $("agentsCount").textContent = `${list.length} ordinateur(s)`;
  $("agentsTable").innerHTML = list.length ? list.map(a => `<tr>
    <td><span class="computer-name">${escapeHtml(getHostname(a))}</span></td>
    <td>${escapeHtml(getAgentId(a))}</td><td>${escapeHtml(getIp(a))}</td>
    <td>${escapeHtml(getOs(a))}</td><td>${statusHtml(a)}</td>
    <td>${escapeHtml(getLastSeen(a))}</td>
    <td><button class="small-action" data-intervene="${escapeHtml(getAgentId(a))}" ${getStatus(a) !== "ONLINE" ? "disabled" : ""}>Intervenir</button></td>
  </tr>`).join("") : `<tr><td colspan="7" class="empty">Aucun résultat.</td></tr>`;
}

function updateAgentSelect() {
  const online = agents.filter(a => getStatus(a) === "ONLINE");
  $("targetAgent").innerHTML = `<option value="">Sélectionner un ordinateur en ligne</option>` +
    online.map(a => `<option value="${escapeHtml(getAgentId(a))}">${escapeHtml(getHostname(a))} — ${escapeHtml(getAgentId(a))}</option>`).join("");
}



async function getNextTask(agentId) {
  const container = document.getElementById("next-task-result");

  container.innerHTML = "<p>Chargement de la prochaine tâche...</p>";

  try {
    const response = await fetch(
      `${API_BASE_URL}/api/tasks/agents/${agentId}/next`
    );

    if (response.status === 204) {
      container.innerHTML = `
                <p class="empty-message">
                    Aucune tâche en attente pour cet agent.
                </p>
            `;
      return;
    }

    if (!response.ok) {
      throw new Error(`Erreur HTTP : ${response.status}`);
    }

    const task = await response.json();

    container.innerHTML = `
            <div class="task-result">
                <h3>Prochaine tâche</h3>
                <p><strong>ID :</strong> ${task.id ?? "-"}</p>
                <p><strong>Type :</strong> ${task.taskType ?? "-"}</p>
                <p><strong>Statut :</strong> ${task.status ?? "-"}</p>
                <p><strong>Paramètres :</strong></p>
                <pre>${JSON.stringify(task.parameters, null, 2)}</pre>
            </div>
        `;

  } catch (error) {
    container.innerHTML = `
            <p class="error-message">
                Impossible de récupérer la tâche : ${error.message}
            </p>
        `;
  }
}

async function loadAgents() {

  try {
    const data = await apiRequest("/api/agents");
    agents = Array.isArray(data) ? data : (data?.content ?? data?.agents ?? []);
    const online = agents.filter(a => getStatus(a) === "ONLINE").length;
    $("totalAgents").textContent = agents.length;
    $("onlineAgents").textContent = online;
    $("offlineAgents").textContent = agents.length - online;
    $("navAgentCount").textContent = agents.length;
    $("apiStatus").textContent = "Connecté";
    $("serverBadge").textContent = "Connecté";
    $("serverBadge").className = "status-pill online";
    renderDashboardAgents(agents);
    renderAgents();
    updateAgentSelect();
  } catch (error) {
    $("apiStatus").textContent = "Indisponible";
    $("serverBadge").textContent = "Déconnecté";
    $("serverBadge").className = "status-pill offline";
    $("dashboardAgents").innerHTML = `<tr><td colspan="4" class="empty">Impossible de charger les agents.
     Vérifiez le serveur et CORS.</td></tr>`;
    $("agentsTable").innerHTML = `<tr><td colspan="7" class="empty">${escapeHtml(error.message)}</td></tr>`;
    notify("Connexion API impossible : " + error.message);
  }
}

const field = (label, id, placeholder, type = "text", value = "") =>
  `<label>${label}<input id="${id}" type="${type}" placeholder="${placeholder}" value="${value}" required></label>`;

function renderTaskFields() {
  const type = $("taskType").value;
  const previousMode = $("mode")?.value || "WINDOWS";

  let html = "";

  if (type === "PING_PRINTER") {
    html = field(
      "Adresse IPv4 de l'imprimante",
      "ipAddress",
      "192.168.1.100"
    );
  }

  if (type === "CHECK_PRINTER_PORT") {
    html = field("Adresse IPv4", "ipAddress", "192.168.1.100") +
      `<label>Port réseau
                <select id="port">
                    <option value="9100">9100 (RAW)</option>
                    <option value="515">515 (LPD)</option>
                    <option value="631">631 (IPP)</option>
                </select>
            </label>`;
  }

  if (type === "TEST_PRINT") {
    html = `
            <label>Mode de test
                <select id="mode">
                    <option value="WINDOWS" ${previousMode === "WINDOWS" ? "selected" : ""}>
                        Imprimante Windows
                    </option>
                    <option value="NETWORK" ${previousMode === "NETWORK" ? "selected" : ""}>
                        Impression réseau (RAW)
                    </option>
                </select>
            </label>
        `;

    if (previousMode === "NETWORK") {
      html += field(
        "Adresse IPv4 de l'imprimante",
        "ipAddress",
        "192.168.137.194"
      );

      html += `
                <label>Port réseau
                    <input id="port" type="number" value="9100" required>
                </label>
            `;
    } else {
      html += field(
        "Nom exact de l'imprimante Windows",
        "printerName",
        "Brother DCP-B7558W BBB"
      );
    }
  }

  if (type === "INSTALL_DEFAULT_PRINTER") {
    html = field("Nom de l'imprimante", "printerName", "Brother DCP-B7548W") +
      field("Adresse IPv4", "ipAddress", "192.168.1.100") +
      `<label>Port réseau
                <input id="port" value="9100" readonly>
            </label>` +
      field("Nom du pilote", "driverName", "Brother DCP-B7548W Printer");
  }

  $("taskFields").innerHTML = html;
}


$("taskFields").addEventListener("change", e => {
  if (e.target.id === "mode") {
    renderTaskFields();
  }
});

function collectParameters(type) {
  const value = id => $(id)?.value?.trim();

  if (
    type === "GET_SYSTEM_INFO" ||
    type === "RESTART_PRINT_SPOOLER"
  ) {
    return {};
  }

  if (type === "PING_PRINTER") {
    return {
      ipAddress: value("ipAddress")
    };
  }

  if (type === "CHECK_PRINTER_PORT") {
    return {
      ipAddress: value("ipAddress"),
      port: Number(value("port"))
    };
  }

  if (type === "TEST_PRINT") {
    const mode = value("mode");

    if (mode === "NETWORK") {
      return {
        mode: "NETWORK",
        ipAddress: value("ipAddress"),
        port: Number(value("port"))
      };
    }

    return {
      mode: "WINDOWS",
      printerName: value("printerName")
    };
  }

  if (type === "INSTALL_DEFAULT_PRINTER") {
    return {
      printerName: value("printerName"),
      ipAddress: value("ipAddress"),
      port: 9100,
      driverName: value("driverName")
    };
  }

  return {};
}

function navigate(page) {
  document.querySelectorAll(".page").forEach(p => p.classList.toggle("active", p.id === `page-${page}`));
  document.querySelectorAll(".nav-link").forEach(b => b.classList.toggle("active", b.dataset.page === page));
  const labels = { dashboard: "Tableau de bord", agents: "Ordinateurs", interventions: "Interventions", history: "Historique" };
  $("breadcrumbTitle").textContent = labels[page] || "Tableau de bord";
  $("sidebar").classList.remove("open");
}

document.querySelectorAll(".nav-link").forEach(b => b.addEventListener("click", () => navigate(b.dataset.page)));
document.querySelectorAll("[data-go]").forEach(b => b.addEventListener("click", () => navigate(b.dataset.go)));

$("mobileMenu").addEventListener("click", () => $("sidebar").classList.toggle("open"));
$("refreshDashboard").addEventListener("click", loadAgents);
$("refreshAgents").addEventListener("click", loadAgents);
$("agentSearch").addEventListener("input", renderAgents);
$("statusFilter").addEventListener("change", renderAgents);
$("taskType").addEventListener("change", renderTaskFields);

document.addEventListener("click", e => {
  const button = e.target.closest("[data-intervene]");
  if (!button) return;
  const id = button.dataset.intervene;
  const agent = agents.find(a => String(getAgentId(a)) === String(id));
  if (!agent || getStatus(agent) !== "ONLINE") return notify("Cet ordinateur n'est pas en ligne.");
  navigate("interventions");
  $("targetAgent").value = id;
});

$("taskForm").addEventListener("submit", async e => {
  e.preventDefault();
  const agentId = $("targetAgent").value;
  const taskType = $("taskType").value;
  if (!agentId) return notify("Sélectionnez un ordinateur en ligne.");
  const agent = agents.find(a => String(getAgentId(a)) === agentId);
  if (!agent || getStatus(agent) !== "ONLINE") return notify("L'agent sélectionné n'est plus en ligne.");
  const parameters = collectParameters(taskType);
  if (parameters.ipAddress && !/^(25[0-5]|2[0-4]\d|1?\d?\d)(\.(25[0-5]|2[0-4]\d|1?\d?\d)){3}$/.test(parameters.ipAddress)) return notify("Adresse IPv4 invalide.");
  if (taskType === "INSTALL_DEFAULT_PRINTER" && (!parameters.printerName || !parameters.driverName)) return notify("Complétez les informations de l'imprimante.");
  const payload = { agentId, taskType, parametersJson: JSON.stringify(parameters) };
  const btn = $("submitTask");
  btn.disabled = true; btn.textContent = "Envoi en cours...";

  try {
    const result = await apiRequest("/api/tasks", { method: "POST", body: JSON.stringify(payload) });
    sentTasks++; $("sentTasks").textContent = sentTasks;
    const taskId = result?.id ?? result?.taskId ?? "non communiqué";
    $("taskResult").hidden = false;
    $("taskResult").innerHTML = `<h2>✓ Tâche envoyée au serveur</h2><p>Type : <b>${escapeHtml(taskType)}</b><br>Ordinateur : <b>${escapeHtml(agentId)}</b><br>Identifiant de tâche : <b>${escapeHtml(taskId)}</b></p><p>La tâche est créée ou mise en attente. Son exécution et son résultat doivent être confirmés par l'agent ; cette interface ne prétend pas qu'elle est déjà terminée.</p>`;
    notify("Intervention envoyée au serveur.");
  } catch (error) {
    notify("Échec de l'envoi : " + error.message);
  } finally {
    btn.disabled = false; btn.innerHTML = 'Envoyer l’intervention <span>→</span>';
  }
});


// ==========================================
// HISTORIQUE DES INTERVENTIONS
// ==========================================

const API_TASKS = "http://localhost:8080/api/tasks";

let historyCurrentPage = 0;
const historyPageSize = 10;
let historyTotalPages = 0;

// Éléments HTML
const historyTable = document.getElementById("historyTable");
const historyCount = document.getElementById("historyCount");
const historyPage = document.getElementById("historyPage");

const historySearch = document.getElementById("historySearch");
const historyStatusFilter = document.getElementById("historyStatusFilter");

const historyPrevious = document.getElementById("historyPrevious");
const historyNext = document.getElementById("historyNext");
const refreshHistory = document.getElementById("refreshHistory");


// ==========================================
// CHARGER L'HISTORIQUE
// ==========================================




async function loadHistory() {

    if (!historyTable) return;

    historyTable.innerHTML = `
        <tr>
            <td colspan="7" class="empty">
                Chargement de l'historique...
            </td>
        </tr>
    `;

    try {

        const params = new URLSearchParams({
            page: historyCurrentPage,
            size: historyPageSize
        });

        const status = historyStatusFilter?.value;

        if (status) {
            params.append("status", status);
        }

        const response = await fetch(
            `${API_TASKS}?${params.toString()}`
        );

        if (!response.ok) {
            throw new Error("Erreur HTTP : " + response.status);
        }

        const data = await response.json();

        renderHistory(data);

    } catch (error) {

        console.error("Erreur historique :", error);

        historyTable.innerHTML = `
            <tr>
                <td colspan="7" class="empty">
                    Impossible de charger l'historique.
                </td>
            </tr>
        `;

        historyCount.textContent = "0 intervention(s)";
    }
}

const statu = historyStatusFilter.value;

if (statu) {
    params.append("status", statu);
}

// ==========================================
// AFFICHER LES DONNÉES DANS LE TABLEAU
// ==========================================


function renderHistory(data) {

    const tasks = data.content || [];

    historyTotalPages = data.totalPages || 0;

    historyCount.textContent =
        `${data.totalElements || 0} intervention(s)`;

    historyPage.textContent =
        `Page ${data.number + 1} / ${Math.max(historyTotalPages, 1)}`;

    historyPrevious.disabled = data.first;
    historyNext.disabled = data.last;

    if (tasks.length === 0) {
        historyTable.innerHTML = `
            <tr>
                <td colspan="7" class="empty">
                    Aucune intervention trouvée.
                </td>
            </tr>
        `;
        return;
    }

    // Stocker les interventions chargées
    window.historyTasks = tasks;

    historyTable.innerHTML = tasks.map(task => {

        const status = task.status || "UNKNOWN";

        const statusLabel = {
            PENDING: "En attente",
            IN_PROGRESS: "En cours",
            SUCCESS: "Réussie",
            FAILED: "Échec"
        };

        const statusClass = {
            PENDING: "warning",
            IN_PROGRESS: "info",
            SUCCESS: "success",
            FAILED: "danger"
        };

        return `
            <tr>
                <td>#${task.id}</td>

                <td>
                    <strong>${escapeHistory(task.agentId)}</strong>
                </td>

                <td>${escapeHistory(task.taskType)}</td>

                <td>
                    <span class="badge ${statusClass[status] || ''}">
                        ${statusLabel[status] || escapeHistory(status)}
                    </span>
                </td>

                <td>${formatHistoryDate(task.createdAt)}</td>

                <td>${formatHistoryDate(task.completedAt)}</td>

                <td>
                    <button
                        type="button"
                        class="btn btn-secondary btn-sm"
                        data-history-id="${task.id}">
                        Voir résultat
                    </button>
                </td>
            </tr>
        `;

    }).join("");
}



// ==========================================
// FORMATER LES DATES
// ==========================================

function formatHistoryDate(date) {

    if (!date) return "-";

    const value = new Date(date);

    if (isNaN(value.getTime())) return "-";

    return value.toLocaleString("fr-FR", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit"
    });
}


// ==========================================
// PROTÉGER LES DONNÉES HTML
// ==========================================

function escapeHistory(value) {

    return String(value ?? "").replace(/[&<>"']/g, char => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#039;"
    })[char]);
}


// ==========================================
// AFFICHER LE RÉSULTAT COMPLET
// ==========================================


function showHistoryResult(result) {

    const existing = document.getElementById("historyResultModal");

    if (existing) existing.remove();

    const modal = document.createElement("div");

    modal.id = "historyResultModal";

    modal.innerHTML = `
        <div class="modal-content">

            <div class="modal-header">
                <h2>Résultat de l'intervention</h2>

                <button
                    type="button"
                    class="btn btn-secondary"
                    id="closeHistoryModal">
                    ✕
                </button>
            </div>

            <pre class="history-result">${escapeHistory(
                result || "Aucun résultat disponible."
            )}</pre>

        </div>
    `;

    document.body.appendChild(modal);

    document.getElementById("closeHistoryModal")
        .addEventListener("click", () => modal.remove());

    modal.addEventListener("click", event => {
        if (event.target === modal) {
            modal.remove();
        }
    });
}



historyTable.addEventListener("click", event => {

    const button = event.target.closest("[data-history-id]");

    if (!button) return;

    const id = Number(button.dataset.historyId);

    const task = (window.historyTasks || [])
        .find(item => item.id === id);

    if (!task) {
        console.error("Intervention introuvable :", id);
        return;
    }

    showHistoryResult(task.resultMessage);

});



// ==========================================
// RECHERCHE PAR AGENT OU TYPE
// ==========================================

historySearch?.addEventListener("input", () => {

    const search = historySearch.value.toLowerCase().trim();

    const rows = historyTable.querySelectorAll("tr");

    rows.forEach(row => {

        const content = row.textContent.toLowerCase();

        row.style.display = content.includes(search)
            ? ""
            : "none";
    });
});


// ==========================================
// FILTRAGE PAR STATUT
// ==========================================

historyStatusFilter?.addEventListener("change", () => {

    historyCurrentPage = 0;

    loadHistory();
});


// ==========================================
// PAGINATION
// ==========================================

historyPrevious?.addEventListener("click", () => {

    if (historyCurrentPage > 0) {

        historyCurrentPage--;

        loadHistory();
    }
});


historyNext?.addEventListener("click", () => {

    if (historyCurrentPage < historyTotalPages - 1) {

        historyCurrentPage++;

        loadHistory();
    }
});


// ==========================================
// ACTUALISATION
// ==========================================

refreshHistory?.addEventListener("click", () => {

    loadHistory();
});


// ==========================================
// INITIALISATION
// ==========================================



loadHistory();


renderTaskFields();
loadAgents();
setInterval(loadAgents, 15000);
