
import java.io.*;
import java.net.*;
import eduni.simjava.Sim_entity;
import eduni.simjava.Sim_event;
import eduni.simjava.Sim_port;
import eduni.simjava.Sim_system;
import eduni.simjava.Sim_type_p;
class adjlist
{
	public int[] L=new int[100];
	public int n;
	
	public adjlist()
	{
		this.n=0;
	}
	public adjlist(int[] a,int k)
	{
		if(k>0)
		{
			for(int i=0;i<k;i++)
				this.L[i]=a[i];
			this.n=k;
		}
		else
			this.n=0;
	}
	public boolean search(int j)
	{
		boolean found=false;
		for(int i=0;i<n;i++)
		{
			if(L[i]==j)
				found=true;;
		}
		return found;
	}
};

class AckThread implements Runnable
{
	public AckThread(node n)
	{
		
	}
	public void run()
	{
		while(true)
		{
			
		}
	}
};

class headerPacket
{
	public int src;
	public int dest;
	public int noOfPackets;
	
	public headerPacket(int s, int d, int nop)
	{
		this.src=s;
		this.dest=d;
		this.noOfPackets=nop;
	}
	
	public int getNoOfPackets()
	{
		return this.noOfPackets;
	}
	
};

class dataPacket
{
	public int dest;
	public int src;
	public byte[] data;
	
	public dataPacket(int s,int d,int size)
	{
		this.src=s;
		this.dest=d;
		this.data=new byte[size];
	}
	public dataPacket(int size)
	{
		data=new byte[size];
	}
	
	public dataPacket(int s,int d)
	{
		this.src=s;
		this.dest=d;
		this.data=new byte[1000];
	}
	public void setDest(int d)
	{
		this.dest=d;
	}
	public void setSrc(int s)
	{
		this.src=s;
	}
	public int getDest()
	{
		return this.dest;
	}
	public int getSrc()
	{
		return this.src;
	}
};

class scheduler extends Sim_entity
{
	scheduler()
	{
		super("scheduler");
	}
	public void body()
	{
		int noOfNodes=0;
		int[] a=new int[100];
		BufferedReader br=null;
		try
		{
			String currentLine;
			br=new BufferedReader(new FileReader("topology.txt"));
			currentLine=br.readLine();
			noOfNodes=Integer.parseInt(currentLine.trim());
			if(noOfNodes<1)
			{
				System.out.println("Invalid input");
				System.exit(0);
			}
			int i=0;
			node[] nodeList=new node[noOfNodes];
			while(i<noOfNodes)
			{
				String name=" ";
				currentLine=br.readLine();
				String[] str=currentLine.split(" ");
				int label=Integer.parseInt(str[0])-1;
				char ntype=str[1].charAt(0);
				if(ntype=='T')
				{
					name="Trans"+label;
				}
				else if(ntype=='R')
				{
					name="Rec"+label;
				}
				else
				{
					System.out.println("Invalid input");
					System.exit(0);
				}
				int adjNodes=Integer.parseInt(str[2]);
				for(int j=0;j<adjNodes;j++)
				{
					a[j]=Integer.parseInt(str[j+3])-1;
				}
				nodeList[i]=new node(name,ntype,label,a,adjNodes);
				Sim_system.add(nodeList[i]);
				i++;
			}
			
		}
		catch(IOException e)
		{
			System.out.println("Check input file");
			e.printStackTrace();
		}
	}
};

class node extends Sim_entity
{
	public char nodeType;
	public int status;
	public Sim_port out;
	public Sim_port in;
	public adjlist adjacencyList;
	
	node(String name, char type,int port_num, int[] a,int k)
	{
		super(name);
		this.status=1;
		this.nodeType=type;
		this.adjacencyList=new adjlist(a,k);
		out=new Sim_port("transmit");
		add_port(out);
		in=new Sim_port("receive");
		add_port(in);	
	}
	
	public boolean isAvailable()
	{
		if(status==1)
			return true;
		return false;
	}
	
	public char getType()
	{
		return this.nodeType;
	}
	public void printStatus()
	{
		if(this.nodeType=='T')
		{
			String aList=" ";
			for(int j=0;j<this.adjacencyList.n;j++)
			{
				aList=aList.concat(String.valueOf((this.adjacencyList.L[j]))+" ");
			}
			System.out.println("Transmitter created. Connected to "+aList);
		}
		if(this.nodeType=='R')
		{
			
			String aList=" ";
			for(int j=0;j<this.adjacencyList.n;j++)
			{
				aList=aList.concat(String.valueOf((this.adjacencyList.L[j]))+" ");
			}
			System.out.println("Receiver created. Connected to "+aList);
		}
	}
	
