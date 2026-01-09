require('dotenv').config();
const express = require('express');
const mysql = require('mysql2');
const fs = require('fs');
const https = require('https');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcrypt');
const app = express();
const port = 3000;
app.use(express.json()); // Middleware pour parser le JSON

const SECRET_KEY = process.env.SECRET_KEY;

//Charger les variables d'environnement
//const SECRET_KEY = process.env.SECRET_KEY;
//const httpsPort = process.env.HTTPS_PORT || 3003;

//Connexion à la base de données
const db = mysql.createConnection({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
});

db.connect(err => {
    if (err) {
        console.error('Erreur de connexion MySQL :', err);
        return;
    }
    console.log('Connecté à MySQL');
});

/*Charger les certificats SSL
const sslOptions = {
    key: fs.readFileSync(process.env.SSL_KEY_PATH),
    cert: fs.readFileSync(process.env.SSL_CERT_PATH)
};*/

// ==MIDDLEWARE JWT ==
// Middleware pour protéger les routes avec JWT
function verifyToken(req, res, next) {
    const token = req.headers['authorization'];
    if (!token) {
        return res.status(403).json({ message: "Un token est requis pour l'authentification" });
    }

    try {
        const decoded = jwt.verify(token.split(" ")[1], SECRET_KEY);
        req.user = decoded;
        next();
    } catch (err) {
        return res.status(401).json({ message: "Token invalide ou expiré" });
    }
}

// ========================= ROUTES =========================

// Route de test
app.get('/', verifyToken, (req, res) => {
    res.send('🚀 API BikeTrack est en ligne et sécurisée !');
});

// Route de connexion avec JWT
app.post('/login', (req, res) => {
    const { pseudo, psw } = req.body;

    if (!pseudo || !psw) {
        return res.status(400).json({ message: "Pseudo et mot de passe requis" });
    }

    db.query('SELECT id, UUID_velo, pseudo, psw FROM user WHERE pseudo = ?', [pseudo], async (err, results) => {
        if (err) {
            return res.status(500).json({ error: err.message });
        } else if (results.length === 0) {
            return res.status(401).json({ message: "Identifiants incorrects" });
        } else {
            const user = results[0];

            // Vérifier le mot de passe (si hashé, utiliser bcrypt.compare)
            const isMatch = user.psw === psw;
            if (!isMatch) {
                return res.status(401).json({ message: "Identifiants incorrects" });
            }

            // Générer un token JWT
            const token = jwt.sign(
                { id: user.id, pseudo: user.pseudo, UUID_velo: user.UUID_velo }, 
                SECRET_KEY, 
                { expiresIn: "2h" } 
            );

            res.json({ 
                message: "Connexion réussie", 
                token,
                user: { id: user.id, pseudo: user.pseudo, UUID_velo: user.UUID_velo } 
            });
        }
    });
});

// Afficher tous les utilisateurs (sécurisé)
app.get('/users', verifyToken, (req, res) => {
    db.query('SELECT * FROM user', (err, results) => {
        if (err) {
            res.status(500).json({ error: err.message });
        } else {
            res.json(results);
        }
    });
});

// Récupérer les données GPS d'un vélo (sécurisé)
app.get('/gps/:UUID', verifyToken, (req, res) => {
    const { UUID } = req.params;

    db.query('SELECT gps, timestamp FROM localisation WHERE UUID_velo = ?', [UUID], (err, results) => {
        if (err) {
            res.status(500).json({ error: err.message });
        } else if (results.length === 0) {
            res.status(404).json({ message: "Aucune donnée GPS trouvée pour cet UUID" });
        } else {
            res.json(results);
        }
    });
});

// Marquer un vélo comme volé (sécurisé)
app.put('/velo/vole/:UUID', verifyToken, (req, res) => {
    const { UUID } = req.params;
    const { user_id } = req.body;

    if (!user_id) {
        return res.status(400).json({ message: "L'ID utilisateur est requis" });
    }

    db.query('SELECT * FROM velo WHERE UUID = ? AND user_id = ?', [UUID, user_id], (err, results) => {
        if (err) {
            res.status(500).json({ error: err.message });
        } else if (results.length === 0) {
            res.status(403).json({ message: "Action non autorisée. Ce vélo ne vous appartient pas." });
        } else {
            db.query('UPDATE velo SET statut = 1 WHERE UUID = ?', [UUID], (err, result) => {
                if (err) {
                    res.status(500).json({ error: err.message });
                } else {
                    res.json({ message: `Le vélo ${UUID} est maintenant marqué comme volé.` });
                }
            });
        }
    });
});

// Modifier le statut d'un vélo retrouvé (sécurisé)
app.put('/velo/retrouve/:UUID', verifyToken, (req, res) => {
    const { UUID } = req.params;
    const { user_id } = req.body;

    if (!user_id) {
        return res.status(400).json({ message: "L'ID utilisateur est requis" });
    }

    db.query('SELECT * FROM velo WHERE UUID = ? AND user_id = ?', [UUID, user_id], (err, results) => {
        if (err) {
            res.status(500).json({ error: err.message });
        } else if (results.length === 0) {
            res.status(403).json({ message: "Action non autorisée. Ce vélo ne vous appartient pas." });
        } else {
            db.query('UPDATE velo SET statut = 0 WHERE UUID = ?', [UUID], (err, result) => {
                if (err) {
                    res.status(500).json({ error: err.message });
                } else {
                    res.json({ message: `Le vélo ${UUID} a été retrouvé.` });
                }
            });
        }
    });
});

// Démarer HTTP
app.listen(port,'0.0.0.0', () => {
    console.log(`API REST démarrée sur http://13.36.126.63:${port}`);
});

/* ========================= LANCEMENT DU SERVEUR HTTPS =========================
https.createServer(sslOptions, app).listen(httpsPort, '0.0.0.0', () => {
    console.log(`🚀 API REST démarrée en HTTPS sur le port ${httpsPort}`);
});*/
