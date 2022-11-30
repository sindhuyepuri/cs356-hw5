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
	public static final int ROOT_PORT = 53;
	public static final int BUFFER_SZ = 100_000;
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
			// this.root = new DatagramSocket();
			this.root_addr = InetAddress.getByName(rootServerIp);
			// this.root.connect(root_addr, SimpleDNS.PORT);
		// } catch (SocketException e) {
		//	System.exit(-1);
		} catch (UnknownHostException e) {
			System.exit(-1);
		}
	}

	private void run() {
		try (DatagramSocket ds = new DatagramSocket(SimpleDNS.PORT)) {
			// ds.connect(root_addr, PORT);
			// System.out.println("***** SOCKET local socket address: " + ds.getLocalSocketAddress());
			// System.out.println("***** SOCKET remote socket address: " + ds.getRemoteSocketAddress());
			byte[] data = new byte[SimpleDNS.BUFFER_SZ];
			DatagramPacket udpPacket = new DatagramPacket(data, SimpleDNS.BUFFER_SZ);
			
			// keep checking for new packets to process
			while (true) {
				// receive a packet for which we need to generate a response
				ds.receive(udpPacket);
				DNS dns = DNS.deserialize(udpPacket.getData(), udpPacket.getLength());

				// we only consider standard queries
				if (!dns.isQuery() || dns.getOpcode() != DNS.OPCODE_STANDARD_QUERY) continue;

				boolean recursion = dns.isRecursionDesired();

				// Implementation for handling multiple questions is not required.
				// we only consider the first question
				DNSQuestion question = dns.getQuestions().get(0);

				switch (question.getType()) {
					case DNS.TYPE_A:
					case DNS.TYPE_AAAA:
					case DNS.TYPE_CNAME:
					case DNS.TYPE_NS:
						udpPacket.setAddress(this.root_addr);
						udpPacket.setPort(SimpleDNS.ROOT_PORT);
						// udpPacket.setSocketAddress(this.root.getRemoteSocketAddress());
						ds.send(udpPacket);
						byte[] responseData = new byte[SimpleDNS.BUFFER_SZ];
						DatagramPacket response = new DatagramPacket(responseData, SimpleDNS.BUFFER_SZ);
						ds.receive(response);
						System.out.println("******** RESPONSE: \n"+DNS.deserialize(response.getData(), response.getLength()));
						break;
					default:
						// we silently ignore all other question types
						break;
				}
				
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