	public void scheduleTransmit(int dest,int size)
	{
		this.status=2;
		dataPacket packet=new dataPacket(this.get_id(),dest,size);
		sim_schedule(dest,0,4,packet);
	
	}
	
	public void receive(int nop)
	{
		//while(true)
		{
			Sim_event eR=new Sim_event();
			Sim_type_p dataPack=new Sim_type_p(4);
			int k=0;
			while(k<nop)
			{
				sim_get_next(dataPack,eR);
				if(eR.get_data()!=null)
				{	
					//System.out.println("Transmitter "+eR.get_src()+" to receiver "+eR.get_dest()+": "+eR.get_data());
					k++;
				}
			}
			String ack=k+" packets. Data received @ "+eR.get_dest();
			sim_schedule(eR.get_src(),0,5,ack);	
			}
	}
	
	public void pokeReceiver(int dest)
	{
		sim_schedule(dest,0,1,"Available?");
		//System.out.println(this.get_id()+" poking "+dest);
	}
	
	public boolean initialReply(int dest)
	{
		int MCS;
		if(this.isAvailable())
		{
			MCS=4;     		//Change MCS here. values 1 to 6
			sim_schedule(dest,0,2,MCS);
			this.status=2;
			return true;
		}
		else
		{
			sim_schedule(dest,0,2,0);
			return false;
		}
	}
	
	public void sendHeader(int dest,int nop)
	{
		headerPacket header=new headerPacket(this.get_id(),dest,nop);
		sim_schedule(dest,0,3,header);
	}
	
	public double getDataRate(int n)
	{
		if(n==1) //BPSK
			return 6.5;
		else if(n==2) //QPSK
			return 13.0;
		else if(n==3) //QPSK
			return 19.5;
		else if(n==4) //16-QAM
			return 26.0;
		else if(n==5) //16-QAM
			return 39.0;
		else if(n==6) //64-QAM
			return 52.0;
		else
			return -1;
	}
	
	public double getCodingRate(int n)
	{
		if(n==1)
			return 1/2;
		else if(n==2)
			return 1/2;
		else if(n==3)
			return 3/4;
		else if(n==4)
			return 1/2;
		else if(n==5)
			return 3/4;
		else if(n==6)
			return 2/3;
		else
			return -1;		
	}
	
