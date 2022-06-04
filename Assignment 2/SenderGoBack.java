import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;
class SenderGoBack
{
    public static void main(String args[])
    {
        try{
        SenderGoBackThread.socket = new DatagramSocket();
        }
        catch (SocketException e)
        {
            e.printStackTrace();
        }
        SenderGoBackThread.attempts = new ConcurrentHashMap<Integer,Integer>();
        SenderGoBackThread.unacknowledged_packets = new ConcurrentHashMap<Integer,Boolean>();
        SenderGoBackThread.accepted_packets = new ConcurrentHashMap<Integer,Boolean>();
        SenderGoBackThread.time_sent = new ConcurrentHashMap<Integer,Long>();
        SenderGoBackThread.rejected_packet = new ConcurrentHashMap<Integer,Boolean>();
        SenderGoBackThread.printed_data = false;
        SenderGoBackThread obj = new SenderGoBackThread();
        SenderGoBackThread obj1 = new SenderGoBackThread();
        SenderGoBackThread obj2 = new SenderGoBackThread();
        obj.arg = 1;
        obj1.arg = 2;
        obj2.arg = 3;
        SenderGoBackThread.receiver_name = "Receiver";
        try{
            SenderGoBackThread.receiver_IP = InetAddress.getLocalHost();
        }
        catch(UnknownHostException e)
        {
            e.printStackTrace();
        }
        SenderGoBackThread.port_num = 12345;
        SenderGoBackThread.timeout  = 100000000;//in nanoseconds
        SenderGoBackThread.packet_length = 64;
        SenderGoBackThread.ack_packets = 0;
        SenderGoBackThread.generation_rate = 1;//packets per second
        SenderGoBackThread.max_packets = 1000;
        SenderGoBackThread.window_size = 5;
        SenderGoBackThread.buffer_size = 10;
        SenderGoBackThread.total_ack_p = 0;
        SenderGoBackThread.debug = false;
        SenderGoBackThread.buffer = new LinkedBlockingDeque<DatagramPacket>();
        SenderGoBackThread.repeat_packets = new LinkedBlockingDeque<DatagramPacket>();
        SenderGoBackThread.retrans_p = 0;
        SenderGoBackThread.total_packets = 0;
        int l = args.length;
        int p = 0;
        String cur_arg;
        while(p < l)
        {
            cur_arg = args[p];
            if(cur_arg.equals("-d") || cur_arg.equals("-D"))
            {
                SenderGoBackThread.debug = true;
                p++;
            }
            else if(cur_arg.equals("-s") || cur_arg.equals("-S"))
            {
                SenderGoBackThread.receiver_name = args[p+1];
                try{
                    SenderGoBackThread.receiver_IP = InetAddress.getByName(SenderGoBackThread.receiver_name);
                }
                catch(UnknownHostException e)
                {
                    e.printStackTrace();
                }
                p += 2;
            }
            else if(cur_arg.equals("-p") || cur_arg.equals("-P"))
            {   
                SenderGoBackThread.port_num = Integer.parseInt(args[p+1]);
                p += 2;
            }
            else if(cur_arg.equals("-l") || cur_arg.equals("-L"))
            {
                SenderGoBackThread.packet_length = Integer.parseInt(args[p+1]);
                p += 2;
            }
            else if(cur_arg.equals("-r")|| cur_arg.equals("-R"))
            {
                SenderGoBackThread.generation_rate = Integer.parseInt(args[p+1]);
                p += 2;
            }
            else if(cur_arg.equals("-n")|| cur_arg.equals("-N"))
            {
                SenderGoBackThread.max_packets = Integer.parseInt(args[p+1]);
                p += 2;
            }
            else if(cur_arg.equals("-w")|| cur_arg.equals("-W"))
            {
                SenderGoBackThread.window_size = Integer.parseInt(args[p+1]);
                p += 2;
            }
            else if(cur_arg.equals("-b")|| cur_arg.equals("-B"))
            {
                SenderGoBackThread.buffer_size = Integer.parseInt(args[p+1]);
                p += 2;
            }
            else
            {
                System.out.println("Invalid Command line arguments. Exiting now.");
                System.exit(0);
            }
        }
        SenderGoBackThread.begin_time = System.nanoTime();
        obj.start();
        obj1.start();
        obj2.start();
    }
}
class SenderGoBackThread extends Thread
{
    public static volatile String receiver_name;
    public static volatile InetAddress receiver_IP;
    DatagramPacket data_to_be_sent;
    public int arg;
    public static volatile int port_num;
    public static volatile int packet_length;
    public static volatile int generation_rate;//packets per second
    public static volatile int max_packets;
    public static volatile long timeout;
    public static volatile int tot_time;
    public static volatile int ack_packets;
    public static volatile int retrans_p;
    public static volatile int total_packets;
    public static volatile boolean printed_data;
    public static volatile int total_ack_p;
    public static volatile BlockingDeque<DatagramPacket> repeat_packets;
    static volatile ConcurrentHashMap<Integer,Integer> attempts;
    static volatile ConcurrentHashMap<Integer,Boolean> unacknowledged_packets;
    static volatile ConcurrentHashMap<Integer,Boolean> accepted_packets;
    static volatile ConcurrentHashMap<Integer,Long> time_sent;
    static volatile ConcurrentHashMap<Integer,Boolean> rejected_packet;
    static volatile long begin_time;
    public static volatile int window_size;
    public static volatile int buffer_size;
    static int last_generated = 0;
    DatagramPacket p;
    public static volatile int seq_num;
    public static volatile BlockingDeque<DatagramPacket> buffer;
    static volatile DatagramSocket socket;
    public static volatile boolean debug;
    static volatile PrintWriter pw = new PrintWriter(System.out,true);
    @Override
    public void run()
    {
        if(arg == 1)
            this.generate();//generator thread that adds to buffer
        else if(arg == 2)
            this.fire();//fire generated threads
        else if(arg == 3)
            this.receive();//receive acknowledgements
        else if(arg == 4)
            this.run_packet();
    }
    public void generate()
    {
        byte[] buf = new byte[packet_length];
        DatagramPacket da;
        do{
            //pw.printf("Generating!\n");
            int gp = 0;
            long b = System.nanoTime();
            while(gp < generation_rate)
            {
                int x = last_generated;
                buf[0] = (byte) ((x >> 24) & 0xff);
                buf[1] = (byte) ((x >> 16) & 0xff);
                buf[2] = (byte) ((x >> 8) & 0xff);
                buf[3] = (byte) (x & 0xff);
                da = new DatagramPacket(buf, packet_length,receiver_IP,port_num);
                gp++;
                if(buffer.size() >= buffer_size)
                    continue;
                buffer.addLast(da);
                last_generated += 1;
                buf = new byte[packet_length];
            }
            long e = System.nanoTime();
            long x = Math.max(1000000000-e+b,0);//one second
            long s = x/1000000;
            int n = (int)x%1000000;
            try{
            Thread.sleep(s,n);//wait for a second to elapse
            }
            catch(InterruptedException ex)
            {
                ex.printStackTrace();
            }
            //pw.print("Generator is running peacefully!\n");
        }
        while(accepted_packets.size() < max_packets);
        //decide what to do post firing packets
        this.terminate();
    }
    public void terminate()
    {
        if(!SenderGoBackThread.printed_data)
        {
            SenderGoBackThread.printed_data = true;
            pw.printf("Packet rate = %d\nLength = %d\nRetransmission Ratio = %f\nAverage RTT = %f\n",generation_rate,packet_length,(double)total_packets/ack_packets,(double)(tot_time)/total_ack_p);
            System.exit(0);
        }
        
    }

