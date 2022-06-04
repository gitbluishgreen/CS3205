# CS3205: Assignment 2 README File

## Instructions to Compile
1. Run the provided Makefile by simply typing "make" in a terminal window.
## Execution Procedure
### Go Back N Protocol:
1. To run the sender, simply run **java SenderGoBack (command line arguments)**
2. To run the receiver first, simply run **java ReceiverGoBack (command line arguments)**
3. It is recommended to run the receiver first and then begin running the sender to avoid a timeout.
4.  Port number **12345** works just fine, and may be used.

### Selective repeat protocol:

1. To run the sender, simply run **java SenderSelective (command line arguments)**
2. To run the receiver first, simply run **java ReceiverSelective (command line arguments)**
3. It is recommended to run the receiver first and then begin running the sender to avoid a timeout.
4. Port number **12345** works just fine, and may be used.


### Assumptions Made:

1. The RTT is calculated in nanoseconds and is taken into account for every packet received as an acknowledgement,**irrespective of timeout.**
2. The Retransmission ratio is calculated as the ratio of total transmitted packets to packets acknowledged within time, this it is **atleast 1**.
