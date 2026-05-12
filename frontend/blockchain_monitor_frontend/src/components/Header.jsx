import { Group, Button, ActionIcon, useMantineColorScheme, useComputedColorScheme, Image, Badge } from '@mantine/core';
import { IconSun, IconMoon, IconRefresh } from '@tabler/icons-react';
import { Link } from 'react-router-dom';

export function Header({ hasNewBlock, setHasNewBlock }) {
  const { setColorScheme } = useMantineColorScheme();
  const computedColorScheme = useComputedColorScheme('light');

  const toggleColorScheme = () => setColorScheme(computedColorScheme === 'dark' ? 'light' : 'dark');

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
          variant={hasNewBlock ? "filled" : "light"}
          color={hasNewBlock ? "green" : "gray"}
          leftSection={hasNewBlock ? <IconRefresh size={18} /> : null}
          disabled={!hasNewBlock}
          onClick={() => setHasNewBlock(false)}
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