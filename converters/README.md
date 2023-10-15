# game-retriever changelog converter

## Handler types

Game Retriever supports different types of handlers that can be used to manipulate the retrieved data during the
conversion process. Each handler type performs a specific operation based on the defined configuration.

### Regex generator handler

The regex generator handler allows you to generate lines of text using regex substitution. It uses a regular expression
pattern to match specific parts of the data in an input sql-file and replaces them with the specified substitution
expression.

Example regex generator handler configuration:

```json
{
  "type": "regex-generator",
  "name": "game sql",
  "pattern": "INSERT INTO GAME \\(ID, INFO_LINK, NAME\\) VALUES \\((?<id>\\d+), (?<infoLink>'.*?'), (?<name>'.*?')\\);",
  "substitution": "MERGE INTO GAME (SOURCE_TYPE, SOURCE_ID, INFO_LINK, NAME) KEY (SOURCE_TYPE, SOURCE_ID) VALUES ('IGDB', '${id}', ${infoLink}, ${name});"
}
```

### Template handler

The template handler allows you to generate lines of text using a specified [Mustache](https://mustache.github.io/)
template.

The template engine context contains the following values:

| Template expression | Result example  | Description                       |
|---------------------|-----------------|-----------------------------------|
| `{{username}}`      | `User`          | System user name                  |
| `{{timestamp}}`     | `1697382769777` | Unix Timestamp of processing time |

Example template handler configuration:

```json
{
  "type": "template",
  "name": "changeset header",
  "template": "-- changeset {{username}}:{{timestamp}}-1\n"
}
```

## Properties

- **`inputFile`** *(string)*: Input SQL file. Converter uses this data as a source.
- **`outputFile`** *(string)*: Result output file. Converter writes data into this file.
- **`handlers`** *(array)*: Every handler applies to every suitable input file line and generates output file line.
    - **Items** *(object)*
        - **`type`** *(string, required)*: Handler type. Determine which handler should run (`regex-generator`
          or `template`). Must be one of: `["regex-generator", "template"]`.
        - **`name`** *(string, required)*: Handler name. Uses to show progress while converting and to improve
          readability during converter config editing.
        - **`filter`** *(string)*: Optional regex to identify which input file lines should be handled. Uses to skip
          some lines which fit the `pattern` regex.
        - **`pattern`** *(string)*: Regex pattern to extract fields from input file line. Required for `regex-generator`
          handler type. Uses filtered input file lines and extracts regex groups for `substitution`. Useful to define
          named regex groups like `(?<id>\d+)` and `(?<name>'.*?')`.

          Examples:
          ```sql
          "INSERT INTO GAME \\(ID, INFO_LINK, NAME\\) VALUES \\((?<id>\\d+), (?<infoLink>'.*?'), (?<name>'.*?')\\);"
          ```

        - **`substitution`** *(string)*: Regex substitution expression to generate result file line. Required
          for `regex-generator` handler type. `pattern` regex groups are available here. For example, ${id}, ${name}.

          Examples:
          ```sql
          "MERGE INTO GAME (SOURCE_TYPE, SOURCE_ID, INFO_LINK, NAME) KEY (SOURCE_TYPE, SOURCE_ID) VALUES ('IGDB', '${id}', ${infoLink}, ${name});"
          ```

        - **`template`** *(string)*: Mustache template. Required for `template` handler type.

          Examples:
          ```
          "-- changeset {{username}}:{{timestamp}}-1\n"
          ```

## Examples

  ```json
  {
  "$schema": "../schemas/converter.schema.json",
  "inputFile": "data/changelog.h2.sql",
  "outputFile": "data/rgg-assistant.h2.sql",
  "handlers": [
    {
      "type": "template",
      "name": "common changeset header",
      "template": "-- liquibase formatted sql\n\n-- changeset {{username}}:{{timestamp}}-1\n"
    },
    {
      "type": "regex-generator",
      "name": "platform sql",
      "pattern": "INSERT INTO PLATFORM \\(ID, ACTIVE, NAME, SHORT_NAME\\) VALUES \\((?<id>\\d+), TRUE, (?<name>'.*?'), (?<shortName>'.*?')\\);",
      "substitution": "MERGE INTO PLATFORM (SOURCE_TYPE, SOURCE_ID, NAME, SHORT_NAME) KEY (SOURCE_TYPE, SOURCE_ID) VALUES ('IGDB', ${id}, ${name}, ${shortName});"
    },
    {
      "type": "template",
      "name": "game changeset header",
      "template": "\n-- changeset {{username}}:{{timestamp}}-2\n"
    },
    {
      "type": "regex-generator",
      "name": "game sql",
      "pattern": "INSERT INTO GAME \\(ID, INFO_LINK, NAME\\) VALUES \\((?<id>\\d+), (?<infoLink>'.*?'), (?<name>'.*?')\\);",
      "substitution": "MERGE INTO GAME (SOURCE_TYPE, SOURCE_ID, INFO_LINK, NAME) KEY (SOURCE_TYPE, SOURCE_ID) VALUES ('IGDB', '${id}', ${infoLink}, ${name});"
    },
    {
      "type": "template",
      "name": "game_platform changeset header",
      "template": "\n-- changeset {{username}}:{{timestamp}}-3\n"
    },
    {
      "type": "regex-generator",
      "name": "game_platform sql",
      "pattern": "INSERT INTO GAME_PLATFORM \\(ID, GAME_ID, PLATFORM_ID\\) VALUES \\(\\d+, (?<gameId>.*?), (?<platformId>.*?)\\);",
      "substitution": "MERGE INTO GAME_PLATFORM (SOURCE_TYPE, GAME_ID, PLATFORM_ID) KEY (GAME_ID, PLATFORM_ID) VALUES ('IGDB', (SELECT ID FROM game WHERE SOURCE_TYPE='IGDB' AND SOURCE_ID='${gameId}'), (SELECT ID FROM platform WHERE SOURCE_TYPE='IGDB' AND SOURCE_ID='${platformId}'));"
    }
  ]
}
  ```

_Generated with [jsonschema2md](https://github.com/sbrunner/jsonschema2md)_
