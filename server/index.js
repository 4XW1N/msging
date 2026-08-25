const http = require('http');
const crypto = require('crypto');
const { WebSocketServer, WebSocket } = require('ws');

const PORT = process.env.PORT || 8080;
let wss;

const server = http.createServer((req, res) => {
  res.writeHead(200, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ status: 'ok', clients: wss ? wss.clients.size : 0 }));
});

wss = new WebSocketServer({ server });

function broadcast(data) {
  const payload = JSON.stringify(data);
  for (const client of wss.clients) {
    if (client.readyState === WebSocket.OPEN) client.send(payload);
  }
}

function systemEvent(text) {
  return { type: 'system', id: crypto.randomUUID(), text, timestamp: Date.now() };
}

wss.on('connection', (socket) => {
  socket.name = null;
  console.log(`[+] client connected (${wss.clients.size} online)`);

  socket.on('message', (raw) => {
    let data;
    try {
      data = JSON.parse(raw.toString());
    } catch {
      return;
    }

    if (data.type === 'join' && typeof data.name === 'string' && data.name.trim()) {
      socket.name = data.name.trim().slice(0, 32);
      broadcast(systemEvent(`${socket.name} joined`));
      return;
    }

    if (data.type === 'chat') {
      const text = String(data.text || '').slice(0, 2000).trim();
      if (!text) return;
      broadcast({
        type: 'chat',
        id: crypto.randomUUID(),
        sender: socket.name || 'anonymous',
        text,
        timestamp: Date.now()
      });
    }
  });

  socket.on('close', () => {
    if (socket.name) broadcast(systemEvent(`${socket.name} left`));
    console.log(`[-] client disconnected (${wss.clients.size} online)`);
  });
});

server.listen(PORT, () => {
  console.log(`Chat server listening on ws://localhost:${PORT}`);
});
