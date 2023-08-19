# game-retriever changelog converter

## Properties

- **`inputFile`** *(string)*: Input SQL file. Converter uses this data as a source.
- **`outputFile`** *(string)*: Result output file. Converter writes data into this file.
- **`handlers`** *(array)*: Every handler applies to every suitable input file line and generates output file line.
  - **Items** *(object)*
    - **`name`** *(string, required)*: Handler name. Uses to show progress while converting and to improve readability during converter config editing.
    - **`filter`** *(string)*: Optional regex to identify which input file lines should be handled. Uses to skip some lines which fit the `pattern` regex.
    - **`pattern`** *(string, required)*: Regex pattern to extract fields from input file line. Uses filtered input file lines and extracts regex groups for `substitution`. Useful to define named regex groups like `(?<id>\d+)` and `(?<name>'.*?')`.

      Examples:
      ```json
      "INSERT INTO GAME \\(ID, INFO_LINK, NAME\\) VALUES \\((?<id>\\d+), (?<infoLink>'.*?'), (?<name>'.*?')\\);"
      ```

    - **`substitution`** *(string, required)*: Regex substitution expression to generate result file line. `pattern` regex groups are available here. For example, ${id}, ${name}.

      Examples:
      ```json
      "MERGE INTO GAME (SOURCE_TYPE, SOURCE_ID, INFO_LINK, NAME) KEY (SOURCE_TYPE, SOURCE_ID) VALUES ('IGDB', '${id}', ${infoLink}, ${name});"
      ```

## Examples

  ```json
  {
      "$schema": "../schemas/converter.schema.json",
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

_Generated with [jsonschema2md](https://github.com/sbrunner/jsonschema2md)_
