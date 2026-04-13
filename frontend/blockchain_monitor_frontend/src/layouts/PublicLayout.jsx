import { Outlet } from "react-router-dom"
import { Header } from '/src/components/Header'
//import { useAuth } from '../context/AuthContext';

function PublicLayout() {
  //const { isAdmin } = useAuth();
  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      
      <Header />

      <main style={{ flex: 1 }}>
        <Outlet />
      </main>
      
    </div>
  );
}

export default PublicLayout;