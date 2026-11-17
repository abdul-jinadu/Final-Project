# Final-Project
Restaurant Ordering System that displays a menu, place orders, and track completed and pending orders.

dj

## Project Overview
The Resteraunt Ordering system is a Java-based application that will simulate the ordering process at a restaurant. 
This progam will allow users to browse a menu, select food and drinks, place orders and view the total cost of their order.


Here is a list of classes to develop:

## SQLite setup and running

Install a JDK and VS Code's Extension Pack for Java. Place
`sqlite-jdbc-3.53.4.0.jar` in the project's `lib` folder. Each teammate needs a
local copy of the JAR, available from the class PetAPI example or
[Xerial's release](https://github.com/xerial/sqlite-jdbc/releases/tag/3.53.4.0).
The JAR, compiled classes, and local database are ignored by Git.

Run the **Restaurant Ordering System** configuration in VS Code, or use PowerShell:

```powershell
New-Item -ItemType Directory -Force bin | Out-Null
javac -cp "lib/*" -d bin *.java
java -cp "bin;lib/*" Main
```

Choose `3` to place an order, enter a name, and select items and quantities.
Enter `0` to save or `-1` to cancel. Choose `4` from the main menu to display
saved orders. Exit with `2`, restart, and choose `4` to check that orders persist.
Two Burgers and one Soda total $20.48. Empty orders and invalid quantities are
rejected. Each teammate's `restaurant.db` stores their own records locally.

The separate Payment class has a 6% tax calculation and accepts a payment method,
but it is not yet connected to checkout. Saved totals currently exclude tax and tips.
