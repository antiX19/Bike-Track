const httpsPort = 3002; // Définit le port HTTPS
const express = require('express');
const mysql = require('mysql2');
const app = express();
const port = 3000;
const fs = require('fs');
const https = require('https');
app.use(express.json()); // Middleware pour parser le JSON

// Charger les certificats SSL

const sslOptions = {
    key: fs.readFileSync('/etc/ssl/private/selfsigned.key'),
    cert: fs.readFileSync('/etc/ssl/certs/selfsigned.crt')
};


// Connexion à la base de données MySQL
const db = mysql.createConnection({
    host: 'localhost',
    user: 'tpreseau', // Remplace par ton utilisateur MySQL
    password: 'tpreseau', // Remplace par ton mot de passe MySQL
    database: 'biketrack'
});

db.connect(err => {
    if (err) {
        console.error('Erreur de connexion à MySQL :', err);
        return;
    }
    console.log('Connecté à MySQL');
});

// ========================= ROUTES =========================

// Route de test
app.get('/', (req, res) => {
    res.send('API BikeTrack est en ligne 🚴‍♂️!');
});

// ROUTE POUR RECUPERER DONNEES GPS EN FCT DE L'UUID
app.get('/gps/:UUID', (req, res) => {
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


// ✅ Inscription d'un utilisateur avec enregistrement du vélo
app.post('/register', (req, res) => {
    const { UUID_velo, pseudo, email, psw, nom_module, pin } = req.body;

    if (!pseudo || !email || !psw) {
        return res.status(400).json({ message: "Pseudo, email et mot de passe sont requis" });
    }

    db.query('INSERT INTO user (UUID_velo, pseudo, email, psw) VALUES (?, ?, ?, ?)',
        [UUID_velo || null, pseudo, email, psw],
        (err, userResult) => {
            if (err) {
                res.status(500).json({ error: err.message });
            } else {
                if (UUID_velo) {
                    db.query('INSERT INTO velo (UUID, nom_module, pin) VALUES (?, ?, ?)',
                        [UUID_velo, nom_module || null, pin || null],
                        (err, veloResult) => {
                            if (err) {
                                res.status(500).json({ error: err.message });
                            } else {
                                res.json({
                                    message: 'Utilisateur et vélo enregistrés avec succès',
                                    user_id: userResult.insertId,
                                    velo_id: veloResult.insertId
                                });
                            }
                        }
                    );
                } else {
                    res.json({
                        message: 'Utilisateur enregistré avec succès',
                        user_id: userResult.insertId
                    });
                }
            }
        }
    );
});
// ✅ Changer le statut d'un vélo en volé
app.put('/velo/vole/:UUID', (req, res) => {
    const { UUID } = req.params;
    const { user_id } = req.body; // ID de l'utilisateur connecté

    if (!user_id) {
        return res.status(400).json({ message: "L'ID utilisateur est requis" });
    }

    // Vérifier que le vélo appartient à cet utilisateur
    db.query('SELECT * FROM velo WHERE UUID = ? AND user_id = ?', [UUID, user_id], (err, results) => {
        if (err) {
            res.status(500).json({ error: err.message });
        } else if (results.length === 0) {
            res.status(403).json({ message: "Action non autorisée. Ce vélo ne vous appartient pas." });
        } else {
            // Mise à jour du statut
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

// ✅ Changer le statut d'un vélo en retrouvé (sécurisé par user_id)
app.put('/velo/retrouve/:UUID', (req, res) => {
    const { UUID } = req.params;
    const { user_id } = req.body;

    if (!user_id) {
        return res.status(400).json({ message: "L'ID utilisateur est requis" });
    }

    // Vérifier que l'utilisateur est bien le propriétaire du vélo
    db.query('SELECT * FROM velo WHERE UUID = ? AND user_id = ?', [UUID, user_id], (err, results) => {
        if (err) {
            res.status(500).json({ error: err.message });
        } else if (results.length === 0) {
            res.status(403).json({ message: "Action non autorisée. Ce vélo ne vous appartient pas." });
        } else {
            // Mise à jour du statut
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
// ✅ Enregistrer les données GPS après vérification de l'UUID
app.post('/gps/test', (req, res) => {
	const { UUID_velo, gps } = req.body;

	if (!UUID_velo || !gps) {
    	return res.status(400).json({ message: "UUID_velo et gps sont requis" });
	}

	// Vérifier si l'UUID_velo existe dans la base
	db.query('SELECT UUID FROM velo WHERE UUID = ?', [UUID_velo], (err, results) => {
    	if (err) {
        	res.status(500).json({ error: err.message });
    	} else if (results.length === 0) {
        	res.status(404).json({ message: "UUID_velo non trouvé dans la base de données" });
    	} else {
        	// Insérer la localisation
        	db.query('INSERT INTO localisation (UUID_velo, gps) VALUES (?, ?)',
            	[UUID_velo, gps],
            	(err, result) => {
                	if (err) {
                    	res.status(500).json({ error: err.message });
                	} else {
                    	res.json({ message: "Localisation ajoutée avec succès", id: result.insertId });
                	}
            	}
        	);
    	}
	});
});

// ✅ Récupérer le statut d'un vélo lié à un utilisateur
app.get('/velo/statut/:UUID', (req, res) => {
	const { UUID } = req.params;

	db.query('SELECT statut FROM velo WHERE UUID = ?', [UUID], (err, results) => {
    	if (err) {
        	res.status(500).json({ error: err.message });
    	} else if (results.length === 0) {
        	res.status(404).json({ message: "UUID non trouvé" });
    	} else {
        	res.json({ UUID, statut: results[0].statut });
    	}
	});
});

// ✅ Lier un UUID à un utilisateur
app.post('/user/velo', (req, res) => {
	const { user_id, UUID_velo } = req.body;

	if (!user_id || !UUID_velo) {
    	return res.status(400).json({ message: "L'ID utilisateur et l'UUID du vélo sont requis" });
	}

	db.query('UPDATE user SET UUID_velo = ? WHERE id = ?', [UUID_velo, user_id], (err, result) => {
    	if (err) {
        	res.status(500).json({ error: err.message });
    	} else {
        	res.json({ message: `Vélo ${UUID_velo} lié à l'utilisateur ${user_id}` });
    	}
	});
});

// ✅ Méthode de connexion avec vérification des identifiants
app.post('/login', (req, res) => {
	const { email, psw } = req.body;

	if (!email || !psw) {
    	return res.status(400).json({ message: "Email et mot de passe requis" });
	}

	db.query('SELECT id, UUID_velo, pseudo FROM user WHERE email = ? AND psw = ?',
    	[email, psw],
    	(err, results) => {
        	if (err) {
            	res.status(500).json({ error: err.message });
        	} else if (results.length === 0) {
            	res.status(401).json({ message: "Identifiants incorrects" });
        	} else {
            	res.json({ message: "Connexion réussie", user: results[0] });
        	}
    	}
	);
});

// ✅ Enregistrer les données GPS uniquement si le vélo est volé
app.post('/gps', (req, res) => {
    const { UUID_velo, gps } = req.body;

    if (!UUID_velo || !gps) {
        return res.status(400).json({ message: "UUID_velo et gps sont requis" });
    }

    // Vérifier si l'UUID_velo existe et récupérer son statut
    db.query('SELECT statut FROM velo WHERE UUID = ?', [UUID_velo], (err, results) => {
        if (err) {
            res.status(500).json({ error: err.message });
        } else if (results.length === 0) {
            res.status(404).json({ message: "UUID_velo non trouvé dans la base de données" });
        } else {
            const statut = results[0].statut;
            
            // Si le vélo est volé, insérer la localisation
            if (statut === 1) {
                db.query('INSERT INTO localisation (UUID_velo, gps) VALUES (?, ?)',
                    [UUID_velo, gps],
                    (err, result) => {
                        if (err) {
                            res.status(500).json({ error: err.message });
                        } else {
                            res.json({ 
                                message: "Localisation ajoutée car le vélo est déclaré volé", 
                                id: result.insertId, 
                                statut_vol: "Attention, ce vélo est déclaré volé" 
                            });
                        }
                    }
                );
            } else {
                res.json({ message: "Le vélo n'est pas volé, aucune localisation enregistrée." });
            }
        }
    });
});

// Lancer le serveur HTTP
//app.listen(port, () => {
//    console.log(`API REST démarrée en HTTP sur http://localhost:${port}`);
//});

// Lancer le serveur HTTPS
https.createServer(sslOptions, app).listen(httpsPort, '0.0.0.0', () => {
    console.log(`API REST démarrée en HTTPS sur https://13.36.126.63`);
});
