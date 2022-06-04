import java.net.*; 
import java.io.*;
import java.util.Hashtable;
class ReceiverSelective
{
    public static void main(String[] args)
    {
        String cur_arg;
        ReceiverSelectiveHelper obj = new ReceiverSelectiveHelper();
        int p = 0;
        int l = args.length;
        while(p < l)
        {
            cur_arg = args[p];
            if(cur_arg.equals("-d") || cur_arg.equals("-D"))
            {
                obj.debug = true;
                p++;
            }
            else if(cur_arg.equals("-p") || cur_arg.equals("-P"))
            {   
                obj.port_number = Integer.parseInt(args[p+1]);
                p += 2;
            }
            else if(cur_arg.equals("-n"))
            {
                obj.sequence_size = Integer.parseInt(args[p+1]);
                p += 2;
            }
            else if(cur_arg.equals("-N"))
            {
                obj.max_packets = Integer.parseInt(args[p+1]);
                p += 2;
            }
            else if(cur_arg.equals("-e") || cur_arg.equals("-E"))
            {
                obj.random_drop_prob = Double.parseDouble(args[p+1]);
                p += 2;
            }
            else
            {
                System.out.println("Invalid Command line arguments. Exiting now.");
                System.exit(0);
            }
        }
        obj.connect();
    }
} 
class ReceiverSelectiveHelper
{ 
    // initialize socket and input output streams 
    private DatagramSocket socket = null; 
    public double random_drop_prob = 0.0;
    public boolean debug = false;
    public int port_number = 12345;
    public int max_packets = 400;
    public int sequence_size = 8;
    PrintWriter pw = new PrintWriter(System.out,true);
    long start_time = 0;
    int rec_packets = 0;
    int count = 0;
    int pack_num = 0;
    int cyc = 0;
    // constructor to put ip address and port 
    public void connect() 
    { 
        // establish a connection 
        try
        { 
            socket = new DatagramSocket(port_number);
            byte[] receive = new byte[65536];
            byte[] send = new byte[4];
            start_time = System.nanoTime();
            Hashtable<Integer,Boolean> received_packets = new Hashtable<Integer,Boolean>();
            while(received_packets.size() < max_packets)
            {
                DatagramPacket d = new DatagramPacket(receive,receive.length);
                socket.setSoTimeout(5000);
                try
                {
                    socket.receive(d);//attempts to read d bytes of data from the sender
                }
                catch(IOException i)
                {
                    //i.printStackTrace();
                    System.exit(0);
                }
                    long t = System.nanoTime() - start_time;
                    count++;
                    double p = Math.random();
                    boolean dr = true;
                    //allocate first 4 bytes for the packet number
                    int pn = ((receive[0] & 0xff) << 24) | ((receive[1] & 0xff) << 16) | ((receive[2] & 0xff) << 8) | (receive[3] & 0xff);
                    if(p > random_drop_prob)
                    {//accepted this packet successfully.
                        pack_num = Math.max(pn+1,pack_num);//send an acknowledgement now,corresponding to the given sequence number
                        dr = false;
                        send[0] = receive[0];
                        send[1] = receive[1];
                        send[2] = receive[2];
                        send[3] = receive[3];
                        DatagramPacket d1 = new DatagramPacket(send,send.length,d.getAddress(),d.getPort());
                        try{
                            socket.send(d1);//send an acknowledgment
                        }
                        catch(IOException e)
                        {
                            //pw.println("Couldn't send the packet due to TLE.");
                            e.printStackTrace();
                            System.exit(0);
                        }
                        received_packets.put(pn,true);
                    }
                    if(debug)
                        pw.printf("%d : Time received: %s Packet dropped: %b\n",pn,format_time(t),dr);
                    receive = new byte[65536];
                    send = new byte[4];
            }
        }
       
        catch(SocketException s)
        {
            s.printStackTrace();
            System.exit(0);
        }
    } 
    public String format_time(long interval)
    {
        String s;
        int x = 1000000;
        long mi = interval/x;
        long mic = (interval % x)/1000;
        s = String.format("%d:%d",mi,mic);
        return s;
    }
} 