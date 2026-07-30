import axios from "axios";

const api = axios.create({
  baseURL: "",
  headers: {
    "Content-Type": "application/json",
  },
});

// Intercepteur : ajoute le token JWT à chaque requête 
api.interceptors.request.use(
  (config) => {
    const isAuthRoute = config.url?.startsWith("/auth/");
    console.log(`[API Request] ${config.method?.toUpperCase()} ${config.url}`, {
      isAuthRoute,
      headers: config.headers,
      data: config.data,
    });

    if (!isAuthRoute) {
      const token = localStorage.getItem("token");
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
        console.log("[API Request] Token JWT ajouté aux headers");
      } else {
        console.warn("[API Request] Aucun token trouvé dans le localStorage");
      }
    } else {
      console.log("[API Request] Route publique — token non ajouté");
    }

    return config;
  },
  (error) => {
    console.error("[API Request Error] Erreur dans l'intercepteur requête :", error);
    return Promise.reject(error);
  }
);

// Intercepteur : gère les erreurs 401 (token expiré) 
api.interceptors.response.use(
  (response) => {
    console.log(`[API Response] ${response.status} ${response.config.method?.toUpperCase()} ${response.config.url}`, {
      data: response.data,
    });
    return response;
  },
  (error) => {
    const status = error.response?.status;
    const url = error.config?.url;
    const method = error.config?.method?.toUpperCase();

    console.error(`[API Response Error] ${status} ${method} ${url}`, {
      message: error.message,
      responseData: error.response?.data,
    });

    if (status === 401) {
      console.warn("[API Response Error] Token expiré ou invalide (401) — redirection vers /login");
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      window.location.href = "/login";
    }

    return Promise.reject(error);
  }
);

// Auth 
export const login = async (credentials) => {
  console.log("[Auth] Tentative de login pour :", credentials.username);
  const response = await api.post("/auth/login", credentials);

  // Le backend retourne { data: { token: "..." } } — on supporte les deux formats
  const payload = response.data?.data ?? response.data;

  console.log("[Auth] Payload reçu :", payload);

  if (payload.token) {
    localStorage.setItem("token", payload.token);
    localStorage.setItem("user", JSON.stringify(payload));
    console.log("[Auth] Login réussi — token stocké pour :", credentials.username);
  } else {
    console.warn("[Auth] Login OK mais aucun token reçu :", payload);
  }

  return payload;
};

export const register = async (userData) => {
  console.log("[Auth] Tentative d'inscription pour :", userData.email);
  const response = await api.post("/auth/register", userData);

  const payload = response.data?.data ?? response.data;

  if (payload.token) {
    localStorage.setItem("token", payload.token);
    localStorage.setItem("user", JSON.stringify(payload));
    console.log("[Auth] Inscription réussie — token stocké pour :", userData.email);
  } else {
    console.warn("[Auth] Inscription OK mais aucun token reçu :", payload);
  }

  return payload;
};

export const logout = () => {
  const user = getCurrentUser();
  console.log("[Auth] Déconnexion de :", user?.username ?? user?.email ?? "utilisateur inconnu");
  localStorage.removeItem("token");
  localStorage.removeItem("user");
  localStorage.removeItem("role");
  window.location.href = "/login";
};

export const getCurrentUser = () => {
  const user = localStorage.getItem("user");
  const parsed = user ? JSON.parse(user) : null;
  console.log("[Auth] getCurrentUser :", parsed ?? "aucun utilisateur en session");
  return parsed;
};

export const isAuthenticated = () => {
  const authenticated = !!localStorage.getItem("token");
  console.log("[Auth] isAuthenticated :", authenticated);
  return authenticated;
};

// Tickets 

// GET /api/tickets — liste tous les tickets (Technicien + Admin)
export const getAllTickets = async () => {
  const response = await api.get("/api/tickets");
  return response.data;
};

// GET /api/tickets/:id — détail d'un ticket (Technicien + Admin)
export const getTicketById = async (id) => {
  const response = await api.get(`/api/tickets/${id}`);
  return response.data;
};

// POST /api/tickets — créer un ticket (utilisateur connecté)
// body: { title: string, description: string }
export const createTicket = async ({ title, description }) => {
  const response = await api.post("/api/tickets", { title, description });
  return response.data;
};

// PATCH /api/tickets/:id — changer le statut d'un ticket (Technicien + Admin)
// status: "OPEN" | "IN_PROGRESS" | "CLOSED"
export const updateTicketStatus = async (id, status) => {
  const response = await api.patch(`/api/tickets/${id}`, { status });
  return response.data;
};

// AI Review
export const runAiReview = async (ticketId) => {
  const response = await api.post(`/api/tickets/${ticketId}/ai-review/basic`);
  return response.data;
};


// GET /api/tickets/:id/ai-review/basic — dernier review basic déjà stocké
// (204 si aucun) — pour réafficher un ancien résultat sans relancer le LLM.
export const getLatestAiReview = async (ticketId) => {
  const response = await api.get(`/api/tickets/${ticketId}/ai-review/basic`);
  return response.status === 204 ? null : response.data;
};


// Knowledge Articles 

// GET /api/articles — liste tous les articles de connaissance
export const getAllArticles = async () => {
  const response = await api.get("/api/articles");
  return response.data;
};