	public double getBitErrorRate(int n)
	{
		if(n==1)
			return 8/1000000;
		else if(n==2)
			return 2/10000;
		else if(n==3)
			return 2/10000;
		else if(n==4)
			return 0.01;
		else if(n==5)
			return 0.01;
		else if(n==6)
			return 0.05;
		else
			return -1;		
	}
	public void body()
	{
		this.printStatus();
		if(Sim_system.running() && this.status==1)
		{
			int packsize,nop;
			
			int d,flag=0,destination=0;
			double errorProb=0;
			
			if(this.nodeType=='T')
			{
				String sendE,receiveE, sleepE,idleE, lowIdleE;
				double start=0,end=0,passiveTime=0,activeTime=0;
				while(true){
				
				this.status=2;
				Sim_event eT=new Sim_event();
				
				
				if(flag==0){
				do
				{
					destination=0+(int)(Math.random()*this.adjacencyList.n);
				}while(!Sim_system.get_entity(destination).get_name().contains("Rec")); }
				

				start=System.nanoTime();
				this.pokeReceiver(destination);
				end=System.nanoTime();
				
				passiveTime+=(end-start)/1000;
									
				Sim_type_p replyInfo=new Sim_type_p(2);
				start=System.nanoTime();
				sim_get_next(replyInfo,eT);
				if(eT.get_data()==null || (int)eT.get_data()==0)
				{
					flag++;
					//System.out.println(this.get_name() + " : No reply from " + destination );
					//sleep(flag*500);
					continue;
				}
				else if((int)eT.get_data() > 0)
				{
					int datasize=50;    //Change Data Size here
					int mcs=(int)eT.get_data();
					packsize=(int)(this.getDataRate(mcs));
					double cr=this.getCodingRate(mcs);
					nop=(int)((datasize/packsize) + (1-cr)*(datasize/packsize));
					nop+=(int)(getBitErrorRate(mcs)*nop);
					
					end=System.nanoTime();
					
					passiveTime+=(end-start)/1000;
					System.out.println(this.get_id()+" got reply from "+eT.get_src()+". Sending header now");

					start=System.nanoTime();
					this.sendHeader(destination,nop);
					end=System.nanoTime();
					
					passiveTime+=(end-start)/1000;
					
					
					start=System.nanoTime();
					int kn=0;
					
					for(int i=0;i<nop;i++)
					{
							
						this.scheduleTransmit(destination,packsize);
						kn++;
						
					}
						
					end=System.nanoTime();
					
					activeTime+=(end-start)/1000;
					sendE=String.valueOf((activeTime/1000000)*1.4);
					System.out.println(this.get_name() + ": Active time : " + activeTime + " microseconds");
					System.out.println(this.get_name() + " : SendE = "+sendE);
					flag=0;
				}
				start=System.nanoTime();
				Sim_type_p ack=new Sim_type_p(2);
				sim_get_next(ack,eT);
				if(eT.get_tag()==5)
				{
					System.out.println("Ack to "+this.get_name()+" "+eT.get_data());
				}
				this.status=1;
				end=System.nanoTime();
				passiveTime+=((end-start)/1000);
				idleE=String.valueOf((passiveTime/1000000)*0.9);
				System.out.println(this.get_name() + " : Passive Time:" + passiveTime+ " microseconds");
				System.out.println(this.get_name() + " : IdleE : " + idleE);
				}
				
			}
			
			else if(this.nodeType=='R')
			{
				String sendE,receiveE, sleepE,idleE, lowIdleE;
				long start=0,end=0,passiveTime=0,activeTime=0;
				int n=0;
				boolean stat=false;
				while(true){
				
				start=System.nanoTime();	
				Sim_event eR=new Sim_event();
				Sim_type_p initialPoke=new Sim_type_p(1);
				sim_get_next(initialPoke,eR);
				if(eR.get_data()==null)
					continue;
				end=System.nanoTime();
				passiveTime+=((end-start)/1000);
				if(eR.get_tag()==1)
				{
					if(this.status==1)
					{
						start=System.nanoTime();
						stat=this.initialReply(eR.get_src());
						if(stat==false)
							continue;
						Sim_type_p headerInfo=new Sim_type_p(3);
						sim_get_next(headerInfo,eR);
						if(eR.get_data()==null)
							continue;
						headerPacket hp=(headerPacket) (eR.get_data());
						end=System.nanoTime();
						passiveTime+=(end-start)/1000;
						
						start=System.nanoTime();
						this.receive(hp.noOfPackets);
						end=System.nanoTime();
						
						activeTime=(end-start)/1000;
						receiveE=String.valueOf((activeTime/1000000)*1.2);
						System.out.println(this.get_name() + " : PassiveTime = "+passiveTime);
						System.out.println(this.get_name() + " : ActiveTime = "+activeTime);
						System.out.println(this.get_name() + " : ReceiveE = "+receiveE);
						this.status=1;
					}
				} }
			}
		}
	}
}

public class simulator{
	
	public static void main(String[] args)
	{
		Sim_system.initialise();
		
		int noOfNodes=0;
		int[] a=new int[100];
		BufferedReader br=null;
		try
		{
			String currentLine;
			br=new BufferedReader(new FileReader("topology.txt"));
			currentLine=br.readLine();
			noOfNodes=Integer.parseInt(currentLine.trim());
			if(noOfNodes<1)
			{
				System.out.println("Invalid input");
				System.exit(0);
			}
			int i=0;
			node[] nodeList=new node[noOfNodes];
			while(i<noOfNodes)
			{
				String name=" ";
				currentLine=br.readLine();
				String[] str=currentLine.split(" ");
				int label=Integer.parseInt(str[0])-1;
				char ntype=str[1].charAt(0);
				if(ntype=='T')
				{
					name="Trans"+label;
				}
				else if(ntype=='R')
				{
					name="Rec"+label;
				}
				else
				{
					System.out.println("Invalid input");
					System.exit(0);
				}
				int adjNodes=Integer.parseInt(str[2]);
				for(int j=0;j<adjNodes;j++)
				{
					a[j]=Integer.parseInt(str[j+3])-1;
				}
				nodeList[i]=new node(name,ntype,label,a,adjNodes);
				Sim_system.add(nodeList[i]);
				i++;
			}
			
		}
		catch(IOException e)
		{
			System.out.println("Check input file");
			e.printStackTrace();
		}
		
		Sim_system.run();
		
	}
}