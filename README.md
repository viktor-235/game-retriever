# Game Retriever

Game Retriever is a CLI application that retrieves platforms and games from [igdb.com](https://igdb.com) and generates
large sql-script (DB migration) with data on thousands of games. Produced script can be used to add the data to your
game-related application's database.

Application was originally created as a helper for [RGG Assistant](https://github.com/viktor-235/rgg-assistant) to
populate its database with platforms and games, but it was later enhanced with the converter feature to make it more
versatile and useful to someone else.

## Features

- **Platform/game retrieving**: Retrieving data from [igdb.com](https://igdb.com) and storing it in a local database for
  easy access and manipulation.
- **Platform management**: Ability to select which platforms do you need.
- **Database changelog generation**: Generating of a database changelog file that contains all retrieved platforms and
  games as SQL insert statements.
- **[Customizable converters](#converters)**: Lets you customize the result SQL script to fit your application's
  database structure.

## Demo

[![asciicast](https://asciinema.org/a/JnASQpxcqJrj4IEBuoniKAfB4.svg)](https://asciinema.org/a/JnASQpxcqJrj4IEBuoniKAfB4)

## Usage

1. Run the application:

```
java -jar game-retriever-vX.Y.Z.jar
```

2. Use the available commands to interact with the application. `wizard` command is the easiest way to interact with the
   application.
3. Result files placed in the "result/" folder.

## Commands

```
Built-In Commands
       help: Display help about available commands
       stacktrace: Display the full stacktrace of the last error.
       clear: Clear the shell screen.
       quit, exit: Exit the shell.
       history: Display or save the history of previously run commands
       version: Show version info
       script: Read and execute commands from a file.

Game Retriever Commands
       wizard: Start interactive wizard. This is the easiest way to interact with the application
       auth: Log into Twitch developers to access igdb.com API. Credentials saves into 'auth.json'. Details: https://api-docs.igdb.com/#account-creation
       platforms update: Grab platforms data from IGDB into local
       platforms ls: Show platform list
       games update: Grab games from selected platforms into local DB
       output changelog: Store all platforms and games as SQL insert file
       output convert: Convert changelog SQL file into result file. Converters located in 'converters/'
```

## Converters
The converters are JSON files located in the [converters/](converters) folder.
- [Converter docs](converters/README.md)
- [JSON schema](schemas/converter.schema.json)
