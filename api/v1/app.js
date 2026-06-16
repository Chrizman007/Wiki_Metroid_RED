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

// Express app setup
const app = express();
app.use(express.json());
app.use(cors());
app.use('/public', express.static(path.join(__dirname, 'api', 'v1', 'articulos', 'public')));

// Health check endpoint
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

// 1. Proxy para el Servicio de Artículos (Puerto 3001)
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

// 2. NUEVO: Proxy para el Servicio de Autenticación/Usuarios (Puerto 3002)
app.use('/auth', createProxyMiddleware({
  // Si no lo tienes en config.js, usamos por defecto el 3002
  target: (config.services && config.services.auth) ? config.services.auth : 'http://localhost:3002',
  changeOrigin: true,
  onProxyReq: (proxyReq, req, res) => {
    // Es vital reenviar el body para que el POST de Login y Registro no se cuelgue
    if (req.body) {
      const bodyData = JSON.stringify(req.body);
      proxyReq.setHeader('Content-Type', 'application/json');
      proxyReq.setHeader('Content-Length', Buffer.byteLength(bodyData));
      proxyReq.write(bodyData);
    }
  }
}));

// 3. Proxy para el Servicio de Comentarios (Puerto 3004)
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

// Export for testing
module.exports = { app, config };

// Start if run directly
if (require.main === module) {
  startServer();
}