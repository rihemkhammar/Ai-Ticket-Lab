import React from "react";
import { Routes, Route } from "react-router-dom";
import Login from "./pages/LoginPage";
import Signup from "./pages/Signup"

import TechnicienLayout from '../src/components/layouts/TechnicienLayout';
import TechnicianDashboard from '../src/pages/technicien/TechnicienDashboard'


export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Login />} />
      <Route path="/signup" element={<Signup/>}/>
      <Route path="/technician/dashboard" element={<TechnicianDashboard />} />
       <Route element={<TechnicienLayout />}>
        

         
        </Route>
    
    </Routes>
  );
}
