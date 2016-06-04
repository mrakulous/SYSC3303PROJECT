
// TFTPClient.java
// This class is the client side for a very simple assignment based on TFTP on
// UDP/IP. The client uses one port and sends a read or write request and gets 
// the appropriate response from the server.  No actual file transfer takes place.   

import java.io.*;
import java.net.*;
import java.util.*;

public class TFTPClient {

	private static final int simPort = 23; // Sim port
	public static final int DATA_SIZE = 512; // Max DATA per packet
	public static final int TOTAL_SIZE = DATA_SIZE + 4; // Max DATA including
														// opcode
	private static final int DATA_PACKET = 3; // DATA code
	private static final int ACK_PACKET = 4; // ACK code
	private static final int TIMEOUT = 50000; // 50 second timeout
	private static final int RETRANSMIT_TIME = 2500; // 2.5 second retransmit
	private static final char QUIT = 'q'; // q key to quit at anytime
	private static boolean toPrint; // to print or not

	public static enum Request {
		READ, WRITE, ERROR
	}; // Type of request

	public static enum Mode {
		NORMAL, TEST
	}; // Type of mode

	private DatagramPacket sendPacket, receivePacket; // Packets
	private DatagramSocket sendReceiveSocket; // Sockets
	private Byte ackCntL, ackCntR; // ACK counter

