require('dotenv').config();
const express = require('express');
const mysql = require('mysql2');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');

const app = express();
const port = process.env.PORT || 3000;

app.use(express.json());

// Connexion à la base de données sécurisée
const db = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
});

// Middleware d'authentification
function authenticateToken(req, res, next) {
    const token = req.header('Authorization');
    if (!token) return res.status(401).json({ message: 'Accès refusé' });

    jwt.verify(token, process.env.JWT_SECRET, (err, user) => {
        if (err) return res.status(403).json({ message: 'Token invalide' });
        req.user = user;
        next();
    });
}

// Création de compte utilisateur
app.post('/register', async (req, res) => {
    const { username, password } = req.body;
    const hashedPassword = await bcrypt.hash(password, 10);

    db.query('INSERT INTO users (username, password) VALUES (?, ?)', [username, hashedPassword], (err) => {
        if (err) return res.status(500).json({ message: 'Erreur serveur' });
        res.status(201).json({ message: 'Utilisateur créé' });
    });
});

// Connexion utilisateur
app.post('/login', (req, res) => {
    const { username, password } = req.body;
    
    db.query('SELECT * FROM users WHERE username = ?', [username], async (err, results) => {
        if (err || results.length === 0) return res.status(401).json({ message: 'Identifiants incorrects' });
        
        const user = results[0];
        const validPassword = await bcrypt.compare(password, user.password);
        if (!validPassword) return res.status(401).json({ message: 'Identifiants incorrects' });

        const token = jwt.sign({ id: user.id, username: user.username }, process.env.JWT_SECRET, { expiresIn: '1h' });
        res.json({ token });
    });
});

// Accès aux données de l'utilisateur connecté
app.get('/me', authenticateToken, (req, res) => {
    db.query('SELECT id, username FROM users WHERE id = ?', [req.user.id], (err, results) => {
        if (err) return res.status(500).json({ message: 'Erreur serveur' });
        res.json(results[0]);
    });
});

app.listen(port, () => console.log(`Serveur démarré sur le port ${port}`));

