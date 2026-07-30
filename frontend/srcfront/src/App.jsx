
import './App.css'
import { BrowserRouter as Router } from "react-router-dom";
import Login from './pages/LoginPage.jsx';
import AppRoutes from './routes';
import { ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

import React from "react";
import AOS from "aos";
import "aos/dist/aos.css";
const App = () => {
  React.useEffect(() => {
    AOS.init({
      offset: 200,
      duration: 800,
      easing: "ease-in-sine",
      delay: 100,
    });
  });
  return (

    <>
      
      

    <Router>
     <AppRoutes />
      <ToastContainer position="bottom-right" autoClose={3000} />
    </Router>
  


    
     

    </>
  );
};

export default App;