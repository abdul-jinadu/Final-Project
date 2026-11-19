# Restaurant Ordering System

A Java console application for browsing restaurant menus, placing orders, managing
inventory, and viewing saved orders and receipts. Data is stored in SQLite.

## What teammates need

- A Java Development Kit (JDK) 17 or newer, with `java` and `javac` on PATH.
- The complete repository, including `restaurant.db` and
  `lib/sqlite-jdbc-3.53.4.0.jar`.
- For VS Code, the Extension Pack for Java.

The SQLite JDBC driver is included, so there is no database server to install and
no database username or password to configure. The bundled driver is Xerial
SQLite JDBC; its license information is included inside the JAR.

## Run in VS Code

1. Open the repository folder in VS Code.
2. Let the Java extension finish loading the project and its library in `lib/`.
3. Open `Main.java` and click **Run**, or select **Restaurant Ordering System**
   in Run and Debug.

The checked-in VS Code configuration includes the JDBC driver and sets the working
directory to the repository folder, where `restaurant.db` lives.

## Run from PowerShell

From the repository folder:

```powershell
.\run.ps1
```

The script compiles the Java source files into `bin/` and starts the application
with the SQLite driver on its classpath. If PowerShell blocks local scripts, you
can compile and run directly instead:

```powershell
New-Item -ItemType Directory -Path bin -Force | Out-Null
javac -encoding UTF-8 -cp "lib/*" -d bin *.java
java -cp "bin;lib/*" Main
```

On macOS or Linux, run these commands from the repository folder:

```bash
mkdir -p bin
javac -encoding UTF-8 -cp "lib/*" -d bin *.java
java -cp "bin:lib/*" Main
```

## How the database works

`DatabaseManager.java` creates the database tables and seeds three restaurants
and their menu items when the application starts. If `restaurant.db` is missing,
it creates a new database automatically. Existing orders, menu prices, and stock
are preserved on subsequent starts; missing sample menu items are added.

The included `restaurant.db` is a snapshot with the project's existing data.
Each teammate runs their own local copy. Orders and inventory changes are saved
on that computer; Git does not synchronize a running database between teammates.

Before sharing a newer database snapshot, close the application and deliberately
include `restaurant.db` in the commit. SQLite journal, WAL, and SHM files are
temporary and remain ignored. Coordinate database changes because Git cannot
merge conflicting SQLite database files. To start with fresh sample data, close
the application, move your local database somewhere safe, and run again.

## Troubleshooting

- **No suitable driver found for jdbc:sqlite:** use the VS Code launch
  configuration, `run.ps1`, or the classpath commands above. Running only
  `java Main` does not include the driver.
- **Database appears empty or different:** run from the repository folder.
  The database path is relative to the working directory.
- **Database is locked:** close other running copies or database editors and retry.