    private int index(byte[] receive)
    {
        int x = ((receive[0] & 0xff) << 24) | ((receive[1] & 0xff) << 16) | ((receive[2] & 0xff) << 8) | (receive[3] & 0xff);
        return x;
    }
    public void fire()
    {
        DatagramPacket da;
        do{
            boolean x = false;
            while(repeat_packets.size() > 0)
            {
                x = true;
                //pw.println("Trying to repeat!");
                try{
                    da = repeat_packets.removeFirst();
                    }
                    catch(NoSuchElementException e)
                    {
                        break;
                    }
                    SenderGoBackThread ob = new SenderGoBackThread();
                    //pw.printf("Trying to repeat %d\n",this.index(da.getData()));
                    ob.data_to_be_sent = da;
                    ob.arg = 4;
                    ob.start();
            }
            if(x)
                continue;
            while((buffer.size() > 0) && (unacknowledged_packets.size() < window_size))
            {
                //pw.printf("Firing!\n");
                try{
                da = buffer.removeFirst();
                }
                catch(NoSuchElementException e)
                {
                    break;
                }
                SenderGoBackThread ob = new SenderGoBackThread();
                ob.data_to_be_sent = da;
                ob.arg = 4;
                ob.start();
            }
            //pw.print("Firing threads peacefully!\n");
        }
        while(accepted_packets.size() < max_packets);
        this.terminate();
    }

