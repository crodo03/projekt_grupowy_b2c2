import { Grid, Paper, Text, Box, ScrollArea, Code } from '@mantine/core';
import { 
  IconBox, 
  IconArrowsLeftRight, 
  IconGasStation, 
  IconChartBar, 
  IconFileText, 
  IconTerminal 
} from '@tabler/icons-react';
import { useEffect, useState, useRef } from 'react';

const smallItems = [
  { title: 'Najnowsze Bloki', icon: IconBox, color: 'blue' },
  { title: 'Transakcje', icon: IconArrowsLeftRight, color: 'green' },
  { title: 'Zużycie Gasu', icon: IconGasStation, color: 'orange' },
  { title: 'Raporty PDF/CSV', icon: IconFileText, color: 'red' },
];

export function Dashboard() {
  const [logs, setLogs] = useState([]);
  const viewportRef = useRef(null);

  useEffect(() => {
    const source = new EventSource("http://localhost:8080");

    source.addEventListener('block', (e) => {
      const block = JSON.parse(e.data);

      const time = new Date().toLocaleTimeString();
      const newLog = `[${time}] BLOK #${block.blockNumber} | TXs: ${block.numberOfTransactions} | Hash: ${block.blockHash.substring(0, 12)}...`;

      setLogs((prevLogs) => [...prevLogs.slice(-100), newLog]);
    });

    source.addEventListener('done', (e) => {
      setLogs((prevLogs) => [...prevLogs, `[SYSTEM] Proces zakończony. Czas: ${e.data}s`]);
      source.close();
    });

    source.onerror = (err) => {
      console.error("Błąd połączenia SSE:", err);
      source.close();
    };

    return () => {
      source.close();
    };
  }, []);


  useEffect(() => {
    if (viewportRef.current) {
      viewportRef.current.scrollTo({ top: viewportRef.current.scrollHeight, behavior: 'smooth' });
    }
  }, [logs]);

  return (
    <Box style={{ height: '100vh', backgroundColor: 'var(--mantine-color-black-1)' }}>
      <Grid gutter={0} style={{ margin: 0 }}>
        
        <Grid.Col span={{ base: 12, md: 4 }}>
          <Paper
            withBorder
            radius={0}
            p="md"
            style={{
              height: '100vh',
              display: 'flex',
              flexDirection: 'column',
              backgroundColor: 'var(--mantine-color-body)',
              borderRight: '1px solid var(--mantine-color-gray-3)'
            }}
          >
            <Box style={{ display: 'flex', alignItems: 'center', marginBottom: '15px' }}>
              <IconTerminal size="1.2rem" style={{ marginRight: '10px' }} />
              <Text fw={700} size="sm">TERMINAL OPERACYJNY</Text>
            </Box>
            
            <ScrollArea 
              viewportRef={viewportRef}
              scrollbarSize={6} 
              style={{ flex: 1, backgroundColor: '#1A1B1E', borderRadius: '4px', padding: '12px' }}
            >
              <Code block style={{ backgroundColor: 'transparent', color: '#40C057', fontSize: '11px', lineHeight: 1.6 }}>
                {logs.length > 0 ? logs.join('\n') : "[SYSTEM] Oczekiwanie na dane..."}
              </Code>
            </ScrollArea>
          </Paper>
        </Grid.Col>


        <Grid.Col span={{ base: 12, md: 8 }}>
          <Grid gutter={0}>
            {smallItems.map((item) => (
              <Grid.Col span={{ base: 12, sm: 6 }} key={item.title}>
                <Paper
                  withBorder
                  radius={0}
                  p="xl"
                  style={{
                    height: '50vh',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    justifyContent: 'center',
                    backgroundColor: 'var(--mantine-color-body)',
                    borderBottom: '1px solid var(--mantine-color-gray-3)',
                    borderRight: '1px solid var(--mantine-color-gray-3)',
                  }}
                >
                  <item.icon size="2.5rem" color={`var(--mantine-color-${item.color}-6)`} stroke={1.5} />
                  <Text fw={600} mt="md" size="sm">{item.title}</Text>
                  <Text size="xs" c="dimmed">POŁĄCZONO</Text>
                </Paper>
              </Grid.Col>
            ))}
          </Grid>
        </Grid.Col>
      </Grid>
    </Box>
  );
}

export default Dashboard;