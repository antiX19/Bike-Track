require('dotenv').config();
const jwt = require('jsonwebtoken');
const bcrypt = require('bcrypt');
const fs = require('fs');

// 🔐 Charger la clé secrète depuis `.env`
const SECRET_KEY = process.env.SECRET_KEY;

// 🎯 Charger les paramètres de la base de données
const db = mysql.createConnection({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
});

// 🌐 Charger les certificats SSL
const sslOptions = {
    key: fs.readFileSync(process.env.SSL_KEY_PATH),
    cert: fs.readFileSync(process.env.SSL_CERT_PATH)
};

// 🌍 Définir le port HTTPS
const httpsPort = process.env.HTTPS_PORT || 3003;

// ✅ Lancer le serveur HTTPS avec le port sécurisé
https.createServer(sslOptions, app).listen(httpsPort, '0.0.0.0', () => {
    console.log(`🚀 API REST démarrée en HTTPS sur port ${httpsPort}`);
});