	public TFTPClient() {
		try {
			sendReceiveSocket = new DatagramSocket(); // Socket with simulator
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	public void runClient(Scanner readInput) {
		Scanner re = readInput;
		// String filepath, workingDir; -- To implement
		String filename;
		String mode;
		Request req;
		String checkInput;
		int cmd = 0;
		int cmd2 = 0;

		while (true) {
			System.out.print("[1]Read  [2]Write  [5]Quit  [q]Shutdown : ");
			checkInput = re.nextLine();
			if (checkInput.equals(QUIT)) {
				shutDown();
			} else {
				try {
					cmd = Integer.parseInt(checkInput); // Get the user input
					if (cmd == 1) {
						req = Request.READ;
						setAckCounter(req);
						break;
					} else if (cmd == 2) {
						req = Request.WRITE;
						setAckCounter(req);
						break;
					} else if (cmd == 5) {
						// User wants to shutdown
						System.exit(1);
					}
				} catch (NumberFormatException e) {
				}
				System.out.println("Invalid input.");
			}
		}

		// Choose to have quiet or verbose
		while (true) {
			try {
				System.out.print("[1]: Quiet  [2]: Verbose : ");
				cmd2 = Integer.parseInt(re.nextLine());
				if (cmd2 == 1) {
					toPrint = false;
					break;
				} else {
					toPrint = true;
					break;
				}
			} catch (NumberFormatException e) {
				System.out.println("Please enter a valid option");
			}
		}

		// read in filename
		while (true) {
			try {
				System.out.print("File to " + req.toString() + " (\"q\" to Quit): ");
				filename = re.nextLine();
				if (filename.equals(QUIT)) {
					shutDown();
				}
				// workingDir = System.getProperty("user.dir");
				// filepath = workingDir + "\\" + filename;
				File input = new File(filename);
				Scanner read = new Scanner(input); // Used to verify if the file
													// is valid
				break;
			} catch (FileNotFoundException e) {
				System.out.println("File does not exist.");
			}
		}

		while (true) {
			try {
				System.out.print("[1]netascii  [2]octet  [q]Shutdown : ");
				checkInput = re.nextLine();
				if (checkInput.equals(QUIT))
					shutDown();
				cmd = Integer.parseInt(checkInput);
				if (cmd == 1) {
					mode = "netascii";
					break;
				} else if (cmd == 2) {
					mode = "octet";
					break;
				}
			} catch (NumberFormatException e) {
			}
			System.out.println("Invalid input.");
		}
		run(filename, mode, req);
		System.out.println("*** Transfer Complete ***\n");
	}

	public void run(String filepath, String mode, Request req) {
		byte[] msg = new byte[TOTAL_SIZE]; // Initialize byte array msg to 516
		msg[0] = 0;

		if (req == Request.READ) {
			msg[1] = 1;
		} else if (req == Request.WRITE) {
			msg[1] = 2;
		}

		int index = 2;
		byte[] filename = filepath.getBytes();
		System.arraycopy(filename, 0, msg, index, filename.length);
		index = index + filename.length;
		msg[index] = 0;
		index++;

		byte[] md = mode.getBytes();
		System.arraycopy(md, 0, msg, index, md.length);
		index = index + md.length;
		msg[index] = 0;
		index++;

		// Create the send packet with request info
		try {
			sendPacket = new DatagramPacket(msg, index, InetAddress.getLocalHost(), simPort);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		}

		// Print send packet info
		if (toPrint == true) {
			System.out.println("\nClient: Sending REQ packet to simulator.");
			System.out.println("To host: " + sendPacket.getAddress());
			System.out.println("Destination host port: " + sendPacket.getPort());
			System.out.println("Packet Length: " + sendPacket.getLength());
			System.out.println("Contents(bytes): " + msg);
			String contents = new String(msg, 2, sendPacket.getLength());
			System.out.println("Contents(string): \n" + contents + "\n");
		}
		// Send the request packet
		try {
			sendReceiveSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		// Inform the user that the client is now waiting to receive
		if (toPrint == true) {
			System.out.println("Client: Waiting for packet from simulator............" + "\n");
		}

		if (req == Request.READ) {
			read(filepath);
		} else if (req == Request.WRITE) {
			write(filepath);
		}
	}

	public void read(String filepath) {
		try {
			// Create the buffered output stream and the output file
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("new" + filepath));
			for (;;) {
				int len;
				byte[] msg = new byte[TOTAL_SIZE];
				byte[] data = new byte[DATA_SIZE];

				receivePacket = new DatagramPacket(msg, msg.length);

				try {
					/*
					 * Network Error Handling ACK retransmission is not needed
					 * as duplicate ack packets are disabled Only have check for
					 * initial timeout
					 */
					for (;;) {
						sendReceiveSocket.setSoTimeout(TIMEOUT); // Set the
																	// timeout
																	// for the
																	// sendReceiveSocket
						sendReceiveSocket.receive(receivePacket); // Break out
																	// of the
																	// forever
																	// loop
																	// after a
																	// packet is
																	// received
						break;
					}
				} catch (SocketTimeoutException e) {
					System.out.println("Shutting down.");
					System.exit(1);
				}

				// Get block number from received packet to compare
				Byte leftByte = new Byte(receivePacket.getData()[2]);
				Byte rightByte = new Byte(receivePacket.getData()[3]);

				len = receivePacket.getLength();

				printDataPacket(receivePacket, msg, leftByte, rightByte); // Print
																			// receive
																			// packet
																			// info

				// Transfer only the DATA into the byte array, skip the opcode
				System.arraycopy(receivePacket.getData(), 4, data, 0, receivePacket.getLength() - 4);

				if (!(ackCntL == 127 && ackCntR == 127)) {
					if (ackCntR == 127) {
						// If the data packet is correct (1 block number higher)
						if (leftByte == ackCntL.byteValue() + 1 && rightByte == 0) {
							out.write(data, 0, len - 4);
						}
					} else {
						if (leftByte == ackCntL.byteValue() && rightByte == ackCntR.byteValue() + 1) {
							out.write(data, 0, len - 4);
						}
					}
				} else {
					// The size limit for this TFTP application has been reached
					System.out.println("Memory limit reached. Aborting...");
					System.exit(1);
				}

				// Initialize opcode
				byte[] ack = new byte[4];
				ack[0] = 0;
				ack[1] = ACK_PACKET;
				ack[2] = leftByte;
				ack[3] = rightByte;

				// Create the ACK packet to send
				sendPacket = new DatagramPacket(ack, ack.length, InetAddress.getLocalHost(), simPort);

				// Print the ACK packet info
				printAckPacket(sendPacket, leftByte, rightByte);

				// Send the ACK packet
				try {
					sendReceiveSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}

				// Inform the user that the client is now waiting to receive
				if (toPrint == true) {
					System.out.println("Client: Waiting for packet from simulator............" + "\n");
				}
				if (len < DATA_SIZE) {
					// This is the last data packet
					out.close();
					System.out.println("#####  OPERATION COMPLETED.  #####" + "\n");
					/*
					 * IMPLEMENT RE-PROMPT FOR NEW FILE TRANSFER
					 */
					break;
				}
				incReadAckCounter(leftByte, rightByte);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void write(String filepath) {
		int len = 0;
		int dataCheck = 0;
		Byte leftByte = null;
		Byte rightByte = null;

		try {
			// Create the buffered input stream to read the file
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(filepath));
			do {
				byte[] msg = new byte[TOTAL_SIZE]; // msg has size 516
				byte[] data = new byte[DATA_SIZE]; // data has size 512
				int i = 0; // DATA delay timer

				receivePacket = new DatagramPacket(msg, msg.length); // Create
																		// the
																		// receive
																		// packet
				try {
					// Network Error Handling
					for (;;) {
						// ****ERROR HANDLING: DATA LOSS****
						while (i <= 5) {
							try {
								sendReceiveSocket.setSoTimeout(RETRANSMIT_TIME); // Set
																					// the
																					// sendReceiveSocket
																					// timeout
								sendReceiveSocket.receive(receivePacket); // Break
																			// out
																			// of
																			// the
																			// forever
																			// loop
																			// after
																			// a
																			// packet
																			// is
																			// received
								break;
							} catch (SocketTimeoutException e) {
								System.out.println("try" + i); // DEBUGGING
																// PURPOSES
								if (i == 5) {
									System.out.println("end"); // DEBUGGING
																// PURPOSES
									System.exit(1);
								}
								// Send the packet to the simulator
								sendReceiveSocket.send(sendPacket);
								i++;
							}
						}

						// ****ERROR HANDLING: DUPLICATE ACK****

						// Get block number from received packet to compare
						leftByte = new Byte(receivePacket.getData()[2]);
						rightByte = new Byte(receivePacket.getData()[3]);

						// If the incoming ACK packet is correct, increment and
						// break
						if (leftByte.compareTo(getAckCntL()) == 0 && rightByte.compareTo(getAckCntR()) == 0) {
							// Increment ACK counter if correct block number
							// received
							incWriteAckCounter(leftByte, rightByte);
							break;
						} else {
							// If is a duplicate ACK packet, ignore and wait
							if (toPrint == true) {
								System.out.println("\n***** DUPLICATE ACK RECEIVED - IGNORING PACKET *****\n");
							}

						}
					} // End forever loop
				} catch (IOException e) {
					if (toPrint == true) {
						System.out.println("No data received: Data lost.");
						System.out.println("Shutting down.");
						System.exit(1);
					}
				}

				// Print the receive packet info
				String contents;

				if(toPrint == true)
				{
				System.out.println("Client: Packet received from simulator.");
				System.out.println("From host: " + receivePacket.getAddress());
				System.out.println("Host port: " + receivePacket.getPort());
				}
				int packetLength = receivePacket.getLength();
				if(toPrint == true)
				{
				System.out.println("Packet Length: " + packetLength);
				System.out.println("Block Number: " + leftByte.toString() + rightByte.toString());
				System.out.println("Contents(bytes): " + msg);
				}
				if (packetLength > 4) {
					// It is not an ACK packet
					contents = new String(msg, 4, DATA_SIZE);
					if(toPrint == true)
					{
					System.out.println("Contents(string): \n" + contents + "\n");
					}
				} else {
					// It is an ACK packet
					if(toPrint == true)
					{
					System.out.println("Contents(string): \n" + "########## ACKPacket ##########\n");
					}
				}

				// Read the file and put its contents into byte array data
				dataCheck = in.read(data);

				// Initialize opcode
				msg[0] = 0;
				msg[1] = DATA_PACKET;
				msg[2] = getAckCntL();
				msg[3] = getAckCntR();

				if (dataCheck != -1) {
					System.arraycopy(data, 0, msg, 4, dataCheck);
				}

				// Create the send packet
				sendPacket = new DatagramPacket(msg, dataCheck + 4, InetAddress.getLocalHost(), simPort);

				packetLength = sendPacket.getLength();

				// Print out the send packet info
				if(toPrint == true)
				{
				System.out.println("Client: Sending packet to simulator.");
				System.out.println("To host: " + sendPacket.getAddress());
				System.out.println("Destination host port: " + sendPacket.getPort());
				}
				packetLength = sendPacket.getLength();
				if(toPrint == true)
				{
				System.out.println("Packet Length: " + packetLength);
				System.out.println("Block Number: " + getAckCntL().toString() + getAckCntR().toString());
				System.out.println("Contents(bytes): " + msg);
				}
				if (packetLength > 4) {
					// It is not an ACK packet
					contents = new String(msg, 4, DATA_SIZE);
					if(toPrint == true)
					{
					System.out.println("Contents(string): \n" + contents + "\n");
					}
				} else {
					// It is an ACK packet
					if(toPrint == true)
					{
					System.out.println("Contents(string): \n" + "########## ACKPacket ##########\n");
					}
				}

				// Send the DATA packet
				try {
					sendReceiveSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}

				if (dataCheck == -1) {
					break;
				}

			} while (dataCheck == DATA_SIZE);
			if (dataCheck < DATA_SIZE) {
				in.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void shutDown() {
		if(toPrint == true)
		{
		System.out.println("Shutting Down...");
		System.out.println("Done.");
		}
		System.exit(1);
	}

	private void setAckCounter(Request req) {
		// Set the ACK counter every new operation
		if (req == Request.READ) {
			ackCntL = 0;
			ackCntR = 0;
		} else if (req == Request.WRITE) {
			ackCntL = 0;
			ackCntR = 0;
		}
	}

	private void incReadAckCounter(Byte left, Byte right) {
		// Increment the ACK counter for READ
		if (!(ackCntL == 127 && ackCntR == 127)) {
			if (ackCntR == 127) {
				if (left == ackCntL.byteValue() + 1 && right == 0) {
					// The data packet is correct (1 block number higher)
					ackCntL++;
					ackCntR = 0;
				}
			} else {
				if (left == ackCntL.byteValue() && right == ackCntR.byteValue() + 1) {
					// It is the correct block, so increment
					ackCntR++;
				}
			}
		} else {
			if(toPrint == true)
			{
			System.out.println("Memory limit reached. Aborting...");
			}
			System.exit(1);
		}
	}

	private void incWriteAckCounter(Byte left, Byte right) {
		// Increment the ACK counter for WRITE
		if (!(ackCntL == 127 && ackCntR == 127)) {
			if (ackCntL.compareTo(left) == 0 && ackCntR.compareTo(right) == 0) {
				// It is the correct block, so increment
				if (ackCntR == 127) {
					ackCntL++;
					ackCntR = 0;
				} else {
					ackCntR++;
				}
			}
		} else {
			System.out.println("Memory limit reached. Aborting...");
			System.exit(1);
		}
	}

	private void printDataPacket(DatagramPacket packet, byte[] msg, Byte left, Byte right) {
		if(toPrint == true)
		{
		System.out.println("Client: DATA Packet received from simulator.");
		System.out.println("From host: " + packet.getAddress());
		System.out.println("Host port: " + packet.getPort());
		System.out.println("Packet Length: " + packet.getLength());
		System.out.println("Block Number: " + left.toString() + right.toString());
		System.out.println("Contents(bytes): " + msg);
		String contents = new String(msg, 4, packet.getLength() - 4);
		System.out.println("Contents(string): \n" + contents + "\n");
		}
	}

	private void printAckPacket(DatagramPacket packet, Byte left, Byte right) {
		if(toPrint == true)
		{
		System.out.println("Client: Sending ACK packet to simulator.");
		System.out.println("To host: " + packet.getAddress());
		System.out.println("Destination host port: " + packet.getPort());
		System.out.println("Packet Length: " + packet.getData().length);
		System.out.println("Block Number: " + left.toString() + right.toString());
		System.out.println("Contents(bytes): " + packet.getData());
		System.out.println("Contents(string): \n" + "########## ACKPacket ##########\n");
		}
	}

	private Byte getAckCntL() {
		return ackCntL; // Returns left block
	}

	private Byte getAckCntR() {
		return ackCntR; // Returns right block
	}

	public static void main(String args[]) {
		TFTPClient client = new TFTPClient();
		Scanner re = new Scanner(System.in);
		for (;;) {
			client.runClient(re);
		}
	}
}
