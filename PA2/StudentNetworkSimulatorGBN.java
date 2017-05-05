//package PA2;

import java.util.ArrayList;

public class StudentNetworkSimulatorGBN extends NetworkSimulator {

	/*
	 * Predefined Constants (static member variables):
	 *
	 * int MAXDATASIZE : the maximum size of the Message data and Packet payload
	 *
	 * int A : a predefined integer that represents entity A int B : a
	 * predefined integer that represents entity B
	 *
	 *
	 * Predefined Member Methods:
	 *
	 * void stopTimer(int entity): Stops the timer running at "entity" [A or B]
	 * void startTimer(int entity, double increment): Starts a timer running at
	 * "entity" [A or B], which will expire in "increment" time units, causing
	 * the interrupt handler to be called. You should only call this with A.
	 * void toLayer3(int callingEntity, Packet p) Puts the packet "p" into the
	 * network from "callingEntity" [A or B] void toLayer5(int entity, String
	 * dataSent) Passes "dataSent" up to layer 5 from "entity" [A or B] double
	 * getTime() Returns the current time in the simulator. Might be useful for
	 * debugging. void printEventList() Prints the current event list to stdout.
	 * Might be useful for debugging, but probably not.
	 *
	 *
	 * Predefined Classes:
	 *
	 * Message: Used to encapsulate a message coming from layer 5 Constructor:
	 * Message(String inputData): creates a new Message containing "inputData"
	 * Methods: boolean setData(String inputData): sets an existing Message's
	 * data to "inputData" returns true on success, false otherwise String
	 * getData(): returns the data contained in the message Packet: Used to
	 * encapsulate a packet Constructors: Packet (Packet p): creates a new
	 * Packet that is a copy of "p" Packet (int seq, int ack, int check, String
	 * newPayload) creates a new Packet with a sequence field of "seq", an ack
	 * field of "ack", a checksum field of "check", and a payload of
	 * "newPayload" Packet (int seq, int ack, int check) chreate a new Packet
	 * with a sequence field of "seq", an ack field of "ack", a checksum field
	 * of "check", and an empty payload Methods: boolean setSeqnum(int n) sets
	 * the Packet's sequence field to "n" returns true on success, false
	 * otherwise boolean setAcknum(int n) sets the Packet's ack field to "n"
	 * returns true on success, false otherwise boolean setChecksum(int n) sets
	 * the Packet's checksum to "n" returns true on success, false otherwise
	 * boolean setPayload(String newPayload) sets the Packet's payload to
	 * "newPayload" returns true on success, false otherwise int getSeqnum()
	 * returns the contents of the Packet's sequence field int getAcknum()
	 * returns the contents of the Packet's ack field int getChecksum() returns
	 * the checksum of the Packet int getPayload() returns the Packet's payload
	 *
	 */

	private int base;
	private static final int BUFFERSIZE = 50;
	// private Packet[] buffer;
	private ArrayList<Packet> buffer;
	private int windowSize;
	private int nextSeq;
	private int timeIncrement;
	private Packet pac_B;
	private int seq_B;
	private int numPackets = 0, numPacketsResend = 0, numAcks = 0,
			corruptPac = 0;

	public StudentNetworkSimulatorGBN(int numMessages, double loss, double corrupt, double avgDelay, int trace,
			long seed) {
		super(numMessages, loss, corrupt, avgDelay, trace, seed);
	}

	@Override
	public void runSimulator() {
		super.runSimulator();
		System.out.println("\nNumber of data packets transmitted:\t\t" + numPackets);
		System.out.println("Number of data packets retransmitted:\t\t" + numPacketsResend);
		System.out.println("Number of ACK packets transimitted:\t\t" + numAcks);
		System.out.println("Number of corrupt packets received:\t\t" + corruptPac);
	}

	// Add any necessary class variables here. Remember, you cannot use
	// these variables to send messages error free! They can only hold
	// state information for A or B.
	// Also add any necessary methods (e.g. checksum of a String)
	// This routine will be called whenever the upper layer at the sender [A]
	// has a message to send. It is the job of your protocol to insure that
	// the data in such a message is delivered in-order, and correctly, to
	// the receiving upper layer.
	protected void aOutput(Message message) {
		if (buffer.size() < this.base + BUFFERSIZE + this.windowSize) {
			System.out.println("Sender: New message received from layer 5");
			String mes = message.getData();
			int seqA, ack;
			ack = seqA = this.buffer.size();
			int checkSum = setMessageCheckSum(seqA, ack, A, mes);
			Packet p = new Packet(seqA, ack, checkSum, mes);
			this.buffer.add(p);
			try {
				while (this.nextSeq < base + windowSize) {
					if (this.nextSeq < buffer.size())
						System.out.println("Sender: Sending packet " + this.nextSeq + " to receiver");
					toLayer3(A, buffer.get(this.nextSeq));
					if (base == this.nextSeq)
						startTimer(A, this.timeIncrement);
					this.nextSeq++;
				}			
			} catch (IndexOutOfBoundsException e) {
				System.err.println("Sender: Window and buffer are empty. No more packets to send.");
			}
			numPackets++;
		} else {
			System.err.println("Sender: buffer is full, some packets might be dropped");
		}
	}

