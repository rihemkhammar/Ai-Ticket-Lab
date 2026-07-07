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

// Agentic Ticket Investigation 

export const runAgentInvestigation = async (ticketId, options = {}) => {
  const response = await api.post(`/api/tickets/${ticketId}/agent/investigate`, options);
  return response.data;
};


export default api;