# topN
This is a map-reduce solution for the problem of searching the top N numbers on a big file.

## Complexity
The spatial complexity of this problem is O(N). The reason for this complexity is that the program only keeps
in memory the N numbers.
The temporary complexity is O(M) having M as the number of lines on the file. The reason for that is the fact that
the program has to full-scan the file (read all the lines once) in order to know the biggest N numbers.

## Reasoning behind using Hadoop
When I read the problem for the first time, the most obvious solution was write a for loop that read line by line
and kept only the N biggest numbers. I was trying to workout the minimum possible complexity which, being O(N), leaves
the for loop on a decent position (as it is an O(n) solution.

The only way that this could be improved was using paralelism.

The first approach I was thinking of is creating a queue (with rabbitMQ or something similar) that read chuncks of
the files, queued the message (the chunk itself) and the few process consumed this message getting the top N numbers
for each chunk combining the partial results after all the calculations.

The problem with this approach is the I/O of the system. Although the calculation is done in parallel, if the file is
big enough, the system could experience problems with the hard drive as, no matther what technology you use, it is 
always hundreds of times (at the very best) slower than the CPU. 

Another catch on the queue approach is the scalability. Once you reach the full capacity of any of the elements on the 
computer, it would be hard to scale it across machines to improve the throughput. I have used before a framework like
Terracotta to achieve that but there are better solutions to this problem.

### Why Hadoop?
Hadoop is a Map-Reduced (and more) oriented framework. Map-Reduce is the perfect (or close enough) technique to solve 
this type of problem. It allows the developer to split the problem into logical groups: Mapper, Reducer, Combiner...
that can be executed in parallel across diferent machines and then combine the results.

Hadoop also comes with a more than interesting file system: HDFS (http://hadoop.apache.org/docs/r1.2.1/hdfs_design.html). 
As I mentioned before, the bottleneck here is the hard drive so that, a distributed file system would solve the problem
if the file is placed in it.

In order to run the software, a hadoop distribution is needed (http://hadoop.apache.org/releases.html).

Once the distribution is downloaded and unzipped execute the following commands on the root of the project:
```
gradle assemble;
hadoop-2.7.1/bin/hadoop jar build/libs/topN-1.0-SNAPSHOT.jar com.davidgonzalez.WordCount data.txt results/
```
This will kick off the job on a hosted mode (no paralelism is carried on as configuring the cluster can be
quite hard) leaving the results of the N (in this case N is hardcoded to 2) biggest numbers on a file
in the path `results/part-r-00000` with an order list of the biggest N numbers 
(the order is given by the `descendingMap` method of the `TreeMap`).

There is one part missing on the solution that I didn't write which is the combiner for when the job is running
in multiple nodes (each node will produce a `part-r-XXXXX` file with the partial result for each chunk so the
combiner will need to combine those partial results in a global result).

I haven't worked with Hadoop before so there might be a better approach but as a proof of concept I think it
would be hard to find something more performant as it respects the minimum complexity and makes easy the paralelisation
of the solution.

Of course, the source code could be laid down better:
- Each class on its own file
- `Job` injected via Spring or other DI framework

But my target was make it easy to read and keep it simple. 

I didn't write any test but as you can see, the small classes with a single responsibility (modularity) makes the 
tests easy to be written.
