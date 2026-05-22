const verificarPermisos = (rolesPermitidos) => {
  return (req, res, next) => {
    const authHeader = req.headers['authorization'] || req.headers['Authorization'];
    
    if (!authHeader) {
      return res.status(403).json({ message: "No se proporcionó un token de seguridad." });
    }

    try {
      let tokenLimpio = authHeader;
      
      if (authHeader.toLowerCase().startsWith('bearer ')) {
        const partes = authHeader.split(' ');
        tokenLimpio = partes[1]; 
      }

      if (!tokenLimpio) {
         return res.status(401).json({ message: "El token proporcionado está vacío o mal formado." });
      }

      const decoded = jwt.verify(tokenLimpio, 'firma_super_secreta_metroid');
      req.user = decoded;

      if (rolesPermitidos && !rolesPermitidos.includes(decoded.rol)) {
        return res.status(403).json({ message: "Acceso denegado: No tienes los permisos necesarios." });
      }
      
      next();
    } catch (error) {
      return res.status(401).json({ message: "Token inválido o expirado.", detalle: error.message });
    }
  };
};