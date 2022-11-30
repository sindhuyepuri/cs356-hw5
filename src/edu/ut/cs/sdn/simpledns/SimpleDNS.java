package edu.ut.cs.sdn.simpledns;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import edu.ut.cs.sdn.simpledns.packet.DNS;
import edu.ut.cs.sdn.simpledns.packet.DNSQuestion;
import edu.ut.cs.sdn.simpledns.packet.DNSResourceRecord;

import java.io.IOException;
import java.net.DatagramPacket;

public class SimpleDNS 
{
	public static final int PORT = 8053;
	public static final int BUFFER_SZ = 65_507;
	public static void main(String[] args)
	{
		Arguments arguments = Arguments.parseArguments(args);
		if (arguments == null) return;

		SimpleDNS simpleDNS = new SimpleDNS(arguments.rootServerIp, CSVReader.createCSVReader(arguments.ec2Csv));
		simpleDNS.run();
	}

	private CSVReader reader;
	private DatagramSocket root;
	private InetAddress root_addr;
	private SimpleDNS(String rootServerIp, CSVReader reader) {
		this.reader = reader;
		try {
			this.root = new DatagramSocket();
			this.root_addr = InetAddress.getByName(rootServerIp);
			this.root.connect(this.root_addr, SimpleDNS.PORT);
		} catch (SocketException e) {
			System.exit(-1);
		} catch (UnknownHostException e) {
			System.exit(-1);
		}
	}

	private void run() {
		System.out.println("******** RUN");
		try (DatagramSocket ds = new DatagramSocket(SimpleDNS.PORT) ) {
			byte[] data = new byte[SimpleDNS.BUFFER_SZ];
			DatagramPacket udpPacket = new DatagramPacket(data, SimpleDNS.BUFFER_SZ);
			
			// keep checking for new packets to process
			while (true) {
				ds.receive(udpPacket);
				System.out.println("******** RECEIVED PACKET:\n" + udpPacket);
;				DNS dns = DNS.deserialize(udpPacket.getData(), udpPacket.getLength());
				if (dns.isQuery() && dns.getOpcode() == DNS.OPCODE_STANDARD_QUERY) {
					// boolean recursion = dns.isRecursionDesired();
					DNSQuestion question = dns.getQuestions().get(0);
					switch (question.getType()) {
						case DNS.TYPE_A:
						case DNS.TYPE_AAAA:
						case DNS.TYPE_CNAME:
						case DNS.TYPE_NS:
							// DNSResourceRecord result = handleAQuery(dns, question);
							// forward packet
							System.out.println("******** FORWARDING PACKET:\n" + udpPacket);
							System.out.println("\n\n");
							udpPacket.setSocketAddress(root.getRemoteSocketAddress());
							udpPacket.setAddress(this.root_addr);
							this.root.send(udpPacket);
							// System.out.println("**** ROOT ADDRESS: " +this.root_addr.toString());
							// System.out.println("**** ROOT SOCKET ADDRESS: " +root.getRemoteSocketAddress().toString());

							byte[] responseData = new byte[SimpleDNS.BUFFER_SZ];
							DatagramPacket response = new DatagramPacket(responseData, SimpleDNS.BUFFER_SZ);
							this.root.receive(response);							
							System.out.println("******** RECEIVED RESPONSE:\n" + response);
							System.out.println("\n\n");
							break;
						// we silently ignore all other problems
						default:
							throw new UnsupportedOperationException();
					}
				} // fi : silently drop all other packets
				
			}
		} catch (SocketException se) {
			System.err.printf("error connecting to port: %s\n", se.getMessage());
		} catch (IOException io) {
			System.err.printf("error receiving packet: %s\n", io.getMessage());
		}
	}

	private DNSResourceRecord handleAQuery(DNS dns, DNSQuestion question) {
		DNSResourceRecord answer = null;
		// get IP
		// if query is A && DNS server resolves query
		
		return answer;
	}
}