import { Outlet } from "react-router-dom"
import { Header } from '/src/components/Header'
import { Navbar } from "../components/navbar";
//import { useAuth } from '../context/AuthContext';

function PublicLayout() {
  //const { isAdmin } = useAuth();
  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', width: '100vw' }}>
      <Header />
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        <Navbar />
        <main style={{ flex: 1, overflowY: 'auto' }}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}

export default PublicLayout;