import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { Header } from '../components/Header';

export default function PublicLayout() {
  const [hasNewBlock, setHasNewBlock] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0); 

  return (
    <>
      <Header 
        hasNewBlock={hasNewBlock} 
        setHasNewBlock={setHasNewBlock} 
        triggerRefresh={() => setRefreshKey(prev => prev + 1)} 
      />
      <main>
        <Outlet context={{ setHasNewBlock, refreshKey }} /> 
      </main>
    </>
  );
}