/**
 * Metroid Wiki - UsuarioService (Servicio de Autenticación)
 * API del Microservicio de Usuarios y Seguridad conectado a MongoDB Atlas
 */
require('dotenv').config();

const express = require('express');
const cors = require('cors');
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');

const config = {
  // Prioriza el puerto 3002 configurado en tu .env
  port: process.env.PORT || process.env.PORT_AUTH || 3002, 
  // Lee la URL de la nube de MongoDB Atlas de tu .env
  mongoUri: process.env.MONGO_URI,
  jwtSecret: process.env.JWT_SECRET
};

const router = express.Router();

// ==========================================
// CONFIGURACIÓN DE SWAGGER
// ==========================================
const swaggerUi = require('swagger-ui-express');
const swaggerJsDoc = require('swagger-jsdoc');

const swaggerOptions = {
  definition: {
    openapi: '3.0.0',
    info: {
      title: 'Metroid Wiki Auth API',
      version: '1.0.0',
      description: 'Sistema de Seguridad y Usuarios',
    },
    servers: [{ url: `http://localhost:${config.port}` }]
  },
  apis: ['./UsuarioService.js'], 
};

const swaggerDocs = swaggerJsDoc(swaggerOptions);
router.use('/api-docs', swaggerUi.serve, swaggerUi.setup(swaggerDocs));

// ==========================================
// 1. MODELO DE DATOS (Mongoose)
// ==========================================
const usuarioSchema = new mongoose.Schema({
  nombre: { type: String, required: true },
  correo: { type: String, required: true, unique: true },
  password: { type: String, required: true },
  // Actualizado a los 3 roles oficiales solicitados en minúsculas para consistencia
  rol: { type: String, enum: ['lector', 'administrador', 'desarrollador'], default: 'lector' },
  fechaRegistro: { type: Date, default: Date.now }
});

// Middleware ("Hook"): Antes de guardar en la BD, encriptamos la contraseña
usuarioSchema.pre('save', async function() {
  if (!this.isModified('password')) return;
  
  const salt = await bcrypt.genSalt(10);
  this.password = await bcrypt.hash(this.password, salt);
});

const Usuario = mongoose.model('Usuario', usuarioSchema);

// ==========================================
// 2. EXCEPCIONES ESPECÍFICAS Y DTOs
// ==========================================
class MetroidAuthException extends Error {
  constructor(mensaje, statusCode) {
    super(mensaje);
    this.name = this.constructor.name;
    this.statusCode = statusCode;
  }
}

class CredencialesInvalidasException extends MetroidAuthException {
  constructor() {
    super('El correo o la contraseña son incorrectos.', 401);
  }
}

class UsuarioYaExisteException extends MetroidAuthException {
  constructor(correo) {
    super(`Ya existe un usuario registrado con el correo: ${correo}`, 400);
  }
}

class FaltanDatosAuthException extends MetroidAuthException {
  constructor(campos) {
    super(`Faltan campos obligatorios para el registro/login: ${campos.join(', ')}`, 400);
  }
}

// DTO: ¡NUNCA enviamos el password de vuelta al cliente!
class UsuarioDTO {
  constructor(modeloMongoose) {
    this.id = modeloMongoose._id;
    this.nombre = modeloMongoose.nombre;
    this.correo = modeloMongoose.correo;
    this.rol = modeloMongoose.rol;
  }
}

