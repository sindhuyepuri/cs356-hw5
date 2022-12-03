package edu.ut.cs.sdn.simpledns;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.ut.cs.sdn.simpledns.packet.*;

import java.io.IOException;
import java.net.DatagramPacket;

public class SimpleDNS 
{
	public static final int PORT = 8053;
	public static final int ROOT_PORT = 53;
	public static final int BUFFER_SZ = 65_527;
	public static void main(String[] args)
	{
		Arguments arguments = Arguments.parseArguments(args);
		if (arguments == null) return;

		SimpleDNS simpleDNS = new SimpleDNS(arguments.rootServerIp, EC2Resolver.createEC2Resolver(arguments.ec2Csv));
		simpleDNS.run();
	}

	private EC2Resolver resolver;
	private InetAddress root_addr;
	private SimpleDNS(String rootServerIp, EC2Resolver reader) {
		this.resolver = reader;
		try {
			this.root_addr = InetAddress.getByName(rootServerIp);
		} catch (UnknownHostException e) {
			System.exit(-1);
		}
	}
	
	private void run() {
		try (DatagramSocket ds = new DatagramSocket(SimpleDNS.PORT)) {
			byte[] data = new byte[SimpleDNS.BUFFER_SZ];
			DatagramPacket digRequest = new DatagramPacket(data, SimpleDNS.BUFFER_SZ);
			
			// keep checking for new packets to process
			while (true) {
				// receive a packet for which we need to generate a response
				ds.receive(digRequest);
				SocketAddress returnAddr = digRequest.getSocketAddress();
				DNS digDNS = DNS.deserialize(digRequest.getData(), digRequest.getLength());

				// we only consider standard queries
				if (!digDNS.isQuery() || digDNS.getOpcode() != DNS.OPCODE_STANDARD_QUERY) continue;

				boolean recursion = digDNS.isRecursionDesired();

				// Implementation for handling multiple questions is not required.
				// we only consider the first question
				DNSQuestion question = digDNS.getQuestions().get(0);

				switch (question.getType()) {
					case DNS.TYPE_A:
					case DNS.TYPE_AAAA:
					case DNS.TYPE_CNAME:
					case DNS.TYPE_NS:
						digRequest.setAddress(this.root_addr);
						digRequest.setPort(SimpleDNS.ROOT_PORT);
						ds.send(digRequest);
						byte[] responseData = new byte[SimpleDNS.BUFFER_SZ];
						DatagramPacket answer = new DatagramPacket(responseData, SimpleDNS.BUFFER_SZ);
						ds.receive(answer);
						if (recursion) {
							// answer = this.recursion(ds, digRequest, answer, null);
							answer = this.recursion(ds, digRequest, answer);
							if (answer == null) answer = new DatagramPacket(responseData, SimpleDNS.BUFFER_SZ);
							DNS answerDNS = DNS.deserialize(answer.getData(), answer.getLength());
							
							DNSResourceRecord rr = null;
							if (answerDNS.getAdditional().size() > 1)
								rr = answerDNS.getAdditional().get(0);

							// resolve a CNAME
							// answerDNS = this.resolveCNAME(ds, answerDNS, question.getType(), rr);
							answerDNS = this.resolveCNAME(ds, answerDNS, question.getType());

							answerDNS.setRecursionAvailable(true);
							answer = new DatagramPacket(answerDNS.serialize(), answerDNS.getLength());
						}

						// handle ec2 regions for A queries
						if (question.getType() == DNS.TYPE_A) {
							DNS answerDNS = DNS.deserialize(answer.getData(), answer.getLength());
							List<DNSResourceRecord> newAnswers = new ArrayList<>();
							for (DNSResourceRecord rr : answerDNS.getAnswers()) {
								if (rr.getType() != DNS.TYPE_A) continue;
								DNSRdataAddress rdata = (DNSRdataAddress) rr.getData();
								String ip = rdata.toString();
								String ec2Region = resolver.get(ip);
								if (ec2Region == null) continue;
								DNSRdataString string = new DNSRdataString(ec2Region + "-" + ip);
								DNSResourceRecord txt = new DNSResourceRecord(question.getName(), DNS.TYPE_TXT, new DNSRdataBytes(string.serialize()));
								newAnswers.add(txt);
							}
							newAnswers.stream().forEach(answerDNS::addAnswer);
							answer = new DatagramPacket(answerDNS.serialize(), answerDNS.getLength());
						}
						answer.setSocketAddress(returnAddr);
						ds.send(answer);
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
	
	private DatagramPacket sendDigReq(DatagramSocket ds, DatagramPacket digRequest) throws IOException {
		// System.out.println(digRequest.getAddress().getHostAddress());
		ds.send(digRequest);
		// recurse on response from authority NS
		byte[] responseData = new byte[SimpleDNS.BUFFER_SZ];
		DatagramPacket response = new DatagramPacket(responseData, SimpleDNS.BUFFER_SZ);
		ds.receive(response);
		return response;
	}
	
	private DatagramPacket sendDigReq(DatagramSocket ds, DatagramPacket digRequest, InetAddress addr) throws IOException {
		digRequest.setAddress(addr);
		return this.sendDigReq(ds, digRequest);
	}
	
	private DatagramPacket sendDigReq(DatagramSocket ds, DatagramPacket digRequest, InetAddress addr, int port) throws IOException {
		digRequest.setAddress(addr);
		digRequest.setPort(port);
		return this.sendDigReq(ds, digRequest);
	}
	private DatagramPacket recursion(DatagramSocket ds, DatagramPacket digRequest, DatagramPacket answer) throws IOException {
	// private DatagramPacket recursion(DatagramSocket ds, DatagramPacket digRequest, DatagramPacket answer, DNSResourceRecord cookie) throws IOException {
		DNS responseDNS = DNS.deserialize(answer.getData(), answer.getLength());

		// handle base case -- we received an answer
		if (responseDNS.getAnswers().size() > 0) return answer;

		// get the authorities and additional sections
		List<DNSResourceRecord> authorities = responseDNS.getAuthorities();
		List<DNSResourceRecord> additionals = responseDNS.getAdditional();
		for (DNSResourceRecord authority : authorities) {
			// lookup authority in additional to get IP
			if (authority.getType() != DNS.TYPE_NS) continue;

			String hostname = ((DNSRdataName) authority.getData()).getName();

			// find IP corresponding to authority
			for (DNSResourceRecord additional : additionals) {
				if (additional.getName().equals(hostname) &&
					(additional.getType() == DNS.TYPE_A  || additional.getType() == DNS.TYPE_AAAA)) {
						InetAddress hostIP = ((DNSRdataAddress) additional.getData()).getAddress();
						// send request to IP of authority NS
						// System.out.println("**** sending dig request to host ip");
						answer = this.sendDigReq(ds, digRequest, hostIP);
						// System.out.println("***** after sending");

						// if we find an answer from this NS, return, else keep looking
						// answer = this.recursion(ds, digRequest, answer, cookie);
						answer = this.recursion(ds, digRequest, answer);
						if (answer != null) {
							DNS answerDNS = DNS.deserialize(answer.getData(), answer.getLength());
							answerDNS.addAuthority(authority);
							answerDNS.addAdditional(additional);
							// if (cookie != null) answerDNS.addAdditional(cookie);
							answer = new DatagramPacket(answerDNS.serialize(), answerDNS.getLength());
							return answer;
						}
				}
			}
			
			// we did not find IP address for nameserver in the additional section
			// query root name server for the ip address of the name server from authority section, and then make query
			DNS query = new DNS();
			// we ask for an A or AAAA response
			DNSQuestion questionA = new DNSQuestion(hostname, DNS.TYPE_A);
			DNSQuestion questionAAAA = new DNSQuestion(hostname, DNS.TYPE_AAAA);
			query.setQuestions(new ArrayList<>(Arrays.asList(new DNSQuestion[]{questionA, questionAAAA})));
			query.setOpcode(DNS.OPCODE_STANDARD_QUERY);
			query.setQuery(true);
			query.setAuthoritative(false);
			// if (cookie != null) query.addAdditional(cookie);
			DatagramPacket authorityRootQuery = new DatagramPacket(query.serialize(), query.getLength());
			DatagramPacket authorityRootAnswer = this.sendDigReq(ds, authorityRootQuery, this.root_addr, SimpleDNS.ROOT_PORT);
			DNS authDNS = DNS.deserialize(authorityRootAnswer.getData(), authorityRootAnswer.getLength());
			if (authDNS.getAnswers().size() != 0) {
				for (DNSResourceRecord rr : authDNS.getAnswers()) {
					if (rr.getType() == DNS.TYPE_A || rr.getType() == DNS.TYPE_AAAA) {
						InetAddress hostIP = ((DNSRdataAddress) rr.getData()).getAddress();
						// send request to IP of authority NS
						answer = this.sendDigReq(ds, digRequest, hostIP);

						// if we find an answer from this NS, return, else keep looking
						// answer = this.recursion(ds, digRequest, answer, cookie);
						answer = this.recursion(ds, digRequest, answer);
						if (answer != null) {
							DNS answerDNS = DNS.deserialize(answer.getData(), answer.getLength());
							answerDNS.addAuthority(authority);
							// if (cookie != null) answerDNS.addAdditional(cookie);
							answer = new DatagramPacket(answerDNS.serialize(), answerDNS.getLength());
							return answer;
						}
					}
				}
			}
		}

		// System.out.println("**** SOMETHING SCREWED UP --> " + responseDNS);
		return null;
	}
	// private DNS resolveCNAME(DatagramSocket ds, DNS dns, short type, DNSResourceRecord cookie) throws IOException {
	private DNS resolveCNAME(DatagramSocket ds, DNS dns, short type) throws IOException {

		if (type == DNS.TYPE_CNAME) return dns;

		// get the last answer for incoming DNS
		List<DNSResourceRecord> currAnswers = dns.getAnswers();
		DNSResourceRecord rr = currAnswers.get(currAnswers.size() - 1);

		// BASE CASE: we've found a result
		if (rr.getType() != DNS.TYPE_CNAME) return dns;

		DNSRdataName rdata = (DNSRdataName) rr.getData();
		DNSQuestion question = new DNSQuestion(rdata.toString(), type);
		
		DNS request = new DNS();
		request.addQuestion(question);
		request.setOpcode(DNS.OPCODE_STANDARD_QUERY);
		request.setQuery(true);
		request.setAuthoritative(false);
		// if (cookie != null)
		// 	request.addAdditional(cookie);
		DatagramPacket pkt = new DatagramPacket(request.serialize(), request.getLength());
		pkt.setAddress(this.root_addr);
		pkt.setPort(SimpleDNS.ROOT_PORT);
		ds.send(pkt);

		byte[] buffer = new byte[SimpleDNS.BUFFER_SZ];
		DatagramPacket ans = new DatagramPacket(buffer, SimpleDNS.BUFFER_SZ);
		ds.receive(ans);
		DNS resp = DNS.deserialize(ans.getData(), ans.getLength());
		// did we find an answer?
		if (resp.getAnswers().size() == 0) {
			// ans = this.recursion(ds, pkt, ans, cookie);
			ans = this.recursion(ds, pkt, ans);

			resp = DNS.deserialize(ans.getData(), ans.getLength());
		}

		resp.getAnswers().stream().forEach(dns::addAnswer);
		resp.getAuthorities().stream().forEach(dns::addAuthority);
		resp.getAdditional().stream().forEach(dns::addAdditional);
		return this.resolveCNAME(ds, dns, type);
		// return this.resolveCNAME(ds, dns, type, cookie);
	}

}