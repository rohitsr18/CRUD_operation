# Spring Boot Database Demo

This is a simple Spring Boot application that demonstrates database operations (insert, update, delete) using JDBC.

## Database Configuration

To protect sensitive information, database credentials are stored in a properties file that is not committed to the repository.

### Setup Instructions

1. Create a copy of the template file:
   ```
   cp src/main/resources/database.properties.template src/main/resources/database.properties
   ```

2. Edit the `database.properties` file with your database credentials:
   ```
   db.url=jdbc:postgresql://localhost:5432/your_database_name
   db.username=your_username
   db.password=your_password
   ```

3. Make sure your PostgreSQL server is running and the database exists.

### Security Note

The `database.properties` file is included in `.gitignore` to prevent accidentally committing sensitive information to the repository. Never commit your actual database credentials to a public repository.

## Running the Application

1. Make sure you have set up the database configuration as described above.
2. Run the application using your IDE or with Maven:
   ```
   mvn spring-boot:run
   ```

## Features

The application allows you to:
- Insert new car records
- Update existing car records (year or color)
- Delete car records
- View all car records in the database

## Database Schema

The application expects a table named `cars` with the following structure:

```sql
CREATE TABLE cars (
    brand VARCHAR(255),
    model VARCHAR(255),
    year INT,
    color VARCHAR(255)
);
```