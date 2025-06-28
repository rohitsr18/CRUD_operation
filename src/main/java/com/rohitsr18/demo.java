package com.rohitsr18;

import java.io.*;
import java.sql.*;
import java.util.*;

public class demo {
    public static void main(String[] args) {
        // Load database properties from file
        Properties props = new Properties();
        String url = "";
        String username = "";
        String password = "";

        try {
            // Try to load from classpath first
            InputStream input = demo.class.getClassLoader().getResourceAsStream("database.properties");

            // If not found in classpath, try to load from file system
            if (input == null) {
                input = new FileInputStream("src/main/resources/database.properties");
            }

            // Load properties
            props.load(input);
            url = props.getProperty("db.url");
            username = props.getProperty("db.username");
            password = props.getProperty("db.password");
            input.close();

        } catch (IOException e) {
            System.err.println("Error loading database properties: " + e.getMessage());
            System.err.println("Please create a database.properties file based on the template.");
            return;
        }

        try (Connection con = DriverManager.getConnection(url, username, password);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connection Successful");

            // User input for database operation
            System.out.println("Choose operation: 1-Insert, 2-Update, 3-Delete");
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1: // Insert
                    System.out.println("Enter car brand:");
                    String brand = scanner.nextLine();

                    System.out.println("Enter car model:");
                    String model = scanner.nextLine();

                    System.out.println("Enter car year:");
                    int year = scanner.nextInt();
                    scanner.nextLine(); // Consume newline

                    System.out.println("Enter car color:");
                    String color = scanner.nextLine();

                    String insertSQL = "INSERT INTO cars VALUES(?, ?, ?, ?)";
                    try (PreparedStatement pstmt = con.prepareStatement(insertSQL)) {
                        pstmt.setString(1, brand);
                        pstmt.setString(2, model);
                        pstmt.setInt(3, year);
                        pstmt.setString(4, color);
                        pstmt.execute();
                    }
                    System.out.println("Record inserted successfully!");
                    break;

                case 2: // Update
                    System.out.println("Enter brand and model of car to update:");
                    String updateBrand = scanner.nextLine();
                    String updateModel = scanner.nextLine();

                    System.out.println("What do you want to update? 1-Year, 2-Color");
                    int updateChoice = scanner.nextInt();
                    scanner.nextLine(); // Consume newline

                    if (updateChoice == 1) {
                        System.out.println("Enter new year:");
                        int newYear = scanner.nextInt();
                        String updateSQL = "UPDATE cars SET year = ? WHERE brand = ? AND model = ?";
                        int rowsAffectedYear;
                        try (PreparedStatement pstmtYear = con.prepareStatement(updateSQL)) {
                            pstmtYear.setInt(1, newYear);
                            pstmtYear.setString(2, updateBrand);
                            pstmtYear.setString(3, updateModel);
                            rowsAffectedYear = pstmtYear.executeUpdate();
                        }
                        System.out.println(rowsAffectedYear + " record(s) updated!");
                    } else if (updateChoice == 2) {
                        System.out.println("Enter new color:");
                        String newColor = scanner.nextLine();
                        String updateSQL = "UPDATE cars SET color = ? WHERE brand = ? AND model = ?";
                        int rowsAffectedColor;
                        try (PreparedStatement pstmtColor = con.prepareStatement(updateSQL)) {
                            pstmtColor.setString(1, newColor);
                            pstmtColor.setString(2, updateBrand);
                            pstmtColor.setString(3, updateModel);
                            rowsAffectedColor = pstmtColor.executeUpdate();
                        }
                        System.out.println(rowsAffectedColor + " record(s) updated!");
                    }
                    break;

                case 3: // Delete
                    System.out.println("Enter brand and model of car to delete:");
                    String deleteBrand = scanner.nextLine();
                    String deleteModel = scanner.nextLine();

                    String deleteSQL = "DELETE FROM cars WHERE brand = ? AND model = ?";
                    int rowsAffectedDelete;
                    try (PreparedStatement pstmtDelete = con.prepareStatement(deleteSQL)) {
                        pstmtDelete.setString(1, deleteBrand);
                        pstmtDelete.setString(2, deleteModel);
                        rowsAffectedDelete = pstmtDelete.executeUpdate();
                    }
                    System.out.println(rowsAffectedDelete + " record(s) deleted!");
                    break;

                default:
                    System.out.println("Invalid choice!");
            }

            // Display all records after operation
            String selectSQL = "SELECT * FROM cars";
            System.out.println("\nCurrent records in cars table:");
            System.out.println("Brand\tModel\tYear\tColor");

            try (PreparedStatement pstmtSelect = con.prepareStatement(selectSQL);
                 ResultSet rs = pstmtSelect.executeQuery()) {

                while (rs.next()) {
                    System.out.println(
                        rs.getString("brand") + "\t" + 
                        rs.getString("model") + "\t" +
                        rs.getInt("year") + "\t" + 
                        rs.getString("color")
                    );
                }
            }

            System.out.println("Connection Closed");
        } catch (SQLException e) {
            System.err.println("Database error occurred: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
        }
    }
}
