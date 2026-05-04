import { Grid, Paper, Text, Box, ScrollArea, Table, ActionIcon, Loader, Center } from '@mantine/core';
import { 
  IconBox, IconArrowsLeftRight, IconGasStation, IconChartBar, 
  IconFileText, IconTerminal, IconX 
} from '@tabler/icons-react';
import { useEffect, useState, useRef } from 'react';

const smallItems = [
  { title: 'Najnowsze Bloki', icon: IconBox, color: 'blue' },
  { title: 'Transakcje', icon: IconArrowsLeftRight, color: 'green' },
  { title: 'Zużycie Gasu', icon: IconGasStation, color: 'orange' },
  { title: 'Raporty PDF/CSV', icon: IconFileText, color: 'red' },
];

export function Dashboard() {
  const [blocks, setBlock] = useState([]);
  const [selectedBlock, setSelectedBlock] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(false);
  const viewportRef = useRef(null);

  useEffect(() => {
    const source = new EventSource("http://localhost:8080/sse");
    source.addEventListener('block', (e) => {
      const block = JSON.parse(e.data);
      const time = new Date().toLocaleTimeString();
      
      const newLog = {
        id: block.blockNumber,
        text: `[${time}] BLOK #${block.blockNumber} | TXs: ${block.numberOfTransactions} | Hash: ${block.blockHash.substring(0, 12)}...`
      };
      setBlock((prev) => [...prev.slice(-99), newLog]);
    });

    source.onerror = () => source.close();
    return () => source.close();
  }, []);

  const handleBlockClick = async (blockId) => {
    setSelectedBlock(blockId);
    setLoading(true);
    try {
      const response = await fetch(`http://localhost:8080/block/${blockId}`);
      const data = await response.json();
      setTransactions(data);
    } catch (error) {
      console.error("Błąd pobierania:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (viewportRef.current && !selectedBlock) {
      viewportRef.current.scrollTo({ top: viewportRef.current.scrollHeight, behavior: 'smooth' });
    }
  }, [blocks, selectedBlock]);

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
              borderRight: '1px solid var(--mantine-color-gray-3)',
              position: 'relative'
            }}
          >
            <Box style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '15px' }}>
              <Box style={{ display: 'flex', alignItems: 'center' }}>
                <IconTerminal size="1.2rem" style={{ marginRight: '10px' }} />
                <Text fw={700} size="sm">
                  {selectedBlock ? `TRANSAKCJE BLOKU #${selectedBlock}` : 'TERMINAL OPERACYJNY'}
                </Text>
              </Box>
              
              {selectedBlock && (
                <ActionIcon variant="subtle" color="gray" onClick={() => setSelectedBlock(null)}>
                  <IconX size="1.2rem" />
                </ActionIcon>
              )}
            </Box>
            
            <Box style={{ flex: 1, backgroundColor: '#1A1B1E', borderRadius: '4px', overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
              <ScrollArea viewportRef={viewportRef} style={{ flex: 1 }} p="xs">
                
                {selectedBlock ? (
                  /* WIDOK TABELI */
                  loading ? (
                    <Center style={{ height: '200px' }}><Loader color="blue" /></Center>
                  ) : (
                    <Table variant="simple" verticalSpacing="xs" style={{ color: '#ced4da', fontSize: '11px' }}>
                      <Table.Thead>
                        <Table.Tr>
                          <Table.Th style={{ color: '#888' }}>Hash</Table.Th>
                          <Table.Th style={{ color: '#888' }}>Wartość</Table.Th>
                        </Table.Tr>
                      </Table.Thead>
                      <Table.Tbody>
                        {transactions.map((tx, idx) => (
                          <Table.Tr key={idx}>
                            <Table.Td style={{ fontFamily: 'monospace' }}>{tx.hash?.substring(0, 15)}...</Table.Td>
                            <Table.Td>{tx.value} ETH</Table.Td>
                          </Table.Tr>
                        ))}
                      </Table.Tbody>
                    </Table>
                  )
                ) : (

                  blocks
                  .sort((a, b) => a.id - b.id)
                  .map((log, index) => (
                    <Text
                      key={index}
                      onClick={() => handleBlockClick(log.id)}
                      style={{ 
                        fontFamily: 'monospace', 
                        fontSize: '11px', 
                        color: '#40C057', 
                        cursor: 'pointer',
                        padding: '2px 0'
                      }}
                      className="log-item"
                    >
                      {log.text}
                    </Text>
                  ))
                )}
                {!selectedBlock && blocks.length === 0 && <Text size="xs" c="dimmed">[SYSTEM] Oczekiwanie na dane...</Text>}
              </ScrollArea>
            </Box>
          </Paper>
        </Grid.Col>

        {/* PRAWA KOLUMNA: KAFELKI */}
        <Grid.Col span={{ base: 12, md: 8 }}>
          <Grid gutter={0}>
            {smallItems.map((item) => (
              <Grid.Col span={{ base: 12, sm: 6 }} key={item.title}>
                <Paper
                  withBorder radius={0} p="xl"
                  style={{
                    height: '50vh',
                    display: 'flex', flexDirection: 'column',
                    alignItems: 'center', justifyContent: 'center',
                    backgroundColor: 'var(--mantine-color-body)',
                    borderBottom: '1px solid var(--mantine-color-gray-3)',
                    borderRight: '1px solid var(--mantine-color-gray-3)',
                  }}
                >
                  <item.icon size="2.5rem" color={`var(--mantine-color-${item.color}-6)`} stroke={1.5} />
                  <Text fw={600} mt="md" size="sm">{item.title}</Text>
                  <Text size="xs" c="dimmed">STATUS: OK</Text>
                </Paper>
              </Grid.Col>
            ))}
          </Grid>
        </Grid.Col>
      </Grid>
      
      <style>{`
        .log-item:hover { background-color: rgba(64, 192, 87, 0.1); }
      `}</style>
    </Box>
  );
}

export default Dashboard;