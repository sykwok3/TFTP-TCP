/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

/**
 *
 * @author 236197
 */
public class TFTPTCPClient {

    private byte[] sendData = new byte[516];
    private byte[] receiveData = new byte[516];

    Socket clientSocket;

    private final byte RRQ = 1;
    private final byte WRQ = 2;
    private final byte DATA = 3;
    private final byte ERROR = 5;

    private String mode = "octet";
    private String fileName;

    public static void main(String[] args) throws IOException {
        TFTPTCPClient start = new TFTPTCPClient();
        start.menu();

    }

    /**
     * menu method
     *
     * @throws java.io.IOException
     */
    private void menu() throws IOException {
        //create client socket, connect to server
        clientSocket = new Socket("localhost", 9002);

        System.out.println("Please input file name");
        Scanner s1 = new Scanner(System.in);
        fileName = s1.nextLine();

        System.out.println("please select one of the options below");
        System.out.println("1. Read File");
        System.out.println("2. Write File");
        System.out.println("0. Exit");
        System.out.println("");

        Scanner s = new Scanner(System.in);
        String input = s.nextLine();

        switch (input) {
            case "1":
                sendRRQ();
                break;
            case "2":
                sendWRQ();
                break;
            case "0":
                System.out.println("Goodbye");
                clientSocket.close();
                break;
            default:
                System.out.println("Command not recognised");
                break;
        }
    }

    /**
     * method to process read and write requests
     * @param opcode
     */
    private byte[] request(byte opcode) throws IOException {
        int requestArrayLength = 2 + fileName.getBytes().length + 1 + mode.getBytes().length + 1;
        ByteArrayOutputStream os = new ByteArrayOutputStream(requestArrayLength);
        os.write(0);
        os.write(opcode);
        os.write(fileName.getBytes());
        os.write(0);
        os.write(mode.getBytes());
        os.write(0);
        byte[] requestArray = os.toByteArray();
        return requestArray;
    }

    /**
     * method to send read request
     *
     * @throws java.io.IOException
     */
    private void sendRRQ() throws IOException {
        sendData = request(RRQ);
        OutputStream os = clientSocket.getOutputStream();
        DataOutputStream dos = new DataOutputStream(os);
        dos.write(sendData);
        System.out.println("RRQ sent");

        InputStream in = clientSocket.getInputStream();
        DataInputStream dis = new DataInputStream(in);
        dis.read(receiveData);
        System.out.println("Data received");

        String text = new String(receiveData);
        System.out.println("From server: " + text);

        try {
            FileOutputStream fos = new FileOutputStream(fileName);
            fos.write(receiveData, 4, receiveData.length - 4);
            fos.close();
        } catch (FileNotFoundException e) {
            sendError(e.getMessage());

        }
        clientSocket.close();

    }

    /**
     * method to send WRQ, then data
     *
     * @throws java.io.IOException
     */
    private void sendWRQ() throws IOException {
        sendData = request(WRQ);
        OutputStream os = clientSocket.getOutputStream();
        DataOutputStream dos = new DataOutputStream(os);
        dos.write(sendData);
        System.out.println("WRQ sent");

        sendData();
        clientSocket.close();
    }

    /**
     * method to send data
     *
     * @throws java.io.IOException
     */
    private void sendData() throws IOException {
        receiveData[1] = 0;
        System.out.println("File to be sent: " + fileName);

        try {
            BufferedReader fileInput = new BufferedReader(new FileReader(fileName));
            System.out.println("File found.");
            byte fileData[] = new byte[516];
            String nextLine = fileInput.readLine();
            String store = "";
            while (nextLine != null) {
                store = store + nextLine;
                nextLine = fileInput.readLine();
            }
            fileData = store.getBytes();
            byte[] blockNum = {0, 1};
            byte[] dataMSG = {0, DATA, blockNum[0], blockNum[1]};

            System.arraycopy(dataMSG, 0, sendData, 0, dataMSG.length);
            System.arraycopy(fileData, 0, sendData, dataMSG.length, fileData.length);

            OutputStream os = clientSocket.getOutputStream();
            DataOutputStream dos = new DataOutputStream(os);
            dos.write(sendData);

            System.out.println("Data sent.");

        } catch (FileNotFoundException e) {
            System.err.println("File is not found.");
            sendError(e.getMessage());
        }

    }

    /**
     * method to send error message/packet
     *
     * @param e
     * @throws java.io.IOException
     */
    private void sendError(String e) throws IOException {

        int errorArrayLength = 2 + 2 + e.length() + 1;
        ByteArrayOutputStream ebaos = new ByteArrayOutputStream(errorArrayLength);
        ebaos.write(0);
        ebaos.write(ERROR);
        ebaos.write(0);
        ebaos.write(1);
        ebaos.write(e.getBytes());
        ebaos.write(0);
        byte[] errorArr = ebaos.toByteArray();
        System.out.println("Error msg sent.");

    }

    /**
     *  Non-functional method 
     *
     */
    private boolean checkLast(byte[] receiveData) {
        return receiveData.length < 516;
    }
}
