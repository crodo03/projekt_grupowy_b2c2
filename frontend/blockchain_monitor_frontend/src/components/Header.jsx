import { Group, Button, ActionIcon, useMantineColorScheme, useComputedColorScheme, Image, Text } from '@mantine/core';
import { IconSun, IconMoon, IconUser } from '@tabler/icons-react';
import { Link } from 'react-router-dom';

export function Header() {
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
    }}>
      
      {/* LEWA STRONA: LOGO */}
      <Group component={Link} to="/" style={{ textDecoration: 'none', color: 'inherit' }}>
        <Image 
          src="/logo.png" 
          h={40} 
          fallbackSrc="https://placehold.co/40x40?text=BC"
        />
        <Text size="xl" fw={700} variant="gradient" gradient={{ from: 'blue', to: 'cyan' }}>
          BC Monitor
        </Text>
      </Group>

      {/* PRAWA STRONA: AKCJE */}
      <Group gap="md"> 
        
        {/* PRZEŁĄCZNIK MOTYWU */}
        <ActionIcon 
          onClick={toggleColorScheme} 
          variant="default" 
          size="lg" 
          aria-label="Zmień motyw"
        >
          {computedColorScheme === 'dark' ? <IconSun size={18} /> : <IconMoon size={18} />}
        </ActionIcon>

        {/* PROSTY PRZYCISK BEZ PRZYPISANIA */}
        <Button 
          variant="filled" 
          color="blue"
          leftSection={<IconUser size={18} />}
        >
          Zaloguj
        </Button>

      </Group>
    </header>
  );
}

export default Header;