
/* =========================================================
   IT REMOTE SUPPORT - APP.JS
   Backend : Spring Boot
   API     : http://localhost:8080
========================================================= */

/* =========================
   1. CONFIGURATION
========================= */

const API_BASE_URL = "http://localhost:8080";

const HISTORY_PAGE_SIZE = 10;

let agents = [];
let historyTasks = [];
let historyPage = 0;
let historyTotalPages = 0;
let historyTotalElements = 0;

let currentPage = "dashboard";


/* =========================
   2. UTILITAIRES
========================= */

function getElement(id) {
    return document.getElementById(id);
}

function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, char => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#039;"
    })[char]);
}

async function apiRequest(endpoint, options = {}) {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        ...options,
        headers: {
            "Content-Type": "application/json",
            ...(options.headers || {})
        }
    });

    if (response.status === 204) {
        return null;
    }

    const responseText = await response.text();

    let data = null;

    if (responseText) {
        try {
            data = JSON.parse(responseText);
        } catch {
            data = responseText;
        }
    }

    if (!response.ok) {
        const message =
            data?.message ||
            data?.error ||
            `Erreur HTTP ${response.status}`;

        throw new Error(message);
    }

    return data;
}

function showToast(message, type = "success") {
    const toast = getElement("toast");

    if (!toast) {
        console.log(message);
        return;
    }

    toast.textContent = message;
    toast.className = `toast ${type}`;
    toast.classList.remove("hidden");

    setTimeout(() => {
        toast.classList.add("hidden");
    }, 3500);
}

function formatDate(date) {
    if (!date) return "—";

    const parsedDate = new Date(date);

    if (Number.isNaN(parsedDate.getTime())) {
        return "—";
    }

    return parsedDate.toLocaleString("fr-FR", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    });
}

function formatTaskStatus(status) {
    const statuses = {
        PENDING: "En attente",
        IN_PROGRESS: "En cours",
        SUCCESS: "Réussie",
        FAILED: "Échouée",
        EXPIRED: "Expirée"
    };

    return statuses[status] ?? (status || "—");
}

function getStatusClass(status) {
    const classes = {
        ONLINE: "online",
        OFFLINE: "offline",
        PENDING: "pending",
        IN_PROGRESS: "processing",
        SUCCESS: "success",
        FAILED: "failed",
        EXPIRED: "expired"
    };

    return classes[status] || "neutral";
}

function getStatusBadge(status) {
    const label = formatTaskStatus(status);
    const cssClass = getStatusClass(status);

    return `
        <span class="status-badge ${cssClass}">
            ${escapeHtml(label)}
        </span>
    `;
}

function getTaskResult(task) {
    return task.resultMessage || "Aucun résultat retourné";
}


/* =========================
   3. NAVIGATION
========================= */

function navigate(page) {
    if (!page) return;

    currentPage = page;

    // Masquer toutes les pages
    document.querySelectorAll("[data-section]").forEach(section => {
        section.classList.add("hidden");
    });

    // Afficher la page demandée
    const targetSection = document.querySelector(
        `[data-section="${page}"]`
    );

    if (targetSection) {
        targetSection.classList.remove("hidden");
    }

    // Mise à jour du menu actif
    document.querySelectorAll("[data-page]").forEach(link => {
        link.classList.toggle(
            "active",
            link.dataset.page === page
        );
    });

    // Mise à jour du titre
    const titles = {
        dashboard: "Tableau de bord",
        agents: "Gestion des agents",
        tasks: "Nouvelle intervention",
        history: "Historique des interventions"
    };

    const breadcrumb = getElement("breadcrumbTitle");

    if (breadcrumb) {
        breadcrumb.textContent = titles[page] || page;
    }

    // Charger les données de la page
    if (page === "dashboard") {
        loadDashboard();
    }

    if (page === "agents") {
        loadAgents();
    }

    if (page === "history") {
        historyPage = 0;
        loadHistory();
    }

    // Fermer le menu mobile
    const sidebar = getElement("sidebar");

    if (sidebar) {
        sidebar.classList.remove("open");
    }
}