    public void run_packet()
    {
        total_packets++;
        int ind = this.index(data_to_be_sent.getData());
        unacknowledged_packets.put(ind,true);
        Integer x = SenderGoBackThread.attempts.get(ind);
        if(x == null)
            attempts.put(ind,1);
        else if(x < 5)
        {
            attempts.put(ind,x+1);
            retrans_p++;
        }
        else 
            this.terminate();
        time_sent.put(ind,System.nanoTime());
        try
        {
            socket.send(data_to_be_sent);
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }
        try{
            Thread.sleep(timeout/1000000,(int)(timeout%1000000));
        }
        catch(InterruptedException ex1)
        {
            ex1.printStackTrace();
        }
        if(unacknowledged_packets.get(ind) != null)//timed out, so proceed to repeat
        {
            //pw.printf("Packet %d timed out!\n",ind);
            buffer.clear();
            SenderGoBackThread.last_generated = ind+window_size;
            rejected_packet.put(ind,true);
            int n = ind;
            byte[] buf = new byte[packet_length];
            //SenderGoBackThread.last_generated = Math.max(0,last_generated-window_size);//backtrack by those many packets
            int i;
            for(i = n;i < n+window_size;i++)
            {
                unacknowledged_packets.put(i,true);
                buf[0] = (byte) ((i >> 24) & 0xff);
                buf[1] = (byte) ((i >> 16) & 0xff);
                buf[2] = (byte) ((i >> 8) & 0xff);
                buf[3] = (byte) ((i) & 0xff);
                p = new DatagramPacket(buf,packet_length,receiver_IP,port_num);
                repeat_packets.addLast(p);//fire them later, doesn't work!
                //pw.println("Added to repeat packets!");
                buf = new byte[packet_length];
            }
        }
    }
    
    public void receive()
    {   
        //receives the packet acknowledgements from the receiver
        byte[] buf = new byte[4];
        DatagramPacket d = new DatagramPacket(buf,4);
        do
        {
            try{
                // pw.printf("Waiting .....\n");
            socket.receive(d);
            total_ack_p++;
            long ti = System.nanoTime();
            int ind = this.index(d.getData());
            Long s = time_sent.get(ind);
            tot_time += (ti-s);
            if(rejected_packet.containsKey(ind))
            {
                if(ack_packets == 10)
                {
                    timeout = (2 * tot_time)/total_ack_p;//double the RTT in nanoseconds
                }
                if(debug)
                {
                    pw.printf("%d Time Generated: %d RTT %d Number of Attempts %d\n",ind,ti-begin_time,(ti-s),attempts.get(ind));
                }
                rejected_packet.remove(ind);
                continue;  
            }
            unacknowledged_packets.remove(ind);
            accepted_packets.put(ind,true);
            rejected_packet.remove(ind);
            ack_packets++;
            if(ack_packets >= 10)
            {
                timeout = (2 * tot_time)/total_ack_p;//double the RTT in nanoseconds
            }
            if(debug)
            {
                pw.printf("%d Time Generated: %d RTT %d Number of Attempts %d\n",ind,ti-begin_time,(ti-s),attempts.get(ind));
            }
            }
            catch(IOException ex)
            {
                ex.printStackTrace();
            }
            buf = new byte[4];
            d = new DatagramPacket(buf, 4);
        }
        while(accepted_packets.size() < max_packets);
        this.terminate();
    }
}