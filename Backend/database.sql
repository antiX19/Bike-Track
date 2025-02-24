-- Création de la base de données
CREATE DATABASE IF NOT EXISTS biketrack;
USE biketrack;

-- Table des utilisateurs
CREATE TABLE user (
    id INT AUTO_INCREMENT PRIMARY KEY,
    UUID_velo VARCHAR(50) UNIQUE,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    psw VARCHAR(255) NOT NULL
);

-- Table des vélos
CREATE TABLE velo (
    UUID VARCHAR(50) PRIMARY KEY,
    user_id INT,
    statut BOOLEAN DEFAULT FALSE, -- FALSE = normal, TRUE = volé
    gps VARCHAR(255), -- Stocker les coordonnées GPS sous forme de texte (JSON ou lat,long)
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE SET NULL
);

-- Table de localisation
CREATE TABLE localisation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    UUID_velo VARCHAR(50),
    gps VARCHAR(255),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (UUID_velo) REFERENCES velo(UUID) ON DELETE CASCADE
);

-- Index pour optimiser les recherches
CREATE INDEX idx_user_uuid ON user(UUID_velo);
CREATE INDEX idx_velo_statut ON velo(statut);
CREATE INDEX idx_localisation_uuid ON localisation(UUID_velo);
