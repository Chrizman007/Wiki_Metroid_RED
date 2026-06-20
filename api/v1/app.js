/**
 * Metroid Wiki - API Gateway
 * API Gateway centralizado para la Metroid Wiki
 * Solo enruta solicitudes HTTP a los servicios (SOA)
 */
require('dotenv').config();

const express = require('express');
const cors = require('cors');
const { createProxyMiddleware } = require('http-proxy-middleware');
const path = require('path');
const config = require('./config');
const app = express();
app.use(express.json());
app.use(cors());
app.use('/public', express.static(path.join(__dirname, 'api', 'v1', 'articulos', 'public')));

app.get('/health', (req, res) => {
  res.json({ 
    status: 'ok', 
    service: 'Metroid Wiki API Gateway',
    timestamp: new Date().toISOString() 
  });
});

// ==========================================
// PROXYS DE SERVICIOS (SOA)
// ==========================================

app.use('/articulos', createProxyMiddleware({
  target: config.services.articulos || 'http://localhost:3001',
  changeOrigin: true,
  onProxyReq: (proxyReq, req, res) => {
    if (req.body) {
      const bodyData = JSON.stringify(req.body);
      proxyReq.setHeader('Content-Type', 'application/json');
      proxyReq.setHeader('Content-Length', Buffer.byteLength(bodyData));
      proxyReq.write(bodyData);
    }
  }
}));

app.use('/auth', createProxyMiddleware({
  target: (config.services && config.services.auth) ? config.services.auth : 'http://localhost:3002',
  changeOrigin: true,
  onProxyReq: (proxyReq, req, res) => {
    if (req.body) {
      const bodyData = JSON.stringify(req.body);
      proxyReq.setHeader('Content-Type', 'application/json');
      proxyReq.setHeader('Content-Length', Buffer.byteLength(bodyData));
      proxyReq.write(bodyData);
    }
  }
}));

app.use('/comentarios', createProxyMiddleware({
  target: config.services.comentarios || 'http://localhost:3004',
  changeOrigin: true,
  onProxyReq: (proxyReq, req, res) => {
    if (req.body) {
      const bodyData = JSON.stringify(req.body);
      proxyReq.setHeader('Content-Type', 'application/json');
      proxyReq.setHeader('Content-Length', Buffer.byteLength(bodyData));
      proxyReq.write(bodyData);
    }
  }
}));

// ==========================================
// INICIO DEL GATEWAY
// ==========================================
function startServer() {
  const port = config.port || 3000;
  
  app.listen(port, () => {
    console.log(`Metroid Wiki API Gateway running on port ${port}`);
    console.log(`Health check: http://localhost:${port}/health`);
    console.log(`Proxying /articulos -> ${config.services.articulos || 'http://localhost:3001'}`);
    console.log(`Proxying /auth      -> ${(config.services && config.services.auth) ? config.services.auth : 'http://localhost:3002'}`);
    console.log(`Proxying /comentarios -> ${config.services.comentarios || 'http://localhost:3004'}`);
  });
}

module.exports = { app, config };

if (require.main === module) {
  startServer();
}