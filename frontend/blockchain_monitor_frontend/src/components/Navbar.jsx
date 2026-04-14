import { Stack, Paper, UnstyledButton, rem, Tooltip, Center } from '@mantine/core';
import { IconBox, IconArrowsLeftRight, IconChartBar, IconFileText, IconSettings, IconHome } from '@tabler/icons-react';
import { Link, useLocation } from 'react-router-dom';

const data = [
  { icon: IconHome, label: 'Dashboard', link: '/' },
  { icon: IconBox, label: 'Bloki', link: '/blocks' },
  { icon: IconArrowsLeftRight, label: 'Transakcje', link: '/transactions' },
  { icon: IconChartBar, label: 'Statystyki', link: '/stats' },
  { icon: IconFileText, label: 'Raporty', link: '/reports' },
  { icon: IconSettings, label: 'Ustawienia', link: '/settings' },
];

export function Navbar() {
  const location = useLocation();

  return (
    <Paper 
      withBorder 
      style={{ 
        width: rem(80),      // Szerokość docelowa
        minWidth: rem(80),   // Blokada zwężania
        maxWidth: rem(80),   // Blokada rozszerzania
        height: 'calc(100vh - 70px)', 
        borderRadius: 0,
        borderTop: 0,
        borderBottom: 0,
        backgroundColor: 'var(--mantine-color-body)',
      }}
    >
      <Center pt="md">
         {/* Tu możesz wstawić małą ikonkę statusu sieci Sepolia [cite: 4] */}
      </Center>

      <Stack gap="sm" mt="xl" align="center">
        {data.map((item) => {
          const isActive = location.pathname === item.link;
          
          return (
            <Tooltip 
              label={item.label} 
              position="right" 
              withArrow 
              key={item.label}
              transitionProps={{ duration: 0 }}
            >
              <UnstyledButton
                component={Link}
                to={item.link}
                style={{
                  width: rem(45),
                  height: rem(45),
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  borderRadius: 'var(--mantine-radius-md)',
                  transition: 'background-color 100ms ease',
                  backgroundColor: isActive ? 'var(--mantine-color-blue-light)' : 'transparent',
                  color: isActive ? 'var(--mantine-color-blue-filled)' : 'var(--mantine-color-gray-6)',
                }}
              >
                <item.icon size="1.6rem" stroke={1.5} />
              </UnstyledButton>
            </Tooltip>
          );
        })}
      </Stack>
    </Paper>
  );
}