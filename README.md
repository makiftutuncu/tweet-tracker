```
*************************************************************
*                                                           *
*       ________  __    __  ________    ____       ______   *
*      /_/_/_/_/ /_/   /_/ /_/_/_/_/  _/_/_/_   __/_/_/_/   *
*     /_/_____  /_/___/_/    /_/    /_/___/_/  /_/          *
*    /_/_/_/_/   /_/_/_/    /_/    /_/_/_/_/  /_/           *
*   ______/_/       /_/    /_/    /_/   /_/  /_/____        *
*  /_/_/_/_/       /_/    /_/    /_/   /_/    /_/_/_/ . io  *
*                                                           *
*************************************************************
```

# Sytac Scala Exercise #

This development test is used as part of Sytac selection for Scala developers. You are requested to develop a simple application that covers all the requirements listed below. To have an indication of the criteria that will be used to judge your submission, all the following are considered as metrics of good development:

+ Correctness of the implementation
+ Decent test coverage
+ Code cleanliness
+ Efficiency of the solution
+ Careful choice of tools and data formats
+ Use of production-ready approaches

While no specific time limit is mandated to complete the exercise, you will be asked to provide your code within a given deadline from Sytac HR. You are free to choose any library as long as it can run on the JVM.

## Task ##

We would like you to write code that will cover the functionality explained below and provide us with the source, instructions to build and run the application, as well as a sample output of an execution:

+ Connect to [Twitter Streaming API (https://developer.twitter.com/en/docs/tutorials/consuming-streaming-data)
+ Filter messages that track on "crypto"
+ Retrieve the incoming messages for 30 seconds or up to 100 messages, whichever comes first
+ Your application should return the messages grouped by user (users sorted chronologically, ascending)
+ The messages per user should also be sorted chronologically, ascending
+ For each message, we will need the following:
    * The message ID
    * The creation date of the message as epoch value
    * The text of the message
    * The author of the message
+ For each author, we will need the following:
    * The user ID
    * The creation date of the user as epoch value
    * The name of the user
    * The screen name of the user
+ All the above information is provided in either Standard output, or a log file
+ You are free to choose the output format, provided that it makes it easy to parse and process by a machine

### __Bonus points for:__ ###

+ Keep track of messages per second statistics across multiple runs of the application
+ The application can run as a Docker container


## Delivery ##

You are assigned to you own private repository. Please use your own branch and do not commit on master.
When the assignment is finished, please create a pull request on the master of this repository, and your contact person will be notified automatically. 
