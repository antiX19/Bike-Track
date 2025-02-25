<?php
/**
 * @file documentation.php
 * @brief Documentation du fichier api.js
 *
 * Ce fichier décrit le fonctionnement du serveur Node.js qui utilise Express.js et MySQL.
 *
 * ## Présentation
 * Le fichier `api.js` met en place une API avec les fonctionnalités suivantes :
 * - Serveur HTTP sur le port 3000.
 * - Connexion à une base de données MySQL `biketrack`.
 * - Chargement des certificats SSL pour sécuriser les connexions.
 * - Routes pour interagir avec l'API.
 * - Génération de token sécurisé
 *
 * ## Utilisation
 *
 * 1. Installer les dépendances avec `npm install express mysql2`
 * 2. Lancer le serveur avec `node api.js`
 * 3. Accéder à l'API via `http://localhost:3000/`
 * 4. Créer/récupérer la clé de certificat en SSL
 * 5. Mettre à jour le fichier .env avec les paramètres de la base de données MySQL mis à jour
 *
 * @date 24 février 2025
 */
?>