/* =========================
   4. CHARGEMENT DES AGENTS
========================= */

async function loadAgents() {
    try {
        const data = await apiRequest("/api/agents");

        agents = Array.isArray(data)
            ? data
            : data?.content || [];

        renderAgents();
        updateAgentStatistics();

        return agents;

    } catch (error) {
        console.error("Erreur chargement agents :", error);
        showToast("Impossible de charger les agents.", "error");
    }
}


/* =========================
   5. AFFICHAGE DES AGENTS
========================= */

function renderAgents() {
    const table = getElement("agentsTable");

    if (!table) return;

    const tbody = table.tagName === "TBODY"
        ? table
        : table.querySelector("tbody");

    if (!tbody) return;

    const search = (
        getElement("agentSearch")?.value || ""
    ).toLowerCase();

    const statusFilter =
        getElement("statusFilter")?.value || "";

    const filteredAgents = agents.filter(agent => {
        const agentId = String(agent.agentId || "").toLowerCase();

        const computerName = String(
            agent.computerName || agent.hostname || ""
        ).toLowerCase();

        const ipAddress = String(
            agent.ipAddress || agent.ip || ""
        ).toLowerCase();

        const matchesSearch =
            agentId.includes(search) ||
            computerName.includes(search) ||
            ipAddress.includes(search);

        const matchesStatus =
            !statusFilter ||
            agent.status === statusFilter;

        return matchesSearch && matchesStatus;
    });

    if (filteredAgents.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="empty-state">
                    Aucun agent trouvé.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = filteredAgents.map(agent => `
        <tr>
            <td>${escapeHtml(agent.agentId)}</td>

            <td>
                ${escapeHtml(
        agent.computerName ||
        agent.hostname ||
        "—"
    )}
            </td>

            <td>
                ${escapeHtml(
        agent.ipAddress ||
        agent.ip ||
        "—"
    )}
            </td>

            <td>
                ${getStatusBadge(agent.status)}
            </td>

            <td>
                ${formatDate(
        agent.lastHeartbeat ||
        agent.lastSeen
    )}
            </td>

            <td>
                <button
                    class="btn btn-primary"
                    onclick="selectAgent('${escapeHtml(agent.agentId)}')">
                    Intervenir
                </button>
            </td>
        </tr>
    `).join("");
}


/* =========================
   6. STATISTIQUES AGENTS
========================= */

function updateAgentStatistics() {
    const total = agents.length;

    const online = agents.filter(
        agent => agent.status === "ONLINE"
    ).length;

    const offline = agents.filter(
        agent => agent.status === "OFFLINE"
    ).length;

    if (getElement("totalAgents")) {
        getElement("totalAgents").textContent = total;
    }

    if (getElement("onlineAgents")) {
        getElement("onlineAgents").textContent = online;
    }

    if (getElement("offlineAgents")) {
        getElement("offlineAgents").textContent = offline;
    }

    if (getElement("agentsCount")) {
        getElement("agentsCount").textContent = total;
    }

    if (getElement("navAgentCount")) {
        getElement("navAgentCount").textContent = total;
    }

    renderDashboardAgents();
}


/* =========================
   7. AGENTS DU DASHBOARD
========================= */

function renderDashboardAgents() {
    const container = getElement("dashboardAgents");

    if (!container) return;

    const recentAgents = agents.slice(0, 5);

    if (recentAgents.length === 0) {
        container.innerHTML = `
            <p class="empty-state">Aucun agent enregistré.</p>
        `;
        return;
    }

    container.innerHTML = recentAgents.map(agent => `
        <div class="agent-item">
            <div>
                <strong>
                    ${escapeHtml(
        agent.computerName ||
        agent.hostname ||
        agent.agentId
    )}
                </strong>

                <small>
                    ${escapeHtml(agent.agentId)}
                </small>
            </div>

            ${getStatusBadge(agent.status)}
        </div>
    `).join("");
}


/* =========================
   8. TABLEAU DE BORD
========================= */

async function loadDashboard() {
    await loadAgents();
}


/* =========================
   9. SELECTION D'UN AGENT
========================= */

function selectAgent(agentId) {
    const targetAgent = getElement("targetAgent");

    if (targetAgent) {
        targetAgent.value = agentId;
    }

    navigate("tasks");
}


/* =========================
   10. FORMULAIRE INTERVENTION
========================= */

function renderTaskFields() {
    const taskType = getElement("taskType");
    const taskFields = getElement("taskFields");

    if (!taskType || !taskFields) return;

    const selectedType = taskType.value;

    taskFields.innerHTML = "";

    if (!selectedType) return;

    /*
       Les paramètres sont construits à partir des champs
       réellement présents dans le formulaire.
       Tu peux ajouter ici des champs spécifiques à tes
       différents types de tâches.
    */

    const fields = {
        INSTALL_PRINTER: [
            {
                name: "printerName",
                label: "Nom de l'imprimante",
                type: "text"
            }
        ],

        DELETE_PRINTER: [
            {
                name: "printerName",
                label: "Nom de l'imprimante",
                type: "text"
            }
        ]
    };

    const selectedFields = fields[selectedType] || [];

    selectedFields.forEach(field => {
        const wrapper = document.createElement("div");

        wrapper.className = "form-group";

        const label = document.createElement("label");
        label.textContent = field.label;
        label.htmlFor = field.name;

        const input = document.createElement("input");
        input.type = field.type;
        input.name = field.name;
        input.id = field.name;
        input.className = "form-control";

        wrapper.appendChild(label);
        wrapper.appendChild(input);

        taskFields.appendChild(wrapper);
    });
}

function getTaskParameters() {
    const taskFields = getElement("taskFields");

    if (!taskFields) return {};

    const parameters = {};

    taskFields.querySelectorAll("[name]").forEach(input => {
        parameters[input.name] = input.value;
    });

    return parameters;
}


/* =========================
   11. ENVOI D'UNE INTERVENTION
========================= */

async function submitTask(event) {
    event.preventDefault();

    const agentId = getElement("targetAgent")?.value;
    const taskType = getElement("taskType")?.value;
    const submitButton = getElement("submitTask");

    if (!agentId) {
        showToast("Veuillez sélectionner un agent.", "error");
        return;
    }

    if (!taskType) {
        showToast("Veuillez sélectionner un type d'intervention.", "error");
        return;
    }

    const taskData = {
        agentId: agentId,
        taskType: taskType,
        parameters: getTaskParameters()
    };

    // const taskData = {
    //     agentId: agentId,
    //     taskType: taskType,
    //     parametersJson: JSON.stringify(getTaskParameters())
    // };

    try {
        if (submitButton) {
            submitButton.disabled = true;
            submitButton.textContent = "Envoi en cours...";
        }

        const response = await apiRequest("/api/tasks", {
            method: "POST",
            body: JSON.stringify(taskData)
        });

        showToast("Intervention envoyée avec succès.");

        const taskResult = getElement("taskResult");

        if (taskResult) {
            taskResult.textContent =
                response?.message ||
                "Intervention envoyée. En attente de l'agent.";

            taskResult.classList.remove("hidden");
        }

        const form = getElement("taskForm");

        if (form) {
            form.reset();
        }

        const taskFields = getElement("taskFields");

        if (taskFields) {
            taskFields.innerHTML = "";
        }

        await loadDashboard();

        // Actualiser l'historique si la page est ouverte
        if (currentPage === "history") {
            await loadHistory();
        }

    } catch (error) {
        console.error("Erreur envoi intervention :", error);
        showToast(error.message, "error");

    } finally {
        if (submitButton) {
            submitButton.disabled = false;
            submitButton.textContent = "Envoyer l'intervention";
        }
    }
}


/* =========================
   12. HISTORIQUE DES INTERVENTIONS
========================= */

async function loadHistory() {
    const table = getElement("historyTable");

    if (!table) return;

    try {
        const params = new URLSearchParams();

        params.set("page", historyPage);
        params.set("size", HISTORY_PAGE_SIZE);

        const statusFilter =
            getElement("historyStatusFilter")?.value || "";

        const search =
            getElement("historySearch")?.value.trim() || "";

        if (statusFilter) {
            params.set("status", statusFilter);
        }

        /*
           Recherche :
           - Nombre : recherche par ID de tâche.
           - Texte  : recherche par identifiant d'agent.
        */
        if (search) {
            if (/^\d+$/.test(search)) {
                params.set("id", search);
            } else {
                params.set("agentId", search);
            }
        }

        const data = await apiRequest(
            `/api/tasks?${params.toString()}`
        );

        historyTasks = data?.content || [];

        historyTotalPages = data?.totalPages || 0;
        historyTotalElements = data?.totalElements || 0;

        renderHistory(historyTasks);
        updateHistoryPagination();

    } catch (error) {
        console.error("Erreur historique :", error);

        const tbody = getHistoryTableBody();

        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" class="empty-state">
                        Impossible de charger l'historique.
                    </td>
                </tr>
            `;
        }

        showToast("Erreur lors du chargement de l'historique.", "error");
    }
}


/* =========================
   13. CORPS DU TABLEAU HISTORIQUE
========================= */

function getHistoryTableBody() {
    const table = getElement("historyTable");

    if (!table) return null;

    if (table.tagName === "TBODY") {
        return table;
    }

    return table.querySelector("tbody");
}


/* =========================
   14. AFFICHAGE HISTORIQUE
========================= */

function renderHistory(tasks) {
    const tbody = getHistoryTableBody();

    if (!tbody) return;

    if (!tasks || tasks.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" class="empty-state">
                    Aucune intervention trouvée.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = tasks.map(task => `
        <tr>
            <td>
                #${escapeHtml(task.id)}
            </td>

            <td>
                ${escapeHtml(task.agentId)}
            </td>

            <td>
                ${escapeHtml(task.taskType)}
            </td>

            <td>
                ${getStatusBadge(task.status)}
            </td>

            <td>
                ${formatDate(task.createdAt)}
            </td>

            <td>
                ${formatDate(task.startedAt)}
            </td>

            <td>
                ${formatDate(task.completedAt)}
            </td>

            <td>
                <button
                    type="button"
                    class="btn btn-primary"
                    onclick="showTaskDetails(${Number(task.id)})">
                    Voir résultat
                </button>
            </td>
        </tr>
    `).join("");
}


/* =========================
   15. DETAILS D'UNE INTERVENTION
========================= */

function showTaskDetails(taskId) {
    const task = historyTasks.find(
        item => Number(item.id) === Number(taskId)
    );

    if (!task) {
        showToast("Intervention introuvable.", "error");
        return;
    }

    const result = getTaskResult(task);

    alert(
        `INTERVENTION #${task.id}\n\n` +
        `Agent : ${task.agentId}\n` +
        `Type : ${task.taskType}\n` +
        `Statut : ${formatTaskStatus(task.status)}\n\n` +
        `Créée : ${formatDate(task.createdAt)}\n` +
        `Début : ${formatDate(task.startedAt)}\n` +
        `Fin : ${formatDate(task.completedAt)}\n` +
        `Expiration : ${formatDate(task.expiresAt)}\n\n` +
        `RÉSULTAT :\n${result}`
    );
}


/* =========================
   16. PAGINATION HISTORIQUE
========================= */

function updateHistoryPagination() {
    const pageElement = getElement("historyPage");
    const countElement = getElement("historyCount");

    const previousButton = getElement("historyPrevious");
    const nextButton = getElement("historyNext");

    if (pageElement) {
        pageElement.textContent =
            `${historyPage + 1} / ${Math.max(historyTotalPages, 1)}`;
    }

    if (countElement) {
        countElement.textContent =
            `${historyTotalElements} intervention(s)`;
    }

    if (previousButton) {
        previousButton.disabled = historyPage <= 0;
    }

    if (nextButton) {
        nextButton.disabled =
            historyPage >= historyTotalPages - 1 ||
            historyTotalPages === 0;
    }
}

function previousHistoryPage() {
    if (historyPage <= 0) return;

    historyPage--;
    loadHistory();
}

function nextHistoryPage() {
    if (historyPage >= historyTotalPages - 1) return;

    historyPage++;
    loadHistory();
}


/* =========================
   17. ACTUALISATION
========================= */

async function refreshDashboard() {
    await loadDashboard();
    showToast("Tableau de bord actualisé.");
}

async function refreshAgents() {
    await loadAgents();
    showToast("Liste des agents actualisée.");
}

async function refreshHistory() {
    await loadHistory();
    showToast("Historique actualisé.");
}


/* =========================
   18. RECUPERATION PROCHAINE TACHE
========================= */

async function getNextTask(agentId) {
    if (!agentId) return;

    try {
        const task = await apiRequest(
            `/api/tasks/agents/${encodeURIComponent(agentId)}/next`
        );

        const resultElement = getElement("next-task-result");

        if (!resultElement) return;

        resultElement.textContent = task
            ? JSON.stringify(task, null, 2)
            : "Aucune tâche disponible.";

    } catch (error) {
        console.error("Erreur récupération tâche :", error);
        showToast(error.message, "error");
    }
}


/* =========================
   19. EVENEMENTS
========================= */

function initializeEvents() {

    // Navigation
    document.querySelectorAll("[data-page]").forEach(link => {
        link.addEventListener("click", event => {
            event.preventDefault();
            navigate(link.dataset.page);
        });
    });

    // Recherche agents
    getElement("agentSearch")?.addEventListener("input", renderAgents);

    // Filtre agents
    getElement("statusFilter")?.addEventListener("change", renderAgents);

    // Type d'intervention
    getElement("taskType")?.addEventListener(
        "change",
        renderTaskFields
    );

    // Formulaire intervention
    getElement("taskForm")?.addEventListener(
        "submit",
        submitTask
    );

    // Actualisation dashboard
    getElement("refreshDashboard")?.addEventListener(
        "click",
        refreshDashboard
    );

    // Actualisation agents
    getElement("refreshAgents")?.addEventListener(
        "click",
        refreshAgents
    );

    // Actualisation historique
    getElement("refreshHistory")?.addEventListener(
        "click",
        refreshHistory
    );

    // Recherche historique
    getElement("historySearch")?.addEventListener("input", () => {
        historyPage = 0;
        loadHistory();
    });

    // Filtre statut historique
    getElement("historyStatusFilter")?.addEventListener(
        "change",
        () => {
            historyPage = 0;
            loadHistory();
        }
    );

    // Pagination précédente
    getElement("historyPrevious")?.addEventListener(
        "click",
        previousHistoryPage
    );

    // Pagination suivante
    getElement("historyNext")?.addEventListener(
        "click",
        nextHistoryPage
    );

    // Menu mobile
    getElement("mobileMenu")?.addEventListener("click", () => {
        getElement("sidebar")?.classList.toggle("open");
    });
}


/* =========================
   20. INITIALISATION
========================= */

document.addEventListener("DOMContentLoaded", async () => {

    initializeEvents();

    const apiUrlLabel = getElement("apiUrlLabel");

    if (apiUrlLabel) {
        apiUrlLabel.textContent = API_BASE_URL;
    }

    try {
        await loadDashboard();

        const serverBadge = getElement("serverBadge");

        if (serverBadge) {
            serverBadge.textContent = "Connecté";
            serverBadge.classList.add("online");
        }

        if (getElement("apiStatus")) {
            getElement("apiStatus").textContent = "En ligne";
        }

    } catch (error) {
        console.error("Initialisation impossible :", error);

        const serverBadge = getElement("serverBadge");

        if (serverBadge) {
            serverBadge.textContent = "Déconnecté";
            serverBadge.classList.add("offline");
        }

        if (getElement("apiStatus")) {
            getElement("apiStatus").textContent = "Hors ligne";
        }
    }
});
