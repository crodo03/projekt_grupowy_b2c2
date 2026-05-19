import { Group, Button, ActionIcon, useMantineColorScheme, useComputedColorScheme, Image } from '@mantine/core';
import { IconSun, IconMoon, IconRefresh, IconDownload } from '@tabler/icons-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';

export function Header({ hasNewBlock, setHasNewBlock, triggerRefresh }) {
  const { setColorScheme } = useMantineColorScheme();
  const computedColorScheme = useComputedColorScheme('light');

  const [isDownloading, setIsDownloading] = useState(false);
  const toggleColorScheme = () => setColorScheme(computedColorScheme === 'dark' ? 'light' : 'dark');

  const downloadRaport = async (format) => {
    try{
      setIsDownloading(true);
      const respone = await fetch('http://localhost:8080/csv', {
        method: 'GET'
      });

      if(!respone.ok){
        throw new Error(`Błąd serwera: ${respone.status}`);
      }

      const blob = await respone.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `raport${new Date().toISOString().slice(0, 10)}.csv`);
      document.body.appendChild(link);
      link.click();

      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(url);

    }catch(downloadRaportError){
      console.log("raport download error: ", downloadRaport);
    }finally{
      setIsDownloading(false);
    }
  }

  return (
    <header style={{ 
      height: 70, 
      padding: '0 20px', 
      display: 'flex', 
      justifyContent: 'space-between', 
      alignItems: 'center', 
      borderBottom: '1px solid var(--mantine-color-gray-3)',
      backgroundColor: 'var(--mantine-color-body)',
      zIndex: 100
    }}>
      
      <Group component={Link} to="/" style={{ textDecoration: 'none', color: 'inherit' }}>
        <Image src="ep07.png" h={40} fallbackSrc="https://placehold.co/40x40?text=BC" />
      </Group>

      <Group gap="md"> 

        <Button 
          variant="outline" 
          color="blue" 
          leftSection={<IconDownload size={18} />}
          onClick={downloadRaport}
          loading={isDownloading}
          style={{ transition: 'all 0.3s ease' }}
        >
          Raport PDF
        </Button>

        <Button 
          variant={hasNewBlock ? "filled" : "light"}
          color={hasNewBlock ? "green" : "gray"}
          leftSection={hasNewBlock ? <IconRefresh size={18} /> : null}
          disabled={!hasNewBlock}
          onClick={() => {
            setHasNewBlock(false);
            triggerRefresh();
          }}
          style={{ 
            transition: 'all 0.3s ease',
            opacity: hasNewBlock ? 1 : 0.5,
            cursor: hasNewBlock ? 'pointer' : 'not-allowed'
          }}
        >
          {hasNewBlock ? "Nowy blok dostępny!" : "System zsynchronizowany"}
        </Button>

        <ActionIcon 
          onClick={toggleColorScheme} 
          variant="default" 
          size="lg" 
        >
          {computedColorScheme === 'dark' ? <IconSun size={18} /> : <IconMoon size={18} />}
        </ActionIcon>
      </Group>
    </header>
  );
}