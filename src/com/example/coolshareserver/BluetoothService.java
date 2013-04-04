package com.example.coolshareserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.UUID;

import android.R.integer;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore.Files;
import android.util.Log;

public class BluetoothService extends Service {
	
    // Debugging
    private static final String TAG = "CoolShareService";
    private static final boolean D = true;
	private BluetoothAdapter mBluetoothAdapter = null;
	
	private AcceptThread mAcceptThread = null;
    private ConnectedThread mConnectedThread = null;
    private final Handler mHandler = null;

	// Unique UUID for this application
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
 
    // Current connection state
    private int mState;
    
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    // Cool-SHARE Protocol constants
    public static final int BTSTORE_CMD_BYTES = 8;
    public static final int BTSTORE_DATALENGTH_BYTES = 4;
    public static final int BTSTORE_VARLENGTH_BYTES = 2;
    public static final String BTSTORE_CMD_REQUEST_INFO = "req_info";
    public static final String BTSTORE_CMD_REQUEST_DETAILS = "req_detl";
    public static final String BTSTORE_CMD_REQUEST_APK = "req_apk_";
    public static final String BTSTORE_CMD_RESPONSE_INFO = "res_info";
    public static final String BTSTORE_CMD_RESPONSE_DETAILS = "res_detl";
    public static final String BTSTORE_CMD_RESPONSE_APK = "res_apk_";
    
    @Override
    public void onCreate() {
        if (D) Log.d(TAG, "onCreate");

    	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to a call to startService()
        if (D) Log.d(TAG, "onStartCommand");

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            if (D) Log.d(TAG, "Bluetooth not available");
            //throw new Exception("Bluetooth not available");
            return START_NOT_STICKY;
        }
        
        // Make device discoverable/inquiry scan
        if(mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
        	Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        	discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        	discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        	startActivity(discoverableIntent);
        }
        
    	if(mAcceptThread == null) {
    		mAcceptThread = new AcceptThread();
    		mAcceptThread.start();
    	}
    	
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed
        if (D) Log.d(TAG, "onDestroy");
        
    	// Cancel any threads currently running
    	if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
    	if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
    }
    
	@Override
	public IBinder onBind(Intent intent) {
		// Binding not supported so return null
		return null;
	}
    
    /**
     * This thread runs while listening for incoming connections.
     * It runs until a connection is accepted.
     */
	private class AcceptThread extends Thread {
	    private final BluetoothServerSocket mmServerSocket;
	 
	    public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("CoolShare Server", MY_UUID);
            	//tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("CoolShare Server", MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "listen() failed", e);
            }
            mmServerSocket = tmp;
	    }
	 
	    public void run() {
            if (D) Log.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");
            BluetoothSocket socket = null;
            
	        // Keep listening until exception occurs or a socket is returned
	        while (mmServerSocket != null) {
                if (D) Log.d(TAG, "Listening for connection");

	            try {
	                socket = mmServerSocket.accept();
	            } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
	                break;
	            }
	            // If a connection was accepted
	            if (socket != null) {
	                if (D) Log.d(TAG, "connection accepted from: " + socket.getRemoteDevice().getName());
	                // Start the thread to manage the connection and perform transmissions
	                mConnectedThread = new ConnectedThread(socket);
	                mConnectedThread.start();
					cancel();
	                break;
	            }
	        }
            if (D) Log.i(TAG, "END mAcceptThread");
	    }
	 
	    /** Will cancel the listening socket, and cause the thread to finish */
	    public void cancel() {
	        try {
	        	if(mmServerSocket != null) {
	        		mmServerSocket.close();
	        	}
	        } catch (IOException e) { 
                Log.e(TAG, "unable to close() socket during cancel method", e);
	        }
	    }
	}
	
    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     * It restarts the acceptThread when connection is dropped
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BufferedInputStream mmInStream;
        private final BufferedOutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = new BufferedInputStream(tmpIn);
            mmOutStream = new BufferedOutputStream(tmpOut);
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    buffer = readBytesFromInputStream(mmInStream, BTSTORE_CMD_BYTES);
                    String cmd = new String(buffer);
                    
                    if(D) Log.e(TAG, "Received cmd: " + cmd);

                    // Act on command
                    handleCommand(cmd);
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    break;
                }
            }
            // Restart listening
            if(mAcceptThread != null) {
            	mAcceptThread.start();
            }
        }

		private void handleCommand(String cmd) throws FileNotFoundException, IOException {
			if(cmd.equals(BTSTORE_CMD_REQUEST_INFO)) {
				String sdcard = Environment.getExternalStorageDirectory().getPath();
				String filePath = sdcard + "/cool-SHARE-server/info.xml";
				sendFile(filePath, BTSTORE_CMD_RESPONSE_INFO);
			} else if(cmd.equals(BTSTORE_CMD_REQUEST_DETAILS)) {
				String sdcard = Environment.getExternalStorageDirectory().getPath();
				String filePath = sdcard + "/cool-SHARE-server/details.xml";
				sendFile(filePath, BTSTORE_CMD_RESPONSE_DETAILS);				
			} else if(cmd.startsWith(BTSTORE_CMD_REQUEST_APK)) {
				// Get APK name length
				byte[] nameLengthBuffer = readBytesFromInputStream(mmInStream, BTSTORE_VARLENGTH_BYTES);
				int nameLength = ByteBuffer.wrap(nameLengthBuffer).getShort();
				
				// Get APK name
				byte[] nameBuffer = readBytesFromInputStream(mmInStream, nameLength);
				String name = new String(nameBuffer);
				
				// Send the requested APK
				String sdcard = Environment.getExternalStorageDirectory().getPath();
				String filePath = sdcard + "/cool-SHARE-server/" + name;
				sendFile(filePath, BTSTORE_CMD_RESPONSE_APK);
			} else {
				if(D) Log.e(TAG, "Unknown command received: " + cmd);
				
			}
		}

		private void sendFile(String path, String responseType) throws FileNotFoundException, IOException {
			byte[] fileBuffer = new byte[1024];
			File file = new File(path);
			if(file.length() > Integer.MAX_VALUE ) {
				throw new IOException("File is too big (" + Long.toString(file.length()) + "");
			}
			
			// Write reply command to stream
			mmOutStream.write(responseType.getBytes());
			
			// Write file size to stream
			ByteBuffer lengthBuffer = ByteBuffer.allocate(BTSTORE_DATALENGTH_BYTES).putInt((int)file.length());
			mmOutStream.write(lengthBuffer.array());
			
			// Write file to stream
			InputStream in = null;
			try {
				int read = 0;
				in = new BufferedInputStream(new FileInputStream(file));
				
				while( (read = in.read(fileBuffer)) != -1) {
					mmOutStream.write(fileBuffer, 0, read);
				}
				mmOutStream.flush();
			} finally {
				if (in != null) {
					in.close();
				}
			}
		}

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                //mHandler.obtainMessage(BluetoothService.MESSAGE_WRITE, -1, -1, buffer)
                //        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * Reads a specific number of bytes from stream, no more no less. Otherwise exception is thrown
     * 
     * @param stream
     * @param toRead
     * @return
     * @throws IOException
     */
    private static byte[] readBytesFromInputStream(InputStream stream, int toRead) throws IOException {
    	int read = 0;
    	int offset = 0;
    	byte[] buffer = new byte[toRead];

    	while(toRead > 0 && (read = stream.read(buffer, offset, toRead)) > 0) {
    		toRead -= read;
    		offset += read;
    	}
    	
    	if(toRead > 0) throw new IOException("Could not read specified number of bytes from stream.");

    	return buffer;
    }
}
