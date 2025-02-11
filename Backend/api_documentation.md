# API Documentation for Bike Tracker

This document provides an overview of the API endpoints available in the Bike Tracker application. The API is designed to interact with the backend services, allowing users to manage their bike data, track locations, and update statuses.

---

## Base URL

The API is hosted at:

```
http://<your-server-domain>:<port>/api
```

Replace `<your-server-domain>` and `<port>` with the appropriate values for your deployment.

---

## Endpoints

### 1. Fetch User Details by Email

**Endpoint**: `/users/:email`  
**Method**: `GET`  
**Description**: Retrieves user details based on the provided email address.  

**Request Parameters**:  
- `email` (path parameter): The email address of the user.

**Response**:  
- **200 OK**: Returns the user details in JSON format.  
  ```json
  {
      "id": 1,
      "UUID_velo": "123e4567-e89b-12d3-a456-426614174000",
      "nom": "Doe",
      "prenom": "John",
      "email": "john.doe@example.com"
  }
  ```
- **404 Not Found**: If the user is not found.  
- **500 Internal Server Error**: If there is a database error.

---

### 2. Fetch Bike Details by UUID

**Endpoint**: `/bikes/:uuid`  
**Method**: `GET`  
**Description**: Retrieves bike details based on the provided UUID.  

**Request Parameters**:  
- `uuid` (path parameter): The unique identifier of the bike.

**Response**:  
- **200 OK**: Returns the bike details in JSON format.  
  ```json
  {
      "UUID": "123e4567-e89b-12d3-a456-426614174000",
      "user_id": 1,
      "statut": false,
      "gps": "{\"lat\":48.8566,\"long\":2.3522}"
  }
  ```
- **404 Not Found**: If the bike is not found.  
- **500 Internal Server Error**: If there is a database error.

---

### 3. Update Bike Status

**Endpoint**: `/bikes/:uuid/status`  
**Method**: `PUT`  
**Description**: Updates the status of a bike (e.g., stolen or normal).  

**Request Parameters**:  
- `uuid` (path parameter): The unique identifier of the bike.

**Request Body**:  
- `statut` (boolean): The new status of the bike.  
  - `true`: Stolen  
  - `false`: Normal  

**Response**:  
- **200 OK**: If the bike status is updated successfully.  
  ```json
  {
      "message": "Bike status updated successfully"
  }
  ```
- **400 Bad Request**: If the `statut` value is invalid.  
- **404 Not Found**: If the bike is not found.  
- **500 Internal Server Error**: If there is a database error.

---

### 4. Fetch Bike Location History

**Endpoint**: `/bikes/:uuid/locations`  
**Method**: `GET`  
**Description**: Retrieves the location history of a bike based on its UUID.  

**Request Parameters**:  
- `uuid` (path parameter): The unique identifier of the bike.

**Response**:  
- **200 OK**: Returns the location history in JSON format.  
  ```json
  [
      {
          "gps": "{\"lat\":48.8566,\"long\":2.3522}",
          "timestamp": "2023-10-01T12:00:00Z"
      },
      {
          "gps": "{\"lat\":48.8570,\"long\":2.3530}",
          "timestamp": "2023-10-01T12:05:00Z"
      }
  ]
  ```
- **404 Not Found**: If no locations are found for the bike.  
- **500 Internal Server Error**: If there is a database error.

---

## Error Handling

The API uses standard HTTP status codes to indicate the success or failure of a request. Common status codes include:

- **200 OK**: The request was successful.
- **400 Bad Request**: The request was invalid or missing required parameters.
- **404 Not Found**: The requested resource was not found.
- **500 Internal Server Error**: An error occurred on the server.

---

## Notes

- Ensure that the API is secured using authentication and authorization mechanisms before deploying to production.
- Use the `.env` file to configure sensitive information like database credentials and server ports.
- For any issues or questions, please contact the development team.

---