// ==========================================
// 3. CAPA DE NEGOCIO (Servicio)
// ==========================================
const AuthLogicService = {
  registrarUsuario: async (datos) => {
    if (!datos.nombre || !datos.correo || !datos.password) {
      throw new FaltanDatosAuthException(['nombre', 'correo', 'password']);
    }

    const existe = await Usuario.findOne({ correo: datos.correo });
    if (existe) {
      throw new UsuarioYaExisteException(datos.correo);
    }

    const nuevoUsuario = new Usuario({
      nombre: datos.nombre,
      correo: datos.correo,
      password: datos.password,
      rol: datos.rol || 'lector' 
    });

    await nuevoUsuario.save();
    return new UsuarioDTO(nuevoUsuario);
  },

  iniciarSesion: async (correo, password, rol) => {
    if (!correo || !password) {
      throw new FaltanDatosAuthException(['correo', 'password']);
    }

    const usuario = await Usuario.findOne({ correo });
    if (!usuario) {
      throw new CredencialesInvalidasException();
    }

    const esPasswordValido = await bcrypt.compare(password, usuario.password);
    if (!esPasswordValido) {
      throw new CredencialesInvalidasException();
    }

    const token = jwt.sign(
      { id: usuario._id, rol: usuario.rol },
      config.jwtSecret,
      { expiresIn: '2h' }
    );

    return {
      usuario: new UsuarioDTO(usuario),
      token: token
    };
  }
};

// ==========================================
// 4. CAPA DE PRESENTACION (Controladores HTTP)
// ==========================================
function manejarExcepcionAuthHTTP(error, res) {
  if (error instanceof MetroidAuthException) {
    return res.status(error.statusCode).json({
      error: error.name,
      message: error.message
    });
  }
  console.error('Excepcion no controlada en Auth:', error);
  res.status(500).json({ error: 'ErrorCriticoDelServidor', message: 'Error interno en autenticación.' });
}

/**
 * @swagger
 * /auth/registro:
 *   post:
 *     summary: Registra un nuevo usuario en la Wiki
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               nombre:
 *                 type: string
 *                 example: "Christian"
 *               correo:
 *                 type: string
 *                 example: "chris@metroid.com"
 *               password:
 *                 type: string
 *                 example: "123456"
 *               rol:
 *                 type: string
 *                 example: "Administrador"
 *     responses:
 *       201:
 *         description: Usuario creado exitosamente.
 */
router.post('/registro', async (req, res) => {
  try {
    const usuarioDTO = await AuthLogicService.registrarUsuario(req.body);
    res.status(201).json({
      message: 'Usuario creado exitosamente',
      usuario: usuarioDTO
    });
  } catch (error) {
    manejarExcepcionAuthHTTP(error, res);
  }
});

/**
 * @swagger
 * /auth/login:
 *   post:
 *     summary: Inicia sesión y devuelve un Token JWT
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               correo:
 *                 type: string
 *                 example: "chris@metroid.com"
 *               password:
 *                 type: string
 *                 example: "123456"
 *     responses:
 *       200:
 *         description: Login exitoso.
 */
router.post('/login', async (req, res) => {
  try {
    const authData = await AuthLogicService.iniciarSesion(req.body.correo, req.body.password);
    
    // Armamos el JSON exactamente para Java
    res.status(200).json({
      message: 'Login exitoso',
      token: authData.token,
      nombre: authData.usuario.nombre, 
      rol: authData.usuario.rol
    });
    
  } catch (error) {
    manejarExcepcionAuthHTTP(error, res);
  }
});

// ==========================================
// 5. INICIO DEL SERVIDOR
// ==========================================
async function startAuthServer() {
  try {

    console.log("🕵️ Ruta REAL de conexión:", config.mongoUri);
    // Conexión directa y limpia usando la URI completa de MongoDB Atlas
    await mongoose.connect(config.mongoUri);
    console.log('¡Conectado exitosamente a MongoDB Atlas en la nube!');
    
    const app = express();
    app.use(express.json());
    app.use(cors());
    app.use('/auth', router); 
    
    app.listen(config.port, () => {
      console.log(`Metroid Wiki Auth Service running on port ${config.port}`);
      console.log(`Swagger docs available at: http://localhost:${config.port}/auth/api-docs`);
    });
  } catch (error) {
    console.error('Failed to start Auth Service:', error);
    process.exit(1);
  }
}

if (require.main === module) {
  startAuthServer();
}