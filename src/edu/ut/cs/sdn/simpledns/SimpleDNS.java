package edu.ut.cs.sdn.simpledns;

import java.net.DatagramSocket;
import java.net.SocketException;

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
		// arguments.ec2Csv;
		// arguments.rootServerIp; 
		Arguments arguments = Arguments.parseArguments(args);
		if (arguments == null) return;

		CSVReader reader = CSVReader.createCSVReader(arguments.ec2Csv);

		// try-with-resources to open a DatagramSocket
		try (DatagramSocket ds = new DatagramSocket(PORT);) {
			byte[] data = new byte[BUFFER_SZ];
			DatagramPacket udpPacket = new DatagramPacket(data, PORT);
			
			// keep checking for new packets to process
			while (true) {
				ds.receive(udpPacket);
				DNS dns = DNS.deserialize(udpPacket.getData(), udpPacket.getLength());
				if (dns.isQuery() && dns.getOpcode() == DNS.OPCODE_STANDARD_QUERY) {
					// boolean recursion = dns.isRecursionDesired();
					for (DNSQuestion question : dns.getQuestions()) {
						switch (question.getType()) {
							case DNS.TYPE_A -> handleAQuery(dns, question);
							case DNS.TYPE_AAAA -> throw new UnsupportedOperationException();
							case DNS.TYPE_CNAME -> throw new UnsupportedOperationException();
							case DNS.TYPE_NS -> throw new UnsupportedOperationException();

							// we silently ignore all other problems
							default -> throw new UnsupportedOperationException();
						}
					}
				} // fi : silently drop all other packets
				
			}
		} catch (SocketException se) {
			System.err.printf("error connecting to port: %s\n", se.getMessage());
			return;
		} catch (IOException io) {
			System.err.println("error receiving packet - IOException");
			return;
		}
		 
	}

	private static DNSResourceRecord handleAQuery(DNS dns, DNSQuestion question) {
		DNSResourceRecord answer = null;
		// get IP
		// if query is A && DNS server resolves query 
		return answer;
	}
}