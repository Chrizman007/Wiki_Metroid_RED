/**
 * Metroid Wiki - Configuration
 * Centralized configuration for the API Gateway
 */
require('dotenv').config();

module.exports = {
  port: process.env.PORT || 3000,
  services: {
    articulos: process.env.ARTICULOS_URL || 'http://localhost:3001'
  }
};