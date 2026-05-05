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

// Proxy routes to Article Service (SOA - each service has its own DB)
app.use('/articulos', createProxyMiddleware({
  target: config.services.articulos,
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

// Start server
function startServer() {
  const port = config.port;
  
  app.listen(port, () => {
    console.log(`Metroid Wiki API Gateway running on port ${port}`);
    console.log(`Health check: http://localhost:${port}/health`);
    console.log(`Proxying /articulos -> ${config.services.articulos}`);
  });
}

// Export for testing
module.exports = { app, config };

// Start if run directly
if (require.main === module) {
  startServer();
}