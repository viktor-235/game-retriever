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
       platforms manage: Manage active platforms
       platforms ls: Show platform list
       games update: Grab games from activated platforms info into local DB. See 'platform manage'
       output changelog: Store all platforms and games as SQL insert file
       output convert: Convert changelog SQL file into result file. Converters located in 'converters/'
```

## Converters
The converters are JSON files located in the [converters/](https://github.com/viktor-235/game-retriever/tree/main/converters) folder. You can find the JSON schema and description in the file [schemas/converter.json](https://github.com/viktor-235/game-retriever/blob/main/schemas/converter.json).

### Converter example
```
{
  "$schema": "../schemas/converter.json",
  "inputFile": "data/changelog.h2.sql",
  "outputFile": "data/rgg-assistant.h2.sql",
  "handlers": [
    {
      "name": "platform",
      "pattern": "INSERT INTO PLATFORM \\(ID, ACTIVE, NAME, SHORT_NAME\\) VALUES \\((?<id>\\d+), TRUE, (?<name>'.*?'), (?<shortName>'.*?')\\);",
      "substitution": "MERGE INTO PLATFORM (SOURCE_TYPE, SOURCE_ID, NAME, SHORT_NAME) KEY (SOURCE_TYPE, SOURCE_ID) VALUES ('IGDB', ${id}, ${name}, ${shortName});"
    },
    {
      "name": "game",
      "pattern": "INSERT INTO GAME \\(ID, INFO_LINK, NAME\\) VALUES \\((?<id>\\d+), (?<infoLink>'.*?'), (?<name>'.*?')\\);",
      "substitution": "MERGE INTO GAME (SOURCE_TYPE, SOURCE_ID, INFO_LINK, NAME) KEY (SOURCE_TYPE, SOURCE_ID) VALUES ('IGDB', '${id}', ${infoLink}, ${name});"
    },
    {
      "name": "game_platform",
      "pattern": "INSERT INTO GAME_PLATFORM \\(ID, GAME_ID, PLATFORM_ID\\) VALUES \\(\\d+, (?<gameId>.*?), (?<platformId>.*?)\\);",
      "substitution": "MERGE INTO GAME_PLATFORM (SOURCE_TYPE, GAME_ID, PLATFORM_ID) KEY (GAME_ID, PLATFORM_ID) VALUES ('IGDB', (SELECT ID FROM game WHERE SOURCE_TYPE='IGDB' AND SOURCE_ID='${gameId}'), (SELECT ID FROM platform WHERE SOURCE_TYPE='IGDB' AND SOURCE_ID='${platformId}'));"
    }
  ]
}
```
