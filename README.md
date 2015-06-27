Akka-Java8-Cameo-Example
========================

Shows the cameo pattern in Java8.

Profile service will create an "cameo" Profile builder actor on each request.
That Profile builder gets all of the responses from the kv store requests and assembles the profile.
Compared to using futures, there are some benefits and drawbacks:

Negatives vs using ask:
- More code, harder to read
- More complexity

Positives:
- More efficient - using 3 asks would create 3 futures and 3 temporary actors. Using Cameo Pattern only creates one temporary actor.
- Can be easier to deal with immutability concerns in java - you can keep state in the actor safely, and then build an immutable message when you're ready
- Once you understand the pattern, I think it can be maintained quite easily - it's not complex - there is just more LOC than using futures.
