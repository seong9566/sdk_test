// 위치 공유 중계 서버(PoC). 받은 위치를 보낸 사람을 뺀 모두에게 그대로 전달하고, 끊기면 leave를 알린다.
// 위치는 저장하지 않는다. 실서비스는 wss(TLS)·인증·위치정보법상 동의/기록 처리가 필요하다.
import { WebSocketServer, WebSocket } from 'ws';

const port = Number(process.env.PORT ?? 8080);
const wss = new WebSocketServer({ port });

function broadcast(sender, data) {
  for (const client of wss.clients) {
    if (client !== sender && client.readyState === WebSocket.OPEN) client.send(data);
  }
}

wss.on('connection', (socket, req) => {
  console.log(`connect ${req.socket.remoteAddress}`);
  socket.on('message', (raw) => {
    let msg;
    try { msg = JSON.parse(raw); } catch { return; }
    if (msg.type !== 'loc' || typeof msg.id !== 'string') return;
    socket.peerId = msg.id;
    console.log(`loc ${msg.id} ${msg.lat},${msg.lng}`);
    broadcast(socket, JSON.stringify({ type: 'loc', id: msg.id, lat: msg.lat, lng: msg.lng }));
  });
  socket.on('close', () => {
    console.log(`leave ${socket.peerId ?? '(unknown)'}`);
    if (socket.peerId) broadcast(socket, JSON.stringify({ type: 'leave', id: socket.peerId }));
  });
});

console.log(`relay listening on ws://0.0.0.0:${port}`);
