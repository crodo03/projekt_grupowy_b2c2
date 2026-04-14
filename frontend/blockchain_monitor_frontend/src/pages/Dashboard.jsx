import { SimpleGrid, Card, Text, Group, AspectRatio, UnstyledButton, rem, Container } from '@mantine/core';
import { 
  IconBox, 
  IconArrowsLeftRight, 
  IconGasStation, 
  IconChartBar, 
  IconHistory, 
  IconSettings, 
  IconFileText, 
  IconDatabase 
} from '@tabler/icons-react';
import { Link } from 'react-router-dom';


const mockData = [
  { title: 'Najnowsze Bloki', icon: IconBox, color: 'blue', link: '/blocks' },
  { title: 'Transakcje', icon: IconArrowsLeftRight, color: 'green', link: '/transactions' },
  { title: 'Zużycie Gasu', icon: IconGasStation, color: 'orange', link: '/gas' },
  { title: 'Statystyki Sieci', icon: IconChartBar, color: 'violet', link: '/stats' },
  { title: 'Historia Monitoringu', icon: IconHistory, color: 'cyan', link: '/history' },
  { title: 'Raporty PDF/CSV', icon: IconFileText, color: 'red', link: '/reports' },
  { title: 'Baza Danych', icon: IconDatabase, color: 'indigo', link: '/database' },
  { title: 'Konfiguracja API', icon: IconSettings, color: 'gray', link: '/settings' },
];

export function dashboard() {
  const items = mockData.map((item) => (
    <AspectRatio ratio={1} key={item.title}>
      <Card
      withBorder
      radius="0"
      padding="xl"
      //component={link}
      //to={Dashboard}
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        trasistion: 'transform 150ms ease, box-shadow ms ease',
        cursor: 'pointer',
        borderCollapse: 'collapse'
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.transform = 'scale(1.02)';
        e.currentTarget.style.boxShadow = 'var(--manite-shadow-md)';
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.transform = 'scale(1)';
        e.currentTarget.style.boxShadow = 'none';
      }}
      >
        <item.icon
          style = {{width: rem(45), height: rem(45)}}
          color={'var(--manite-color-${item.color}-6)'}
          />
          <Text size = "lg" fw={600} mt="md" textAlign="center">
            {item.title}
          </Text>

          <Text size="xs" c="dimmed" mt={5}>
            otworz zakladke
          </Text>
        </Card>
      </AspectRatio>
  ))

  return (
    <Container fluid size="lg" py="xl">
      <SimpleGrid cols={{base: 1, sm: 2, md: 4}} spacing="0">
        {items}
      </SimpleGrid>

    </Container>
  )




}

export default dashboard