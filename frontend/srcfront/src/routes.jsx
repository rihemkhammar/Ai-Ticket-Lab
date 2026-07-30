import React from "react";
import { Routes, Route } from "react-router-dom";
import Login from "./pages/LoginPage";
import Signup from "./pages/Signup";
import TechnicienLayout      from '../src/components/layouts/TechnicienLayout';
import TechnicianDashboard   from '../src/pages/technicien/TechnicienDashboard';
import TicketPage            from '../src/pages/technicien/Ticketpage';
import KnowledgeArticlesPage from '../src/pages/technicien/Knowledgearticlespage';
import TriagePage            from '../src/pages/technicien/Triagepage';


export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Login />} />
      <Route path="/signup" element={<Signup />} />
      <Route path="/login" element={<Login />} />

      <Route path="/technician" element={<TechnicienLayout />}>
        <Route path="dashboard" element={<TechnicianDashboard />} />
        <Route path="tickets"   element={<TicketPage />} />
        <Route path="articles"  element={<KnowledgeArticlesPage />} />
        <Route path="triage"    element={<TriagePage />} />

      </Route>
    </Routes>
  );
}