// GET /api/articles/:id — détail d'un article
export const getArticleById = async (id) => {
  const response = await api.get(`/api/articles/${id}`);
  return response.data;
};

// POST /api/articles/index — chunk + embed + indexe tous les articles dans pgvector
// retourne { articlesIndexed, chunksCreated }
export const indexArticles = async () => {
  const response = await api.post("/api/articles/index");
  return response.data;
};


// GET /api/articles/index/status — vrai total (articles + chunks) lu en
// base de données, à utiliser à chaque chargement de page au lieu du
// résultat éphémère du dernier indexage (qui disparaît à la navigation).
export const getArticleIndexStatus = async () => {
  const response = await api.get("/api/articles/index/status");
  return response.data; // { articlesTotal, chunksTotal }
};


// Evidence (debug retrieval) 

// GET /api/tickets/:id/evidence — chunks pertinents récupérés pour un ticket (debug, sans appel GPT)
// retourne { ticketId, evidence: [...] }
export const getTicketEvidence = async (ticketId) => {
  const response = await api.get(`/api/tickets/${ticketId}/evidence`);
  return response.data;
};

// RAG AI Review 

// POST /api/tickets/:id/ai-review/rag — lance la review GPT évidence-grounded
// retourne { reviewId, ticketId, promptVersion, modelName, status, result, errorMessage, createdAt, retrievedEvidence }
export const runRagReview = async (ticketId) => {
  const response = await api.post(`/api/tickets/${ticketId}/ai-review/rag`);
  return response.data;
};


// GET /api/tickets/:id/ai-review/rag — dernier RAG review déjà stocké
// (204 si aucun) — pour réafficher un ancien résultat sans relancer le LLM.
export const getLatestRagReview = async (ticketId) => {
  const response = await api.get(`/api/tickets/${ticketId}/ai-review/rag`);
  return response.status === 204 ? null : response.data;
};

// Agentic Ticket Investigation 

export const runAgentInvestigation = async (ticketId, options = {}) => {
  const response = await api.post(`/api/tickets/${ticketId}/agent/investigate`, options);
  return response.data;
};

// GET /api/tickets/:id/agent/investigate — dernière investigation agent
// déjà stockée (204 si aucune) — pour réafficher sans relancer l'agent.
export const getLatestAgentRun = async (ticketId) => {
  const response = await api.get(`/api/tickets/${ticketId}/agent/investigate`);
  return response.status === 204 ? null : response.data;
};

// Human-in-the-Loop Agent Review 

// POST /api/tickets/:id/agent/hitl-review — lance l'investigation agent et
// s'arrête à un checkpoint de revue humaine (WAITING_FOR_HUMAN).
// options: { userGoal?, topK?, includePreviousReviews? }
export const runHitlReview = async (ticketId, options = {}) => {
  const response = await api.post(`/api/tickets/${ticketId}/agent/hitl-review`, options);
  return response.data;
};

// GET /api/tickets/:id/agent/hitl-review — dernier run HITL du ticket
// (pending, finalisé ou rejeté), 204 si aucun — pour réafficher sans relancer.
export const getLatestHitlReview = async (ticketId) => {
  const response = await api.get(`/api/tickets/${ticketId}/agent/hitl-review`);
  return response.status === 204 ? null : response.data;
};

// GET /api/tickets/:id/agent/hitl-review/:runId — recharge le dernier
// checkpoint (pending ou finalisé) d'un run HITL sans relancer l'agent.
export const getHitlReview = async (ticketId, runId) => {
  const response = await api.get(`/api/tickets/${ticketId}/agent/hitl-review/${runId}`);
  return response.status === 204 ? null : response.data;
};

// POST /api/agent-runs/:runId/human-review/decision — décision humaine
// decision: "APPROVE" | "REJECT" | "REQUEST_REVISION"
// comment: optionnel pour APPROVE, requis pour REJECT / REQUEST_REVISION
export const submitHumanDecision = async (runId, decision, comment) => {
  const response = await api.post(`/api/agent-runs/${runId}/human-review/decision`, {
    decision,
    comment,
  });
  return response.data;
};
 
// GET /api/agent-runs/:runId/trace — trace complète du run 
export const getAgentRunTrace = async (runId) => {
  const response = await api.get(`/api/agent-runs/${runId}/trace`);
  return response.data;
};



// Triage — batch (pipeline multi-agent LangGraph4j) 
 
// POST /api/triage/batches 
export const startTriageBatch = async ({ ticketIds = [], includeAllOpenTickets = false } = {}) => {
  const response = await api.post("/api/triage/batches", {
    ticketIds,
    includeAllOpenTickets,
  });
  return response.data;
};
 
// GET /api/triage/batches/:runId — état courant d'un run de triage
// (queue restante, tickets déjà traités, statut). 404 si le run n'existe pas.
export const getTriageBatch = async (runId) => {
  const response = await api.get(`/api/triage/batches/${runId}`);
  return response.data;
};
 
// Triage — classification isolée (Agent 1 du pipeline) 
 
// POST /api/triage/classify/:ticketId — teste la classification seule
// (criticité + rationale), sans créer de triage_run. Endpoint de dev,

export const classifyTicket = async (ticketId) => {
  const response = await api.post(`/api/triage/classify/${ticketId}`);
  return response.data;
};
 
 
 

export default api;