# Game Retriever

Game Retriever is CLI application that helps you set up other application's database.

It fetch and store game information from [igdb.com](https://igdb.com) and creates a large SQL script with data on thousands of games.

Game Retriever has a [configurable converter mechanism](#converters) that lets you customize the result SQL script to fit your application's database structure.

Application was originally created as a helper for [RGG Assistant](https://github.com/viktor-235/rgg-assistant) to populate its database with platforms and games, but it was later enhanced with the converter feature to make it more versatile and useful to someone else.

## Demo

[![asciicast](https://asciinema.org/a/JnASQpxcqJrj4IEBuoniKAfB4.svg)](https://asciinema.org/a/JnASQpxcqJrj4IEBuoniKAfB4)

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
The converters are JSON files located in the [converters/](converters/) folder.
- [Converter docs](converters/README.md)
- [JSON schema](schemas/converter.schema.json)
