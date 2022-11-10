/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;

/**
 *
 * @author 236197
 */
public class TFTPTCPServer {

    private byte[] sendData = new byte[516];
    private byte[] receiveData = new byte[516];

    private final byte RRQ = 1;
    private final byte WRQ = 2;
    private final byte DATA = 3;
    private final byte ERROR = 5;

    String fileName = "";
    ServerSocket serverSocket;
    Socket connectionSocket;

    public static void main(String[] args) throws IOException {
        TFTPTCPServer start = new TFTPTCPServer();
        start.receive();

    }

    /**
     * method to receive and process incoming data,
     *it will also print/write the data 
     * @throws java.io.IOException
     */
    private void receive() throws IOException {
        serverSocket = new ServerSocket(9002);
        connectionSocket = serverSocket.accept();

        while (true) {
            InputStream in = connectionSocket.getInputStream();
            DataInputStream dis = new DataInputStream(in);
            dis.read(receiveData);

            byte[] sortOpCode = {receiveData[0], receiveData[1]};
            if (sortOpCode[1] == RRQ) {
                System.out.println("RRQ received");
                String text = new String(receiveData);
                getFileName(text);
                sendData();

            } else if (sortOpCode[1] == WRQ) {
                System.out.println("WRQ received");
                String text = new String(receiveData);
                getFileName(text);

            } else if (sortOpCode[1] == DATA) {
                System.out.println("DATA received");
                String text = new String(receiveData);
                System.out.println("FROM client: " + text);

                try {
                    FileOutputStream fos = new FileOutputStream(fileName);
                    fos.write(receiveData, 4, receiveData.length - 4);
                    fos.close();
                } catch (FileNotFoundException e) {

                }
                break;
            } else if (sortOpCode[1] == ERROR) {
                String errorMSG = new String(receiveData, 4, receiveData.length - 4);
                System.out.println("Error message: " + errorMSG);
            }

            //non-functional at the moment
            // new TFTPTCPServerThread(connectionSocket).start();
        }
    }

    /**
     *
     * method to get file name
     *
     */
    private void getFileName(String text) {
        System.out.println("Getting file name.");
        receiveData[1] = 0;
        String txt = ".txt";
        String file = "";
        fileName = text.substring(2, text.indexOf(txt) + 4);
        System.out.println("File name: " + fileName);

    }

    /**
     * method to send data packets
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

            OutputStream os = connectionSocket.getOutputStream();
            DataOutputStream dos = new DataOutputStream(os);
            dos.write(sendData);

            System.out.println("Data sent.");

        } catch (FileNotFoundException e) {
            sendError(e.getMessage());
        }

    }

    /**
     * method to send error packets
     *
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
     * method to check if received data is last 
     * Non-functional, not implemented 
     */
    private boolean checkLast(byte[] receiveData) {
        return receiveData.length < 516;
    }

}
