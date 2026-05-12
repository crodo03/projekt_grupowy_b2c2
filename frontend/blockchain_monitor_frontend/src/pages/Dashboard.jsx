import { Grid, Paper, Text, Box, ScrollArea, Table, ActionIcon, Loader, Center } from '@mantine/core';
import { 
  IconGasStation, IconFileText, IconTerminal, IconX 
} from '@tabler/icons-react';
import { useEffect, useState, useRef } from 'react';
import { useOutletContext } from 'react-router-dom';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

const smallItems = [
]

export function Dashboard() {
  const [blocks, setBlock] = useState([]);
  const [selectedBlock, setSelectedBlock] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [chartData, setChartData] = useState([]);
  const { setHasNewBlock } = useOutletContext();
  const viewportRef = useRef(null);

  // 1. Odbieranie bloków przez SSE
  useEffect(() => {
    const source = new EventSource("http://localhost:8080/sse");

    source.addEventListener('block', (e) => {
      const block = JSON.parse(e.data);
      const time = new Date().toLocaleTimeString();
      
      setBlock((prev) => {
        if (prev.find(b => b.id === block.blockNumber)) return prev;
        
        const newLog = {
          id: block.blockNumber,
          text: `[${time}] BLOK #${block.blockNumber} | TXs: ${block.numberOfTransactions} | Hash: ${block.blockHash}`
        };
        return [...prev.slice(-99), newLog].sort((a, b) => b.id - a.id);
      });
    });

    source.addEventListener('done', () => console.log('Synchronizacja zakończona'));
    
    source.addEventListener('new_block', () => {
      console.log('Nowy blok dostępny!');
      setHasNewBlock(true);
    });

    source.onerror = () => source.close();
    return () => source.close();
  }, [setHasNewBlock]);

  // pobieranie danych wykresu
  useEffect(() => {
    const fetchChartData = async () => {
      const top10 = [...blocks].sort((a, b) => b.id - a.id).slice(0, 10).reverse();
      
      if (top10.length === 0) return;

      const newChartData = [];
      
      for (let block of top10) {

        const existingData = chartData.find(d => d.block === block.id);
        
        if (existingData) {
          newChartData.push(existingData);
        } else {
          try {
            const response = await fetch(`http://localhost:8080/block/${block.id}`);
            const data = await response.json();
            newChartData.push({
              block: block.id,
              name: `#${block.id}`,
              gasMean: data.gasPricesMean / 1_000_000_000
            });
          } catch (error) {
            console.error("Błąd pobierania danych do wykresu:", error);
          }
        }
      }
      
      setChartData(newChartData);
    };

    fetchChartData();
  }, [blocks]);

  // 3. Pobieranie po kliknięciu na blok w terminalu
  const handleBlockClick = async (blockId) => {
    setSelectedBlock(blockId);
    setLoading(true);
    try {
      const response = await fetch(`http://localhost:8080/block/${blockId}`);
      const data = await response.json();
      setTransactions(data.blockTransactionInfoList || []);
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
        
        {/* terminal */}
        <Grid.Col span={{ base: 12, md: 6 }}>
          <Paper
            withBorder radius={0} p="md"
            style={{
              height: '100vh', display: 'flex', flexDirection: 'column',
              backgroundColor: 'var(--mantine-color-body)',
              borderRight: '1px solid var(--mantine-color-gray-3)',
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
              <ScrollArea viewportRef={viewportRef} style={{ flex: 1 }} p="md">
                {selectedBlock ? (
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
                  blocks.map((log, index) => (
                    <Text
                      key={index}
                      onClick={() => handleBlockClick(log.id)}
                      style={{ 
                        fontFamily: 'monospace', fontSize: '12px', color: '#40C057', 
                        cursor: 'pointer', padding: '4px 0', whiteSpace: 'nowrap'
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

        {/* prawa czesc */}
        <Grid.Col span={{ base: 12, md: 6 }}>
          <Box style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
        

            {/* wykres */}
            <Paper 
              withBorder radius={0} p="xl" 
              style={{ 
                height: '75vh', 
                backgroundColor: 'var(--mantine-color-body)',
                borderRight: '1px solid var(--mantine-color-gray-3)'
              }}
            >
              <Text fw={700} size="sm" mb="lg">TREND CENY GASU (GWEI)</Text>
              
              <Box style={{ height: 'calc(100% - 40px)', width: '100%' }}>
                {chartData.length > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={chartData} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#444" opacity={0.5} vertical={false} />
                      <XAxis 
                        dataKey="name" 
                        stroke="#888" 
                        fontSize={11} 
                        tickMargin={10} 
                        axisLine={false} 
                        tickLine={false} 
                      />
                      <YAxis 
                        stroke="#888" 
                        fontSize={11} 
                        tickFormatter={(value) => value.toFixed(2)}
                        domain={['auto', 'auto']}
                        axisLine={false}
                        tickLine={false}
                      />
                      <Tooltip 
                        contentStyle={{ backgroundColor: '#1A1B1E', border: '1px solid #333', borderRadius: '4px' }}
                        itemStyle={{ color: '#E03131', fontWeight: 600 }}
                        formatter={(value) => [`${value.toFixed(4)} Gwei`, 'Średnia cena']}
                      />
                      <Line 
                        type="monotone" 
                        dataKey="gasMean" 
                        stroke="#E03131"
                        strokeWidth={3} 
                        dot={{ r: 4, fill: '#E03131', strokeWidth: 0 }} 
                        activeDot={{ r: 6 }} 
                        animationDuration={500}
                      />
                    </LineChart>
                  </ResponsiveContainer>
                ) : (
                  <Center style={{ height: '100%' }}><Loader color="gray" variant="dots" /></Center>
                )}
              </Box>
            </Paper>

          </Box>
        </Grid.Col>

      </Grid>
      
      <style>{`
        .log-item:hover { background-color: rgba(64, 192, 87, 0.1); }
      `}</style>
    </Box>
  );
}

export default Dashboard;