// obsługa autoryzacji
import { createContext, useState, useContext } from 'react';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(localStorage.getItem('authToken'));
  const [role, setRole] = useState(localStorage.getItem('role'));


  const login = (newToken, newRole) => {
    localStorage.setItem('authToken', newToken);
    localStorage.setItem('role', newRole)
    setToken(newToken); 
    setRole(newRole);
  };

  const logout = () => {
    localStorage.removeItem('authToken');
    localStorage.removeItem('role');
    setToken(null);
    setRole(null);
  };

  const isLoggedIn = !!token; 

  const isAdmin = role === 'ADMIN';

  return (
    <AuthContext.Provider value={{token, role, isLoggedIn, isAdmin, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);