# Use CloudAMQP in Java from Heroku

This project illustrates how to use the [Java client AMQP library]() to access [CloudAMQP](http://www.cloudamqp.com) from [Heroku](http://www.heroku.com). 

It consists of one [worker](https://github.com/cloudamqp/java-amqp-example/blob/master/src/main/java/WorkerProcess.java) which listens to a queue and prints the messages to the console (and thus to the Heroku log), and a ["oneoff" process](https://github.com/cloudamqp/java-amqp-example/blob/master/src/main/java/WorkerProcess.java) which enqeues messages to that queue. 

For more information on AMQP and how to use it from Java, see [RabbitMQ's tutorial](http://www.rabbitmq.com/getstarted.html). 

## Usage

Click this button to deploy the sample code to a new app on Heroku for free:

[![Deploy](https://www.herokucdn.com/deploy/button.png)](https://heroku.com/deploy)

Make sure you have the [Heroku Toolbelt](https://toolbelt.heroku.com/) installed. Scale the worker process, start the one-off process that enqueues messages and then inspect the log:

    $ heroku ps:scale worker=1 -a your-app-name

    $ heroku run "sh target/bin/oneoff" -a your-app-name
    Running sh target/bin/oneoff attached to terminal... up, run.1
     [x] Sent 'Hello CloudAMQP!'

    $ heroku logs -a your-app-name
    2012-03-28T16:59:59+00:00 app[worker.1]:  [*] Waiting for messages
    2012-03-28T17:02:34+00:00 heroku[api]: Scale to worker=1 by carl.hoerberg@gmail.com
    2012-03-28T17:03:05+00:00 heroku[run.1]: State changed from created to starting
    2012-03-28T17:03:06+00:00 app[run.1]: Awaiting client
    2012-03-28T17:03:06+00:00 app[run.1]: Starting process with command `sh target/bin/oneoff`
    2012-03-28T17:03:06+00:00 heroku[run.1]: State changed from starting to up
    2012-03-28T17:03:07+00:00 app[worker.1]:  [x] Received 'Hello CloudAMQP!'
    2012-03-28T17:03:08+00:00 heroku[run.1]: Process exited with status 0
    2012-03-28T17:03:08+00:00 heroku[run.1]: State changed from up to complete

