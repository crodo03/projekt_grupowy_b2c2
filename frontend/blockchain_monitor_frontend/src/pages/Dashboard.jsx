import { Grid, Paper, Text, Box, ScrollArea, Table, ActionIcon, Loader, Center } from '@mantine/core';
import { 
  IconGasStation, IconFileText, IconTerminal, IconX 
} from '@tabler/icons-react';
import { useEffect, useState, useRef } from 'react';
import { useOutletContext } from 'react-router-dom';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

export function Dashboard() {
  const [blocks, setBlock] = useState([]);
  const [pendingBlocks, setPendingBlocks] = useState([]);
  const [selectedBlock, setSelectedBlock] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [chartData, setChartData] = useState([]);
  const { setHasNewBlock, refreshKey } = useOutletContext();
  const viewportRef = useRef(null);
  const chartCacheRef = useRef({});

  // odbieranie bloków przez sse
  useEffect(() => {
    const source = new EventSource("http://localhost:8080/sse");

    // odbieranie bloków pojedyczo
    source.addEventListener('block', (e) => {
      const block = JSON.parse(e.data);
      
      setBlock((prev) => {
        if (prev.find(b => b.id === block.blockNumber)) return prev;
        
        const newLog = {
          id: block.blockNumber,
          time: block.fetchedAt,
          text: `[${block.fetchedAt}] BLOK #${block.blockNumber} | TXs: ${block.numberOfTransactions} | Hash: ${block.blockHash}`
        };
        return [...prev.slice(-100), newLog].sort((a, b) => a.id - b.id);
      });
    });

    source.addEventListener('done', () => {
      console.log('🏁 [SSE] Synchronizacja początkowa zakończona');
    });
    
    // odbieranie paczki po refresh
    source.addEventListener('updated_blocks', (e) => {
      const parsedArray = JSON.parse(e.data);
      console.log(`🔔 [SSE] Otrzymano nową paczkę (${parsedArray.length} bloków) w tle. Czeka na odświeżenie.`); 
      
      const formattedBlocks = parsedArray.map(block => ({
        id: block.blockNumber,
        time: block.fetchedAt,
        text: `[${block.fetchedAt}] BLOK #${block.blockNumber} | TXs: ${block.numberOfTransactions} | Hash: ${block.blockHash}`
      })).sort((a, b) => a.id - b.id);

      setPendingBlocks(formattedBlocks);
      setHasNewBlock(true);
    });

    source.onerror = () => source.close();
    
    return () => {
      source.close();
    };
  }, [setHasNewBlock]); 

  useEffect(() => {
    if (pendingBlocks) {
      setBlock(pendingBlocks); 
      setPendingBlocks(null); 
    }
  }, [refreshKey]);


  // pobieranie danych wykresu
  useEffect(() => {
    const fetchChartData = async () => {
      const top10 = [...blocks].sort((a, b) => b.id - a.id).slice(0, 15).reverse();
      
      if (top10.length === 0) return;

      const newChartData = [];
      
      for (let block of top10) {
        if (chartCacheRef.current[block.id]) {
          newChartData.push(chartCacheRef.current[block.id]);
        } else {
          try {
            const response = await fetch(`http://localhost:8080/block/${block.id}`);
            const data = await response.json();

            console.log(`[DEBUG] Pobrane dane dla bloku ${block.id}:`, data); 

            const gas = data.gasPricesMean ? (data.gasPricesMean / 1_000_000_000) : 0;
            const newDataPoint = {
              block: block.id,
              time: block.time,
              gasMean: gas
            };
            
            chartCacheRef.current[block.id] = newDataPoint;
            newChartData.push(newDataPoint);
          } catch (error) {
            console.error(`Błąd pobierania danych do wykresu dla bloku ${block.id}:`, error);
          }
        }
      }
      
      setChartData(newChartData);
    };

    fetchChartData();
  }, [blocks]);

  // pobieranie po kliknięciu na blok w terminalu
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
    <Box style={{ height: '100vh', backgroundColor: 'var(--mantine-color-black-1)', position: 'relative' }}>
      
      {selectedBlock ? (
        <Paper
          withBorder
          radius={0}
          p="md"
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            width: '100%',
            height: '100vh',
            display: 'flex',
            flexDirection: 'column',
            backgroundColor: 'var(--mantine-color-body)',
            zIndex: 10
          }}
        >

          <Box style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '15px' }}>
            <Box style={{ display: 'flex', alignItems: 'center' }}>
              <IconTerminal size="1.2rem" style={{ marginRight: '10px' }} />
              <Text fw={700} size="sm">
                SZCZEGÓŁOWE TRANSAKCJE BLOKU #{selectedBlock}
              </Text>
            </Box>
            

            <ActionIcon variant="subtle" color="gray" size="lg" onClick={() => setSelectedBlock(null)}>
              <IconX size="1.4rem" />
            </ActionIcon>
          </Box>
          
          <Box style={{ flex: 1, backgroundColor: '#1A1B1E', borderRadius: '4px', overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
            <ScrollArea style={{ flex: 1 }} p="md">
              {loading ? (
                <Center style={{ height: '100%' }}><Loader color="blue" /></ Center>
              ) : (
                // tabela transakcji
                <Table variant="simple" verticalSpacing="xs" style={{ color: '#ced4da', fontSize: '11px' }}>
                  <Table.Thead>
                    <Table.Tr>
                      <Table.Th style={{ color: '#888' }}>Hash TX</Table.Th>
                      <Table.Th style={{ color: '#888' }}>Od (Nadawca)</Table.Th>
                      <Table.Th style={{ color: '#888' }}>Do (Odbiorca)</Table.Th>
                      <Table.Th style={{ color: '#888' }}>Wartość</Table.Th>
                      <Table.Th style={{ color: '#888' }}>Zużycie Gasu</Table.Th>
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {transactions.map((tx, idx) => (
                      <Table.Tr key={idx}>
                        <Table.Td style={{ fontFamily: 'monospace', color: '#4dabf7' }}>
                          {tx.hash?.substring(0, 15)}...
                        </Table.Td>
                        <Table.Td style={{ fontFamily: 'monospace' }}>
                          {tx.from?.substring(0, 12)}...
                        </Table.Td>
                        <Table.Td style={{ fontFamily: 'monospace' }}>
                          {tx.to?.substring(0, 12)}...
                        </Table.Td>
                        <Table.Td style={{ fontWeight: 600, color: '#fcc419' }}>
                          {tx.value} ETH
                        </Table.Td>
                        <Table.Td>
                          {tx.gasUsed || tx.gas}
                        </Table.Td>
                      </Table.Tr>
                    ))}
                  </Table.Tbody>
                </Table>
              )}
            </ScrollArea>
          </Box>
        </Paper>
      ) : (
        // widok konsola + wykres
        <Grid gutter={0} style={{ margin: 0 }}>
          
          <Grid.Col span={{ base: 12, md: 6 }}>
            <Paper
              withBorder radius={0} p="md"
              style={{
                height: '100vh', display: 'flex', flexDirection: 'column',
                backgroundColor: 'var(--mantine-color-body)',
                borderRight: '1px solid var(--mantine-color-gray-3)',
              }}
            >
              <Box style={{ display: 'flex', alignItems: 'center', marginBottom: '15px' }}>
                  <IconTerminal size="1.2rem" style={{ marginRight: '10px' }} />
                  <Text fw={700} size="sm">TERMINAL OPERACYJNY</Text>
              </Box>
              
              <Box style={{ flex: 1, backgroundColor: '#1A1B1E', borderRadius: '4px', overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
                <ScrollArea viewportRef={viewportRef} style={{ flex: 1 }} p="md">
                    {blocks.map((log, index) => (
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
                    ))}
                  {!selectedBlock && blocks.length === 0 && <Text size="xs" c="dimmed">[SYSTEM] Oczekiwanie na dane...</Text>}
                </ScrollArea>
              </Box>
            </Paper>
          </Grid.Col>

          <Grid.Col span={{ base: 12, md: 6 }}>

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
                        dataKey="time" 
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
                        labelStyle={{ color: '#888', marginBottom: '5px' }} 
                        labelFormatter={(label) => `Czas: ${label}`} 
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
          </Grid.Col>
        </Grid>
      )}
      
      <style>{`
        .log-item:hover { background-color: rgba(64, 192, 87, 0.1); }
      `}</style>
    </Box>
  );
}

export default Dashboard;