//package PA2;

import java.util.*;

public class StudentNetworkSimulator extends NetworkSimulator {
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

	protected Queue<Packet> outputQueue;
	protected boolean timing;
	protected int seq_A;
	protected int seq_B;
	protected double timeIncrement;
	protected Packet packetB;
	protected int totalPacket = 0;
	protected int totalAcks = 0;
	protected int numOfCorruptPacket = 0;
	// Add any necessary class variables here. Remember, you cannot use
	// these variables to send messages error free! They can only hold
	// state information for A or B.
	// Also add any necessary methods (e.g. checksum of a String)

	// This is the constructor. Don't touch!
	public StudentNetworkSimulator(int numMessages, double loss, double corrupt, double avgDelay, int trace,
			long seed) {
		super(numMessages, loss, corrupt, avgDelay, trace, seed);
	}

	//Override for final statistics
	@Override
	public void runSimulator() {
		super.runSimulator();
		System.out.println("\n\nNumber of original data packets transmitted:\t" + this.totalPacket);
		System.out.println("Number of ACK packets:\t\t\t\t" + this.totalAcks);
		System.out.println("Number of corrupt packets\t\t\t" + this.numOfCorruptPacket);
	}

	protected int setMessageCheckSum(int seqNum, int ack, int isSender, String payload) {
		int checkSum = ack + seqNum;
		// if (isSender == A) {
		for (char c : payload.toCharArray()) {//Set every character to numeric value
			checkSum += Character.getNumericValue(c);
		}
		// }
		return checkSum;
	}

	// This routine will be called whenever the upper layer at the sender [A]
	// has a message to send. It is the job of your protocol to insure that
	// the data in such a message is delivered in-order, and correctly, to
	// the receiving upper layer.
	protected void aOutput(Message message) {
		System.out.println("Sender: Reveived message from process layer: " + message.getData());
		if (this.timing) {//If data is sending, then return
			System.out.println("The timer is timing, some messages are sending");
			// stopTimer(A);
			// this.timing = false;
			return;
		}

		String mes = message.getData();
		int seqA, ack;
		ack = seqA = this.seq_A;
		int checkSum = setMessageCheckSum(seqA, ack, A, mes);
		Packet p = new Packet(seqA, ack, checkSum, mes);
		this.outputQueue.offer(p);

		toLayer3(A, this.outputQueue.peek());
		this.timing = true;
		startTimer(A, this.timeIncrement);
		this.totalPacket++;

	}
	
	/**To check if the packet is corrupt*/
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

	// This routine will be called whenever a packet sent from the B-side
	// (i.e. as a result of a toLayer3() being done by a B-side procedure)
	// arrives at the A-side. "packet" is the (possibly corrupted) packet
	// sent from the B-side.
	protected void aInput(Packet packet) {
		// this.timing = false;

		int ack = packet.getAcknum();
		System.out.println("Sender: The packet with the sequence number " + this.seq_A
				+ " is sended and incoming packet has sequence " + packet.getSeqnum() + " and ACK "
				+ packet.getAcknum());

		boolean isCorrupt = corrupt(packet, A); // Check ACK only
		if (isCorrupt)
			this.numOfCorruptPacket++;

		else if (this.seq_A == ack && !isCorrupt) {
			stopTimer(A);
			this.outputQueue.poll();
			this.seq_A = this.seq_A == 0 ? 1 : 0;
			this.timing = false;
			toLayer5(A, packet.getPayload());
		}
		// else {//
		// {
		// toLayer3(A, this.outputQueue.peek());
		// this.timing = true;
		// startTimer(A, this.timeIncrement);
		//
		// System.out.println("Sender: Packet lost, waiting for timeout");
		// }
	}

	// This routine will be called when A's timer expires (thus generating a
	// timer interrupt). You'll probably want to use this routine to control
	// the retransmission of packets. See startTimer() and stopTimer(), above,
	// for how the timer is started and stopped.
	protected void aTimerInterrupt() {
		// this.timing = false;
		// stopTimer(A);
		// if (this.outputQueue.size() > 1) {
		System.out.println("Sender: Timer interruptted, packet resended");
		toLayer3(A, this.outputQueue.peek());
		// this.timing = true;
		startTimer(A, this.timeIncrement);
		// }
	}

	// This routine will be called once, before any of your other A-side
	// routines are called. It can be used to do any required
	// initialization (e.g. of member variables you add to control the state
	// of entity A).
	protected void aInit() {
		this.outputQueue = new LinkedList<Packet>();
		this.timing = false;
		this.seq_A = 0;
		this.timeIncrement = 200.0;
	}

	// This routine will be called whenever a packet sent from the B-side
	// (i.e. as a result of a toLayer3() being done by an A-side procedure)
	// arrives at the B-side. "packet" is the (possibly corrupted) packet
	// sent from the A-side.
	protected void bInput(Packet packet) {

		System.out.println("Receiver: Packet sent from sender has been received with payload " + packet.getPayload());

		if (corrupt(packet, B) || (packet.getSeqnum() != this.seq_B)) {
			// packet.setPayload("");
			// Packet p = new Packet(packet);

			System.out.println("Receiver: Packet is corrput or duplicate");
			if (corrupt(packet, B))
				this.numOfCorruptPacket++;
			if (this.packetB == null) {
				int seqNum = packet.getSeqnum() == 0 ? 1 : 0;
				int checksum = setMessageCheckSum(0, seqNum, B, " ");
				this.packetB = new Packet(0, seqNum, checksum, " ");
			}
		} else {

			System.out.println("Receiver: The received packet is not incorrupt, the payload has bees sent to layer 5");
			toLayer5(B, packet.getPayload());

			this.seq_B = packet.getSeqnum() == 0 ? 1 : 0;
			int chksm = setMessageCheckSum(0, packet.getSeqnum(), B, " ");
			// toLayer3(B, new Packet(this.seq_B, this.seq_B, chksm, ""));
			// if(this.seq_B == 0)
			// {
			// this.seq_B = 1;
			// }
			// else
			// {
			// this.seq_B = 0;
			// }
			// if (this.packetB == null)
			this.packetB = new Packet(0, packet.getSeqnum(), chksm, " ");

		}
		toLayer3(B, this.packetB);
		this.totalAcks++;
	}

	// This routine will be called once, before any of your other B-side
	// routines are called. It can be used to do any required
	// initialization (e.g. of member variables you add to control the state
	// of entity B).
	protected void bInit() {
		this.packetB = null;
		this.seq_B = 0;
	}
}
