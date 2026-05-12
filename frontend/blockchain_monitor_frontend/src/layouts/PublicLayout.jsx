import { Outlet } from "react-router-dom";
import { Header } from '../components/Header';
import { useState } from 'react';

function PublicLayout() {
  const [hasNewBlock, setHasNewBlock] = useState(false);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', width: '100vw' }}>
      <Header hasNewBlock={hasNewBlock} setHasNewBlock={setHasNewBlock} />
      
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        <main style={{ flex: 1, overflowY: 'auto' }}>
          <Outlet context={{ setHasNewBlock }} />
        </main>
      </div>
    </div>
  );
}

export default PublicLayout;