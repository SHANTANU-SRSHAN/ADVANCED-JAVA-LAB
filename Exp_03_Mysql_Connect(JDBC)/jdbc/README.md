# MySQL Connection using JDBC

## Aim
To connect a Java application to MySQL using JDBC and insert a student record.

## Technologies
Java, JDBC, MySQL, MySQL Connector/J.

## Requirements
- MySQL Server running
- A `college` database and the required `student` table
- Connector/J JAR in the project's `lib` folder

## How to Run
From this folder, compile:

```bash
javac -cp "lib/mysql-connector-j-26.7.0.jar" -d bin src/App.java
```

On Windows, run:

```powershell
java -cp "bin;lib/mysql-connector-j-26.7.0.jar" App
```

Ensure the database credentials in the source code match your local MySQL configuration. The JAR filename may differ by version.
