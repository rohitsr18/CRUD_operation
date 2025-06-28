package com.rohitsr18;

import java.sql.*;
import java.util.Scanner;

public class demo {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/sample";
        String username = "postgres";
        String password = "rohitsr1824";

        try (Connection con = DriverManager.getConnection(url, username, password);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connection Successful");
            Statement st = con.createStatement();

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

                    String insertSQL = "INSERT INTO cars VALUES('" + brand + "', '" + model + "', " + year + ", '" + color + "')";
                    st.execute(insertSQL);
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
                        String updateSQL = "UPDATE cars SET year = " + newYear + 
                                          " WHERE brand = '" + updateBrand + "' AND model = '" + updateModel + "'";
                        int rowsAffected = st.executeUpdate(updateSQL);
                        System.out.println(rowsAffected + " record(s) updated!");
                    } else if (updateChoice == 2) {
                        System.out.println("Enter new color:");
                        String newColor = scanner.nextLine();
                        String updateSQL = "UPDATE cars SET color = '" + newColor + 
                                          "' WHERE brand = '" + updateBrand + "' AND model = '" + updateModel + "'";
                        int rowsAffected = st.executeUpdate(updateSQL);
                        System.out.println(rowsAffected + " record(s) updated!");
                    }
                    break;

                case 3: // Delete
                    System.out.println("Enter brand and model of car to delete:");
                    String deleteBrand = scanner.nextLine();
                    String deleteModel = scanner.nextLine();

                    String deleteSQL = "DELETE FROM cars WHERE brand = '" + deleteBrand + 
                                      "' AND model = '" + deleteModel + "'";
                    int rowsAffected = st.executeUpdate(deleteSQL);
                    System.out.println(rowsAffected + " record(s) deleted!");
                    break;

                default:
                    System.out.println("Invalid choice!");
            }

            // Display all records after operation
            ResultSet rs = st.executeQuery("SELECT * FROM cars");
            System.out.println("\nCurrent records in cars table:");
            System.out.println("Brand\tModel\tYear\tColor");
            while (rs.next()) {
                System.out.println(rs.getString("brand") + "\t" + rs.getString("model") + "\t" +
                                  rs.getString("color") + "\t" + rs.getString("year"));
            }

            System.out.println("Connection Closed");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
