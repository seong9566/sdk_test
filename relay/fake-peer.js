// 가짜 상대 차량(PoC). 강남역 주변을 원을 그리며 1초마다 위치를 보내고, 받은 상대 위치를 출력한다.
// 사용: node fake-peer.js [ws://localhost:8080] [id]
const url = process.argv[2] ?? 'ws://localhost:8080';
const id = process.argv[3] ?? 'peer-demo';
const center = { lat: 37.4979, lng: 127.0276 };
const radius = 0.003; // 약 300m

const ws = new WebSocket(url); // Node 22+ 내장 WebSocket
let step = 0;

ws.addEventListener('open', () => {
  console.log(`connected as ${id}`);
  setInterval(() => {
    const t = (step++ % 60) / 60 * 2 * Math.PI;
    const lat = center.lat + radius * Math.sin(t);
    const lng = center.lng + radius * Math.cos(t) * 1.25;
    ws.send(JSON.stringify({ type: 'loc', id, lat, lng }));
  }, 1000);
});
ws.addEventListener('message', (e) => console.log(`recv ${e.data}`));
ws.addEventListener('close', () => { console.log('closed'); process.exit(0); });
ws.addEventListener('error', (e) => { console.error('error', e.message ?? e); process.exit(1); });