	// This routine will be called whenever a packet sent from the B-side
	// (i.e. as a result of a toLayer3() being done by a B-side procedure)
	// arrives at the A-side. "packet" is the (possibly corrupted) packet
	// sent from the B-side.
	protected void aInput(Packet packet) {
		System.out.println("Sender: Packet from layer 3 has been received");
		if (corrupt(packet, B)) {
			corruptPac++;
			System.out.println("Sender: Packet corruption found, timeout");
		} else {
			System.out.println("Sender: ACK packet has been checked");
			this.base = packet.getAcknum() + 1;
			if (base == this.nextSeq)
				stopTimer(A);
		}

	}

	// This routine will be called when A's timer expires (thus generating a
	// timer interrupt). You'll probably want to use this routine to control
	// the retransmission of packets. See startTimer() and stopTimer(), above,
	// for how the timer is started and stopped.
	protected void aTimerInterrupt() {

		System.out.println("Sender: Timer interruptted, packet resended");
		startTimer(A, this.timeIncrement);
		for (int i = base; i < this.nextSeq; i++) {
			System.out.println("Sender: Retransmitting the unacked packets from buffer");
			toLayer3(A, buffer.get(i));
			numPacketsResend++;
		}
	}

	// This routine will be called once, before any of your other A-side
	// routines are called. It can be used to do any required
	// initialization (e.g. of member variables you add to control the state
	// of entity A).
	protected void aInit() {
		System.out.println("Sender: Setting base to 0");
		this.base = 0;
		System.out.println("Sender: Setting buffer size to 50");
		// this.buffer = new Packet[50];
		this.buffer = new ArrayList<Packet>();
		System.out.println("Sender: Setting window size to 8");
		this.windowSize = 8;
		System.out.println("Sender: Next sequence number is 0");
		this.nextSeq = 0;
		System.out.println("Sender: Timer interval has been set to 200 ms");
		this.timeIncrement = 200;
	}

	// This routine will be called whenever a packet sent from the B-side
	// (i.e. as a result of a toLayer3() being done by an A-side procedure)
	// arrives at the B-side. "packet" is the (possibly corrupted) packet
	// sent from the A-side.
	protected void bInput(Packet packet) {
		System.out.println(
				"Sender: Incoming packet has sequence " + packet.getSeqnum() + " and ACK " + packet.getAcknum());

		if (corrupt(packet, B) || packet.getSeqnum() != this.seq_B) {
			System.out.println(
					"SIDE B: Packet " + packet.getSeqnum() + " is corrupt or duplicate. Sending duplicate ACK.");
			if (corrupt(packet, B)) {
				corruptPac++;
			}
			//
			// if (currentPacketB == null)
			// currentPacketB = makePacket(new Message(" "), B, A, 0,
			// expectedSeqNum);
		} else {
			System.out.println(
					"SIDE B: Packet " + packet.getSeqnum() + " is valid. Delivering to layer 5 and sending ACK.");
			toLayer5(B, packet.getPayload());
			int checkSum = setMessageCheckSum(0, this.seq_B, B, " ");
			this.pac_B = new Packet(0, this.seq_B, checkSum, " ");
			this.seq_B++;
		}

		toLayer3(B, this.pac_B);
		numAcks++;

	}

	// This routine will be called once, before any of your other B-side
	// routines are called. It can be used to do any required
	// initialization (e.g. of member variables you add to control the state
	// of entity B).
	protected void bInit() {
		this.seq_B = 0;
		this.pac_B = null;
	}

	protected int setMessageCheckSum(int seqNum, int ack, int isSender, String payload) {
		int checkSum = ack + seqNum;
		// if (isSender == A) {
		for (char c : payload.toCharArray()) {// Set every character to numeric
												// value
			checkSum += Character.getNumericValue(c);
		}
		// }
		return checkSum;
	}

	protected boolean corrupt(Packet p, int receiver) {
		int toCompare = p.getAcknum() + p.getSeqnum();
		int checksum = p.getChecksum();
		// if (receiver == B) {
		for (char c : p.getPayload().toCharArray()) {
			toCompare += Character.getNumericValue(c);
		}
		// }
		return checksum != toCompare;

	}

}
