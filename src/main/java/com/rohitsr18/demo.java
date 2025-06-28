package com.rohitsr18;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.nio.file.*;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class demo {
    // Car class to hold car data
    static class Car {
        private String brand;
        private String model;
        private int year;
        private String color;

        public Car(String brand, String model, int year, String color) {
            this.brand = brand;
            this.model = model;
            this.year = year;
            this.color = color;
        }

        public String getBrand() {
            return brand;
        }

        public String getModel() {
            return model;
        }

        public int getYear() {
            return year;
        }

        public String getColor() {
            return color;
        }

        @Override
        public String toString() {
            return "Car{" +
                    "brand='" + brand + '\'' +
                    ", model='" + model + '\'' +
                    ", year=" + year +
                    ", color='" + color + '\'' +
                    '}';
        }
    }

    // Method to read cars from file
    private static List<Car> readCarsFromFile(String filePath, String fileType) throws IOException {
        List<Car> cars = new ArrayList<>();
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new IOException("File not found: " + filePath);
        }

        // Handle Excel files
        if (fileType.equalsIgnoreCase("XLS") || fileType.equalsIgnoreCase("XLSX")) {
            return readCarsFromExcel(filePath, fileType);
        }

        // Handle CSV and TXT files
        List<String> lines = Files.readAllLines(path);

        // Skip header line if CSV
        if (fileType.equalsIgnoreCase("CSV") && !lines.isEmpty()) {
            lines = lines.subList(1, lines.size());
        }

        for (String line : lines) {
            try {
                Car car = parseLine(line, fileType);
                if (car != null) {
                    cars.add(car);
                }
            } catch (IllegalArgumentException e) {
                System.err.println("Error parsing line: " + line + " - " + e.getMessage());
            }
        }

        return cars;
    }

    // Method to read cars from Excel file
    private static List<Car> readCarsFromExcel(String filePath, String fileType) throws IOException {
        List<Car> cars = new ArrayList<>();

        try (InputStream inputStream = new FileInputStream(filePath);
             Workbook workbook = fileType.equalsIgnoreCase("XLSX") ? 
                                new XSSFWorkbook(inputStream) : new HSSFWorkbook(inputStream)) {

            // Get the first sheet
            Sheet sheet = workbook.getSheetAt(0);

            // Determine if the first row is a header (assume it is)
            boolean hasHeader = true;
            int startRow = hasHeader ? 1 : 0;

            // Iterate through rows
            for (int rowIndex = startRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;

                try {
                    // Extract car data from row
                    String brand = getCellValueAsString(row.getCell(0));
                    String model = getCellValueAsString(row.getCell(1));
                    int year = (int) Double.parseDouble(getCellValueAsString(row.getCell(2)));
                    String color = getCellValueAsString(row.getCell(3));

                    if (!brand.isEmpty() && !model.isEmpty()) {
                        cars.add(new Car(brand, model, year, color));
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing row " + rowIndex + ": " + e.getMessage());
                }
            }
        }

        return cars;
    }

    // Helper method to get cell value as string
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    // Helper method to truncate strings that are too long for display
    private static String truncateString(String str, int maxLength) {
        if (str == null) {
            return "";
        }

        if (str.length() <= maxLength) {
            return str;
        }

        // If string is longer than maxLength, truncate and add ellipsis
        // Make sure we have at least 3 characters of space for the ellipsis
        if (maxLength <= 3) {
            return str.substring(0, maxLength);
        } else {
            return str.substring(0, maxLength - 3) + "...";
        }
    }

    // Method to parse a line from file into a Car object
    private static Car parseLine(String line, String fileType) {
        if (line.trim().isEmpty()) {
            return null;
        }

        String[] parts;
        if (fileType.equalsIgnoreCase("CSV")) {
            parts = line.split(",");
        } else {
            // For TXT files, assume tab or space separated
            parts = line.split("\\s+");
        }

        if (parts.length < 4) {
            throw new IllegalArgumentException("Line does not contain enough data: " + line);
        }

        String brand = parts[0].trim();
        String model = parts[1].trim();
        int year;
        try {
            year = Integer.parseInt(parts[2].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid year format: " + parts[2]);
        }
        String color = parts[3].trim();

        return new Car(brand, model, year, color);
    }

    // Method to check if a car with the same brand, model, and year already exists
    private static boolean carExists(String brand, String model, int year, Connection con) throws SQLException {
        String checkSQL = "SELECT COUNT(*) FROM cars WHERE UPPER(brand) = UPPER(?) AND UPPER(model) = UPPER(?) AND year = ?";
        try (PreparedStatement pstmt = con.prepareStatement(checkSQL)) {
            pstmt.setString(1, brand);
            pstmt.setString(2, model);
            pstmt.setInt(3, year);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    // Method to insert cars into database
    private static int insertCarsIntoDB(List<Car> cars, Connection con) throws SQLException {
        if (cars.isEmpty()) {
            return 0;
        }

        int count = 0;
        int skipped = 0;
        String insertSQL = "INSERT INTO cars VALUES(?, ?, ?, ?)";

        try (PreparedStatement pstmt = con.prepareStatement(insertSQL)) {
            for (Car car : cars) {
                // Check if car already exists
                if (carExists(car.getBrand(), car.getModel(), car.getYear(), con)) {
                    System.out.println("Skipping duplicate car: " + car.getBrand() + " " + car.getModel() + " (" + car.getYear() + ")");
                    skipped++;
                    continue;
                }

                pstmt.setString(1, car.getBrand());
                pstmt.setString(2, car.getModel());
                pstmt.setInt(3, car.getYear());
                pstmt.setString(4, car.getColor());
                pstmt.addBatch();
                count++;
            }
            try {
                pstmt.executeBatch();
            } catch (BatchUpdateException bue) {
                System.err.println("Batch insert error: " + bue.getMessage());
                SQLException next = bue.getNextException();
                while (next != null) {
                    System.err.println("Cause: " + next.getMessage());
                    next = next.getNextException();
                }
                System.err.println("Some records may not have been inserted due to duplicates or constraint violations.");
            }
        }

        if (skipped > 0) {
            System.out.println(skipped + " duplicate record(s) skipped.");
        }

        return count;
    }

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
            System.out.println("Choose operation: 1-Insert, 2-Update, 3-Delete, 4-Import from File");
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

                    // Check if car already exists
                    if (carExists(brand, model, year, con)) {
                        System.out.println("Error: A car with brand '" + brand + "', model '" + model + "', and year '" + year + "' already exists in the database.");
                        System.out.println("Duplicate record not inserted.");
                        break;
                    }

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

                    System.out.println("What do you want to update? 1-Year, 2-Color, 3-Both");
                    int updateChoice = scanner.nextInt();
                    scanner.nextLine(); // Consume newline

                    if (updateChoice == 1) {
                        System.out.println("Enter new year:");
                        int newYear = scanner.nextInt();
                        String updateSQL = "UPDATE cars SET year = ? WHERE UPPER(brand) = UPPER(?) AND UPPER(model) = UPPER(?)";
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
                        String updateSQL = "UPDATE cars SET color = ? WHERE UPPER(brand) = UPPER(?) AND UPPER(model) = UPPER(?)";
                        int rowsAffectedColor;
                        try (PreparedStatement pstmtColor = con.prepareStatement(updateSQL)) {
                            pstmtColor.setString(1, newColor);
                            pstmtColor.setString(2, updateBrand);
                            pstmtColor.setString(3, updateModel);
                            rowsAffectedColor = pstmtColor.executeUpdate();
                        }
                        System.out.println(rowsAffectedColor + " record(s) updated!");
                    } else if (updateChoice == 3) {
                        System.out.println("Enter new year:");
                        int newYear = scanner.nextInt();
                        scanner.nextLine(); // Consume newline

                        System.out.println("Enter new color:");
                        String newColor = scanner.nextLine();

                        String updateSQL = "UPDATE cars SET year = ?, color = ? WHERE UPPER(brand) = UPPER(?) AND UPPER(model) = UPPER(?)";
                        int rowsAffected;
                        try (PreparedStatement pstmt = con.prepareStatement(updateSQL)) {
                            pstmt.setInt(1, newYear);
                            pstmt.setString(2, newColor);
                            pstmt.setString(3, updateBrand);
                            pstmt.setString(4, updateModel);
                            rowsAffected = pstmt.executeUpdate();
                        }
                        System.out.println(rowsAffected + " record(s) updated!");
                    }
                    break;

                case 3: // Delete
                    System.out.println("Enter brand and model of car to delete:");
                    String deleteBrand = scanner.nextLine();
                    String deleteModel = scanner.nextLine();

                    String deleteSQL = "DELETE FROM cars WHERE UPPER(brand) = UPPER(?) AND UPPER(model) = UPPER(?)";
                    int rowsAffectedDelete;
                    try (PreparedStatement pstmtDelete = con.prepareStatement(deleteSQL)) {
                        pstmtDelete.setString(1, deleteBrand);
                        pstmtDelete.setString(2, deleteModel);
                        rowsAffectedDelete = pstmtDelete.executeUpdate();
                    }
                    System.out.println(rowsAffectedDelete + " record(s) deleted!");
                    break;

                case 4: // Import from File
                    // Create a JFrame to serve as the parent for the file chooser
                    JFrame frame = new JFrame("File Import");
                    frame.setSize(400, 300);
                    frame.setLocationRelativeTo(null);
                    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    frame.setVisible(true);

                    // Give the JFrame time to become visible
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("Select File to Import");

                    // Add file filters
                    FileNameExtensionFilter csvFilter = new FileNameExtensionFilter("CSV Files (*.csv)", "csv");
                    FileNameExtensionFilter txtFilter = new FileNameExtensionFilter("Text Files (*.txt)", "txt");
                    FileNameExtensionFilter xlsFilter = new FileNameExtensionFilter("Excel Files (*.xls)", "xls");
                    FileNameExtensionFilter xlsxFilter = new FileNameExtensionFilter("Excel Files (*.xlsx)", "xlsx");
                    fileChooser.addChoosableFileFilter(csvFilter);
                    fileChooser.addChoosableFileFilter(txtFilter);
                    fileChooser.addChoosableFileFilter(xlsFilter);
                    fileChooser.addChoosableFileFilter(xlsxFilter);
                    fileChooser.setAcceptAllFileFilterUsed(true);

                    String filePath = "";
                    String fileType = "";

                    int result = fileChooser.showOpenDialog(frame);
                    // Dispose of the frame after dialog is closed
                    frame.dispose();

                    if (result == JFileChooser.APPROVE_OPTION) {
                        File selectedFile = fileChooser.getSelectedFile();
                        filePath = selectedFile.getAbsolutePath();

                        // Auto-detect file type based on extension
                        String extension = filePath.substring(filePath.lastIndexOf(".") + 1).toUpperCase();
                        if (extension.equals("CSV")) {
                            fileType = "CSV";
                        } else if (extension.equals("TXT")) {
                            fileType = "TXT";
                        } else if (extension.equals("XLS")) {
                            fileType = "XLS";
                        } else if (extension.equals("XLSX")) {
                            fileType = "XLSX";
                        } else {
                            // Default to TXT if unknown extension
                            fileType = "TXT";
                        }

                        System.out.println("Selected file: " + filePath);
                        System.out.println("Detected file type: " + fileType);
                    } else {
                        System.out.println("File selection cancelled.");
                        break;
                    }

                    try {
                        List<Car> cars = readCarsFromFile(filePath, fileType);
                        int insertedCount = insertCarsIntoDB(cars, con);
                        System.out.println(insertedCount + " record(s) inserted successfully!");
                    } catch (IOException e) {
                        System.err.println("Error reading file: " + e.getMessage());
                    } catch (IllegalArgumentException e) {
                        System.err.println("Error in file data: " + e.getMessage());
                    }
                    break;

                default:
                    System.out.println("Invalid choice!");
            }

            // Display all records after operation
            String selectSQL = "SELECT * FROM cars";
            System.out.println("\nCurrent records in cars table:");

            // Define column widths
            final int BRAND_WIDTH = 15;
            final int MODEL_WIDTH = 15;
            final int YEAR_WIDTH = 6;
            final int COLOR_WIDTH = 15;

            // Define column formats with fixed width for better alignment
            String headerFormat = "%-" + BRAND_WIDTH + "s %-" + MODEL_WIDTH + "s %-" + YEAR_WIDTH + "s %-" + COLOR_WIDTH + "s\n";
            String dataFormat = "%-" + BRAND_WIDTH + "s %-" + MODEL_WIDTH + "s %-" + YEAR_WIDTH + "d %-" + COLOR_WIDTH + "s\n";

            // Print header with proper formatting
            System.out.printf(headerFormat, "Brand", "Model", "Year", "Color");
            System.out.println("-".repeat(BRAND_WIDTH + MODEL_WIDTH + YEAR_WIDTH + COLOR_WIDTH + 3)); // 3 spaces between columns

            try (PreparedStatement pstmtSelect = con.prepareStatement(selectSQL);
                 ResultSet rs = pstmtSelect.executeQuery()) {

                while (rs.next()) {
                    // Truncate strings if they're too long for the column
                    String brand = truncateString(rs.getString("brand"), BRAND_WIDTH);
                    String model = truncateString(rs.getString("model"), MODEL_WIDTH);
                    int year = rs.getInt("year");
                    String color = truncateString(rs.getString("color"), COLOR_WIDTH);

                    System.out.printf(dataFormat, brand, model, year, color);
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
