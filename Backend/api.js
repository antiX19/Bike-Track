const express = require('express');
const mysql = require('mysql2');
const dotenv = require('dotenv');

// Load environment variables from .env file
dotenv.config();

const app = express();
const port = process.env.PORT || 3000;

// Middleware to parse JSON requests
app.use(express.json());

// MySQL database connection
const db = mysql.createConnection({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
});

// Connect to the database
db.connect((err) => {
    if (err) {
        console.error('Error connecting to the database:', err.message);
        process.exit(1);
    }
    console.log('Connected to the MySQL database.');
});

// API Endpoints

// 1. Fetch user details by email
app.get('/api/users/:email', (req, res) => {
    const { email } = req.params;
    const query = 'SELECT id, UUID_velo, nom, prenom, email FROM user WHERE email = ?';

    db.query(query, [email], (err, results) => {
        if (err) {
            console.error('Error fetching user details:', err.message);
            return res.status(500).json({ error: 'Internal server error' });
        }
        if (results.length === 0) {
            return res.status(404).json({ error: 'User not found' });
        }
        res.json(results[0]);
    });
});

// 2. Fetch bike details by UUID
app.get('/api/bikes/:uuid', (req, res) => {
    const { uuid } = req.params;
    const query = 'SELECT UUID, user_id, statut, gps FROM velo WHERE UUID = ?';

    db.query(query, [uuid], (err, results) => {
        if (err) {
            console.error('Error fetching bike details:', err.message);
            return res.status(500).json({ error: 'Internal server error' });
        }
        if (results.length === 0) {
            return res.status(404).json({ error: 'Bike not found' });
        }
        res.json(results[0]);
    });
});

// 3. Update bike status (e.g., stolen or normal)
app.put('/api/bikes/:uuid/status', (req, res) => {
    const { uuid } = req.params;
    const { statut } = req.body;

    if (typeof statut !== 'boolean') {
        return res.status(400).json({ error: 'Invalid status value' });
    }

    const query = 'UPDATE velo SET statut = ? WHERE UUID = ?';

    db.query(query, [statut, uuid], (err, results) => {
        if (err) {
            console.error('Error updating bike status:', err.message);
            return res.status(500).json({ error: 'Internal server error' });
        }
        if (results.affectedRows === 0) {
            return res.status(404).json({ error: 'Bike not found' });
        }
        res.json({ message: 'Bike status updated successfully' });
    });
});

// 4. Fetch bike location history by UUID
app.get('/api/bikes/:uuid/locations', (req, res) => {
    const { uuid } = req.params;
    const query = 'SELECT gps, timestamp FROM localisation WHERE UUID_velo = ? ORDER BY timestamp DESC';

    db.query(query, [uuid], (err, results) => {
        if (err) {
            console.error('Error fetching bike location history:', err.message);
            return res.status(500).json({ error: 'Internal server error' });
        }
        res.json(results);
    });
});

// Error handling for undefined routes
app.use((req, res) => {
    res.status(404).json({ error: 'Route not found' });
});

// Start the server
app.listen(port, () => {
    console.log(`Server is running on http://localhost:${port}`);
});
```

---

### Explanation of the Code:
1. **Environment Variables**:
   - The `dotenv` library is used to manage sensitive information like database credentials and the server port.

2. **Database Connection**:
   - The `mysql2` library is used to connect to the AWS RDS MySQL database.

3. **Endpoints**:
   - `/api/users/:email`: Fetches user details by email.
   - `/api/bikes/:uuid`: Fetches bike details by UUID.
   - `/api/bikes/:uuid/status`: Updates the status of a bike (e.g., stolen or normal).
   - `/api/bikes/:uuid/locations`: Fetches the location history of a bike.

4. **Error Handling**:
   - Proper error messages are returned for database errors, invalid inputs, and undefined routes.

5. **Security**:
   - The API does not expose sensitive information like passwords.
   - Input validation is performed for endpoints like updating bike status.

---

### Final Notes:
- Ensure that a `.env` file is created with the following variables:
  ```
  DB_HOST=<your-database-host>
  DB_USER=<your-database-username>
  DB_PASSWORD=<your-database-password>
  DB_NAME=biketrack
  PORT=3000
  