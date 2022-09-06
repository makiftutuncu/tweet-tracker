# Tweet Tracker

## Table of Contents

1. [Introduction](#introduction)
2. [Configuration](#configuration)
3. [Development and Testing](#development-and-testing)
4. [Docker](#docker)
5. [Notes](#notes)
6. [Contributing](#contributing)
7. [License](#license)

## Introduction

Tweet Tracker is a standalone application for streaming tweets containing a search term on [Twitter](https://twitter.com) in real time for a predefined duration or number of tweets. It is built using [Scala 3.2](https://www.scala-lang.org), [ZIO](https://zio.dev) and [sttp](https://sttp.softwaremill.com/en/latest). It logs the tweets it's streaming as Json objects to standard output.

## Configuration

Application can be configured via [application.conf](src/main/resources/application.conf). You can also override config values with following environment variables.

| Variable Name | Data Type | Description                                                                                   | Required                                                |
|---------------|-----------|-----------------------------------------------------------------------------------------------|---------------------------------------------------------|
| TOKEN         | String    | [Twitter API v2](https://developer.twitter.com/en/docs/authentication/oauth-2-0) access token | Yes, otherwise defaults to `change-me` which won't work |
| SEARCH_TERM   | String    | The term to search in tweets                                                                  | No, defaults to `crypto`                                |
| FOR_SECONDS   | Int       | How long to stream tweets, in seconds                                                         | No, defaults to `30`                                    |
| UP_TO_TWEETS  | Int       | Maximum number of tweets to stream                                                            | No, defaults to `100`                                   |

For log configuration, see [logback.xml](src/main/resources/logback.xml).

## Development and Testing

Application is built with SBT. So, standard SBT tasks like `clean`, `compile` and `run` can be used. To run the application locally:

```bash
sbt run
```

Example to stream up to 50 tweets containing `bitcoin` for 5 seconds:

```bash
TOKEN=your-own-token SEARCH_TERM=bitcoin FOR_SECONDS=5 UP_TO_TWEETS=50 sbt run
```

To run automated tests, you can use `test` and `testOnly` tasks of SBT. To run all tests:

```bash
sbt test
```

To run specific test(s):

```bash
sbt 'testOnly fullyQualifiedTestClassName1 fullyQualifiedTestClassName2 ...'
```

## Docker

You may also run the application in a Docker container. Environment variables mentioned in [Configuration](#configuration) are passed to the container while building image so you don't have to pass them with `-e` while creating the container with `docker run`.

To build the Docker image:

```bash

First build an image locally with

```bash
sbt 'Docker / publishLocal'
```

Then start a container from generated Docker image with

```bash
docker run --rm tweet-tracker
```

## Notes

* The streaming only logs to standard out. They could be persisted for future use.
* There is no statistics across multiple runs of the application, app runs for a single time, every time.

## Contributing

All contributions are welcome. Please feel free to send a pull request. Thank you.

## License

Tweet Tracker is licensed with [MIT License](LICENSE.md).
